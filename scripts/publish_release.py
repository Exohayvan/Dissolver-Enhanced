#!/usr/bin/env python3
import argparse
import hashlib
import json
import mimetypes
import os
import random
import string
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path


GITHUB_API = "https://api.github.com"
MODRINTH_API = "https://api.modrinth.com/v2"


def request_json(method, url, headers=None, body=None, expected=(200, 201)):
    headers = dict(headers or {})
    if body is not None and not isinstance(body, bytes):
        body = json.dumps(body).encode("utf-8")
        headers.setdefault("Content-Type", "application/json")
    request = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request) as response:
            payload = response.read()
            if response.status not in expected:
                raise RuntimeError(f"{method} {url} returned HTTP {response.status}: {payload.decode('utf-8', 'replace')}")
            return json.loads(payload.decode("utf-8")) if payload else None
    except urllib.error.HTTPError as error:
        payload = error.read().decode("utf-8", "replace")
        raise RuntimeError(f"{method} {url} returned HTTP {error.code}: {payload}") from error


def request_empty(method, url, headers=None, body=None, expected=(200, 201, 204)):
    headers = dict(headers or {})
    request = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request) as response:
            payload = response.read()
            if response.status not in expected:
                raise RuntimeError(f"{method} {url} returned HTTP {response.status}: {payload.decode('utf-8', 'replace')}")
            return payload
    except urllib.error.HTTPError as error:
        payload = error.read().decode("utf-8", "replace")
        raise RuntimeError(f"{method} {url} returned HTTP {error.code}: {payload}") from error


def multipart(fields, files):
    boundary = "----DissolverEnhanced" + "".join(random.choice(string.ascii_letters + string.digits) for _ in range(24))
    chunks = []

    for name, value in fields.items():
        chunks.append(f"--{boundary}\r\n".encode("utf-8"))
        chunks.append(f'Content-Disposition: form-data; name="{name}"\r\n\r\n'.encode("utf-8"))
        chunks.append(value.encode("utf-8") if isinstance(value, str) else value)
        chunks.append(b"\r\n")

    for name, path in files.items():
        path = Path(path)
        content_type = mimetypes.guess_type(path.name)[0] or "application/octet-stream"
        chunks.append(f"--{boundary}\r\n".encode("utf-8"))
        chunks.append(
            f'Content-Disposition: form-data; name="{name}"; filename="{path.name}"\r\n'
            f"Content-Type: {content_type}\r\n\r\n"
            .encode("utf-8")
        )
        chunks.append(path.read_bytes())
        chunks.append(b"\r\n")

    chunks.append(f"--{boundary}--\r\n".encode("utf-8"))
    return b"".join(chunks), f"multipart/form-data; boundary={boundary}"


def github_headers(token):
    return {
        "Accept": "application/vnd.github+json",
        "Authorization": f"Bearer {token}",
        "X-GitHub-Api-Version": "2022-11-28",
    }


def get_or_create_github_release(repo, token, tag, name, changelog, prerelease, dry_run):
    if dry_run:
        print(f"[dry-run] Would create or update GitHub release {tag}.")
        return {"id": 0, "upload_url": f"https://uploads.github.com/repos/{repo}/releases/0/assets{{?name,label}}", "assets": []}

    headers = github_headers(token)
    release_url = f"{GITHUB_API}/repos/{repo}/releases/tags/{urllib.parse.quote(tag, safe='')}"
    try:
        release = request_json("GET", release_url, headers=headers)
        print(f"Using existing GitHub release {tag}.")
        return release
    except RuntimeError as error:
        if "HTTP 404" not in str(error):
            raise

    body = {
        "tag_name": tag,
        "name": name,
        "body": changelog,
        "draft": False,
        "prerelease": prerelease,
        "generate_release_notes": False,
    }
    print(f"Creating GitHub release {tag}.")
    return request_json("POST", f"{GITHUB_API}/repos/{repo}/releases", headers=headers, body=body)


def upload_github_asset(repo, token, release, path, dry_run):
    path = Path(path)
    headers = github_headers(token)

    for asset in release.get("assets", []):
        if asset.get("name") == path.name:
            print(f"Deleting existing GitHub asset {path.name}.")
            if not dry_run:
                request_empty("DELETE", f"{GITHUB_API}/repos/{repo}/releases/assets/{asset['id']}", headers=headers)

    upload_url = release["upload_url"].split("{", 1)[0] + "?name=" + urllib.parse.quote(path.name)
    headers = dict(headers)
    headers["Content-Type"] = mimetypes.guess_type(path.name)[0] or "application/octet-stream"
    print(f"Uploading GitHub asset {path.name}.")
    if dry_run:
        print("[dry-run] Would upload GitHub asset.")
        return
    request_empty("POST", upload_url, headers=headers, body=path.read_bytes())


