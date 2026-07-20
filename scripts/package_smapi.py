#!/usr/bin/env python3
"""Create and verify a distributable SMAPI ZIP from a completed build."""

from __future__ import annotations

import argparse
import json
import struct
import sys
import zipfile
import zlib
from pathlib import Path


PACKAGE_ROOT = "DissolverEnhanced"
RUNTIME_TEXTURE_PATH = "assets/big-craftables.png"
REQUIRED_FILES = (
    "DissolverEnhanced.StardewValley.Smapi.dll",
    "DissolverEnhanced.StardewValley.Common.dll",
    "manifest.json",
    RUNTIME_TEXTURE_PATH,
)
EXCLUDED_SUFFIXES = (".pdb", ".deps.json")
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


def paeth_predictor(left: int, above: int, upper_left: int) -> int:
    estimate = left + above - upper_left
    left_distance = abs(estimate - left)
    above_distance = abs(estimate - above)
    upper_left_distance = abs(estimate - upper_left)
    if left_distance <= above_distance and left_distance <= upper_left_distance:
        return left
    if above_distance <= upper_left_distance:
        return above
    return upper_left


def validate_png(texture: bytes, source: str) -> tuple[int, int]:
    error = f"{source} is not a complete decodable PNG texture"
    if len(texture) < 8 or texture[:8] != PNG_SIGNATURE:
        raise ValueError(error)

    position = 8
    header: bytes | None = None
    compressed = bytearray()
    reached_end = False
    while position < len(texture):
        if position + 12 > len(texture):
            raise ValueError(error)
        length = struct.unpack(">I", texture[position : position + 4])[0]
        chunk_type = texture[position + 4 : position + 8]
        data_start = position + 8
        data_end = data_start + length
        crc_end = data_end + 4
        if crc_end > len(texture):
            raise ValueError(error)

        data = texture[data_start:data_end]
        expected_crc = struct.unpack(">I", texture[data_end:crc_end])[0]
        if zlib.crc32(chunk_type + data) & 0xFFFFFFFF != expected_crc:
            raise ValueError(error)

        if chunk_type == b"IHDR":
            if header is not None or length != 13:
                raise ValueError(error)
            header = data
        elif chunk_type == b"IDAT":
            compressed.extend(data)
        elif chunk_type == b"IEND":
            if length != 0 or crc_end != len(texture):
                raise ValueError(error)
            reached_end = True
            position = crc_end
            break
        position = crc_end

    if header is None or not compressed or not reached_end or position != len(texture):
        raise ValueError(error)

    width, height, bit_depth, color_type, compression, filtering, interlace = struct.unpack(">IIBBBBB", header)
    if (
        width < 1
        or height < 1
        or bit_depth != 8
        or color_type != 6
        or compression != 0
        or filtering != 0
        or interlace != 0
    ):
        raise ValueError(error)

    stride = width * 4
    try:
        scanlines = zlib.decompress(bytes(compressed))
    except zlib.error as exception:
        raise ValueError(error) from exception
    if len(scanlines) != height * (stride + 1):
        raise ValueError(error)

    previous = bytearray(stride)
    offset = 0
    for _ in range(height):
        filter_type = scanlines[offset]
        offset += 1
        current = bytearray(scanlines[offset : offset + stride])
        offset += stride
        if filter_type > 4:
            raise ValueError(error)
        for index in range(stride):
            left = current[index - 4] if index >= 4 else 0
            above = previous[index]
            upper_left = previous[index - 4] if index >= 4 else 0
            if filter_type == 1:
                current[index] = (current[index] + left) & 0xFF
            elif filter_type == 2:
                current[index] = (current[index] + above) & 0xFF
            elif filter_type == 3:
                current[index] = (current[index] + ((left + above) // 2)) & 0xFF
            elif filter_type == 4:
                current[index] = (current[index] + paeth_predictor(left, above, upper_left)) & 0xFF
        previous = current

    return width, height


def validate_build(build_dir: Path) -> tuple[int, int]:
    missing = [relative for relative in REQUIRED_FILES if not (build_dir / relative).is_file()]
    if missing:
        raise FileNotFoundError("build output is missing required package files: " + ", ".join(missing))

    json.loads((build_dir / "manifest.json").read_text(encoding="utf-8"))

    mod_assembly = (build_dir / "DissolverEnhanced.StardewValley.Smapi.dll").read_bytes()
    if RUNTIME_TEXTURE_PATH.encode("utf-16le") not in mod_assembly:
        raise ValueError(f"compiled SMAPI assembly does not reference {RUNTIME_TEXTURE_PATH}")

    return validate_png((build_dir / RUNTIME_TEXTURE_PATH).read_bytes(), str(build_dir / RUNTIME_TEXTURE_PATH))


def should_package(relative: Path) -> bool:
    relative_text = relative.as_posix()
    return "ref" not in relative.parts and not relative_text.endswith(EXCLUDED_SUFFIXES)


def create_archive(build_dir: Path, archive: Path) -> None:
    archive.parent.mkdir(parents=True, exist_ok=True)
    archive.unlink(missing_ok=True)

    with zipfile.ZipFile(archive, "w", compression=zipfile.ZIP_DEFLATED) as package:
        for source in sorted(path for path in build_dir.rglob("*") if path.is_file()):
            relative = source.relative_to(build_dir)
            if should_package(relative):
                package.write(source, f"{PACKAGE_ROOT}/{relative.as_posix()}")


def verify_archive(archive: Path) -> tuple[int, int]:
    with zipfile.ZipFile(archive) as package:
        names = set(package.namelist())
        expected = {f"{PACKAGE_ROOT}/{relative}" for relative in REQUIRED_FILES}
        missing = sorted(expected - names)
        if missing:
            raise FileNotFoundError("package is missing required runtime files: " + ", ".join(missing))

        texture_name = f"{PACKAGE_ROOT}/{RUNTIME_TEXTURE_PATH}"
        return validate_png(package.read(texture_name), texture_name)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--build-dir", type=Path, default=Path("build"))
    parser.add_argument("--archive", type=Path, required=True)
    args = parser.parse_args()

    try:
        build_dir = args.build_dir.resolve()
        archive = args.archive.resolve()
        if archive == build_dir or build_dir in archive.parents:
            raise ValueError("--archive must be outside --build-dir")

        build_dimensions = validate_build(build_dir)
        create_archive(build_dir, archive)
        packaged_dimensions = verify_archive(archive)
        if build_dimensions != packaged_dimensions:
            raise ValueError("packaged texture dimensions changed unexpectedly")
    except (FileNotFoundError, json.JSONDecodeError, OSError, ValueError, zipfile.BadZipFile) as error:
        print(f"SMAPI package verification failed: {error}", file=sys.stderr)
        return 1

    print(
        f"Created verified SMAPI package {args.archive} with "
        f"{RUNTIME_TEXTURE_PATH} ({packaged_dimensions[0]}x{packaged_dimensions[1]})."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
