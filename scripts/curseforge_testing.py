#!/usr/bin/env python3
"""Manage CurseForge testing profiles for Dissolver Enhanced."""

import argparse
import contextlib
import copy
import concurrent.futures
import datetime
import difflib
import errno
import hashlib
import io
import json
import os
import platform
import queue
import re
import signal
import shutil
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
import warnings
from pathlib import Path


warnings.filterwarnings(
    "ignore",
    message=r".*pin_memory.*not supported on MPS.*",
    category=UserWarning,
)

CONFIG_FILE = Path(__file__).with_name("config.data")
CACHE_FILE = Path(__file__).with_name("cache.data")
RELATIVE_LOCATION_FILE = Path(__file__).with_name("relative_location.data")
TEST_RESULTS_FILE = Path(__file__).with_name("tests.txt")
TEST_LOG_DIR = Path(__file__).with_name("logs")
TEST_RESULTS_LOCK = threading.Lock()
CONFIG_TEMPLATE = """# Dissolver Enhanced CurseForge testing profile manager
curseforge_instances_path: ""
curseforge_groups_path: ""
curseforge_game_instances_path: ""
"""

TEST_CONFIG_TEXT = """emc_on_hud=false # Display current EMC on HUD (top left corner) [default: false]
private_emc=false # Should each player have their own EMC storage? [default: false]
creative_items=false # Should creative items have EMC? [default: false]
difficulty=hard # easy | normal | hard - Changes crafting recipe for Dissolver block. [default: hard]
mode=default # default | skyblock - Changes some EMC values. [default: default]
# This only tracks things that are needed to keep the mod going. As you are able to turn these off, we ask that you don't, as it gives us the motivation to keep going :)
analytics_enabled=true # Send anonymous analytics and error reports. [default: true]

analytics_tester=true
"""

METADATA_FILENAMES = (
    "minecraftinstance.json",
    "instance.json",
    "manifest.json",
    "profile.json",
    "instance.cfg",
    "instance.properties",
    "minecraftinstance.properties",
)
PRESERVED_DISSOLVER_CONFIG_FILES = {"analytics-instance-id.txt"}

DE_NAME_RE = re.compile(r"^DE\s*[\(\[]\s*([^\)\]]+?)\s*[\)\]]\s*$", re.IGNORECASE)
BUILD_BRANCH_RE = re.compile(
    r"^(?:minecraft[-_/])?(fabric|forge|neoforge|quilt)[-_/](\d+(?:\.\d+)*(?:\.x)?)(?:[-_/].+)?$",
    re.IGNORECASE,
)
USE_EASYOCR = True
TARGET_VERSION_RANGES = {
    "1.20.x": ("1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6"),
    "26.1.x": ("26.1", "26.1.1", "26.1.2"),
}

LOADER_DEPENDENCIES = {
    "fabric": [
        {
            "name": "Fabric API",
            "project": "fabric-api",
            "loader": "fabric",
            "filename_prefixes": ("fabric-api-",),
        }
    ],
    "quilt": [
        {
            "name": "QFAPI/QSL",
            "project": "qsl",
            "loader": "quilt",
            "filename_prefixes": ("qsl-", "quilted-fabric-api-"),
        }
    ],
}


def print_section(title):
    print()
    print(title)
    print("-" * len(title))


def clear_terminal_scrollback():
    if not sys.stdout.isatty():
        return
    # ESC c resets the terminal; CSI 3J clears scrollback in most modern terminals.
    print("\033c\033[3J", end="", flush=True)


def print_step_bar(label, current, total, status, width=24):
    total = max(1, total)
    current = min(total, current)
    filled = int(width * current / total)
    bar = "#" * filled + "." * (width - filled)
    percent = int(100 * current / total)
    print(f"{label} [{bar}] {current}/{total} {percent:3d}% {status}")


def step_bar_text(label, current, total, status, width=24):
    total = max(1, total)
    current = min(total, current)
    filled = int(width * current / total)
    bar = "#" * filled + "." * (width - filled)
    percent = int(100 * current / total)
    return f"{label:<28} [{bar}] {current}/{total} {percent:3d}% {status}"


class BranchProgressDisplay:
    def __init__(self, plans):
        self.plans = list(plans)
        self.state = {plan["index"]: (0, "building") for plan in self.plans}
        self.live = sys.stdout.isatty()
        self.rendered = False
        self.render()

    def render(self):
        if self.live and self.rendered:
            print(f"\033[{len(self.plans)}A", end="")
        for plan in self.plans:
            current, status = self.state[plan["index"]]
            print("\033[K" + step_bar_text(plan["branch"]["name"], current, plan["setup_steps"], status))
        sys.stdout.flush()
        self.rendered = True

    def update(self, plan, current, status):
        self.state[plan["index"]] = (current, status)
        if self.live:
            self.render()
        else:
            print_step_bar(plan["branch"]["name"], current, plan["setup_steps"], status)

    def advance(self, plan, amount, status):
        current, _ = self.state[plan["index"]]
        self.update(plan, current + amount, status)


class SingleProgressDisplay:
    def __init__(self, label, total):
        self.label = label
        self.total = max(1, total)
        self.current = 0
        self.live = sys.stdout.isatty()
        self.rendered = False
        self.render("starting")

    def render(self, status):
        if self.live and self.rendered:
            print("\033[1A", end="")
        print("\033[K" + step_bar_text(self.label, self.current, self.total, status))
        sys.stdout.flush()
        self.rendered = True

    def step(self, status):
        self.current = min(self.total, self.current + 1)
        self.render(status)


@contextlib.contextmanager
def quiet_stdout(enabled=True):
    if enabled:
        with contextlib.redirect_stdout(io.StringIO()):
            yield
    else:
        yield


def progress_step(callback, status, amount=1):
    if callback:
        callback(status, amount)


def reset_test_results_file():
    try:
        TEST_RESULTS_FILE.write_text("", encoding="utf-8")
    except OSError as error:
        print(f"Warning: could not clear {TEST_RESULTS_FILE}: {error}")


def append_test_result(line):
    with TEST_RESULTS_LOCK:
        try:
            with TEST_RESULTS_FILE.open("a", encoding="utf-8") as file:
                file.write(f"{line}\n")
        except OSError as error:
            print(f"Warning: could not append to {TEST_RESULTS_FILE}: {error}")


def safe_log_name(value):
    return re.sub(r"[^A-Za-z0-9_.-]+", "-", str(value)).strip("-") or "instance"