def publish_modrinth(meta, token, release_type, dry_run):
    project_id = meta.get("modrinth_project_id")
    if not project_id:
        return

    path = Path(meta.get("modrinth_file_path") or meta["file_path"])
    data = {
        "name": meta["release_name"],
        "version_number": f"{meta['mod_version']}-{meta['loader_slug']}-{meta['game_version']}",
        "changelog": meta["changelog"],
        "dependencies": [],
        "game_versions": meta["game_versions"],
        "version_type": release_type,
        "loaders": meta["loaders"],
        "featured": release_type == "release",
        "status": "listed",
        "project_id": project_id,
        "file_parts": ["file"],
        "primary_file": "file",
    }

    body, content_type = multipart({"data": json.dumps(data)}, {"file": path})
    headers = {
        "Authorization": token,
        "Content-Type": content_type,
        "User-Agent": "Exohayvan/Dissolver-Enhanced release workflow",
    }
    print(f"Publishing {path.name} to Modrinth project {project_id}.")
    if dry_run:
        print("[dry-run] Would publish Modrinth version:")
        print(json.dumps(data, indent=2))
        return
    request_json("POST", f"{MODRINTH_API}/version", headers=headers, body=body)


def normalize_key(value):
    return "".join(character.lower() for character in str(value) if character.isalnum())


def curseforge_versions(meta, token):
    base_url = meta["curseforge_base_url"].rstrip("/")
    return request_json("GET", base_url + "/api/game/versions", headers={"X-Api-Token": token})


def curseforge_version_ids(meta, token):
    ids = list(meta.get("curseforge_game_version_ids", []))
    ids.extend(meta.get("curseforge_dependency_ids", []))

    names = list(meta.get("curseforge_game_versions", []))
    names.extend(meta.get("curseforge_dependency_slugs", []))
    if not names:
        return sorted(set(int(item) for item in ids))

    versions = curseforge_versions(meta, token)
    by_name = {normalize_key(version.get("name")): version.get("id") for version in versions}
    missing = []
    for version_name in names:
        version_id = by_name.get(normalize_key(version_name))
        if version_id is None:
            missing.append(str(version_name))
        else:
            ids.append(version_id)
    if missing:
        raise RuntimeError(f"CurseForge game versions/loaders were not found for {meta['target_id']}: {', '.join(missing)}")

    return sorted(set(int(item) for item in ids))


def curseforge_preview_ids(meta, token):
    explicit_ids = meta.get("curseforge_game_version_ids", []) + meta.get("curseforge_dependency_ids", [])
    if token == "dry-run-token":
        return explicit_ids, False
    return curseforge_version_ids(meta, token), True


def publish_curseforge(meta, token, release_type, manual_release, dry_run):
    project_id = meta.get("curseforge_project_id")
    if not project_id:
        return

    path = Path(meta.get("curseforge_file_path") or meta["file_path"])
    if dry_run:
        game_versions, resolved = curseforge_preview_ids(meta, token)
    else:
        game_versions = curseforge_version_ids(meta, token)
        resolved = True
    metadata = {
        "changelog": meta["changelog"],
        "changelogType": "markdown",
        "displayName": meta["display_name"],
        "gameVersions": game_versions,
        "releaseType": release_type,
        "isMarkedForManualRelease": manual_release,
    }
    body, content_type = multipart({"metadata": json.dumps(metadata)}, {"file": path})
    headers = {
        "X-Api-Token": token,
        "Content-Type": content_type,
    }
    url = f"{meta['curseforge_base_url'].rstrip('/')}/api/projects/{project_id}/upload-file"
    print(f"Publishing {path.name} to CurseForge project {project_id}.")
    if dry_run:
        print("[dry-run] Would publish CurseForge file:")
        preview = dict(metadata)
        preview["resolvedThroughCurseForgeApi"] = resolved
        if not resolved:
            preview["unresolvedGameVersionNames"] = meta.get("curseforge_game_versions", [])
            preview["loaderDependencySlugs"] = meta.get("curseforge_dependency_slugs", [])
        print(json.dumps(preview, indent=2))
        return
    request_json("POST", url, headers=headers, body=body)


