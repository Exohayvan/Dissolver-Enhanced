import json
import struct
import subprocess
import tempfile
import unittest
import zipfile
import zlib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RUNTIME_TEXTURE_PATH = "assets/big-craftables.png"


def png_bytes(width: int = 16, height: int = 32) -> bytes:
    signature = b"\x89PNG\r\n\x1a\n"
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)

    def chunk(kind: bytes, data: bytes) -> bytes:
        payload = kind + data
        return struct.pack(">I", len(data)) + payload + struct.pack(">I", zlib.crc32(payload) & 0xFFFFFFFF)

    pixels = b"".join(b"\x00" + (b"\x00\x00\x00\x00" * width) for _ in range(height))
    return signature + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(pixels)) + chunk(b"IEND", b"")


class SmapiPackagingTests(unittest.TestCase):
    def test_documented_defaults_resolve_in_standard_workspace(self) -> None:
        project = (ROOT / "DissolverEnhanced.StardewValley.Smapi.csproj").read_text(encoding="utf-8")
        expected_project = ROOT / "../../common/stardewvalley/src/DissolverEnhanced.StardewValley.Common.csproj"
        expected_assets = ROOT / "../../common/stardewvalley/assets"

        self.assertIn("../../common/stardewvalley/src/DissolverEnhanced.StardewValley.Common.csproj", project)
        self.assertIn("../../common/stardewvalley/assets", project)
        self.assertTrue(expected_project.resolve().is_file())
        self.assertTrue((expected_assets / "big-craftables.png").resolve().is_file())

    def test_packager_preserves_and_loads_runtime_texture(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            temporary = Path(temporary_directory)
            build = temporary / "build"
            (build / "assets").mkdir(parents=True)
            (build / "DissolverEnhanced.StardewValley.Smapi.dll").write_bytes(
                b"compiled-prefix" + RUNTIME_TEXTURE_PATH.encode("utf-16le") + b"compiled-suffix"
            )
            (build / "DissolverEnhanced.StardewValley.Common.dll").write_bytes(b"common")
            (build / "manifest.json").write_text(json.dumps({"Name": "Dissolver Enhanced"}), encoding="utf-8")
            (build / RUNTIME_TEXTURE_PATH).write_bytes(png_bytes())
            (build / "ignored.pdb").write_bytes(b"debug")
            archive = temporary / "mod.zip"

            result = subprocess.run(
                [
                    "python3",
                    str(ROOT / "scripts/package_smapi.py"),
                    "--build-dir",
                    str(build),
                    "--archive",
                    str(archive),
                ],
                cwd=ROOT,
                text=True,
                capture_output=True,
                check=False,
            )

            self.assertEqual(0, result.returncode, result.stdout + result.stderr)
            with zipfile.ZipFile(archive) as package:
                names = set(package.namelist())
                self.assertIn("DissolverEnhanced/assets/big-craftables.png", names)
                self.assertNotIn("DissolverEnhanced/ignored.pdb", names)
                texture = package.read("DissolverEnhanced/assets/big-craftables.png")
                self.assertEqual(b"\x89PNG\r\n\x1a\n", texture[:8])
                self.assertEqual((16, 32), struct.unpack(">II", texture[16:24]))

    def test_packager_rejects_archive_inside_build_directory(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            build = Path(temporary_directory) / "build"
            (build / "assets").mkdir(parents=True)
            (build / "DissolverEnhanced.StardewValley.Smapi.dll").write_bytes(
                RUNTIME_TEXTURE_PATH.encode("utf-16le")
            )
            (build / "DissolverEnhanced.StardewValley.Common.dll").write_bytes(b"common")
            (build / "manifest.json").write_text(json.dumps({"Name": "Dissolver Enhanced"}), encoding="utf-8")
            (build / RUNTIME_TEXTURE_PATH).write_bytes(png_bytes())

            result = subprocess.run(
                [
                    "python3",
                    str(ROOT / "scripts/package_smapi.py"),
                    "--build-dir",
                    str(build),
                    "--archive",
                    str(build / "mod.zip"),
                ],
                cwd=ROOT,
                text=True,
                capture_output=True,
                check=False,
            )

            self.assertNotEqual(0, result.returncode)
            self.assertIn("outside --build-dir", result.stderr)

    def test_packager_rejects_truncated_png_that_cannot_be_decoded(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            temporary = Path(temporary_directory)
            build = temporary / "build"
            (build / "assets").mkdir(parents=True)
            (build / "DissolverEnhanced.StardewValley.Smapi.dll").write_bytes(
                RUNTIME_TEXTURE_PATH.encode("utf-16le")
            )
            (build / "DissolverEnhanced.StardewValley.Common.dll").write_bytes(b"common")
            (build / "manifest.json").write_text(json.dumps({"Name": "Dissolver Enhanced"}), encoding="utf-8")
            (build / RUNTIME_TEXTURE_PATH).write_bytes(png_bytes()[:33])

            result = subprocess.run(
                [
                    "python3",
                    str(ROOT / "scripts/package_smapi.py"),
                    "--build-dir",
                    str(build),
                    "--archive",
                    str(temporary / "mod.zip"),
                ],
                cwd=ROOT,
                text=True,
                capture_output=True,
                check=False,
            )

            self.assertNotEqual(0, result.returncode)
            self.assertIn("complete decodable PNG", result.stderr)

    def test_workflow_builds_stardew_fix_branches_outside_protected_namespace(self) -> None:
        workflow = (ROOT / ".github/workflows/build.yml").read_text(encoding="utf-8")

        self.assertEqual(3, workflow.count("startsWith(github.ref_name, 'fix/stardew')"))
        self.assertEqual(2, workflow.count('loader_name="smapi"'))

    def test_workflow_checks_out_common_branch(self) -> None:
        workflow = (ROOT / ".github/workflows/build.yml").read_text(encoding="utf-8")

        self.assertIn("COMMON_BRANCH: common", workflow)

    def test_workflow_passes_common_assets_and_uses_verified_packager(self) -> None:
        workflow = (ROOT / ".github/workflows/build.yml").read_text(encoding="utf-8")

        self.assertEqual(2, workflow.count("-p:CommonAssetsPath="))
        self.assertEqual(2, workflow.count("scripts/package_smapi.py"))
        self.assertEqual(2, workflow.count("tests/SmapiTextureSmoke/SmapiTextureSmoke.csproj"))
        self.assertNotIn("cp build/*.dll build/manifest.json", workflow)


if __name__ == "__main__":
    unittest.main()