def artifact_sha256(path):
    digest = hashlib.sha256()
    with Path(path).open("rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def branch_cache_key(branch):
    return f"{branch['loader']}-{branch['version']}"


def branch_cache_display(branch):
    return f"{branch_loader_display(branch['loader'])}-{branch['version']}"


def load_test_cache(path=CACHE_FILE):
    path = Path(path)
    if not path.is_file():
        return {}
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        print(f"Warning: could not read test cache {path}: {error}")
        return {}
    return data if isinstance(data, dict) else {}


def save_test_cache(cache, path=CACHE_FILE):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(f"{path.name}.tmp")
    temporary.write_text(json.dumps(cache, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    temporary.replace(path)


def record_cached_pass(cache, plan, jar_hash, test_count, cache_path=CACHE_FILE):
    branch = plan["branch"]
    display = branch_cache_display(branch)
    cache[branch_cache_key(branch)] = {
        "jar_sha256": str(jar_hash),
        "passed_tests": int(test_count),
        "note": f"{display} = passed {int(test_count)} tests",
        "updated": utc_now_text(),
    }
    save_test_cache(cache, cache_path)


def instance_cache_key(instance):
    return str(
        instance.get("de_game_version")
        or instance.get("game_version")
        or instance.get("version")
        or instance.get("folder")
        or "unknown"
    )


def record_cached_instance_pass(
    cache,
    plan,
    instance,
    jar_hash,
    test_count,
    cache_path=CACHE_FILE,
):
    branch = plan["branch"]
    cache_key = branch_cache_key(branch)
    entry = cache.get(cache_key)
    if not isinstance(entry, dict) or entry.get("jar_sha256") != str(jar_hash):
        entry = {"jar_sha256": str(jar_hash), "instances": {}}
        cache[cache_key] = entry
    instances = entry.setdefault("instances", {})
    instances[instance_cache_key(instance)] = {
        "passed_tests": int(test_count),
        "updated": utc_now_text(),
    }
    passed_tests = sum(
        int(item.get("passed_tests", 0))
        for item in instances.values()
        if isinstance(item, dict)
    )
    entry["passed_tests"] = passed_tests
    entry["note"] = (
        f"{branch_cache_display(branch)} = passed {passed_tests} tests "
        f"across {len(instances)} versions"
    )
    entry["updated"] = utc_now_text()
    save_test_cache(cache, cache_path)


def cached_instance_pass(cache, plan, instance, jar_hash, test_count):
    entry = cache.get(branch_cache_key(plan["branch"]))
    if not isinstance(entry, dict) or entry.get("jar_sha256") != str(jar_hash):
        return False
    instances = entry.get("instances")
    if not isinstance(instances, dict):
        return False
    instance_entry = instances.get(instance_cache_key(instance))
    return (
        isinstance(instance_entry, dict)
        and instance_entry.get("passed_tests") == int(test_count)
    )


def cached_pass_result(cache, plan, jar_hash, test_count):
    entry = cache.get(branch_cache_key(plan["branch"]))
    if not isinstance(entry, dict):
        return None
    if entry.get("jar_sha256") != str(jar_hash):
        return None
    if entry.get("passed_tests") != int(test_count):
        return None
    return {
        "branch": plan["branch"],
        "passed": True,
        "tested_instances": len(plan.get("instances", [])),
        "passed_tests": int(test_count),
        "logs": [],
        "cached": True,
        "jar_sha256": str(jar_hash),
    }


def write_test_failure_log(name, content, log_dir=None):
    log_dir = Path(log_dir or TEST_LOG_DIR)
    log_dir.mkdir(parents=True, exist_ok=True)
    path = log_dir / f"{safe_log_name(name)}.log"
    path.write_text(str(content), encoding="utf-8", errors="replace")
    return path


def instance_test_label(instance):
    loader = branch_loader_display(instance.get("de_loader") or "loader").replace(" ", "")
    version = instance.get("de_game_version") or instance.get("game_version") or instance.get("version") or "unknown"
    return f"{loader}-{version}"


def copy_instance_logs_for_failure(instance, label):
    copied = []
    source_logs = Path(instance["path"]) / "logs"
    crash_reports = Path(instance["path"]) / "crash-reports"
    TEST_LOG_DIR.mkdir(parents=True, exist_ok=True)
    safe_label = safe_log_name(label)

    candidates = []
    for name in ("latest.log", "debug.log", "stdout-logs.txt"):
        path = source_logs / name
        if path.exists():
            candidates.append(path)
    if crash_reports.exists():
        candidates.extend(sorted(crash_reports.glob("*"), key=lambda path: path.stat().st_mtime))

    for path in candidates:
        if not path.is_file():
            continue
        target = TEST_LOG_DIR / f"{safe_label}-{safe_log_name(path.name)}.log"
        try:
            shutil.copy2(path, target)
            copied.append(target)
        except OSError as error:
            print(f"Warning: could not copy log {path}: {error}")
    return copied


class ProgressBar:
    def __init__(self, label, total, width=24, enabled=True):
        self.label = label
        self.total = max(1, total)
        self.width = width
        self.enabled = enabled
        self.current = 0
        self.lock = threading.Lock()
        if self.enabled:
            self.render()

    def render(self, detail=None):
        filled = int(self.width * self.current / self.total)
        bar = "#" * filled + "." * (self.width - filled)
        percent = int(100 * self.current / self.total)
        suffix = f" - {detail}" if detail else ""
        print(f"{self.label} [{bar}] {self.current}/{self.total} {percent:3d}%{suffix}")

    def step(self, detail=None):
        with self.lock:
            self.current = min(self.total, self.current + 1)
            if self.enabled:
                self.render(detail)

    def finish(self):
        with self.lock:
            self.current = self.total
            if self.enabled:
                self.render("done")


def normalize_text(value):
    return re.sub(r"[^a-z0-9]+", "", str(value).lower())


def utc_now_text():
    return datetime.datetime.now(datetime.timezone.utc).replace(microsecond=0, tzinfo=None).isoformat() + ".000Z"


def parse_yaml_style_config(path):
    config = {}
    if not path.exists():
        return config
    with path.open("r", encoding="utf-8") as file:
        for line_number, raw_line in enumerate(file, 1):
            line = raw_line.strip()
            if not line or line.startswith("#"):
                continue
            if ":" not in line:
                print(f"Ignoring config line {line_number}: expected key: value")
                continue
            key, value = line.split(":", 1)
            value = value.strip()
            if len(value) >= 2 and value[0] == value[-1] and value[0] in ("'", '"'):
                value = value[1:-1]
            config[key.strip()] = os.path.expandvars(os.path.expanduser(value))
    return config


def load_relative_locations(path=RELATIVE_LOCATION_FILE):
    if not path.exists():
        return {}
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return {}
    return data if isinstance(data, dict) else {}


def save_relative_locations(locations, path=RELATIVE_LOCATION_FILE):
    path.write_text(json.dumps(locations, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def relative_point_from_screen(region, point):
    left, top, width, height = region
    return {
        "x": max(0.0, min(1.0, (point[0] - left) / width)),
        "y": max(0.0, min(1.0, (point[1] - top) / height)),
    }


def screen_point_from_relative(region, relative):
    return region_point(region, float(relative["x"]), float(relative["y"]))


def relative_version_key(version):
    return str(version or "default")


def relative_location_entry(locations, control_name, version=None):
    version_key = relative_version_key(version)
    versions = locations.get("versions")
    if isinstance(versions, dict):
        version_locations = versions.get(version_key)
        if isinstance(version_locations, dict) and isinstance(version_locations.get(control_name), dict):
            return version_locations[control_name]
    # Backward-compatible fallback for caches written before per-version storage.
    if isinstance(locations.get(control_name), dict):
        return locations[control_name]
    return None


def cached_control_clicks(control_name, version=None):
    locations = load_relative_locations()
    entry = relative_location_entry(locations, control_name, version)
    if isinstance(entry, dict):
        try:
            return int(entry.get("clicks", 0))
        except (TypeError, ValueError):
            return 0
    return 0


def has_relative_location(control_name, version=None):
    locations = load_relative_locations()
    entry = relative_location_entry(locations, control_name, version)
    return isinstance(entry, dict) and "x" in entry and "y" in entry


def cached_menu_sequence_available(version=None):
    return (
        has_relative_location("singleplayer", version)
        and has_relative_location("game_mode", version)
        and cached_control_clicks("game_mode", version) > 0
        and has_relative_location("world_tab", version)
        and has_relative_location("world_type", version)
        and cached_control_clicks("world_type", version) > 0
        and has_relative_location("create_world", version)
    )


def cached_relative_click(region, control_name, verbose=True, version=None):
    if not region:
        return False
    locations = load_relative_locations()
    relative = relative_location_entry(locations, control_name, version)
    if not isinstance(relative, dict) or "x" not in relative or "y" not in relative:
        return False
    import pyautogui

    x, y = screen_point_from_relative(region, relative)
    if verbose:
        print(f"    Clicking cached {control_name} at {x},{y}")
    pyautogui.click(x, y)
    return True


def cached_relative_click_any(region, control_names, verbose=True, version=None):
    for control_name in control_names:
        if cached_relative_click(region, control_name, verbose, version):
            return True
    return False


def remember_relative_location(region, control_name, point, text=None, variant=None, version=None, clicks=None):
    if not region:
        return
    locations = load_relative_locations()
    version_key = relative_version_key(version)
    versions = locations.setdefault("versions", {})
    version_locations = versions.setdefault(version_key, {})
    relative = relative_point_from_screen(region, point)
    relative["text"] = text or ""
    relative["variant"] = variant or ""
    relative["updated"] = utc_now_text()
    if clicks is not None:
        relative["clicks"] = int(clicks)
    existing = version_locations.get(control_name)
    if isinstance(existing, dict) and clicks is None and "clicks" in existing:
        relative["clicks"] = existing["clicks"]
    version_locations[control_name] = relative
    save_relative_locations(locations)


def remember_control_clicks(control_name, version, clicks):
    locations = load_relative_locations()
    entry = relative_location_entry(locations, control_name, version)
    if not isinstance(entry, dict):
        return
    version_key = relative_version_key(version)
    locations.setdefault("versions", {}).setdefault(version_key, {})[control_name] = dict(entry)
    locations["versions"][version_key][control_name]["clicks"] = int(clicks)
    locations["versions"][version_key][control_name]["updated"] = utc_now_text()
    save_relative_locations(locations)


def load_or_create_config(config_path=CONFIG_FILE):
    if not config_path.exists():
        config_path.write_text(CONFIG_TEMPLATE, encoding="utf-8")
        print(f"Created default config: {config_path}")
    config = parse_yaml_style_config(config_path)
    config.setdefault("curseforge_instances_path", "")
    config.setdefault("curseforge_groups_path", "")
    config.setdefault("curseforge_game_instances_path", "")
    return config


def candidate_curseforge_instance_paths():
    home = Path.home()
    candidates = [
        home / "Documents" / "curseforge" / "minecraft" / "Instances",
        home / "Documents" / "CurseForge" / "minecraft" / "Instances",
        home / "curseforge" / "minecraft" / "Instances",
        home / "CurseForge" / "minecraft" / "Instances",
        home / ".curseforge" / "minecraft" / "Instances",
    ]
    system = platform.system().lower()
    if system == "windows":
        for env_name in ("APPDATA", "LOCALAPPDATA", "USERPROFILE"):
            value = os.environ.get(env_name)
            if value:
                candidates.append(Path(value) / "CurseForge" / "minecraft" / "Instances")
                candidates.append(Path(value) / "curseforge" / "minecraft" / "Instances")
    elif system == "darwin":
        candidates.extend(
            [
                home / "Library" / "Mobile Documents" / "com~apple~CloudDocs" / "Documents" / "curseforge" / "minecraft" / "Instances",
                home / "Library" / "Mobile Documents" / "com~apple~CloudDocs" / "Documents" / "CurseForge" / "minecraft" / "Instances",
                home / "Library" / "Application Support" / "CurseForge" / "minecraft" / "Instances",
                home / "Library" / "Application Support" / "curseforge" / "minecraft" / "Instances",
            ]
        )
    else:
        xdg_data_home = os.environ.get("XDG_DATA_HOME")
        if xdg_data_home:
            candidates.append(Path(xdg_data_home) / "CurseForge" / "minecraft" / "Instances")
        candidates.extend(
            [
                home / ".local" / "share" / "CurseForge" / "minecraft" / "Instances",
                home / ".config" / "CurseForge" / "minecraft" / "Instances",
            ]
        )
    return unique_paths(candidates)


def candidate_curseforge_groups_paths():
    home = Path.home()
    candidates = []
    system = platform.system().lower()
    if system == "darwin":
        candidates.append(home / "Library" / "Application Support" / "CurseForge" / "agent" / "GameInstances" / "groups.json")
    elif system == "windows":
        for env_name in ("APPDATA", "LOCALAPPDATA"):
            value = os.environ.get(env_name)
            if value:
                candidates.append(Path(value) / "CurseForge" / "agent" / "GameInstances" / "groups.json")
    else:
        candidates.extend(
            [
                home / ".config" / "CurseForge" / "agent" / "GameInstances" / "groups.json",
                home / ".local" / "share" / "CurseForge" / "agent" / "GameInstances" / "groups.json",
            ]
        )
    return unique_paths(candidates)


def candidate_curseforge_game_instances_paths():
    groups_paths = candidate_curseforge_groups_paths()
    return unique_paths([path.with_name("MinecraftGameInstance.json") for path in groups_paths])


def unique_paths(paths):
    seen = set()
    unique = []
    for path in paths:
        text = str(path)
        if text not in seen:
            seen.add(text)
            unique.append(path)
    return unique


def looks_like_instances_path(path):
    if not path or not path.exists() or not path.is_dir():
        return False
    children = [child for child in path.iterdir() if child.is_dir()]
    if not children:
        return True
    return any((child / "minecraftinstance.json").exists() or (child / "mods").is_dir() for child in children[:50])


def locate_curseforge_instances(config):
    configured = config.get("curseforge_instances_path", "").strip()
    if configured:
        path = Path(configured).expanduser()
        if looks_like_instances_path(path):
            print(f"Using configured CurseForge instances path: {path}")
            return path.resolve()
        print(f"Configured curseforge_instances_path is invalid or unreadable: {path}")
    for candidate in candidate_curseforge_instance_paths():
        if looks_like_instances_path(candidate):
            print(f"Auto-detected CurseForge instances path: {candidate}")
            return candidate.resolve()
    print("Could not auto-detect the CurseForge Minecraft instances folder.")
    print(f"Fill in curseforge_instances_path in {CONFIG_FILE} and run this script again.")
    return None


def locate_curseforge_groups(config):
    configured = config.get("curseforge_groups_path", "").strip()
    if configured:
        path = Path(configured).expanduser()
        if path.is_file():
            print(f"Using configured CurseForge groups metadata: {path}")
            return path.resolve()
        print(f"Configured curseforge_groups_path is invalid or unreadable: {path}")
    for candidate in candidate_curseforge_groups_paths():
        if candidate.is_file():
            print(f"Auto-detected CurseForge groups metadata: {candidate}")
            return candidate.resolve()
    print("Could not auto-detect CurseForge groups.json; group updates will be skipped.")
    return None


def locate_curseforge_game_instances(config, groups_path=None):
    configured = config.get("curseforge_game_instances_path", "").strip()
    if configured:
        path = Path(configured).expanduser()
        if path.is_file():
            print(f"Using configured CurseForge game instances metadata: {path}")
            return path.resolve()
        print(f"Configured curseforge_game_instances_path is invalid or unreadable: {path}")

    candidates = []
    if groups_path:
        candidates.append(Path(groups_path).with_name("MinecraftGameInstance.json"))
    candidates.extend(candidate_curseforge_game_instances_paths())
    for candidate in unique_paths(candidates):
        if candidate.is_file():
            print(f"Auto-detected CurseForge game instances metadata: {candidate}")
            return candidate.resolve()
    print("Could not auto-detect MinecraftGameInstance.json; UI cache updates will be skipped.")
    return None


def read_json_file(path):
    with path.open("r", encoding="utf-8") as file:
        return json.load(file)


def write_json_file(path, data, compact=False):
    if compact:
        text = json.dumps(data, separators=(",", ":"))
    else:
        text = json.dumps(data, indent=4) + "\n"
    path.write_text(text, encoding="utf-8")


def load_groups(groups_path):
    if not groups_path:
        return []
    try:
        groups = read_json_file(groups_path)
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as error:
        print(f"Warning: could not read groups metadata {groups_path}: {error}")
        return []
    if not isinstance(groups, list):
        print(f"Warning: expected a list in {groups_path}; group updates disabled.")
        return []
    return groups


def group_by_id(groups):
    return {group.get("id"): group for group in groups if group.get("id")}


def child_group(groups, parent_id, name):
    wanted = normalize_text(name)
    for group in groups:
        if group.get("parentId") == parent_id and normalize_text(group.get("name", "")) == wanted:
            return group
    return None


def root_group(groups, names):
    wanted_names = {normalize_text(name) for name in names}
    for group in groups:
        if not group.get("parentId") and normalize_text(group.get("name", "")) in wanted_names:
            return group
    return None


def new_group(name, parent_id=None):
    now = utc_now_text()
    group = {
        "id": str(uuid.uuid4()),
        "gameId": 432,
        "name": name,
        "createdAt": now,
        "updatedAt": now,
    }
    if parent_id:
        group["parentId"] = parent_id
    return group


def group_path(group_id, groups):
    lookup = group_by_id(groups)
    parts = []
    current = lookup.get(group_id)
    while current:
        parts.append(current.get("name", ""))
        current = lookup.get(current.get("parentId"))
    return " / ".join(reversed([part for part in parts if part]))


def ensure_child_group(groups, parent, name, apply):
    existing = child_group(groups, parent.get("id"), name)
    if existing:
        return existing, False
    group = new_group(name, parent.get("id"))
    if apply:
        groups.append(group)
    return group, True


def ensure_target_group(groups, branch, apply):
    created = []
    root = root_group(groups, ("Testing Sets", "Test Sets"))
    if not root:
        root = new_group("Testing Sets")
        created.append(root)
        if apply:
            groups.append(root)

    de_root, was_created = ensure_child_group(groups, root, "Dissolver Enhanced", apply)
    if was_created:
        created.append(de_root)

    loader_group, was_created = ensure_child_group(groups, de_root, branch_loader_display(branch["loader"]), apply)
    if was_created:
        created.append(loader_group)

    version_group, was_created = ensure_child_group(groups, loader_group, branch["version"], apply)
    if was_created:
        created.append(version_group)

    return version_group, created


def read_properties_file(path):
    data = {}
    with path.open("r", encoding="utf-8", errors="replace") as file:
        for raw_line in file:
            line = raw_line.strip()
            if not line or line.startswith("#") or line.startswith(";") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            data[key.strip()] = value.strip()
    return data


def read_instance_json(instance_path):
    path = instance_path / "minecraftinstance.json"
    if not path.is_file():
        return None
    try:
        return read_json_file(path)
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as error:
        print(f"Warning: could not read profile JSON {path}: {error}")
        return None


def parse_de_target(value):
    match = DE_NAME_RE.match(str(value).strip())
    if not match or "-" not in match.group(1):
        return None
    loader, version = match.group(1).split("-", 1)
    return loader.lower(), version


def loader_from_base_mod_loader(base_mod_loader):
    name = str(base_mod_loader.get("name") or "").lower()
    loader_type = base_mod_loader.get("type")
    if name.startswith("neoforge") or loader_type == 6:
        return "neoforge"
    if name.startswith("fabric") or loader_type == 4:
        return "fabric"
    if name.startswith("forge") or loader_type == 1:
        return "forge"
    if name.startswith("quilt"):
        return "quilt"
    return None


def branch_loader_display(loader):
    return {
        "fabric": "Fabric",
        "forge": "Forge",
        "neoforge": "NeoForge",
        "quilt": "Quilt",
    }.get(loader.lower(), loader)


def version_matches_branch(instance_version, branch_version):
    if not instance_version:
        return False
    if branch_version.endswith(".x"):
        base = branch_version[:-2]
        return instance_version == base or instance_version.startswith(base + ".")
    return instance_version == branch_version


def version_sort_key(version):
    parts = []
    for part in str(version).split("."):
        if part.isdigit():
            parts.append((0, int(part)))
        else:
            parts.append((1, part))
    return parts


def target_versions_for_branch(instances, branch):
    versions = {
        instance["de_game_version"]
        for instance in instances
        if instance.get("de_game_version") and version_matches_branch(instance["de_game_version"], branch["version"])
    }
    versions.update(TARGET_VERSION_RANGES.get(branch["version"], ()))
    if not versions and not branch["version"].endswith(".x"):
        versions.add(branch["version"])
    return sorted(versions, key=version_sort_key)


def scan_instances(instances_path):
    instances = []
    for child in sorted(instances_path.iterdir(), key=lambda item: item.name.lower()):
        if not child.is_dir():
            continue
        instance_json = read_instance_json(child) or {}
        base_mod_loader = instance_json.get("baseModLoader") or {}
        name = instance_json.get("name") or child.name
        de_target = parse_de_target(name) or parse_de_target(child.name)
        instances.append(
            {
                "path": child,
                "name": name,
                "folder": child.name,
                "metadata_path": child / "minecraftinstance.json",
                "de_version": f"{branch_loader_display(de_target[0])}-{de_target[1]}" if de_target else None,
                "de_loader": de_target[0] if de_target else None,
                "de_game_version": de_target[1] if de_target else None,
                "loader": de_target[0] if de_target else loader_from_base_mod_loader(base_mod_loader),
                "game_version": instance_json.get("gameVersion"),
                "group_id": instance_json.get("groupId"),
                "guid": instance_json.get("guid"),
                "has_profile": (child / "minecraftinstance.json").is_file(),
            }
        )
    return instances


def read_local_git_branches(repo_root):
    try:
        output = subprocess.check_output(
            ["git", "branch", "--format=%(refname:short)"],
            cwd=repo_root,
            text=True,
            stderr=subprocess.STDOUT,
        )
    except (OSError, subprocess.CalledProcessError) as error:
        print(f"Warning: could not read local Git branches: {error}")
        return []
    return [line.strip() for line in output.splitlines() if line.strip()]


def parse_loader_version_from_branch(branch_name):
    match = BUILD_BRANCH_RE.match(branch_name)
    if not match:
        return None
    loader, version = match.groups()
    return {"name": branch_name, "loader": loader.lower(), "version": version}


def is_pull_request_branch(branch):
    normal_branch = f"minecraft/{branch['loader']}-{branch['version']}"
    return branch["name"] != normal_branch


def pull_request_test_comment(result, max_log_characters=60000):
    branch_name = result["branch"]["name"]
    tested_instances = int(result.get("tested_instances", 0))
    if result["passed"]:
        if result.get("cached"):
            passed_tests = int(result.get("passed_tests", 0))
            return (
                f"Local CurseForge testing passed for `{branch_name}` using the local test cache.\n\n"
                f"This exact build jar already passed {passed_tests} local tests. "
                "Launcher testing was skipped."
            )
        noun = "instance" if tested_instances == 1 else "instances"
        return (
            f"Local CurseForge testing passed for `{branch_name}`.\n\n"
            f"All {tested_instances} tested CurseForge {noun} passed."
        )

    sections = [f"Local CurseForge testing failed for `{branch_name}`."]
    remaining = max_log_characters
    for log_path in result.get("logs", []):
        path = Path(log_path)
        try:
            content = path.read_text(encoding="utf-8", errors="replace")
        except OSError as error:
            content = f"Could not read local failure log: {error}"
        if len(content) > remaining:
            content = content[-remaining:]
            content = "[log truncated to fit GitHub comment]\n" + content
        remaining = max(0, remaining - len(content))
        sections.append(f"### `{path.name}`\n```text\n{content.rstrip()}\n```")
        if remaining == 0:
            break
    return "\n\n".join(sections)


def publish_pull_request_test_result(result, repo_root, runner=None):
    branch = result["branch"]
    if not is_pull_request_branch(branch):
        return False

    runner = runner or subprocess.run
    common_options = {
        "cwd": repo_root,
        "text": True,
        "stdout": subprocess.PIPE,
        "stderr": subprocess.PIPE,
        "check": False,
    }

    try:
        lookup = runner(
            [
                "gh", "pr", "list", "--state", "open", "--head", branch["name"],
                "--json", "number,labels",
            ],
            **common_options,
        )
    except OSError as error:
        print(f"Warning: could not run GitHub CLI for {branch['name']}: {error}")
        return False
    if lookup.returncode != 0:
        print(f"Warning: could not find the pull request for {branch['name']}: {lookup.stderr.strip()}")
        return False
    try:
        pull_requests = json.loads(lookup.stdout or "[]")
    except json.JSONDecodeError:
        print(f"Warning: GitHub CLI returned invalid pull request data for {branch['name']}.")
        return False

    target_repo = None
    if not pull_requests:
        try:
            global_lookup = runner(
                [
                    "gh", "search", "prs", "--state", "open", "--head", branch["name"],
                    "--limit", "100", "--json", "number,labels,repository",
                ],
                **common_options,
            )
            if global_lookup.returncode == 0:
                pull_requests = json.loads(global_lookup.stdout or "[]")
                if pull_requests:
                    repository = pull_requests[0].get("repository") or {}
                    target_repo = repository.get("nameWithOwner")
        except (OSError, json.JSONDecodeError):
            pull_requests = []

    if not pull_requests:
        try:
            head = runner(["git", "rev-parse", branch["name"]], **common_options)
            if head.returncode == 0:
                all_open = runner(
                    [
                        "gh", "pr", "list", "--state", "open", "--limit", "100",
                        "--json", "number,labels,headRefOid",
                    ],
                    **common_options,
                )
                if all_open.returncode == 0:
                    candidates = json.loads(all_open.stdout or "[]")
                    head_sha = head.stdout.strip()
                    pull_requests = [
                        candidate for candidate in candidates
                        if candidate.get("headRefOid") == head_sha
                    ]
        except (OSError, json.JSONDecodeError):
            pull_requests = []

    if not pull_requests:
        print(f"Warning: no open pull request found for branch {branch['name']}; skipping GitHub test status.")
        return False

    pull_request = pull_requests[0]
    pull_request_number = str(pull_request["number"])
    repo_arguments = ["--repo", target_repo] if target_repo else []
    current_labels = {label.get("name") for label in pull_request.get("labels", [])}
    desired_label = "testing:passed" if result["passed"] else "testing:failed"
    opposite_label = "testing:failed" if result["passed"] else "testing:passed"
    label_color = "2DA44E" if result["passed"] else "D1242F"
    label_description = (
        "Local CurseForge testing passed"
        if result["passed"]
        else "Local CurseForge testing failed"
    )

    if result.get("cached") and desired_label in current_labels and opposite_label not in current_labels:
        print(
            f"Verified PR #{pull_request_number} already has {desired_label} "
            f"for cached result {branch['name']}."
        )
        return True

    commands = [
        [
            "gh", "label", "create", desired_label, *repo_arguments, "--color", label_color,
            "--description", label_description, "--force",
        ]
    ]
    edit_command = ["gh", "pr", "edit", pull_request_number, *repo_arguments, "--add-label", desired_label]
    if opposite_label in current_labels:
        edit_command.extend(["--remove-label", opposite_label])
    commands.append(edit_command)

    for command in commands:
        try:
            completed = runner(command, **common_options)
        except OSError as error:
            print(f"Warning: GitHub update failed for PR #{pull_request_number}: {error}")
            return False
        if completed.returncode != 0:
            print(f"Warning: GitHub update failed for PR #{pull_request_number}: {completed.stderr.strip()}")
            return False

    if result.get("cached"):
        print(
            f"Ensured PR #{pull_request_number} has {desired_label} "
            f"for cached result {branch['name']}."
        )
        return True

    comment = pull_request_test_comment(result)
    try:
        completed = runner(
            ["gh", "pr", "comment", pull_request_number, *repo_arguments, "--body-file", "-"],
            input=comment,
            **common_options,
        )
    except OSError as error:
        print(f"Warning: could not comment on PR #{pull_request_number}: {error}")
        return False
    if completed.returncode != 0:
        print(f"Warning: could not comment on PR #{pull_request_number}: {completed.stderr.strip()}")
        return False
    print(f"Updated PR #{pull_request_number} with {desired_label} for {branch['name']}.")
    return True


def local_build_branches(repo_root):
    parsed = []
    for branch in read_local_git_branches(repo_root):
        build_branch = parse_loader_version_from_branch(branch)
        if build_branch:
            parsed.append(build_branch)
    return sorted(parsed, key=lambda item: (item["loader"], item["version"], item["name"]))


def read_git_worktrees(repo_root):
    try:
        output = subprocess.check_output(
            ["git", "worktree", "list", "--porcelain"],
            cwd=repo_root,
            text=True,
            stderr=subprocess.STDOUT,
        )
    except (OSError, subprocess.CalledProcessError):
        return {}

    worktrees = {}
    current_path = None
    for raw_line in output.splitlines():
        line = raw_line.strip()
        if not line:
            current_path = None
            continue
        if line.startswith("worktree "):
            current_path = Path(line[len("worktree "):])
        elif line.startswith("branch refs/heads/") and current_path:
            branch = line[len("branch refs/heads/"):]
            worktrees[branch] = current_path
    return worktrees


def attach_worktree_paths(repo_root, build_branches):
    """Return only active loader worktrees located under workspace/minecraft.

    The test runner must build the checkout a developer can see and switch in the
    normal minecraft/<loader>-<version> folders.  Ignoring inactive local branch
    refs prevents stale branches from falling back to non-existent sibling paths.
    """
    worktrees = read_git_worktrees(repo_root)
    minecraft_root = repo_root.parent / "minecraft"
    attached = []
    for branch in build_branches:
        expected_path = minecraft_root / f"{branch['loader']}-{branch['version']}"
        worktree_path = worktrees.get(branch["name"])
        if worktree_path is None or worktree_path != expected_path or not worktree_path.is_dir():
            continue
        branch["worktree_path"] = worktree_path
        attached.append(branch)
    return attached


def instance_matches_branch(instance, branch):
    return (
        instance.get("de_loader") == branch["loader"]
        and version_matches_branch(instance.get("de_game_version"), branch["version"])
    )


def de_instances_for_branch(instances, branch):
    return [instance for instance in instances if instance_matches_branch(instance, branch)]


def de_instances_for_target(instances, branch, version):
    return [
        instance for instance in instances
        if instance.get("de_loader") == branch["loader"] and instance.get("de_game_version") == version
    ]


def choose_template_instance(instances, branch):
    candidates = [instance for instance in de_instances_for_branch(instances, branch) if instance["has_profile"]]
    if not candidates:
        candidates = [
            instance for instance in instances
            if instance.get("de_loader") == branch["loader"] and instance["has_profile"]
        ]
    if not candidates:
        return None
    return sorted(candidates, key=lambda item: item["folder"].lower())[0]


def choose_template_for_target(instances, branch, version):
    exact = [instance for instance in de_instances_for_target(instances, branch, version) if instance["has_profile"]]
    if exact:
        return sorted(exact, key=lambda item: item["folder"].lower())[0], True
    return choose_template_instance(instances, branch), False


def retarget_loader_metadata(data, game_version):
    old_version = data.get("gameVersion")
    data["gameVersion"] = game_version
    base_loader = data.get("baseModLoader")
    if not isinstance(base_loader, dict):
        return

    loader_name = base_loader.get("name")
    if old_version and isinstance(loader_name, str):
        base_loader["name"] = loader_name.replace(str(old_version), str(game_version))
    base_loader["minecraftVersion"] = game_version

    version_json_text = base_loader.get("versionJson")
    if not isinstance(version_json_text, str):
        return
    try:
        version_json = json.loads(version_json_text)
    except json.JSONDecodeError:
        return
    if old_version:
        if isinstance(version_json.get("id"), str):
            version_json["id"] = version_json["id"].replace(str(old_version), str(game_version))
        if isinstance(version_json.get("inheritsFrom"), str):
            version_json["inheritsFrom"] = str(game_version)
        for library in version_json.get("libraries", []):
            if isinstance(library, dict) and isinstance(library.get("name"), str):
                library["name"] = library["name"].replace(str(old_version), str(game_version))
    base_loader["versionJson"] = json.dumps(version_json, separators=(",", ":"))


def write_profile_from_template(template, new_path, new_name, group_id, game_version):
    data = copy.deepcopy(read_json_file(template["metadata_path"]))
    data["name"] = new_name
    data["guid"] = str(uuid.uuid4())
    data["groupId"] = group_id
    data["installPath"] = str(new_path.resolve()) + os.sep
    retarget_loader_metadata(data, game_version)
    data["cachedScans"] = []
    data["installedAddons"] = []
    data["installedGamePrerequisites"] = []
    data["lastPlayed"] = "0001-01-01T00:00:00"
    data["playedCount"] = 0
    data["timePlayed"] = 0
    data["installDate"] = utc_now_text()
    data["lastPreviousMatchUpdate"] = "0001-01-01T00:00:00"
    data["lastRefreshAttempt"] = "0001-01-01T00:00:00"

    new_path.mkdir(parents=True, exist_ok=False)
    write_json_file(new_path / "minecraftinstance.json", data, compact=True)
    for folder in ("config", "mods", "resourcepacks", "shaderpacks", "saves"):
        (new_path / folder).mkdir(exist_ok=True)
    return data


def update_instance_group(instance, group_id):
    data = read_json_file(instance["metadata_path"])
    data["groupId"] = group_id
    write_json_file(instance["metadata_path"], data, compact=True)
    return data


def load_game_instances(game_instances_path):
    if not game_instances_path:
        return []
    try:
        data = read_json_file(game_instances_path)
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as error:
        print(f"Warning: could not read game instances metadata {game_instances_path}: {error}")
        return []
    if not isinstance(data, list):
        print(f"Warning: expected a list in {game_instances_path}; UI cache updates disabled.")
        return []
    return data


def upsert_game_instance(game_instances, instance_data):
    guid = instance_data.get("guid")
    install_path = instance_data.get("installPath")
    for index, item in enumerate(game_instances):
        if guid and item.get("guid") == guid:
            game_instances[index] = copy.deepcopy(instance_data)
            return
        if install_path and item.get("installPath") == install_path:
            game_instances[index] = copy.deepcopy(instance_data)
            return
    game_instances.append(copy.deepcopy(instance_data))


def update_game_instance_group(game_instances, instance, group_id):
    changed = False
    for item in game_instances:
        if instance.get("guid") and item.get("guid") == instance["guid"]:
            item["groupId"] = group_id
            changed = True
        elif item.get("installPath") == str(instance["path"].resolve()) + os.sep:
            item["groupId"] = group_id
            changed = True
    return changed


def ensure_testing_sets_group_layout(instances_path, instances, build_branches, groups_path, game_instances_path, apply):
    print_section("Testing Profile Check")
    if not build_branches:
        print("No local Minecraft build branches found.")
        return

    groups = load_groups(groups_path)
    if not groups:
        print("No CurseForge group metadata loaded; cannot create grouped profiles.")
        return

    groups_changed = False
    game_instances = load_game_instances(game_instances_path)
    game_instances_changed = False
    for branch in build_branches:
        target_group, created_groups = ensure_target_group(groups, branch, apply)
        path_groups = groups + created_groups
        target_group_path = group_path(target_group["id"], path_groups)
        target_versions = target_versions_for_branch(instances, branch)

        print(f"{branch['name']} -> {target_group_path}")
        for group in created_groups:
            groups_changed = True
            print(f"  {'Created' if apply else '[dry-run] Would create'} group: {group_path(group['id'], path_groups)}")

        if not target_versions:
            print(f"  No detected target versions for {branch['version']}; skipping profile creation.")
            continue

        print(f"  Target versions: {', '.join(target_versions)}")
        for version in target_versions:
            matches = de_instances_for_target(instances, branch, version)
            already_in_group = [instance for instance in matches if instance.get("group_id") == target_group["id"]]
            migrate = [
                instance for instance in matches
                if instance.get("group_id") and instance.get("group_id") != target_group["id"]
            ]

            if already_in_group:
                print(f"  Found {version}: {', '.join(instance['folder'] for instance in already_in_group)}")
                if apply and game_instances:
                    for instance in already_in_group:
                        if update_game_instance_group(game_instances, instance, target_group["id"]):
                            game_instances_changed = True
                            print(f"    Synced CurseForge UI cache group for {instance['folder']}")
                continue

            if migrate:
                for instance in migrate:
                    old_path = group_path(instance["group_id"], groups) or instance["group_id"]
                    print(f"  {'Moving' if apply else '[dry-run] Would move'} {version} profile {instance['folder']}: {old_path} -> {target_group_path}")
                    if apply:
                        updated_data = update_instance_group(instance, target_group["id"])
                        if game_instances:
                            if update_game_instance_group(game_instances, instance, target_group["id"]):
                                game_instances_changed = True
                            else:
                                upsert_game_instance(game_instances, updated_data)
                                game_instances_changed = True
                continue

            template, exact_template = choose_template_for_target(instances, branch, version)
            new_name = f"DE ({branch_loader_display(branch['loader'])}-{version})"
            new_path = instances_path / new_name
            if not template:
                print(f"  No existing {branch['loader']} template profile found; cannot create {new_name}.")
                continue
            print(f"  {'Creating' if apply else '[dry-run] Would create'} {version} profile: {new_name}")
            print(f"    Folder: {new_path}")
            print(f"    Template: {template['folder']}{'' if exact_template else ' (retargeted metadata)'}")
            if apply:
                if new_path.exists():
                    print(f"    Skipped; folder already exists: {new_path}")
                else:
                    new_data = write_profile_from_template(template, new_path, new_name, target_group["id"], version)
                    if game_instances:
                        upsert_game_instance(game_instances, new_data)
                        game_instances_changed = True

    if apply and groups_changed:
        write_json_file(groups_path, groups)
    if apply and game_instances_changed:
        write_json_file(game_instances_path, game_instances, compact=True)


def safe_relative(path, root):
    root = root.resolve()
    path = path.resolve()
    try:
        path.relative_to(root)
    except ValueError as error:
        raise RuntimeError(f"Refusing to operate outside {root}: {path}") from error
    return path


def is_metadata_path(path):
    return path.name in METADATA_FILENAMES


def is_dissolver_config_dir(path):
    normalized = normalize_text(path.name)
    return "dissolver" in normalized and ("enhanced" in normalized or normalized == "dissolver")


def is_preserved_dissolver_config_file(path):
    return path.name in PRESERVED_DISSOLVER_CONFIG_FILES and is_dissolver_config_dir(path.parent)


def cleanup_plan_for_instance(instance):
    delete_paths = []
    for child in sorted(instance["path"].iterdir(), key=lambda item: item.name.lower()):
        if child.is_file() and (child.name == "options.txt" or is_metadata_path(child)):
            continue
        if child.is_dir() and child.name.lower() == "config":
            for config_child in sorted(child.iterdir(), key=lambda item: item.name.lower()):
                if config_child.is_dir() and is_dissolver_config_dir(config_child):
                    continue
                delete_paths.append(config_child)
            continue
        delete_paths.append(child)
    return delete_paths


def remove_path(path, instance_root):
    path = safe_relative(path, instance_root)
    if path == instance_root:
        raise RuntimeError(f"Refusing to delete instance root: {instance_root}")
    if is_preserved_dissolver_config_file(path):
        raise RuntimeError(f"Refusing to delete preserved Dissolver config file: {path}")
    if path.is_dir():
        shutil.rmtree(path)
    elif path.exists():
        path.unlink()


def clean_instance_folders(de_instances, apply):
    print_section("DE Cleanup Plan")
    if not de_instances:
        print("No DE (version) instances or groups detected.")
        return
    for instance in de_instances:
        print(f"{instance['name']} [{instance['de_version']}]")
        print(f"  Path: {instance['path']}")
        delete_paths = cleanup_plan_for_instance(instance)
        if not delete_paths:
            print("  Nothing to delete.")
            continue
        for path in delete_paths:
            relative = path.relative_to(instance["path"])
            prefix = "Deleting" if apply else "[dry-run] Would delete"
            print(f"  {prefix}: {relative}")
            if apply:
                remove_path(path, instance["path"])


def clean_instance_folders_compact(de_instances, apply):
    if not de_instances:
        print("No DE instances to clean.")
        return
    try:
        from rich.progress import BarColumn, Progress, TextColumn, TimeElapsedColumn
    except ImportError:
        progress = SingleProgressDisplay("cleanup", len(de_instances))
        for instance in de_instances:
            delete_paths = cleanup_plan_for_instance(instance)
            for path in delete_paths:
                if apply:
                    remove_path(path, instance["path"])
            progress.step(f"{instance['folder']} ({len(delete_paths)} removed)")
        return

    with Progress(
        TextColumn("{task.description}", justify="left"),
        BarColumn(bar_width=24),
        TextColumn("{task.completed:.0f}/{task.total:.0f}"),
        TextColumn("{task.fields[status]}"),
        TimeElapsedColumn(),
        transient=False,
    ) as progress:
        task = progress.add_task("cleanup", total=len(de_instances), status="starting")
        for instance in de_instances:
            delete_paths = cleanup_plan_for_instance(instance)
            for path in delete_paths:
                if apply:
                    remove_path(path, instance["path"])
            progress.advance(task, 1)
            progress.update(task, status=f"{instance['folder']} ({len(delete_paths)} removed)")


def launch_instances_compact(plan, apply, automate_menus=False, ocr_debug=False):
    instances = plan["instances"]
    description = f"launch {plan['loader_version']}"
    try:
        from rich.progress import BarColumn, Progress, TextColumn, TimeElapsedColumn
    except ImportError:
        progress = SingleProgressDisplay(description, len(instances))
        for instance in instances:
            progress.step(instance["folder"])
            launch_instance(instance, apply, automate_menus, ocr_debug, verbose=False)
        return

    with Progress(
        TextColumn("{task.description}", justify="left"),
        BarColumn(bar_width=24),
        TextColumn("{task.completed:.0f}/{task.total:.0f}"),
        TextColumn("{task.fields[status]}"),
        TimeElapsedColumn(),
        transient=False,
    ) as progress:
        task = progress.add_task(description, total=len(instances), status="waiting")
        for instance in instances:
            progress.update(task, status=instance["folder"])
            launch_instance(instance, apply, automate_menus, ocr_debug, verbose=False)
            progress.advance(task, 1)


def recipe_command_count():
    tests = load_recipe_unlock_tests()
    trigger_items = {item for test in tests for item in test["trigger_items"]}
    return 1 + len(trigger_items) + 1


def smoke_test_command_count():
    return 12


def estimated_instance_steps(instance):
    version = instance.get("de_game_version") or instance.get("game_version") or instance.get("version") or "default"
    return (
        1  # launch
        + 1  # wait for window
        + 1  # wait for menu
        + 1  # singleplayer
        + 1  # create-world screen
        + max(1, cached_control_clicks("game_mode", version))
        + 1  # world tab
        + max(1, cached_control_clicks("world_type", version))
        + 1  # create world
        + 1  # wait for world load
        + 1  # "Testing has started"
        + recipe_command_count()
        + 1  # pause to save
        + 1  # recipe result
        + smoke_test_command_count()
        + 1  # wait for close
    )


def branch_test_count(plan, instance_counter=estimated_instance_steps):
    return sum(instance_counter(instance) for instance in plan.get("instances", []))


def partition_cached_instances(cache, plan, jar_hash, instance_counter=estimated_instance_steps):
    cached = []
    pending = []
    for instance in plan.get("instances", []):
        test_count = int(instance_counter(instance))
        if cached_instance_pass(cache, plan, instance, jar_hash, test_count):
            cached.append(instance)
        else:
            pending.append(instance)
    return cached, pending


def instance_progress_label(plan, instance):
    loader = str(plan["branch"].get("loader", "")).title() or str(plan.get("loader_version", "loader"))
    version = instance.get("de_game_version") or instance.get("game_version") or instance.get("version") or "unknown"
    return f"{loader}-{version}"


def initialize_branch_test_results(plans):
    return {
        plan["branch"]["name"]: {
            "branch": plan["branch"],
            "passed": True,
            "tested_instances": len(plan.get("cached_instances", [])),
            "logs": [],
        }
        for plan in plans
    }


def record_branch_instance_result(results, plan, passed, logs):
    result = results[plan["branch"]["name"]]
    result["tested_instances"] += 1
    result["passed"] = result["passed"] and bool(passed)
    result["logs"].extend(Path(log_path) for log_path in logs)


def emit_branch_test_result(results, plan, on_plan_complete=None):
    result = results[plan["branch"]["name"]]
    if on_plan_complete:
        on_plan_complete(result, plan)
    return result


def complete_branch_test_results(setup_plans, launch_results):
    results_by_branch = {result["branch"]["name"]: result for result in launch_results}
    completed = []
    for plan in setup_plans:
        branch = plan["branch"]
        result = results_by_branch.get(branch["name"])
        if result is None:
            failure_logs = [Path(path) for path in branch.get("test_failure_logs", [])]
            if not failure_logs:
                failure_logs = [write_test_failure_log(
                    f"{branch['name']}-setup-failure",
                    "Local CurseForge testing could not start because branch setup failed.\n",
                )]
            result = {
                "branch": branch,
                "passed": False,
                "tested_instances": 0,
                "logs": failure_logs,
            }
        completed.append(result)
    return completed


def finalize_branch_test_result(
    result,
    plan,
    test_cache,
    repo_root,
    cache_path=CACHE_FILE,
    publisher=publish_pull_request_test_result,
):
    test_count = int(plan.get("test_count", 0))
    result["passed_tests"] = test_count
    if plan.get("cache_enabled") and result["passed"] and not result.get("cached"):
        record_cached_pass(
            test_cache,
            plan,
            plan["jar_sha256"],
            test_count,
            cache_path=cache_path,
        )
    publisher(result, repo_root)
    return result


def launch_all_instances_compact(
    plans,
    apply,
    automate_menus=False,
    ocr_debug=False,
    on_plan_complete=None,
    on_instance_complete=None,
):
    all_instances = [(plan, instance) for plan in plans for instance in plan["instances"]]
    total_instances = len(all_instances)
    results = initialize_branch_test_results(plans)
    if not total_instances:
        return list(results.values())

    try:
        from rich.progress import BarColumn, Progress, TextColumn, TimeElapsedColumn
    except ImportError:
        total_progress = SingleProgressDisplay("Total Progress", total_instances)
        for plan in plans:
            for instance in plan["instances"]:
                total_progress.step(f"{instance_progress_label(plan, instance)} testing")
                passed = launch_instance(instance, apply, automate_menus, ocr_debug, verbose=False)
                failure_logs = [] if passed else copy_instance_logs_for_failure(instance, instance_test_label(instance))
                if not passed and not failure_logs:
                    failure_logs = [write_test_failure_log(
                        f"{instance_test_label(instance)}-failure",
                        "Local CurseForge testing failed, but Minecraft did not produce a readable log file.\n",
                    )]
                record_branch_instance_result(results, plan, passed, failure_logs)
                if on_instance_complete:
                    on_instance_complete(results[plan["branch"]["name"]], plan, instance, passed)
            emit_branch_test_result(results, plan, on_plan_complete)
        return list(results.values())

    with Progress(
        TextColumn("{task.description}", justify="left"),
        BarColumn(bar_width=24),
        TextColumn("{task.completed:.0f}/{task.total:.0f}"),
        TextColumn("{task.fields[status]}"),
        TimeElapsedColumn(),
        transient=False,
    ) as progress:
        first_plan = plans[0]
        loader_task = progress.add_task(str(first_plan["loader_version"]), total=len(first_plan["instances"]), status="0 tested")
        total_task = progress.add_task("Total Progress", total=total_instances, status="0 tested")
        total_done = 0

        for plan in plans:
            loader_done = 0
            progress.update(
                loader_task,
                description=str(plan["loader_version"]),
                total=len(plan["instances"]),
                completed=0,
                status=f"{len(plan['instances'])} left",
            )
            for instance in plan["instances"]:
                total_steps = estimated_instance_steps(instance)
                label = instance_test_label(instance)
                append_test_result(label)
                instance_task = progress.add_task(
                    instance_progress_label(plan, instance),
                    total=total_steps,
                    status="starting",
                )

                progress_events = queue.Queue()
                passed_steps = 0

                def report(status, amount=1):
                    progress_events.put((status, amount))

                def drain_progress_events():
                    nonlocal passed_steps
                    while True:
                        status, amount = progress_events.get_nowait()
                        amount = max(1, int(amount or 1))
                        for _ in range(amount):
                            passed_steps += 1
                            append_test_result(f"step{passed_steps}:pass")
                        progress.advance(instance_task, amount)
                        progress.update(instance_task, status=str(status)[:80])

                with concurrent.futures.ThreadPoolExecutor(max_workers=1) as executor:
                    future = executor.submit(
                        launch_instance,
                        instance,
                        apply,
                        automate_menus,
                        ocr_debug,
                        False,
                        report,
                    )
                    while not future.done():
                        try:
                            drain_progress_events()
                        except queue.Empty:
                            pass
                        progress.refresh()
                        time.sleep(0.1)

                    try:
                        drain_progress_events()
                    except queue.Empty:
                        pass
                    try:
                        passed = future.result()
                    except Exception as error:
                        passed = False
                        progress.update(instance_task, status=f"error: {error}")

                if passed:
                    failure_logs = []
                    progress.update(instance_task, completed=total_steps, status="done")
                else:
                    failed_step = min(total_steps, passed_steps + 1)
                    failure_logs = copy_instance_logs_for_failure(instance, label)
                    if not failure_logs:
                        failure_logs = [write_test_failure_log(
                            f"{label}-failure",
                            f"Local CurseForge testing failed at step {failed_step}, but Minecraft did not produce a readable log file.\n",
                        )]
                    first_log = f"./logs/{failure_logs[0].name}" if failure_logs else "./logs"
                    append_test_result(f"step{failed_step}:fail see {first_log}")
                    progress.update(instance_task, completed=passed_steps, status=f"failed step {failed_step}")
                record_branch_instance_result(results, plan, passed, failure_logs)
                if on_instance_complete:
                    on_instance_complete(results[plan["branch"]["name"]], plan, instance, passed)
                loader_done += 1
                total_done += 1
                progress.update(loader_task, completed=loader_done, status=f"{len(plan['instances']) - loader_done} left")
                progress.update(total_task, completed=total_done, status=f"{total_instances - total_done} left")
                progress.refresh()
                progress.remove_task(instance_task)
            emit_branch_test_result(results, plan, on_plan_complete)
    return list(results.values())


def prompt_for_build_branches(build_branches, selection_text=None, verbose=True):
    if verbose:
        print_section("Testing Branch Selection")
    if not build_branches:
        print("No Minecraft build branches are available.")
        return []

    if verbose:
        for index, branch in enumerate(build_branches, 1):
            worktree_path = branch.get("worktree_path")
            suffix = f" [{worktree_path}]" if worktree_path else ""
            print(f"{index}. {branch['name']} -> {branch_loader_display(branch['loader'])} {branch['version']}{suffix}")

    if selection_text is None:
        selection_text = input("Type 'all' or branch numbers separated by commas: ").strip()
    elif verbose:
        print(f"Selected from argument: {selection_text}")

    if normalize_text(selection_text) == "all":
        return list(build_branches)

    selected = []
    seen = set()
    for raw_part in selection_text.split(","):
        part = raw_part.strip()
        if not part:
            continue
        if not part.isdigit():
            print(f"Ignoring invalid selection: {part}")
            continue
        index = int(part)
        if index < 1 or index > len(build_branches):
            print(f"Ignoring out-of-range selection: {part}")
            continue
        if index not in seen:
            seen.add(index)
            selected.append(build_branches[index - 1])
    return selected


def gradle_command(worktree_path, gradle_task):
    if platform.system().lower() == "windows":
        wrapper = worktree_path / "gradlew.bat"
        return [str(wrapper if wrapper.exists() else "gradle"), gradle_task]
    wrapper = worktree_path / "gradlew"
    if not wrapper.exists():
        return ["gradle", gradle_task]
    if os.access(wrapper, os.X_OK):
        return [str(wrapper), gradle_task]
    return ["sh", str(wrapper), gradle_task]


def gradle_clean_command(command):
    clean_command = list(command)
    # Generated loader outputs can retain iCloud conflict-copy jars (for example
    # "dependency 2.jar") even when every tracked source is unchanged. Reusing
    # that output changes the final artifact hash and defeats the test cache.
    if "clean" not in clean_command:
        clean_command.insert(len(clean_command) - 1, "clean")
    if "--console=plain" not in clean_command:
        clean_command.append("--console=plain")
    if "--warning-mode=summary" not in clean_command:
        clean_command.append("--warning-mode=summary")
    return clean_command


def print_command_failure_tail(output, max_lines=80):
    lines = (output or "").splitlines()
    if not lines:
        return
    print("  Build output tail:")
    for line in lines[-max_lines:]:
        print(f"    {line}")


def repair_unreadable_tracked_files(worktree_path, verbose=True):
    """Restore clean tracked files that iCloud has left locally unreadable.

    Gradle 9 fails during input hashing when CloudDocs returns EDEADLK for a
    placeholder file. Only clean Git-tracked files are repaired, so local edits
    are never overwritten.
    """
    try:
        tracked = subprocess.check_output(
            ["git", "ls-files", "-z"], cwd=worktree_path
        ).split(b"\0")
        dirty_output = subprocess.check_output(
            ["git", "status", "--porcelain", "-z"], cwd=worktree_path
        )
    except (OSError, subprocess.CalledProcessError):
        return 0

    dirty_paths = set()
    for record in dirty_output.split(b"\0"):
        if len(record) >= 4:
            dirty_paths.add(record[3:].decode("utf-8", "replace"))

    repaired = 0
    for raw_path in tracked:
        if not raw_path:
            continue
        relative_path = raw_path.decode("utf-8", "replace")
        if relative_path in dirty_paths:
            continue
        path = worktree_path / relative_path
        try:
            with path.open("rb") as file:
                file.read(1)
        except OSError as error:
            if error.errno != errno.EDEADLK:
                continue
            try:
                path.unlink()
                subprocess.run(
                    ["git", "checkout", "HEAD", "--", relative_path],
                    cwd=worktree_path,
                    check=True,
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.PIPE,
                    text=True,
                )
                with path.open("rb") as file:
                    file.read(1)
            except (OSError, subprocess.CalledProcessError) as repair_error:
                if verbose:
                    print(f"  Could not rehydrate {relative_path}: {repair_error}")
                continue
            repaired += 1
            if verbose:
                print(f"  Rehydrated iCloud source file: {relative_path}")
    return repaired


def build_branch_artifact(branch, apply, gradle_task, verbose=True):
    worktree_path = Path(branch["worktree_path"])
    command = gradle_clean_command(gradle_command(worktree_path, gradle_task))
    if verbose:
        print(f"  {'Running' if apply else '[dry-run] Would run'}: {' '.join(command)}")
        print(f"  Worktree: {worktree_path}")
    if not worktree_path.is_dir():
        print("  Missing worktree; skipping branch.")
        return None
    if apply:
        repair_unreadable_tracked_files(worktree_path, verbose=True)
        try:
            result = subprocess.run(
                command,
                cwd=worktree_path,
                check=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                errors="replace",
            )
        except subprocess.CalledProcessError as error:
            print(f"  Build failed with exit code {error.returncode}; skipping branch.")
            print_command_failure_tail(error.stdout)
            failure_output = error.stdout or f"Gradle build failed with exit code {error.returncode}.\n"
            failure_log = write_test_failure_log(f"{branch['name']}-build", failure_output)
            branch.setdefault("test_failure_logs", []).append(failure_log)
            return None
        if verbose and result.stdout.strip():
            print_command_failure_tail(result.stdout, max_lines=20)
    return find_branch_artifact(worktree_path)


def find_branch_artifact(worktree_path):
    libs = worktree_path / "build" / "libs"
    if not libs.is_dir():
        return None
    jars = [
        path for path in libs.glob("*.jar")
        if not re.search(r"-(sources|javadoc|dev|all-dev)\.jar$", path.name, re.IGNORECASE)
        and not re.search(r" \d+\.jar$", path.name, re.IGNORECASE)
    ]
    if not jars:
        return None
    return max(jars, key=lambda path: path.stat().st_mtime)


def remove_old_test_jars(mods_dir, apply, verbose=True):
    removed = []
    for path in sorted(mods_dir.glob("dissolver-enhanced*.jar"), key=lambda item: item.name.lower()):
        removed.append(path)
        if verbose:
            print(f"    {'Deleting' if apply else '[dry-run] Would delete'} old mod jar: {path.name}")
        if apply:
            remove_path(path, mods_dir)
    return removed


def modrinth_headers():
    return {"User-Agent": "DissolverEnhancedTestingScript/1.0"}


def modrinth_latest_file(project, loader, game_version):
    query = urllib.parse.urlencode(
        {
            "loaders": json.dumps([loader]),
            "game_versions": json.dumps([game_version]),
        }
    )
    url = f"https://api.modrinth.com/v2/project/{urllib.parse.quote(project)}/version?{query}"
    request = urllib.request.Request(url, headers=modrinth_headers())
    with urllib.request.urlopen(request, timeout=30) as response:
        versions = json.load(response)
    if not versions:
        return None
    version = versions[0]
    files = version.get("files") or []
    primary_files = [file for file in files if file.get("primary")]
    file_info = (primary_files or files)[0] if files else None
    if not file_info:
        return None
    return {
        "version": version.get("version_number"),
        "filename": file_info.get("filename"),
        "url": file_info.get("url"),
    }


def remove_old_dependency_jars(mods_dir, spec, apply, verbose=True):
    prefixes = tuple(prefix.lower() for prefix in spec.get("filename_prefixes", ()))
    for path in sorted(mods_dir.glob("*.jar"), key=lambda item: item.name.lower()):
        if path.name.lower().startswith(prefixes):
            if verbose:
                print(f"    {'Deleting' if apply else '[dry-run] Would delete'} old {spec['name']} jar: {path.name}")
            if apply:
                remove_path(path, mods_dir)


def loader_dependency_steps(instance, enabled=True):
    if not enabled:
        return 0
    return len(LOADER_DEPENDENCIES.get(instance.get("de_loader"), []))


def setup_step_count(instances, install_loader_api=True):
    # Build once, then per instance: old jar cleanup, mod jar copy, config write, and loader deps if needed.
    return 1 + sum(3 + loader_dependency_steps(instance, install_loader_api) for instance in instances)


def install_loader_dependencies(instance, apply, enabled=True, verbose=True, progress=None):
    if not enabled:
        return
    specs = LOADER_DEPENDENCIES.get(instance.get("de_loader"), [])
    if not specs:
        return

    mods_dir = instance["path"] / "mods"
    for spec in specs:
        if verbose:
            print(f"    Resolving {spec['name']} for Minecraft {instance['de_game_version']}...")
        try:
            file_info = modrinth_latest_file(spec["project"], spec["loader"], instance["de_game_version"])
        except (OSError, TimeoutError, urllib.error.URLError, json.JSONDecodeError) as error:
            if verbose:
                print(f"    Warning: could not resolve {spec['name']}: {error}")
            if progress:
                progress(f"{instance['de_game_version']} dependency failed")
            continue
        if not file_info:
            if verbose:
                print(f"    Warning: no {spec['name']} version found for Minecraft {instance['de_game_version']}.")
            if progress:
                progress(f"{instance['de_game_version']} dependency skipped")
            continue

        target = mods_dir / file_info["filename"]
        if verbose:
            print(f"    {'Downloading' if apply else '[dry-run] Would download'} {spec['name']} {file_info['version']}: {target.name}")
        if not apply:
            if progress:
                progress(f"{instance['de_game_version']} dependency checked")
            continue

        mods_dir.mkdir(parents=True, exist_ok=True)
        remove_old_dependency_jars(mods_dir, spec, apply, verbose)
        request = urllib.request.Request(file_info["url"], headers=modrinth_headers())
        with urllib.request.urlopen(request, timeout=120) as response:
            with target.open("wb") as file:
                shutil.copyfileobj(response, file)
        if progress:
            progress(f"{instance['de_game_version']} dependency installed")


def install_artifact_into_instance(artifact, instance, apply, install_loader_api=True, verbose=True, progress=None):
    mods_dir = instance["path"] / "mods"
    target = mods_dir / artifact.name if artifact else None
    if verbose:
        print(f"  {instance['folder']} ({instance['de_game_version']})")
        print(f"    {'Copying' if apply else '[dry-run] Would copy'}: {artifact} -> {target}")
        print("    Writing tester config: config/dissolver-enhanced/dissolver_enhanced.properties")
    if not apply:
        install_loader_dependencies(instance, apply, install_loader_api, verbose, progress)
        return
    mods_dir.mkdir(parents=True, exist_ok=True)
    remove_old_test_jars(mods_dir, apply, verbose)
    if progress:
        progress(f"{instance['de_game_version']} old jars checked")
    shutil.copy2(artifact, target)
    if progress:
        progress(f"{instance['de_game_version']} jar copied")

    config_dir = instance["path"] / "config" / "dissolver-enhanced"
    config_dir.mkdir(parents=True, exist_ok=True)
    config_path = config_dir / "dissolver_enhanced.properties"
    config_path.write_text(TEST_CONFIG_TEXT, encoding="utf-8")
    if progress:
        progress(f"{instance['de_game_version']} config written")
    install_loader_dependencies(instance, apply, install_loader_api, verbose, progress)


def install_artifacts_parallel(artifact, instances, apply, install_loader_api=True, max_workers=6, label="Install prep", show_progress=True, progress_factory=None):
    if not instances:
        return
    if not apply:
        for instance in instances:
            install_artifact_into_instance(artifact, instance, apply, install_loader_api)
        return

    progress = ProgressBar(f"  {label}", len(instances), enabled=show_progress)
    workers = min(max_workers, len(instances))
    executor = concurrent.futures.ThreadPoolExecutor(max_workers=workers)
    try:
        future_to_instance = {
            executor.submit(
                install_artifact_into_instance,
                artifact,
                instance,
                apply,
                install_loader_api,
                False,
                progress_factory(instance) if progress_factory else None,
            ): instance
            for instance in instances
        }
        for future in concurrent.futures.as_completed(future_to_instance):
            instance = future_to_instance[future]
            try:
                future.result()
                progress.step(instance["folder"])
            except Exception as error:
                progress.step(instance["folder"])
                print(f"  Warning: install failed for {instance['folder']}: {error}")
    except KeyboardInterrupt:
        executor.shutdown(wait=False, cancel_futures=True)
        raise
    else:
        executor.shutdown(wait=True)
    progress.finish()


def curseforge_launch_url(instance):
    # CurseForge currently handles this deep link in the Electron app.
    # If their route changes, update this function rather than the test flow.
    return f"curseforge://launch-game?instanceId={instance['guid']}&gameId=432"


def open_url(url):
    system = platform.system().lower()
    if system == "darwin":
        subprocess.Popen(["open", url])
    elif system == "windows":
        os.startfile(url)  # type: ignore[attr-defined]
    else:
        subprocess.Popen(["xdg-open", url])


def process_rows():
    system = platform.system().lower()
    if system == "windows":
        command = [
            "powershell",
            "-NoProfile",
            "-Command",
            "Get-CimInstance Win32_Process | Select-Object ProcessId,CommandLine | ConvertTo-Json -Compress",
        ]
        try:
            output = subprocess.check_output(command, text=True, stderr=subprocess.DEVNULL)
            data = json.loads(output) if output.strip() else []
            if isinstance(data, dict):
                data = [data]
            return [(str(item.get("ProcessId")), str(item.get("CommandLine") or "")) for item in data]
        except (OSError, subprocess.CalledProcessError, json.JSONDecodeError):
            return []

    try:
        output = subprocess.check_output(["ps", "-axo", "pid=,command="], text=True, stderr=subprocess.DEVNULL)
    except (OSError, subprocess.CalledProcessError):
        return []
    rows = []
    for line in output.splitlines():
        stripped = line.strip()
        if not stripped:
            continue
        pid, _, command = stripped.partition(" ")
        rows.append((pid, command))
    return rows


def minecraft_processes_for_instance(instance):
    instance_path = str(instance["path"].resolve())
    rows = []
    for pid, command in process_rows():
        lower_command = command.lower()
        if instance_path in command and ("java" in lower_command or "minecraft" in lower_command):
            rows.append((pid, command))
    return rows


def kill_minecraft_processes_for_instance(instance, verbose=True):
    rows = minecraft_processes_for_instance(instance)
    if not rows:
        return False
    system = platform.system().lower()
    for pid, _ in rows:
        try:
            if system == "windows":
                subprocess.run(["taskkill", "/PID", str(pid), "/T", "/F"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            else:
                os.kill(int(pid), signal.SIGTERM)
        except (OSError, ValueError):
            continue
    if verbose:
        print("    Minecraft process stopped after successful test.")
    return True


def wait_for_instance_to_close(instance, apply, launch_timeout=180, poll_seconds=5, verbose=True):
    if not apply:
        if verbose:
            print(f"    [dry-run] Would wait for Minecraft process using instance path: {instance['path']}")
        return

    import time

    if verbose:
        print("    Waiting for Minecraft to start...")
    deadline = time.time() + launch_timeout
    seen = False
    while time.time() < deadline:
        if minecraft_processes_for_instance(instance):
            seen = True
            break
        time.sleep(poll_seconds)

    if not seen:
        if verbose:
            print("    No matching Minecraft process was detected before timeout; continuing.")
        return

    if verbose:
        print("    Minecraft detected. Waiting for it to close...")
    while minecraft_processes_for_instance(instance):
        time.sleep(poll_seconds)
    if verbose:
        print("    Minecraft closed.")


def require_menu_automation_backend():
    missing = []
    try:
        import pyautogui  # noqa: F401
    except ImportError:
        missing.append("pyautogui")
    try:
        import pytesseract  # noqa: F401
    except ImportError:
        missing.append("pytesseract")
    try:
        from PIL import Image  # noqa: F401
    except ImportError:
        missing.append("pillow")
    if shutil.which("tesseract") is None:
        missing.append("tesseract executable")
    return missing


def has_easyocr():
    try:
        import easyocr  # noqa: F401
        return True
    except ImportError:
        return False


def automation_install_help(missing=None):
    missing = set(missing or [])
    print("    Menu automation dependencies are missing.")
    python_packages = [name for name in ("pyautogui", "pillow", "pytesseract") if name in missing]
    if python_packages:
        print(f"    Python packages: {sys.executable} -m pip install {' '.join(python_packages)}")
        print(f"    Better Minecraft-font OCR, optional: {sys.executable} -m pip install easyocr")
    system = platform.system().lower()
    if "tesseract executable" in missing:
        if system == "darwin":
            print("    Tesseract: brew install tesseract")
        elif system == "windows":
            print("    Tesseract: install from https://github.com/UB-Mannheim/tesseract/wiki and add it to PATH.")
        else:
            print("    Tesseract: install with your package manager, for example sudo apt install tesseract-ocr.")
    elif shutil.which("tesseract"):
        print(f"    Tesseract executable found: {shutil.which('tesseract')}")
    if system == "darwin":
        print("    macOS permissions: allow Terminal/Python screen recording and accessibility access.")


def open_macos_privacy_pane(anchor):
    subprocess.Popen(["open", f"x-apple.systempreferences:com.apple.preference.security?Privacy_{anchor}"])


def preflight_macos_automation_permissions(apply):
    if platform.system().lower() != "darwin" or not apply:
        return

    print_section("macOS Automation Permissions")
    print("Live menu automation needs Screen Recording for OCR and Accessibility for mouse clicks.")
    print("macOS will not let this script grant those permissions automatically.")
    print("If a prompt appears, approve Terminal/Python. If no prompt appears, the Privacy panes will open.")

    missing = require_menu_automation_backend()
    if missing:
        print(f"Missing automation backend before permission check: {', '.join(missing)}")
        automation_install_help(missing)
        return

    try:
        import pyautogui

        pyautogui.screenshot()
        print("Screen capture check completed.")
    except Exception as error:
        print(f"Screen capture check failed or was blocked: {error}")
        print("Opening Screen Recording settings.")
        open_macos_privacy_pane("ScreenCapture")

    try:
        import pyautogui

        x, y = pyautogui.position()
        pyautogui.moveTo(x, y)
        print("Mouse control check completed.")
    except Exception as error:
        print(f"Mouse control check failed or was blocked: {error}")
        print("Opening Accessibility settings.")
        open_macos_privacy_pane("Accessibility")

    print("If you changed permissions, quit and reopen the terminal app before running the full automation.")


def macos_focus_minecraft_window():
    script = '''
tell application "System Events"
    repeat with processName in {"Minecraft", "java"}
        if exists process processName then
            set frontmost of process processName to true
            return processName
        end if
    end repeat
end tell
return ""
'''
    try:
        output = subprocess.check_output(["osascript", "-e", script], text=True, stderr=subprocess.DEVNULL).strip()
        return output or None
    except (OSError, subprocess.CalledProcessError):
        return None


def macos_front_window_region():
    script = '''
tell application "System Events"
    set frontProcess to first process whose frontmost is true
    if not (exists window 1 of frontProcess) then return ""
    set windowPosition to position of window 1 of frontProcess
    set windowSize to size of window 1 of frontProcess
    return (item 1 of windowPosition as text) & "," & (item 2 of windowPosition as text) & "," & (item 1 of windowSize as text) & "," & (item 2 of windowSize as text) & "," & (name of frontProcess as text)
end tell
'''
    try:
        output = subprocess.check_output(["osascript", "-e", script], text=True, stderr=subprocess.DEVNULL).strip()
    except (OSError, subprocess.CalledProcessError):
        return None
    if not output:
        return None
    parts = output.split(",")
    if len(parts) < 5:
        return None
    try:
        left, top, width, height = [int(float(part)) for part in parts[:4]]
    except ValueError:
        return None
    process_name = ",".join(parts[4:])
    if width <= 0 or height <= 0:
        return None
    return {"region": (left, top, width, height), "process": process_name}


def pyautogui_focus_minecraft_window():
    try:
        import pyautogui
    except ImportError:
        return None
    for title in ("Minecraft",):
        try:
            windows = pyautogui.getWindowsWithTitle(title)
        except Exception:
            windows = []
        for window in windows:
            try:
                window.activate()
                return {"region": (window.left, window.top, window.width, window.height), "process": window.title}
            except Exception:
                continue
    return None


def focus_minecraft_window():
    if platform.system().lower() == "darwin":
        name = macos_focus_minecraft_window()
        if name:
            time.sleep(1)
            info = macos_front_window_region()
            if info:
                return info
    return pyautogui_focus_minecraft_window()


def wait_for_minecraft_window(timeout=180, poll_seconds=2):
    deadline = time.time() + timeout
    next_progress = time.time() + 10
    while time.time() < deadline:
        info = focus_minecraft_window()
        if info:
            return info
        if time.time() >= next_progress:
            remaining = max(0, int(deadline - time.time()))
            print(f"    Minecraft window not found yet; still waiting ({remaining}s left).")
            next_progress = time.time() + 10
        time.sleep(poll_seconds)
    return None


def minecraft_ocr_variants(image):
    from PIL import ImageOps

    variants = [("raw", image, 1)]
    scale = 3
    scaled_size = (image.width * scale, image.height * scale)
    resized = image.resize(scaled_size)
    variants.append(("resized", resized, scale))

    grayscale = ImageOps.grayscale(resized)
    contrast = ImageOps.autocontrast(grayscale)
    variants.append(("contrast", contrast, scale))

    # Minecraft's pixel font is usually bright text with dark shadow on mixed backgrounds.
    # This pass turns bright glyph pixels into black text on white paper for Tesseract.
    bright_text = contrast.point(lambda pixel: 0 if pixel >= 165 else 255)
    variants.append(("bright-text", bright_text, scale))

    # A looser pass helps on grey buttons where anti-aliased text is less bright.
    medium_text = contrast.point(lambda pixel: 0 if pixel >= 125 else 255)
    variants.append(("medium-text", medium_text, scale))
    return variants


def ocr_image_lines(image, offset_x=0, offset_y=0, scale=1, config="--oem 3 --psm 11", min_confidence=15):
    import pytesseract

    data = pytesseract.image_to_data(image, output_type=pytesseract.Output.DICT, config=config)
    lines = {}
    for index, raw_text in enumerate(data.get("text", [])):
        text = str(raw_text).strip()
        if not text:
            continue
        try:
            confidence = float(data.get("conf", [0])[index])
        except (TypeError, ValueError):
            confidence = 0
        if confidence < min_confidence:
            continue
        key = (
            data.get("block_num", [0])[index],
            data.get("par_num", [0])[index],
            data.get("line_num", [0])[index],
        )
        entry = lines.setdefault(key, {"words": [], "left": [], "top": [], "right": [], "bottom": []})
        left = int(data["left"][index] / scale) + offset_x
        top = int(data["top"][index] / scale) + offset_y
        width = int(data["width"][index] / scale)
        height = int(data["height"][index] / scale)
        entry["words"].append(text)
        entry["left"].append(left)
        entry["top"].append(top)
        entry["right"].append(left + width)
        entry["bottom"].append(top + height)

    results = []
    for entry in lines.values():
        text = " ".join(entry["words"])
        results.append(
            {
                "text": text,
                "normalized": normalize_text(text),
                "center": (
                    (min(entry["left"]) + max(entry["right"])) // 2,
                    (min(entry["top"]) + max(entry["bottom"])) // 2,
                ),
            }
        )
    return results


def ocr_image_text_line(image, offset_x=0, offset_y=0, scale=1, config="--oem 3 --psm 7"):
    import pytesseract

    text = pytesseract.image_to_string(image, config=config).strip()
    normalized = normalize_text(text)
    if not normalized:
        return None
    return {
        "text": text,
        "normalized": normalized,
        "center": (offset_x + image.width // (2 * scale), offset_y + image.height // (2 * scale)),
    }


def crop_region_fraction(region, center, size):
    import pyautogui

    left, top, width, height = region
    crop_width = int(width * size[0])
    crop_height = int(height * size[1])
    crop_left = int(left + width * center[0] - crop_width / 2)
    crop_top = int(top + height * center[1] - crop_height / 2)
    return (crop_left, crop_top, crop_width, crop_height), pyautogui.screenshot(region=(crop_left, crop_top, crop_width, crop_height))


def ocr_expected_control(region, center, size=(0.70, 0.09)):
    if not region:
        return []
    crop_region, image = crop_region_fraction(region, center, size)
    offset_x, offset_y = crop_region[0], crop_region[1]
    lines = []
    seen = set()
    for variant_name, variant, scale in minecraft_ocr_variants(image):
        if variant_name not in ("resized", "contrast", "bright-text", "medium-text"):
            continue
        config = "--oem 3 --psm 7"
        line = ocr_image_text_line(variant, offset_x, offset_y, scale, config)
        if not line:
            continue
        key = (line["normalized"], line["center"], variant_name, config)
        if key in seen:
            continue
        seen.add(key)
        line["variant"] = f"{variant_name}/7"
        lines.append(line)
    return lines


def ocr_screen(region=None):
    import pyautogui

    offset_x = 0
    offset_y = 0
    if region:
        offset_x, offset_y = region[0], region[1]
        image = pyautogui.screenshot(region=region)
    else:
        image = pyautogui.screenshot()

    all_lines = []
    seen = set()
    for variant_name, variant, scale in minecraft_ocr_variants(image):
        for line in ocr_image_lines(variant, offset_x, offset_y, scale):
            key = (line["normalized"], line["center"])
            if line["normalized"] and key not in seen:
                seen.add(key)
                line["variant"] = variant_name
                all_lines.append(line)
    return all_lines


def easyocr_screen(region=None):
    try:
        import easyocr
        import numpy
        import pyautogui
    except ImportError:
        return []

    if not hasattr(easyocr_screen, "_reader"):
        easyocr_screen._reader = easyocr.Reader(["en"], gpu=False)

    offset_x = 0
    offset_y = 0
    if region:
        offset_x, offset_y = region[0], region[1]
        image = pyautogui.screenshot(region=region)
    else:
        image = pyautogui.screenshot()
    image = numpy.array(image)
    results = []
    try:
        read_results = easyocr_screen._reader.readtext(image)
    except Exception as error:
        print(f"    Warning: EasyOCR failed and will be skipped for this screen: {error}")
        return []
    for box, text, confidence in read_results:
        if confidence < 0.25:
            continue
        xs = [point[0] for point in box]
        ys = [point[1] for point in box]
        results.append(
            {
                "text": str(text),
                "normalized": normalize_text(text),
                "center": (offset_x + int((min(xs) + max(xs)) / 2), offset_y + int((min(ys) + max(ys)) / 2)),
                "variant": "easyocr",
            }
        )
    return results


def all_ocr_lines(region=None):
    lines = ocr_screen(region)
    if USE_EASYOCR and has_easyocr():
        lines.extend(easyocr_screen(region))
    return lines


def calibration_ocr_lines(region=None):
    lines = []
    if has_easyocr():
        lines.extend(easyocr_screen(region))
    lines.extend(ocr_screen(region))
    return lines


def relative_center(region, point):
    left, top, width, height = region
    return ((point[0] - left) / width, (point[1] - top) / height)


def find_calibration_candidate(region, control_name, phrases, expected=None, bounds=None, reject=(), version=None):
    wanted = [normalize_text(phrase) for phrase in phrases]
    rejected = [normalize_text(value) for value in reject]
    candidates = []
    for line in calibration_ocr_lines(region):
        text = line.get("normalized", "")
        if not text or any(value and value in text for value in rejected):
            continue
        if not any(ocr_text_matches(text, phrase) for phrase in wanted):
            continue
        rx, ry = relative_center(region, line["center"])
        if bounds:
            min_x, max_x, min_y, max_y = bounds
            if rx < min_x or rx > max_x or ry < min_y or ry > max_y:
                continue
        distance = 0
        if expected:
            distance = abs(rx - expected[0]) + abs(ry - expected[1])
        candidates.append((distance, line))
    if not candidates:
        return None
    candidates.sort(key=lambda item: item[0])
    line = candidates[0][1]
    remember_relative_location(region, control_name, line["center"], line.get("text"), line.get("variant"), version)
    return line


def ocr_text_matches(text, phrase):
    if not text or not phrase:
        return False
    if phrase == "world":
        if any(alias in text for alias in ("worldname", "worldharme", "worldtype", "newworld", "createworld")):
            return False
        return text == "world" or (len(text) <= 8 and difflib.SequenceMatcher(None, text, phrase).ratio() >= 0.72)
    if phrase in text:
        return True
    if phrase == "singleplayer":
        if any(alias in text for alias in ("singleplayer", "sinsleplayer", "iisleplayer", "isleplayer", "leplayer")):
            return "multi" not in text
        if "sinsle" in text or ("player" in text and any(part in text for part in ("single", "sngle", "slnsle", "iisle", "isle"))):
            return "multi" not in text
    if phrase == "multiplayer":
        if any(alias in text for alias in ("multiplayer", "aultser", "tlser")):
            return True
    if phrase == "minecraftrealms":
        if "realms" in text or "fealms" in text or "healms" in text:
            return True
    return difflib.SequenceMatcher(None, text, phrase).ratio() >= 0.78


def print_ocr_debug(region=None, limit=40):
    lines = all_ocr_lines(region)
    print(f"    OCR debug: {len(lines)} line(s) recognized.")
    for line in lines[:limit]:
        print(f"      [{line.get('variant', 'ocr')}] {line['text']} -> {line['normalized']} @ {line['center']}")
    if region:
        expected = {
            "main-singleplayer": (0.5, 0.515),
            "main-multiplayer": (0.5, 0.60),
            "main-realms": (0.5, 0.69),
            "create-world": (0.5, 0.90),
            "game-mode/world-type": (0.5, 0.38),
            "world-tab": (0.50, 0.19),
        }
        for label, center in expected.items():
            crop_lines = ocr_expected_control(region, center)
            if crop_lines:
                print(f"    OCR debug targeted {label}:")
                for line in crop_lines[:8]:
                    print(f"      [{line.get('variant', 'ocr')}] {line['text']} -> {line['normalized']} @ {line['center']}")


def find_ocr_phrase(phrases, region=None):
    wanted = [normalize_text(phrase) for phrase in phrases]
    if region:
        expected_centers = [
            (0.5, 0.58),
            (0.5, 0.72),
            (0.5, 0.515),
            (0.5, 0.60),
            (0.5, 0.69),
            (0.5, 0.90),
            (0.5, 0.38),
            (0.50, 0.19),
        ]
        for center in expected_centers:
            for line in ocr_expected_control(region, center):
                for phrase in wanted:
                    if ocr_text_matches(line["normalized"], phrase):
                        return line
    lines = all_ocr_lines(region)
    for line in lines:
        for phrase in wanted:
            if ocr_text_matches(line["normalized"], phrase):
                return line
    return None


def find_expected_control_phrase(phrases, region, fallback, size=(0.70, 0.09)):
    wanted = [normalize_text(phrase) for phrase in phrases]
    for line in ocr_expected_control(region, fallback, size):
        for phrase in wanted:
            if ocr_text_matches(line["normalized"], phrase):
                return line
    return None


def scan_expected_controls_parallel(region, controls, max_workers=4):
    if not region or not controls:
        return {}

    def scan_one(name, spec):
        center = spec["center"]
        size = spec.get("size", (0.70, 0.09))
        phrases = [normalize_text(phrase) for phrase in spec.get("phrases", ())]
        lines = ocr_expected_control(region, center, size)
        for line in lines:
            if any(ocr_text_matches(line["normalized"], phrase) for phrase in phrases):
                return name, line
        return name, None

    matches = {}
    with concurrent.futures.ThreadPoolExecutor(max_workers=min(max_workers, len(controls))) as executor:
        futures = [executor.submit(scan_one, name, spec) for name, spec in controls.items()]
        for future in concurrent.futures.as_completed(futures):
            name, match = future.result()
            if match:
                matches[name] = match
    return matches


def find_expected_control_any(region, controls, order):
    matches = scan_expected_controls_parallel(region, controls)
    for name in order:
        if name in matches:
            return name, matches[name]
    return None, None


def click_ocr_phrase(phrases, label=None, retries=8, delay=1.0, region=None):
    import pyautogui

    for _ in range(retries):
        match = find_ocr_phrase(phrases, region)
        if match:
            x, y = match["center"]
            print(f"    Clicking {label or phrases[0]} at {x},{y}: {match['text']} [{match.get('variant', 'ocr')}]")
            pyautogui.click(x, y)
            return True
        time.sleep(0.25)
    print(f"    Could not find: {label or ', '.join(phrases)}")
    return False


def region_point(region, x_fraction, y_fraction):
    left, top, width, height = region
    return int(left + width * x_fraction), int(top + height * y_fraction)


def click_region_fraction(region, x_fraction, y_fraction, label):
    import pyautogui

    x, y = region_point(region, x_fraction, y_fraction)
    print(f"    Clicking {label} by window position at {x},{y}")
    pyautogui.click(x, y)


def click_ocr_or_region(phrases, label, region, fallback=None, retries=8, delay=1.0, size=(0.70, 0.09)):
    if region and fallback:
        for _ in range(max(1, retries)):
            match = find_expected_control_phrase(phrases, region, fallback, size)
            if match:
                x, y = match["center"]
                print(f"    Clicking {label} from targeted OCR at {x},{y}: {match['text']} [{match.get('variant', 'ocr')}]")
                import pyautogui

                pyautogui.click(x, y)
                return True
            time.sleep(0.25)
    if region and fallback:
        click_region_fraction(region, fallback[0], fallback[1], label)
        return True
    if click_ocr_phrase(phrases, label, retries=retries, delay=delay, region=region):
        return True
    return False


def click_calibrated_control(region, control_name, phrases, fallback, expected=None, bounds=None, reject=(), verbose=True, version=None, size=(0.70, 0.09)):
    if cached_relative_click(region, control_name, verbose, version):
        return True
    match = find_calibration_candidate(
        region,
        control_name,
        phrases,
        expected=expected or fallback,
        bounds=bounds,
        reject=reject,
        version=version,
    )
    if match:
        import pyautogui

        if verbose:
            print(f"    Clicking calibrated {control_name} at {match['center'][0]},{match['center'][1]}: {match['text']} [{match.get('variant', 'ocr')}]")
        pyautogui.click(*match["center"])
        return True
    return click_ocr_or_region(phrases, control_name, region, fallback=fallback, retries=10, size=size)


WORLD_TYPE_CENTERS = ((0.5, 0.38),)
WORLD_TYPE_CLICK_FALLBACK = (0.5, 0.38)


def find_expected_control_in_centers(region, phrases, centers, size=(0.70, 0.09)):
    wanted = [normalize_text(phrase) for phrase in phrases]
    for center in centers:
        for line in ocr_expected_control(region, center, size):
            for phrase in wanted:
                if ocr_text_matches(line["normalized"], phrase):
                    return line
    return None


def find_type_text_in_world_tab(region, version=None):
    match = find_calibration_candidate(
        region,
        "world_type",
        ("Type", "World Type", "World Type:", "Default", "Superflat", "Large Biomes", "Amplified", "Single Biome"),
        expected=WORLD_TYPE_CLICK_FALLBACK,
        bounds=(0.20, 0.80, 0.20, 0.70),
        reject=("Difficulty", "Game Mode", "World Name", "New World"),
        version=version,
    )
    if match:
        return match
    return find_expected_control_in_centers(
        region,
        ("Type", "World Type", "World Type:", "Default", "Superflat", "Large Biomes", "Amplified", "Single Biome"),
        WORLD_TYPE_CENTERS,
        size=(0.78, 0.10),
    )


def world_type_visible(region, version=None):
    return bool(find_type_text_in_world_tab(region, version))


def click_world_tab(region, verbose=True, version=None):
    if not region:
        return click_ocr_or_region(("World",), "World tab", region, fallback=None, retries=4, size=(0.20, 0.07))

    if cached_relative_click(region, "world_tab", verbose, version):
        time.sleep(0.35)
        return True

    match = find_calibration_candidate(
        region,
        "world_tab",
        ("World",),
        expected=(0.57, 0.15),
        bounds=(0.35, 0.80, 0.05, 0.35),
        reject=("World Name", "New World", "World Type", "Create World"),
        version=version,
    )
    if match:
        import pyautogui

        if verbose:
            print(f"    Clicking calibrated World tab at {match['center'][0]},{match['center'][1]}: {match['text']} [{match.get('variant', 'ocr')}]")
        pyautogui.click(*match["center"])
        time.sleep(0.35)
        return True

    # Do not OCR-match "World" here: Tesseract can isolate "World" from the
    # "New World" name field. Use the fixed tab-strip location that matched
    # the observed Minecraft create-world UI.
    for fallback in ((0.57, 0.12), (0.57, 0.15)):
        click_region_fraction(region, fallback[0], fallback[1], "World tab")
        time.sleep(0.35)
        if world_type_visible(region, version):
            return True
    if verbose:
        print("    Could not confirm World tab after coordinate click; continuing with fixed World Type coordinate.")
    return True


def click_create_button(region, verbose=True, version=None):
    if cached_relative_click(region, "create_world", verbose, version):
        return True
    phrases = ("Create", "Create New", "Create New World")
    match = find_calibration_candidate(
        region,
        "create_world",
        phrases,
        expected=(0.5, 0.90),
        bounds=(0.20, 0.80, 0.70, 0.98),
        version=version,
    )
    if match:
        import pyautogui

        if verbose:
            print(f"    Clicking calibrated Create at {match['center'][0]},{match['center'][1]}: {match['text']} [{match.get('variant', 'ocr')}]")
        pyautogui.click(*match["center"])
        return True
    if click_ocr_or_region(phrases, "Create", region, fallback=(0.5, 0.90), retries=10, size=(0.42, 0.09)):
        return True
    if verbose:
        print("    Could not click Create button.")
    return False


def recipe_advancement_dir():
    return Path(__file__).resolve().parents[1] / "minecraft" / "src" / "main" / "resources" / "data" / "dissolver_enhanced" / "advancement" / "recipes" / "misc"


def load_recipe_unlock_tests():
    tests = []
    directory = recipe_advancement_dir()
    if not directory.exists():
        return tests
    for path in sorted(directory.glob("*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue
        rewards = data.get("rewards", {}).get("recipes", [])
        if not rewards:
            continue
        trigger_items = []
        for criterion in data.get("criteria", {}).values():
            if criterion.get("trigger") != "minecraft:inventory_changed":
                continue
            for item_condition in criterion.get("conditions", {}).get("items", []):
                items = item_condition.get("items")
                if isinstance(items, str):
                    trigger_items.append(items)
                elif isinstance(items, list):
                    trigger_items.extend(item for item in items if isinstance(item, str))
        tests.append(
            {
                "advancement": f"dissolver_enhanced:recipes/misc/{path.stem}",
                "recipes": rewards,
                "trigger_items": sorted(set(trigger_items)),
            }
        )
    return tests


def latest_advancement_file(instance):
    saves_path = Path(instance["path"]) / "saves"
    if not saves_path.exists():
        return None
    files = []
    for pattern in (
        "*/players/advancements/*.json",  # Minecraft 26.1+
        "*/advancements/*.json",          # Legacy layout
    ):
        files.extend(path for path in saves_path.glob(pattern) if path.is_file())
    if not files:
        return None
    return max(files, key=lambda path: path.stat().st_mtime)


def check_recipe_advancements(instance, tests):
    path = latest_advancement_file(instance)
    if not path:
        return 0, len(tests), []
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return 0, len(tests), []
    passed = []
    for test in tests:
        if data.get(test["advancement"], {}).get("done") is True:
            passed.append(test["advancement"])
    return len(passed), len(tests), passed


def wait_for_recipe_advancements(instance, tests, timeout=20):
    deadline = time.time() + timeout
    best = (0, len(tests), [])
    while time.time() < deadline:
        result = check_recipe_advancements(instance, tests)
        if result[0] > best[0]:
            best = result
        if result[0] == result[1]:
            return result
        time.sleep(1.0)
    return best


def minecraft_instance_is_running(instance, stage=None, verbose=True):
    if minecraft_processes_for_instance(instance):
        return True
    if verbose:
        suffix = f" before {stage}" if stage else ""
        print(f"    Minecraft instance exited early{suffix}; stopping automation.")
    return False


def send_chat_line(pyautogui, message):
    pyautogui.press("t")
    time.sleep(0.5)
    pyautogui.write(message, interval=0.01)
    pyautogui.press("enter")


def pause_unpause_to_save(pyautogui, verbose=True, progress_callback=None, instance=None):
    if instance is not None and not minecraft_instance_is_running(instance, "pause/save", verbose):
        return False
    if verbose:
        print("    Pausing briefly to nudge singleplayer save.")
    progress_step(progress_callback, "pause to save")
    time.sleep(0.5)
    pyautogui.press("esc")
    time.sleep(2.0)
    if instance is not None and not minecraft_instance_is_running(instance, "unpause", verbose):
        return False
    pyautogui.press("esc")
    time.sleep(0.5)
    return True


def place_smoke_test_blocks(pyautogui, progress_callback=None, instance=None, verbose=True):
    if instance is not None and not minecraft_instance_is_running(instance, "smoke-test placement", verbose):
        return False
    progress_step(progress_callback, "look down")
    send_chat_line(pyautogui, "/tp @s ~ ~ ~ ~ 65")
    time.sleep(0.5)

    for slot in ("1", "2", "3"):
        if instance is not None and not minecraft_instance_is_running(instance, f"placing slot {slot}", verbose):
            return False
        progress_step(progress_callback, f"place slot {slot}")
        pyautogui.press(slot)
        time.sleep(0.2)
        pyautogui.click(button="right")
        time.sleep(0.3)
        if slot != "3":
            if instance is not None and not minecraft_instance_is_running(instance, "smoke-test movement", verbose):
                return False
            progress_step(progress_callback, "move right")
            send_chat_line(pyautogui, "/tp @s ~1 ~ ~ ~ 65")
            time.sleep(0.5)
    return True


def type_testing_started_message(instance, verbose=True, progress_callback=None):
    import pyautogui

    recipe_tests = load_recipe_unlock_tests()
    trigger_items = sorted({item for test in recipe_tests for item in test["trigger_items"]})
    startup_messages = ["Testing has started:"]
    smoke_test_commands = [
        "/give @s dissolver_enhanced:dissolver_block 1",
        "/give @s dissolver_enhanced:condenser_block 1",
        "/give @s dissolver_enhanced:materializer_block 1",
        "/give @s dissolver_enhanced:gold_emc_core 1",
        "/give @s minecraft:copper_ingot 64",
        "/give @s minecraft:cherry_log 64",
    ]
    recipe_commands = ["/recipe take @s *"]
    for item in trigger_items:
        recipe_commands.append(f"/give @s {item} 1")
    recipe_commands.append("/clear @s")

    if verbose:
        print("    Waiting for world load, then typing test-start commands.")
    progress_step(progress_callback, "waiting for world")
    time.sleep(5.0)
    for index, message in enumerate(startup_messages + recipe_commands):
        if index:
            time.sleep(0.5)
        if not minecraft_instance_is_running(instance, f"typing {message!r}", verbose):
            return False
        progress_step(progress_callback, message)
        send_chat_line(pyautogui, message)

    if not pause_unpause_to_save(pyautogui, verbose, progress_callback, instance):
        return False
    time.sleep(1.0)
    passed, total, _ = wait_for_recipe_advancements(instance, recipe_tests)
    result_message = f"Recipe test: {passed}/{total} recipes work"
    if not minecraft_instance_is_running(instance, "typing the recipe result", verbose):
        return False
    progress_step(progress_callback, result_message)
    send_chat_line(pyautogui, result_message)
    for message in smoke_test_commands:
        time.sleep(0.5)
        if not minecraft_instance_is_running(instance, f"typing {message!r}", verbose):
            return False
        progress_step(progress_callback, message)
        send_chat_line(pyautogui, message)
    if not place_smoke_test_blocks(pyautogui, progress_callback, instance, verbose):
        return False
    return total > 0 and passed == total


def wait_for_ocr_phrase(phrases, timeout=120, delay=2.0, progress_label=None, progress_interval=10, region=None):
    deadline = time.time() + timeout
    next_progress = time.time() + progress_interval
    while time.time() < deadline:
        match = find_ocr_phrase(phrases, region)
        if match:
            return match
        if progress_label and time.time() >= next_progress:
            remaining = max(0, int(deadline - time.time()))
            print(f"    {progress_label}; still waiting ({remaining}s left).")
            next_progress = time.time() + progress_interval
        time.sleep(delay)
    return None


def wait_for_expected_controls(region, controls, order, timeout=20, delay=0.5, progress_label=None):
    deadline = time.time() + timeout
    next_progress = time.time() + 10
    while time.time() < deadline:
        name, match = find_expected_control_any(region, controls, order)
        if match:
            return name, match
        if progress_label and time.time() >= next_progress:
            remaining = max(0, int(deadline - time.time()))
            print(f"    {progress_label}; still waiting ({remaining}s left).")
            next_progress = time.time() + 10
        time.sleep(delay)
    return None, None


def click_game_mode_control(region, phrases, verbose=True, version=None):
    if cached_relative_click(region, "game_mode", verbose, version):
        return True
    match = find_calibration_candidate(
        region,
        "game_mode",
        phrases,
        expected=(0.5, 0.38),
        bounds=(0.20, 0.80, 0.20, 0.55),
        reject=("World Type", "Difficulty", "World Name"),
        version=version,
    )
    if match:
        import pyautogui

        if verbose:
            print(f"    Clicking calibrated Game Mode at {match['center'][0]},{match['center'][1]}: {match['text']} [{match.get('variant', 'ocr')}]")
        pyautogui.click(*match["center"])
        return True
    return click_ocr_or_region(phrases, "Game Mode", region, fallback=(0.5, 0.38), retries=10)


def set_game_mode_creative(region, version=None, verbose=True):
    creative_phrases = ("Game Mode: Creative", "Game Mode Creative")
    game_mode_phrases = (
        "Game Mode: Survival",
        "Game Mode Survival",
        "Game Mode: Hardcore",
        "Game Mode Hardcore",
        "Game Mode: Adventure",
        "Game Mode Adventure",
        "Game Mode:",
        "Game Mode",
    )
    for _ in range(8):
        controls = {
            "creative": {"phrases": creative_phrases, "center": (0.5, 0.38)},
        }
        _, creative_match = find_expected_control_any(region, controls, ("creative",))
        if creative_match or find_ocr_phrase(creative_phrases, region):
            if verbose:
                print("    Game Mode is Creative.")
            return True
        break
    cached_clicks = cached_control_clicks("game_mode", version)
    if cached_clicks > 0:
        for _ in range(cached_clicks):
            click_game_mode_control(region, game_mode_phrases, verbose, version)
            time.sleep(0.35)
        _, creative_match = find_expected_control_any(region, controls, ("creative",))
        if creative_match or find_ocr_phrase(creative_phrases, region):
            if verbose:
                print(f"    Game Mode set to Creative using cached {cached_clicks} click(s).")
            return True
    for attempt in range(8):
        click_game_mode_control(region, game_mode_phrases, verbose, version)
        time.sleep(0.35)
        _, creative_match = find_expected_control_any(region, controls, ("creative",))
        if creative_match or find_ocr_phrase(creative_phrases, region):
            remember_control_clicks("game_mode", version, attempt + 1)
            if verbose:
                print("    Game Mode is Creative.")
            return True
    if verbose:
        print("    Could not confirm Game Mode: Creative.")
    return False


def click_world_type_control(region, phrases, attempt, verbose=True, version=None):
    if cached_relative_click(region, "world_type", verbose, version):
        return True
    match = find_type_text_in_world_tab(region, version)
    if match:
        import pyautogui

        x, y = match["center"]
        if verbose:
            print(f"    Clicking World Type at {x},{y}: {match['text']} [{match.get('variant', 'ocr')}]")
        pyautogui.click(x, y)
        return True

    click_region_fraction(region, WORLD_TYPE_CLICK_FALLBACK[0], WORLD_TYPE_CLICK_FALLBACK[1], "World Type")
    return True


def set_world_type_superflat(region, verbose=True, version=None):
    superflat_phrases = ("World Type: Superflat", "World Type Superflat")
    world_type_phrases = (
        "Type",
        "World Type:",
        "World Type",
        "World Type: Default",
        "World Type Default",
        "World Type: Large Biomes",
        "World Type Large Biomes",
        "World Type: Amplified",
        "World Type Amplified",
        "World Type: Single Biome",
        "World Type Single Biome",
    )
    if find_expected_control_in_centers(region, superflat_phrases, WORLD_TYPE_CENTERS) or find_ocr_phrase(superflat_phrases, region):
        if verbose:
            print("    World Type is already Superflat.")
        return True
    cached_clicks = cached_control_clicks("world_type", version)
    if cached_clicks > 0:
        for _ in range(cached_clicks):
            click_world_type_control(region, world_type_phrases, 0, verbose, version)
            time.sleep(0.35)
        if find_expected_control_in_centers(region, superflat_phrases, WORLD_TYPE_CENTERS) or find_ocr_phrase(superflat_phrases, region):
            if verbose:
                print(f"    World Type set to Superflat using cached {cached_clicks} click(s).")
            return True
    for attempt in range(12):
        click_world_type_control(region, world_type_phrases, attempt, verbose, version)
        time.sleep(0.35)
        if find_expected_control_in_centers(region, superflat_phrases, WORLD_TYPE_CENTERS) or find_ocr_phrase(superflat_phrases, region):
            remember_control_clicks("world_type", version, attempt + 1)
            if verbose:
                print("    World Type set to Superflat.")
            return True
    if verbose:
        print("    Could not confirm World Type: Superflat; continuing after fixed World Type clicks.")
    return True


def run_cached_menu_sequence(region, version, instance, verbose=True, progress_callback=None):
    if not cached_menu_sequence_available(version):
        return False

    game_mode_clicks = cached_control_clicks("game_mode", version)
    world_type_clicks = cached_control_clicks("world_type", version)
    if verbose:
        print(f"    Using cached menu sequence for Minecraft {version}.")

    progress_step(progress_callback, "singleplayer")
    cached_relative_click(region, "singleplayer", verbose, version)
    time.sleep(5.0)

    progress_step(progress_callback, "create world screen")
    if not cached_relative_click(region, "open_create_world", verbose, version):
        x, y = region_point(region, 0.5, 0.90)
        if verbose:
            print(f"    Clicking cached fallback open_create_world at {x},{y}")
        import pyautogui

        pyautogui.click(x, y)
        remember_relative_location(region, "open_create_world", (x, y), "Create New World", "fallback", version)
    time.sleep(0.5)

    for _ in range(game_mode_clicks):
        progress_step(progress_callback, "game mode")
        cached_relative_click(region, "game_mode", verbose, version)
        time.sleep(0.5)

    progress_step(progress_callback, "world tab")
    cached_relative_click(region, "world_tab", verbose, version)
    time.sleep(0.5)

    for _ in range(world_type_clicks):
        progress_step(progress_callback, "world type")
        cached_relative_click(region, "world_type", verbose, version)
        time.sleep(0.5)

    progress_step(progress_callback, "create world")
    cached_relative_click(region, "create_world", verbose, version)
    return type_testing_started_message(instance, verbose, progress_callback)


def wait_for_cached_singleplayer_ready(region, verbose=True, timeout=60):
    singleplayer_phrases = [normalize_text(value) for value in ("Singleplayer", "Single Player")]
    narrator_phrases = [normalize_text(value) for value in ("Narrator: ON", "Narrator ON", "Narrator: OFF", "Narrator OFF", "Narrator: Yes", "Narrator Yes", "Narrator: No", "Narrator No")]
    deadline = time.time() + timeout
    last_progress = 0

    while time.time() < deadline:
        lines = easyocr_screen(region) if has_easyocr() else all_ocr_lines(region)
        for line in lines:
            text = line.get("normalized", "")
            if any(ocr_text_matches(text, phrase) for phrase in singleplayer_phrases):
                if verbose:
                    print(f"    Singleplayer menu ready: {line.get('text')} [{line.get('variant', 'ocr')}]")
                return True
            if any(ocr_text_matches(text, phrase) for phrase in narrator_phrases):
                if verbose:
                    print("    Narrator screen found before main menu; using normal OCR flow.")
                return False

        now = time.time()
        if verbose and now - last_progress >= 10:
            print("    No Singleplayer menu text found, still waiting...")
            last_progress = now
        time.sleep(0.25)

    if verbose:
        print("    Timed out waiting for Singleplayer before cached clicks; using normal OCR flow.")
    return False


def automate_minecraft_menus(instance, apply, ocr_debug=False, verbose=True, progress_callback=None):
    game_version = instance.get("de_game_version") or instance.get("game_version") or instance.get("version") or "default"
    if verbose:
        print(f"    {'Running' if apply else '[dry-run] Would run'} live menu automation.")
    if not apply:
        if verbose:
            print("    [dry-run] Would handle narrator screen, then Singleplayer/Create New World/Creative/Superflat.")
        return False

    missing = require_menu_automation_backend()
    if missing:
        print(f"    Missing automation backend: {', '.join(missing)}")
        automation_install_help(missing)
        return False

    import pyautogui

    pyautogui.PAUSE = 0.0
    progress_step(progress_callback, "waiting for window")
    window_info = wait_for_minecraft_window()
    if window_info:
        region = window_info["region"]
        if verbose:
            print(f"    OCR limited to {window_info['process']} window region: {region}")
    else:
        if verbose:
            print("    Minecraft window was not detected before timeout; failing this instance.")
        return False
    progress_step(progress_callback, "waiting for menu")
    if region and cached_menu_sequence_available(game_version) and wait_for_cached_singleplayer_ready(region, verbose):
        return run_cached_menu_sequence(region, game_version, instance, verbose, progress_callback)
    if verbose:
        print("    Waiting for Minecraft menu text...")
    if ocr_debug:
        print_ocr_debug(region)
    narrator_on_phrases = ("Narrator: ON", "Narrator ON", "Narrator: Yes", "Narrator Yes")
    narrator_off_phrases = ("Narrator: OFF", "Narrator OFF", "Narrator: No", "Narrator No")
    main_controls = {
        "narrator_on": {"phrases": narrator_on_phrases, "center": (0.5, 0.58)},
        "narrator_off": {"phrases": narrator_off_phrases, "center": (0.5, 0.58)},
        "singleplayer": {"phrases": ("Singleplayer", "Single Player"), "center": (0.5, 0.515)},
    }
    if region:
        first_name, first_screen = wait_for_expected_controls(
            region,
            main_controls,
            ("narrator_on", "narrator_off", "singleplayer"),
            timeout=45,
            progress_label="No narrator or Singleplayer menu text found" if verbose else None,
        )
    else:
        first_name = None
        first_screen = wait_for_ocr_phrase(
            narrator_on_phrases + narrator_off_phrases + ("Singleplayer",),
            timeout=180,
            progress_label="No narrator or Singleplayer menu text found" if verbose else None,
            region=region,
        )
    if not first_screen:
        if ocr_debug:
            print_ocr_debug(region)
        if verbose:
            print("    No recognizable Minecraft menu text found; failing this instance.")
        return False

    if first_name in ("narrator_on", "narrator_off") or find_expected_control_any(region, main_controls, ("narrator_on", "narrator_off"))[1]:
        if first_name == "narrator_on" or find_expected_control_any(region, main_controls, ("narrator_on",))[1]:
            click_calibrated_control(
                region,
                "narrator_toggle",
                narrator_on_phrases,
                fallback=(0.5, 0.58),
                bounds=(0.25, 0.75, 0.45, 0.70),
                verbose=verbose,
                version=game_version,
            )
            wait_for_expected_controls(region, main_controls, ("narrator_off",), timeout=10)
        click_calibrated_control(
            region,
            "continue",
            ("Continue",),
            fallback=(0.5, 0.72),
            bounds=(0.25, 0.75, 0.60, 0.85),
            verbose=verbose,
            version=game_version,
        )
        time.sleep(1)

    if click_calibrated_control(
        region,
        "singleplayer",
        ("Singleplayer", "Single Player"),
        fallback=(0.5, 0.515),
        bounds=(0.20, 0.80, 0.40, 0.65),
        reject=("Multiplayer",),
        verbose=verbose,
        version=game_version,
    ):
        progress_step(progress_callback, "singleplayer")
        time.sleep(5)

    create_controls = {
        "game_mode": {"phrases": ("Game Mode", "Game Mode: Survival", "Game Mode Survival", "Game Mode: Creative", "Game Mode Creative"), "center": (0.5, 0.38)},
        "create_world": {"phrases": ("Create", "Create New World", "Create New"), "center": (0.5, 0.90)},
    }
    if not wait_for_expected_controls(region, create_controls, ("game_mode", "create_world"), timeout=8, delay=0.5)[1]:
        if verbose:
            print("    Create-world screen was not detected yet.")
    if not find_expected_control_any(region, create_controls, ("game_mode",))[1]:
        click_calibrated_control(
            region,
            "open_create_world",
            ("Create", "Create New World", "Create New"),
            fallback=(0.5, 0.90),
            bounds=(0.20, 0.80, 0.75, 0.98),
            verbose=verbose,
            version=game_version,
        )
        progress_step(progress_callback, "create world screen")
        time.sleep(0.5)

    progress_step(progress_callback, "game mode")
    set_game_mode_creative(region, game_version, verbose)

    world_type_ready = False
    if click_world_tab(region, verbose, game_version):
        progress_step(progress_callback, "world tab")
        time.sleep(0.5)
        progress_step(progress_callback, "world type")
        world_type_ready = set_world_type_superflat(region, verbose, game_version)
    elif verbose:
        print("    Skipping World Type selection because the World tab was not confirmed.")

    if world_type_ready:
        if click_create_button(region, verbose, game_version):
            progress_step(progress_callback, "create world")
            return type_testing_started_message(instance, verbose, progress_callback)
    elif verbose:
        print("    Skipping Create New World because World Type: Superflat was not confirmed.")
    return False


def launch_instance(instance, apply, automate_menus=False, ocr_debug=False, verbose=True, progress_callback=None):
    url = curseforge_launch_url(instance)
    if verbose:
        print(f"  {'Launching' if apply else '[dry-run] Would launch'} {instance['folder']}")
        print(f"    URL: {url}")
    if apply:
        progress_step(progress_callback, "launching")
        open_url(url)
    if automate_menus:
        # redirect_stdout is process-wide, so do not use it while the compact
        # progress renderer is active in another thread.
        with quiet_stdout(not verbose and not ocr_debug and progress_callback is None):
            if automate_minecraft_menus(instance, apply, ocr_debug, verbose, progress_callback):
                kill_minecraft_processes_for_instance(instance, verbose)
                return True
            kill_minecraft_processes_for_instance(instance, verbose=False)
            return False
    progress_step(progress_callback, "waiting for close")
    wait_for_instance_to_close(instance, apply, verbose=verbose)
    return not automate_menus


def run_testing_phase(instances, build_branches, apply, selection_text=None, gradle_task="assemble", install_loader_api=True, automate_menus=False, ocr_debug=False, debug=False):
    verbose_setup = debug or not apply
    selected_branches = prompt_for_build_branches(build_branches, selection_text, verbose=verbose_setup)
    repo_root = Path(__file__).resolve().parents[1]
    test_cache = load_test_cache()
    if verbose_setup:
        print_section("Testing Phase")
    if not selected_branches:
        print("No branches selected.")
        return

    setup_plans = []
    seen_setup_keys = set()
    for branch_index, branch in enumerate(selected_branches, 1):
        setup_key = (branch["loader"], branch["version"])
        if setup_key in seen_setup_keys:
            print(f"==> [{branch_index}/{len(selected_branches)}] {branch['name']}")
            print(f"  Duplicate loader/version setup for {branch_loader_display(branch['loader'])} {branch['version']}; skipping duplicate build.")
            continue
        seen_setup_keys.add(setup_key)
        branch_instances = sorted(de_instances_for_branch(instances, branch), key=lambda item: version_sort_key(item["de_game_version"]))
        loader_version = f"{branch_loader_display(branch['loader'])} {branch['version']}"
        if not branch_instances:
            print(f"==> [{branch_index}/{len(selected_branches)}] {branch['name']} ({loader_version})")
            print("  No matching CurseForge DE instances found; skipping.")
            continue
        setup_plans.append(
            {
                "index": branch_index,
                "total": len(selected_branches),
                "branch": branch,
                "instances": branch_instances,
                "loader_version": loader_version,
                "setup_steps": setup_step_count(branch_instances, install_loader_api),
                "test_count": branch_test_count({"instances": branch_instances}),
                "cache_enabled": bool(apply and automate_menus),
            }
        )

    if not setup_plans:
        print("No branch setups to run.")
        return

    launch_plans = []

    def run_apply_setups_with_rich():
        try:
            from rich.progress import BarColumn, Progress, TextColumn, TimeElapsedColumn
        except ImportError:
            return False

        task_by_index = {}
        with Progress(
            TextColumn("{task.description}", justify="left"),
            BarColumn(bar_width=24),
            TextColumn("{task.completed:.0f}/{task.total:.0f}"),
            TextColumn("{task.fields[status]}"),
            TimeElapsedColumn(),
            transient=False,
        ) as progress:
            for plan in setup_plans:
                task_by_index[plan["index"]] = progress.add_task(
                    plan["branch"]["name"],
                    total=plan["setup_steps"],
                    status="building",
            )
            progress_events = queue.Queue()
            # Every loader includes the same Common composite build. Keep Gradle
            # builds serialized so the shared source tree is never read or cached
            # concurrently, especially from iCloud-backed workspaces.
            setup_workers = 1
            executor = concurrent.futures.ThreadPoolExecutor(max_workers=setup_workers)
            try:
                future_to_plan = {executor.submit(setup_branch, plan, progress_events): plan for plan in setup_plans}
                pending = set(future_to_plan)
                while pending:
                    try:
                        while True:
                            plan_index, amount, status = progress_events.get_nowait()
                            progress.advance(task_by_index[plan_index], amount)
                            progress.update(task_by_index[plan_index], status=status)
                    except queue.Empty:
                        pass
                    done, pending = concurrent.futures.wait(
                        pending,
                        timeout=0.25,
                        return_when=concurrent.futures.FIRST_COMPLETED,
                    )
                    try:
                        while True:
                            plan_index, amount, status = progress_events.get_nowait()
                            progress.advance(task_by_index[plan_index], amount)
                            progress.update(task_by_index[plan_index], status=status)
                    except queue.Empty:
                        pass
                    for future in done:
                        plan = future_to_plan[future]
                        try:
                            result = future.result()
                            if result:
                                launch_plans.append(result)
                                status = f"ready ({len(plan['instances'])} instances)"
                            else:
                                status = "skipped"
                        except Exception:
                            status = "failed"
                        progress.update(task_by_index[plan["index"]], completed=plan["setup_steps"], status=status)
                try:
                    while True:
                        plan_index, amount, status = progress_events.get_nowait()
                        progress.advance(task_by_index[plan_index], amount)
                        progress.update(task_by_index[plan_index], status=status)
                except queue.Empty:
                    pass
            except KeyboardInterrupt:
                executor.shutdown(wait=False, cancel_futures=True)
                raise
            else:
                executor.shutdown(wait=True)
        return True

    def setup_branch(plan, progress_events=None):
        branch = plan["branch"]
        branch_instances = plan["instances"]
        loader_version = plan["loader_version"]
        verbose = debug or not apply
        if verbose:
            print(f"==> [{plan['index']}/{plan['total']}] {branch['name']} ({loader_version})")
            print(f"==> Building {branch['name']}")
        artifact = build_branch_artifact(branch, apply, gradle_task, verbose=verbose)
        if not artifact:
            if not apply:
                artifact = Path(branch["worktree_path"]) / "build" / "libs" / "<newest built dissolver-enhanced jar>"
                print(f"  [dry-run] Would use artifact produced under: {artifact.parent}")
            else:
                return None
        else:
            if verbose:
                print(f"  Artifact: {artifact}")

        if apply and not Path(artifact).is_file():
            return None

        if apply:
            try:
                plan["jar_sha256"] = artifact_sha256(artifact)
            except OSError as error:
                failure_log = write_test_failure_log(
                    f"{branch['name']}-hash-failure",
                    f"Could not hash built artifact {artifact}: {error}\n",
                )
                branch.setdefault("test_failure_logs", []).append(failure_log)
                return None
            if plan["cache_enabled"]:
                cached_result = cached_pass_result(
                    test_cache,
                    plan,
                    plan["jar_sha256"],
                    plan["test_count"],
                )
                if cached_result:
                    plan["cached_result"] = cached_result
                    if progress_events:
                        progress_events.put((plan["index"], plan["setup_steps"], "cached pass"))
                    elif verbose:
                        print(
                            f"  Cache hit: {branch_cache_display(branch)} jar already passed "
                            f"{plan['test_count']} tests; skipping launcher setup."
                        )
                    return plan

                cached_instances, pending_instances = partition_cached_instances(
                    test_cache,
                    plan,
                    plan["jar_sha256"],
                )
                if cached_instances:
                    plan["all_instances"] = list(plan["instances"])
                    plan["cached_instances"] = cached_instances
                    plan["instances"] = pending_instances
                    branch_instances = pending_instances
                    if verbose:
                        versions = ", ".join(instance_cache_key(item) for item in cached_instances)
                        print(f"  Partial cache hit; skipping passed versions: {versions}")

        if progress_events:
            progress_events.put((plan["index"], 1, "preparing"))
        if verbose:
            print(f"==> Preparing {loader_version} instances")

        def progress_factory(instance):
            def report(status):
                progress_events.put((plan["index"], 1, status))
            return report

        install_artifacts_parallel(
            artifact,
            branch_instances,
            apply,
            install_loader_api,
            label=f"Prepare {loader_version}",
            show_progress=verbose,
            progress_factory=progress_factory if progress_events else None,
        )
        return plan

    if apply and not debug:
        if not run_apply_setups_with_rich():
            progress_events = queue.Queue()
            progress_display = BranchProgressDisplay(setup_plans)
            # Every loader includes the same Common composite build. Keep Gradle
            # builds serialized so the shared source tree is never read or cached
            # concurrently, especially from iCloud-backed workspaces.
            setup_workers = 1
            executor = concurrent.futures.ThreadPoolExecutor(max_workers=setup_workers)
            try:
                future_to_plan = {executor.submit(setup_branch, plan, progress_events): plan for plan in setup_plans}
                pending = set(future_to_plan)
                while pending:
                    try:
                        while True:
                            plan_index, amount, status = progress_events.get_nowait()
                            plan = next(item for item in setup_plans if item["index"] == plan_index)
                            progress_display.advance(plan, amount, status)
                    except queue.Empty:
                        pass
                    done, pending = concurrent.futures.wait(
                        pending,
                        timeout=0.25,
                        return_when=concurrent.futures.FIRST_COMPLETED,
                    )
                    try:
                        while True:
                            plan_index, amount, status = progress_events.get_nowait()
                            plan = next(item for item in setup_plans if item["index"] == plan_index)
                            progress_display.advance(plan, amount, status)
                    except queue.Empty:
                        pass
                    for future in done:
                        plan = future_to_plan[future]
                        try:
                            result = future.result()
                            if result:
                                launch_plans.append(result)
                                status = f"ready ({len(plan['instances'])} instances)"
                            else:
                                status = "skipped"
                        except Exception:
                            status = "failed"
                        progress_display.update(plan, plan["setup_steps"], status)
                try:
                    while True:
                        plan_index, amount, status = progress_events.get_nowait()
                        plan = next(item for item in setup_plans if item["index"] == plan_index)
                        progress_display.advance(plan, amount, status)
                except queue.Empty:
                    pass
            except KeyboardInterrupt:
                executor.shutdown(wait=False, cancel_futures=True)
                raise
            else:
                executor.shutdown(wait=True)
    else:
        for plan in setup_plans:
            result = setup_branch(plan)
            if result:
                launch_plans.append(result)

    launch_plans.sort(key=lambda item: item["index"])
    completed_by_branch = {}

    def finish_branch(result, plan):
        if apply:
            finalize_branch_test_result(result, plan, test_cache, repo_root)
        else:
            result["passed_tests"] = int(plan.get("test_count", 0))
        completed_by_branch[plan["branch"]["name"]] = result

    def finish_instance(_result, plan, instance, passed):
        if not (apply and plan.get("cache_enabled") and passed):
            return
        record_cached_instance_pass(
            test_cache,
            plan,
            instance,
            plan["jar_sha256"],
            estimated_instance_steps(instance),
        )

    prepared_names = {plan["branch"]["name"] for plan in launch_plans}
    for plan in setup_plans:
        if plan["branch"]["name"] not in prepared_names:
            failure_result = complete_branch_test_results([plan], [])[0]
            finish_branch(failure_result, plan)

    runnable_plans = []
    for plan in launch_plans:
        cached_result = plan.get("cached_result")
        if cached_result:
            finish_branch(cached_result, plan)
        else:
            runnable_plans.append(plan)
    launch_plans = runnable_plans

    if not launch_plans:
        print("No launcher tests need to run; branches were cached or failed during setup.")
        return [
            completed_by_branch[plan["branch"]["name"]]
            for plan in setup_plans
            if plan["branch"]["name"] in completed_by_branch
        ]

    if apply and not debug:
        clear_terminal_scrollback()

    if apply and not debug:
        launch_all_instances_compact(
            launch_plans,
            apply,
            automate_menus,
            ocr_debug,
            on_plan_complete=finish_branch,
            on_instance_complete=finish_instance,
        )
    else:
        launch_results_by_branch = initialize_branch_test_results(launch_plans)
        for plan in launch_plans:
            loader_version = plan["loader_version"]
            branch_instances = plan["instances"]
            print(f"==> Launching {loader_version} instances")
            print("  Launch order:")
            for instance in branch_instances:
                print(f"    {instance['de_game_version']}: {instance['folder']}")
            for instance in branch_instances:
                passed = launch_instance(instance, apply, automate_menus, ocr_debug)
                failure_logs = [] if passed else copy_instance_logs_for_failure(instance, instance_test_label(instance))
                if not passed and not failure_logs:
                    failure_logs = [write_test_failure_log(
                        f"{instance_test_label(instance)}-failure",
                        "Local CurseForge testing failed, but Minecraft did not produce a readable log file.\n",
                    )]
                record_branch_instance_result(launch_results_by_branch, plan, passed, failure_logs)
                finish_instance(launch_results_by_branch[plan["branch"]["name"]], plan, instance, passed)
            emit_branch_test_result(launch_results_by_branch, plan, finish_branch)

    return [
        completed_by_branch[plan["branch"]["name"]]
        for plan in setup_plans
        if plan["branch"]["name"] in completed_by_branch
    ]


def report_instances(instances, groups):
    print_section("Instance Scan")
    print(f"Found {len(instances)} instance folder(s).")
    grouped = {}
    for instance in instances:
        if instance.get("group_id"):
            grouped[instance["group_id"]] = grouped.get(instance["group_id"], 0) + 1
    if grouped and groups:
        print("Detected CurseForge groups:")
        for group_id, count in sorted(grouped.items(), key=lambda item: group_path(item[0], groups).lower()):
            print(f"  {group_path(group_id, groups) or group_id}: {count}")

    de_instances = [instance for instance in instances if instance.get("de_version")]
    if de_instances:
        print("Detected DE instances/groups:")
        for instance in de_instances:
            suffix = f" in {group_path(instance['group_id'], groups)}" if groups and instance.get("group_id") else ""
            print(f"  {instance['name']} ({instance['de_version']}) -> {instance['folder']}{suffix}")
    return de_instances


def parse_args(argv):
    parser = argparse.ArgumentParser(
        description="Scan and safely clean CurseForge testing instances for Dissolver Enhanced."
    )
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--dry-run", action="store_true", default=True, help="Print planned changes. This is the default.")
    mode.add_argument("--apply", action="store_true", help="Apply profile/group metadata updates and cleanup.")
    parser.add_argument("--config", type=Path, default=CONFIG_FILE, help=f"Path to config.data. Default: {CONFIG_FILE}")
    parser.add_argument("--full-run", action="store_true", help="Run the full workflow: apply setup, cleanup, build/install all selected branches, download loader APIs, automate menus, and launch in order.")
    parser.add_argument("--skip-cleanup", action="store_true", help="Apply or preview profile/group work without DE folder cleanup.")
    parser.add_argument("--test-instances", action="store_true", help="After setup/cleanup, build selected branches, install jars, write tester config, and launch instances in version order.")
    parser.add_argument("--test-branches", help="Selection for --test-instances: 'all' or comma-separated menu numbers, for non-interactive runs.")
    parser.add_argument("--gradle-task", default="assemble", help="Gradle task for --test-instances. Default: assemble. Use build to include unit tests.")
    parser.add_argument("--skip-loader-api", action="store_true", help="Do not download loader API dependency mods during --test-instances.")
    parser.add_argument("--automate-menus", action="store_true", help="Use optional OCR/click automation after each Minecraft launch.")
    parser.add_argument("--use-easyocr", action="store_true", help="Use EasyOCR during menu automation. Slower, but can help with Minecraft's font.")
    parser.add_argument("--ocr-debug", action="store_true", help="Print recognized OCR text lines during menu automation.")
    parser.add_argument("--debug", action="store_true", help="Print detailed scan, profile, cleanup, and setup diagnostics.")
    return parser.parse_args(argv)


def main(argv=None):
    global USE_EASYOCR
    args = parse_args(argv or sys.argv[1:])
    if args.full_run:
        args.apply = True
        args.test_instances = True
        args.automate_menus = True
        if not args.test_branches:
            args.test_branches = "all"
    apply = bool(args.apply)
    quiet = apply and not args.debug
    USE_EASYOCR = True

    reset_test_results_file()

    if quiet:
        clear_terminal_scrollback()

    if not quiet:
        print("Dissolver Enhanced CurseForge testing instance manager")
        print(f"Mode: {'full-run' if args.full_run else 'apply' if apply else 'dry-run'}")

    with quiet_stdout(quiet):
        config = load_or_create_config(args.config)
        instances_path = locate_curseforge_instances(config)
    if not instances_path:
        print("Could not locate CurseForge Minecraft instances. Run with --debug for details or set curseforge_instances_path in config.data.")
        return 2
    with quiet_stdout(quiet):
        groups_path = locate_curseforge_groups(config)
        game_instances_path = locate_curseforge_game_instances(config, groups_path)
        groups = load_groups(groups_path)

    instances = scan_instances(instances_path)
    if quiet:
        de_instances = [instance for instance in instances if instance.get("de_version")]
    else:
        de_instances = report_instances(instances, groups)

    repo_root = Path(__file__).resolve().parents[1]
    build_branches = attach_worktree_paths(repo_root, local_build_branches(repo_root))
    if not quiet:
        print_section("Git Build Branches")
        if build_branches:
            for branch in build_branches:
                print(f"{branch['name']}: loader={branch['loader']}, version={branch['version']}, worktree={branch['worktree_path']}")
        else:
            print("No matching build branches found.")

    if args.automate_menus:
        with quiet_stdout(quiet):
            preflight_macos_automation_permissions(apply)

    with quiet_stdout(quiet):
        ensure_testing_sets_group_layout(instances_path, instances, build_branches, groups_path, game_instances_path, apply)
    if apply:
        # Profile creation/migration changes the instance folders and metadata; use a fresh scan
        # so cleanup and testing include profiles created earlier in this run.
        with quiet_stdout(quiet):
            groups = load_groups(groups_path)
        instances = scan_instances(instances_path)
        de_instances = [instance for instance in instances if instance.get("de_version")]
    if args.skip_cleanup:
        if not quiet:
            print_section("DE Cleanup Plan")
            print("Skipped by --skip-cleanup.")
    else:
        if quiet:
            clean_instance_folders_compact(de_instances, apply)
            clear_terminal_scrollback()
        else:
            clean_instance_folders(de_instances, apply)

    if args.test_instances:
        run_testing_phase(
            instances,
            build_branches,
            apply,
            args.test_branches,
            args.gradle_task,
            not args.skip_loader_api,
            args.automate_menus,
            args.ocr_debug,
            args.debug,
        )

    if not apply:
        print()
        print("Dry-run complete. Re-run with --apply to perform cleanup, profile/group metadata updates, and requested testing actions.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except KeyboardInterrupt:
        print()
        print("Interrupted. Stopping.")
        raise SystemExit(130)