def load_metadata(root):
    metadata = []
    for path in sorted(Path(root).rglob("release-metadata.json")):
        item = json.loads(path.read_text(encoding="utf-8"))
        file_path = Path(item["file_path"])
        if not file_path.exists():
            sibling = path.parent / item["file_name"]
            if sibling.exists():
                item["file_path"] = str(sibling)
        modrinth_file_path = Path(item.get("modrinth_file_path") or item["file_path"])
        if not modrinth_file_path.exists():
            sibling = path.parent / item.get("modrinth_file_name", "")
            if sibling.exists():
                item["modrinth_file_path"] = str(sibling)
        curseforge_file_path = Path(item.get("curseforge_file_path") or item["file_path"])
        if not curseforge_file_path.exists():
            sibling = path.parent / item.get("curseforge_file_name", "")
            if sibling.exists():
                item["curseforge_file_path"] = str(sibling)
        metadata.append(item)
    if not metadata:
        raise FileNotFoundError(f"No release-metadata.json files found under {root}")
    return metadata


def release_body(metadata):
    publishable = [item for item in metadata if not item.get("skip_publish")]
    skipped = [item for item in metadata if item.get("skip_publish")]
    lines = []

    if publishable:
        lines.append("## Included files")
        lines.append("")
        for item in publishable:
            lines.append(f"- `{item['file_name']}`")
        lines.append("")

    if skipped:
        lines.append("## Skipped unchanged files")
        lines.append("")
        for item in skipped:
            lines.append(f"- `{item['file_name']}`")
        lines.append("")

    for item in publishable:
        lines.append(f"## {item['target_id']}")
        lines.append("")
        lines.append(item.get("changelog") or "- No branch changelog was generated.")
        lines.append("")

    if not publishable:
        lines.append("No release files changed. Every target matched an existing asset on the latest GitHub release.")

    return "\n".join(lines).strip() + "\n"


def release_identity(metadata):
    publishable = [item for item in metadata if not item.get("skip_publish")]
    if not publishable:
        return "noop", "Dissolver Enhanced noop"

    tag_items = [
        {
            "file_name": item["file_name"],
            "game": item["game"],
            "game_version": item["game_version"],
            "loader": item["loader"],
            "loader_version": item["loader_version"],
            "mod_version": item["mod_version"],
            "common_version": item["common_version"],
        }
        for item in sorted(publishable, key=lambda entry: entry["file_name"])
    ]
    release_hash = hashlib.sha256(
        json.dumps(tag_items, sort_keys=True, separators=(",", ":")).encode("utf-8")
    ).hexdigest()[:12]

    mod_versions = ", ".join(sorted({item["mod_version"] for item in publishable}))
    common_versions = ", ".join(sorted({item["common_version"] for item in publishable}))
    return f"release-{release_hash}", f"Dissolver Enhanced {release_hash} (mods {mod_versions}, common {common_versions})"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--artifacts-dir", required=True)
    parser.add_argument("--repo", required=True)
    parser.add_argument("--release-type", choices=["release", "beta", "alpha"], required=True)
    parser.add_argument("--manual-curseforge-release", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    metadata = load_metadata(args.artifacts_dir)
    publishable = [item for item in metadata if not item.get("skip_publish")]
    if not publishable:
        print("No release targets need publishing; every expected file already exists on the latest GitHub release.")
        return

    github_token = os.environ.get("GITHUB_TOKEN")
    curseforge_token = os.environ.get("CURSEFORGE_TOKEN")
    modrinth_token = os.environ.get("MODRINTH_TOKEN")

    if not github_token and not args.dry_run:
        raise RuntimeError("GITHUB_TOKEN is required.")

    tag, release_name = release_identity(metadata)
    release = get_or_create_github_release(
        args.repo,
        github_token or "dry-run-token",
        tag,
        release_name,
        release_body(metadata),
        args.release_type != "release",
        args.dry_run,
    )
    for item in publishable:
        upload_github_asset(args.repo, github_token, release, item["file_path"], args.dry_run)

    for item in publishable:
        if item.get("modrinth_project_id"):
            if not modrinth_token and not args.dry_run:
                raise RuntimeError("MODRINTH_TOKEN is required for Modrinth publishing.")
            publish_modrinth(item, modrinth_token or "dry-run-token", args.release_type, args.dry_run)

        if item.get("curseforge_project_id"):
            if not curseforge_token and not args.dry_run:
                raise RuntimeError("CURSEFORGE_TOKEN is required for CurseForge publishing.")
            curseforge_token = curseforge_token or "dry-run-token"
            publish_curseforge(
                item,
                curseforge_token,
                args.release_type,
                args.manual_curseforge_release,
                args.dry_run,
            )


if __name__ == "__main__":
    try:
        main()
    except Exception as exception:
        print(f"release publish failed: {exception}", file=sys.stderr)
        sys.exit(1)
