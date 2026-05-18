#!/usr/bin/env python3
import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import urllib.error
import urllib.request
import zipfile
from pathlib import Path
from xml.etree import ElementTree


GITHUB_API = "https://api.github.com"


def run(command, cwd):
    print(f"+ {' '.join(command)}", flush=True)
    subprocess.run(command, cwd=cwd, check=True)


def capture(command, cwd):
    return subprocess.check_output(command, cwd=cwd, text=True).strip()


def github_headers(token):
    headers = {
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"
    return headers


def github_json(url, token):
    request = urllib.request.Request(url, headers=github_headers(token))
    try:
        with urllib.request.urlopen(request) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        if error.code == 404:
            return None
        raise


def latest_release(repo, token):
    if not repo:
        return None
    return github_json(f"{GITHUB_API}/repos/{repo}/releases/latest", token)


def latest_release_assets(repo, token):
    release = latest_release(repo, token)
    if not release:
        return set()
    return {asset.get("name") for asset in release.get("assets", []) if asset.get("name")}


def changelog(target_dir, latest, target_id):
    if not latest:
        return (
            f"First auto changelog for {target_id}, no release data.\n\n"
            "No previous GitHub release was found."
        )

    tag = latest.get("tag_name")
    if not tag:
        return (
            f"First auto changelog for {target_id}, no release data.\n\n"
            "The latest GitHub release did not have a tag."
        )

    run(["git", "fetch", "--tags", "--force"], target_dir)
    try:
        capture(["git", "merge-base", "--is-ancestor", tag, "HEAD"], target_dir)
        log_range = f"{tag}..HEAD"
        heading = f"Changes for {target_id} since {tag}"
    except subprocess.CalledProcessError:
        log_range = "HEAD"
        heading = (
            f"Changes for {target_id}\n\n"
            f"No previous release commit was found in this branch for {tag}; showing recent branch commits."
        )

    try:
        lines = capture(["git", "log", "--pretty=format:- %s (%h)", log_range], target_dir)
    except subprocess.CalledProcessError:
        lines = ""

    if not lines:
        lines = "- No branch commits found."
    return f"{heading}\n\n{lines}"


def read_properties(path):
    properties = {}
    with path.open("r", encoding="utf-8") as file:
        for raw_line in file:
            line = raw_line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            properties[key.strip()] = value.strip()
    return properties


def xml_property(path, name):
    root = ElementTree.parse(path).getroot()
    for element in root.iter():
        if element.tag.split("}")[-1] == name and element.text:
            return element.text.strip()
    raise ValueError(f"Missing <{name}> in {path}")


def safe_part(value):
    return re.sub(r"[^A-Za-z0-9._+-]+", "-", str(value)).strip("-")


def zip_file(zip_path, source_file):
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as archive:
        archive.write(source_file, source_file.name)


def zip_directory(zip_path, root_dir):
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as archive:
        for path in sorted(root_dir.rglob("*")):
            if path.is_file():
                archive.write(path, path.relative_to(root_dir.parent))


def find_primary_jar(build_libs, mod_version):
    jars = [
        path for path in build_libs.glob("*.jar")
        if "-sources" not in path.name and "-javadoc" not in path.name
    ]
    if not jars:
        raise FileNotFoundError(f"No release jar found in {build_libs}")
    matching_version = [path for path in jars if f"-{mod_version}-" in path.name]
    if matching_version:
        jars = matching_version
    if len(jars) > 1:
        print(f"Multiple release jars found; using newest: {[jar.name for jar in jars]}")
    return max(jars, key=lambda path: path.stat().st_mtime)


def package_name(target, game_version, loader_version, mod_version, common_version):
    return (
        f"{safe_part(target['game'])}-{safe_part(game_version)}_"
        f"{safe_part(target['loader'])}-{safe_part(loader_version)}_"
        f"DissolverEnhanced-{safe_part(mod_version)}_"
        f"Common-{safe_part(common_version)}.zip"
    )


def metadata(
    target,
    output_zip,
    game_version,
    loader_version,
    mod_version,
    common_version,
    changelog_text,
    skip_publish,
    modrinth_file=None,
    curseforge_file=None,
):
    release_name = (
        f"{target['game']} {game_version} {target['loader']} {loader_version} "
        f"Dissolver Enhanced {mod_version}"
    )
    return {
        "target_id": target["id"],
        "branch": target["branch"],
        "platform": target["platform"],
        "game": target["game"],
        "game_version": game_version,
        "game_versions": target.get("game_versions", [game_version]),
        "loader": target["loader"],
        "loader_slug": target["loader_slug"],
        "loaders": target.get("loaders", [target["loader_slug"]]),
        "loader_version": loader_version,
        "mod_version": mod_version,
        "common_version": common_version,
        "release_name": release_name,
        "display_name": output_zip.stem,
        "file_name": output_zip.name,
        "file_path": str(output_zip),
        "modrinth_file_name": modrinth_file.name if modrinth_file else output_zip.name,
        "modrinth_file_path": str(modrinth_file or output_zip),
        "curseforge_file_name": curseforge_file.name if curseforge_file else output_zip.name,
        "curseforge_file_path": str(curseforge_file or output_zip),
        "skip_publish": skip_publish,
        "changelog": changelog_text,
        "modrinth_project_id": target.get("modrinth_project_id"),
        "curseforge_project_id": target.get("curseforge_project_id"),
        "curseforge_base_url": target.get("curseforge_base_url"),
        "curseforge_game_version_ids": target.get("curseforge_game_version_ids", []),
        "curseforge_dependency_ids": target.get("curseforge_dependency_ids", []),
        "curseforge_dependency_slugs": target.get("curseforge_dependency_slugs", []),
        "curseforge_game_versions": target.get("curseforge_game_versions", target.get("game_versions", [game_version])),
    }


def package_minecraft(target, common_dir, target_dir, output_dir, release_assets, branch_changelog):
    props = read_properties(target_dir / "gradle.properties")
    common_props = read_properties(common_dir / "minecraft" / "gradle.properties")
    game_version = target.get("game_version_label") or props.get("minecraft_release_range") or props["minecraft_version"]
    loader_version_property = target.get("loader_version_property", "loader_version")
    loader_version = props.get(loader_version_property) or props.get("loader_version") or props.get("forge_version") or props.get("neo_version")
    mod_version = props["mod_version"]
    common_version = common_props["common_version"]

    output_zip = output_dir / package_name(target, game_version, loader_version, mod_version, common_version)
    skip_publish = output_zip.name in release_assets
    if skip_publish:
        print(f"Skipping {target['id']} because latest GitHub release already has {output_zip.name}.")
        return metadata(target, output_zip, game_version, loader_version, mod_version, common_version, branch_changelog, True)

    run(["chmod", "+x", "./gradlew"], target_dir)
    run(["./gradlew", "build", "-x", "test", "--no-daemon"], target_dir)
    output_zip.parent.mkdir(parents=True, exist_ok=True)
    primary_jar = find_primary_jar(target_dir / "build" / "libs", mod_version)
    packaged_jar = output_dir / primary_jar.name
    shutil.copy2(primary_jar, packaged_jar)
    zip_file(output_zip, primary_jar)
    return metadata(
        target,
        output_zip,
        game_version,
        loader_version,
        mod_version,
        common_version,
        branch_changelog,
        False,
        packaged_jar,
        packaged_jar,
    )


def package_stardew(target, common_dir, target_dir, output_dir, release_assets, branch_changelog):
    common_project = common_dir / "stardewvalley" / "src" / "DissolverEnhanced.StardewValley.Common.csproj"
    project = target_dir / "DissolverEnhanced.StardewValley.Smapi.csproj"
    game_version = target.get("game_version_label") or xml_property(project, "GameVersion")
    loader_version = xml_property(project, "LoaderVersion")
    mod_version = xml_property(project, "Version")
    common_version = xml_property(common_project, "Version")

    output_zip = output_dir / package_name(target, game_version, loader_version, mod_version, common_version)
    skip_publish = output_zip.name in release_assets
    if skip_publish:
        print(f"Skipping {target['id']} because latest GitHub release already has {output_zip.name}.")
        return metadata(target, output_zip, game_version, loader_version, mod_version, common_version, branch_changelog, True)

    run([
        "dotnet",
        "build",
        "--configuration",
        "Release",
        f"-p:CommonProjectPath=../{common_dir.name}/stardewvalley/src/DissolverEnhanced.StardewValley.Common.csproj",
    ], target_dir)

    package_root = output_dir / "package" / "DissolverEnhanced"
    if package_root.parent.exists():
        shutil.rmtree(package_root.parent)
    package_root.mkdir(parents=True)

    for path in (target_dir / "build").iterdir():
        if path.name.endswith((".pdb", ".deps.json")) or path.name == "ref":
            continue
        destination = package_root / path.name
        if path.is_dir():
            shutil.copytree(path, destination)
        else:
            shutil.copy2(path, destination)

    zip_directory(output_zip, package_root)
    return metadata(target, output_zip, game_version, loader_version, mod_version, common_version, branch_changelog, False)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--target", required=True, help="JSON release target object")
    parser.add_argument("--common-dir", required=True)
    parser.add_argument("--target-dir", required=True)
    parser.add_argument("--output-dir", required=True)
    parser.add_argument("--repo", default="")
    parser.add_argument("--github-token", default=os.environ.get("GITHUB_TOKEN", ""))
    args = parser.parse_args()

    target = json.loads(args.target)
    common_dir = Path(args.common_dir).resolve()
    target_dir = Path(args.target_dir).resolve()
    output_dir = Path(args.output_dir).resolve()
    output_dir.mkdir(parents=True, exist_ok=True)
    release = latest_release(args.repo, args.github_token)
    release_assets = latest_release_assets(args.repo, args.github_token)
    branch_changelog = changelog(target_dir, release, target["id"])

    if target["platform"] == "minecraft":
        release_metadata = package_minecraft(target, common_dir, target_dir, output_dir, release_assets, branch_changelog)
    elif target["platform"] == "stardewvalley":
        release_metadata = package_stardew(target, common_dir, target_dir, output_dir, release_assets, branch_changelog)
    else:
        raise ValueError(f"Unsupported release platform: {target['platform']}")

    metadata_path = output_dir / "release-metadata.json"
    metadata_path.write_text(json.dumps(release_metadata, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(release_metadata, indent=2), flush=True)


if __name__ == "__main__":
    main()
