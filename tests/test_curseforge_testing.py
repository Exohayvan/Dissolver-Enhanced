import contextlib
import importlib.util
import io
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest import mock


SCRIPT_PATH = Path(__file__).resolve().parents[1] / "scripts" / "curseforge_testing.py"
SPEC = importlib.util.spec_from_file_location("curseforge_testing", SCRIPT_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError(f"Could not load {SCRIPT_PATH}")
curseforge_testing = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(curseforge_testing)


class PullRequestBranchTests(unittest.TestCase):
    def test_canonical_loader_branch_is_not_a_pull_request_branch(self):
        branch = {
            "name": "minecraft/fabric-1.21.x",
            "loader": "fabric",
            "version": "1.21.x",
        }

        self.assertFalse(curseforge_testing.is_pull_request_branch(branch))

    def test_suffixed_loader_branch_is_a_pull_request_branch(self):
        branch = {
            "name": "minecraft-fabric-1.21.x-fix-recipe_source_null_guard",
            "loader": "fabric",
            "version": "1.21.x",
        }

        self.assertTrue(curseforge_testing.is_pull_request_branch(branch))


class PullRequestCommentTests(unittest.TestCase):
    def test_pass_comment_states_that_local_testing_passed(self):
        result = {
            "branch": {"name": "minecraft-fabric-1.21.x-fix-null-guard"},
            "passed": True,
            "tested_instances": 3,
            "logs": [],
        }

        comment = curseforge_testing.pull_request_test_comment(result)

        self.assertIn("Local CurseForge testing passed", comment)
        self.assertIn("3 tested CurseForge instances passed", comment)

    def test_cached_pass_comment_explains_that_launcher_testing_was_skipped(self):
        result = {
            "branch": {"name": "minecraft-fabric-1.21.x-fix-null-guard"},
            "passed": True,
            "tested_instances": 3,
            "passed_tests": 37,
            "cached": True,
            "logs": [],
        }

        comment = curseforge_testing.pull_request_test_comment(result)

        self.assertIn("exact build jar already passed 37 local tests", comment)
        self.assertIn("Launcher testing was skipped", comment)

    def test_failure_comment_embeds_failure_log_in_code_block(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            log_path = Path(temp_dir) / "Fabric-1.21.1-latest.log"
            log_path.write_text("first line\njava.lang.AssertionError: boom\n", encoding="utf-8")
            result = {
                "branch": {"name": "minecraft-fabric-1.21.x-fix-null-guard"},
                "passed": False,
                "tested_instances": 1,
                "logs": [log_path],
            }

            comment = curseforge_testing.pull_request_test_comment(result)

        self.assertIn("Local CurseForge testing failed", comment)
        self.assertIn("Fabric-1.21.1-latest.log", comment)
        self.assertIn("```text\nfirst line\njava.lang.AssertionError: boom\n```", comment)


class PullRequestPublishingTests(unittest.TestCase):
    def test_pass_result_comments_and_adds_passed_label(self):
        calls = []

        def runner(command, **kwargs):
            calls.append((command, kwargs))
            if command[1:3] == ["pr", "list"]:
                return subprocess.CompletedProcess(
                    command,
                    0,
                    stdout='[{"number": 42, "labels": [{"name": "testing:failed"}]}]\n',
                    stderr="",
                )
            return subprocess.CompletedProcess(command, 0, stdout="", stderr="")

        result = {
            "branch": {
                "name": "minecraft-fabric-1.21.x-fix-null-guard",
                "loader": "fabric",
                "version": "1.21.x",
            },
            "passed": True,
            "tested_instances": 2,
            "logs": [],
        }

        with contextlib.redirect_stdout(io.StringIO()):
            published = curseforge_testing.publish_pull_request_test_result(
                result,
                Path("/repo"),
                runner=runner,
            )

        self.assertTrue(published)
        commands = [call[0] for call in calls]
        self.assertIn(
            [
                "gh", "label", "create", "testing:passed", "--color", "2DA44E",
                "--description", "Local CurseForge testing passed", "--force",
            ],
            commands,
        )
        self.assertIn(
            [
                "gh", "pr", "edit", "42", "--add-label", "testing:passed",
                "--remove-label", "testing:failed",
            ],
            commands,
        )
        comment_call = next(call for call in calls if call[0][1:3] == ["pr", "comment"])
        self.assertEqual(comment_call[0], ["gh", "pr", "comment", "42", "--body-file", "-"])
        self.assertIn("Local CurseForge testing passed", comment_call[1]["input"])

    def test_failure_result_comments_with_log_and_adds_failed_label(self):
        calls = []

        def runner(command, **kwargs):
            calls.append((command, kwargs))
            if command[1:3] == ["pr", "list"]:
                return subprocess.CompletedProcess(
                    command,
                    0,
                    stdout='[{"number": 43, "labels": [{"name": "testing:passed"}]}]\n',
                    stderr="",
                )
            return subprocess.CompletedProcess(command, 0, stdout="", stderr="")

        with tempfile.TemporaryDirectory() as temp_dir:
            log_path = Path(temp_dir) / "failure.log"
            log_path.write_text("Minecraft test failed\n", encoding="utf-8")
            result = {
                "branch": {
                    "name": "minecraft-forge-1.20.x-fix-null-guard",
                    "loader": "forge",
                    "version": "1.20.x",
                },
                "passed": False,
                "tested_instances": 1,
                "logs": [log_path],
            }
            with contextlib.redirect_stdout(io.StringIO()):
                published = curseforge_testing.publish_pull_request_test_result(
                    result,
                    Path("/repo"),
                    runner=runner,
                )

        self.assertTrue(published)
        commands = [call[0] for call in calls]
        self.assertIn(
            [
                "gh", "pr", "edit", "43", "--add-label", "testing:failed",
                "--remove-label", "testing:passed",
            ],
            commands,
        )
        comment_call = next(call for call in calls if call[0][1:3] == ["pr", "comment"])
        self.assertIn("```text\nMinecraft test failed\n```", comment_call[1]["input"])

    def test_cached_pass_with_present_label_does_not_post_duplicate_update(self):
        calls = []

        def runner(command, **kwargs):
            calls.append((command, kwargs))
            if command[1:3] == ["pr", "list"]:
                return subprocess.CompletedProcess(
                    command,
                    0,
                    stdout='[{"number": 42, "labels": [{"name": "testing:passed"}]}]\n',
                    stderr="",
                )
            return subprocess.CompletedProcess(command, 0, stdout="", stderr="")

        result = {
            "branch": {
                "name": "minecraft-fabric-1.21.x-fix-null-guard",
                "loader": "fabric",
                "version": "1.21.x",
            },
            "passed": True,
            "cached": True,
            "passed_tests": 37,
            "tested_instances": 2,
            "logs": [],
        }

        with contextlib.redirect_stdout(io.StringIO()):
            published = curseforge_testing.publish_pull_request_test_result(
                result,
                Path("/repo"),
                runner=runner,
            )

        self.assertTrue(published)
        mutation_calls = [
            command for command, _kwargs in calls
            if command[:3] in (
                ["gh", "pr", "edit"],
                ["gh", "pr", "comment"],
                ["gh", "label", "create"],
            )
        ]
        self.assertEqual(mutation_calls, [])

    def test_cached_pass_restores_missing_pass_label_without_commenting(self):
        calls = []

        def runner(command, **kwargs):
            calls.append((command, kwargs))
            if command[1:3] == ["pr", "list"]:
                return subprocess.CompletedProcess(
                    command,
                    0,
                    stdout='[{"number": 42, "labels": []}]\n',
                    stderr="",
                )
            return subprocess.CompletedProcess(command, 0, stdout="", stderr="")

        result = {
            "branch": {
                "name": "minecraft-fabric-1.21.x-fix-null-guard",
                "loader": "fabric",
                "version": "1.21.x",
            },
            "passed": True,
            "cached": True,
            "passed_tests": 37,
            "tested_instances": 2,
            "logs": [],
        }

        with contextlib.redirect_stdout(io.StringIO()):
            published = curseforge_testing.publish_pull_request_test_result(
                result,
                Path("/repo"),
                runner=runner,
            )

        self.assertTrue(published)
        commands = [command for command, _kwargs in calls]
        self.assertIn(
            ["gh", "pr", "edit", "42", "--add-label", "testing:passed"],
            commands,
        )
        self.assertFalse(any(command[:3] == ["gh", "pr", "comment"] for command in commands))

    def test_github_update_error_is_reported_without_crashing_local_results(self):
        call_count = 0

        def runner(command, **kwargs):
            nonlocal call_count
            call_count += 1
            if call_count == 1:
                return subprocess.CompletedProcess(
                    command,
                    0,
                    stdout='[{"number": 44, "labels": []}]\n',
                    stderr="",
                )
            raise OSError("gh unavailable")

        result = {
            "branch": {
                "name": "minecraft-quilt-1.21.x-fix-null-guard",
                "loader": "quilt",
                "version": "1.21.x",
            },
            "passed": True,
            "tested_instances": 1,
            "logs": [],
        }

        with contextlib.redirect_stdout(io.StringIO()):
            published = curseforge_testing.publish_pull_request_test_result(
                result,
                Path("/repo"),
                runner=runner,
            )

        self.assertFalse(published)

    def test_pr_in_another_repository_is_updated_in_that_repository(self):
        calls = []
        branch_name = "minecraft-fabric-1.21.x-fix-recipe_source_null_guard"
        target_repo = "Exohayvan/Dissolver-Enhanced"

        def runner(command, **kwargs):
            calls.append((command, kwargs))
            if command[:3] == ["gh", "pr", "list"] and "--head" in command:
                return subprocess.CompletedProcess(command, 0, stdout="[]\n", stderr="")
            if command[:3] == ["gh", "search", "prs"]:
                return subprocess.CompletedProcess(
                    command,
                    0,
                    stdout=(
                        '[{"number": 8, "labels": [], "repository": '
                        '{"nameWithOwner": "Exohayvan/Dissolver-Enhanced"}}]\n'
                    ),
                    stderr="",
                )
            return subprocess.CompletedProcess(command, 0, stdout="", stderr="")

        result = {
            "branch": {
                "name": branch_name,
                "loader": "fabric",
                "version": "1.21.x",
            },
            "passed": True,
            "tested_instances": 1,
            "logs": [],
        }

        with contextlib.redirect_stdout(io.StringIO()):
            published = curseforge_testing.publish_pull_request_test_result(
                result,
                Path("/repo"),
                runner=runner,
            )

        self.assertTrue(published)
        commands = [call[0] for call in calls]
        self.assertIn(
            [
                "gh", "pr", "edit", "8", "--repo", target_repo,
                "--add-label", "testing:passed",
            ],
            commands,
        )
        comment_call = next(command for command in commands if command[:3] == ["gh", "pr", "comment"])
        self.assertIn("--repo", comment_call)
        self.assertIn(target_repo, comment_call)

    def test_checked_out_branch_can_find_pr_by_head_commit(self):
        calls = []
        head_sha = "d" * 40

        def runner(command, **kwargs):
            calls.append(command)
            if command[:3] == ["gh", "pr", "list"] and "--head" in command:
                return subprocess.CompletedProcess(command, 0, stdout="[]\n", stderr="")
            if command[:2] == ["git", "rev-parse"]:
                return subprocess.CompletedProcess(command, 0, stdout=f"{head_sha}\n", stderr="")
            if command[:3] == ["gh", "pr", "list"]:
                return subprocess.CompletedProcess(
                    command,
                    0,
                    stdout=f'[(invalid)]',
                    stderr="",
                )
            return subprocess.CompletedProcess(command, 0, stdout="", stderr="")

        # Use a valid JSON response for the all-open-PR fallback without making
        # the command matching above hide what this test is proving.
        def commit_matching_runner(command, **kwargs):
            if command[:3] == ["gh", "pr", "list"] and "--head" not in command:
                calls.append(command)
                return subprocess.CompletedProcess(
                    command,
                    0,
                    stdout=(
                        '[{"number": 45, "labels": [], "headRefOid": "'
                        + head_sha
                        + '"}]\n'
                    ),
                    stderr="",
                )
            return runner(command, **kwargs)

        result = {
            "branch": {
                "name": "local-renamed-pr-branch",
                "loader": "fabric",
                "version": "1.21.x",
            },
            "passed": True,
            "tested_instances": 1,
            "logs": [],
        }

        with contextlib.redirect_stdout(io.StringIO()):
            published = curseforge_testing.publish_pull_request_test_result(
                result,
                Path("/repo"),
                runner=commit_matching_runner,
            )

        self.assertTrue(published)
        self.assertIn(["git", "rev-parse", "local-renamed-pr-branch"], calls)
        self.assertTrue(any(command[:4] == ["gh", "pr", "edit", "45"] for command in calls))


class BranchTestResultTests(unittest.TestCase):
    def test_cached_instances_count_toward_the_completed_branch_result(self):
        first = {"de_game_version": "1.21.1"}
        second = {"de_game_version": "1.21.2"}
        plan = {
            "branch": {
                "name": "minecraft-fabric-1.21.x-fix-null-guard",
                "loader": "fabric",
                "version": "1.21.x",
            },
            "instances": [second],
            "cached_instances": [first],
        }

        results = curseforge_testing.initialize_branch_test_results([plan])
        curseforge_testing.record_branch_instance_result(results, plan, True, [])

        result = results[plan["branch"]["name"]]
        self.assertTrue(result["passed"])
        self.assertEqual(result["tested_instances"], 2)

    def test_any_failed_instance_marks_the_branch_failed_and_keeps_its_logs(self):
        plan = {
            "branch": {
                "name": "minecraft-fabric-1.21.x-fix-null-guard",
                "loader": "fabric",
                "version": "1.21.x",
            }
        }
        results = curseforge_testing.initialize_branch_test_results([plan])
        failure_log = Path("failure.log")

        curseforge_testing.record_branch_instance_result(results, plan, True, [])
        curseforge_testing.record_branch_instance_result(results, plan, False, [failure_log])

        result = results[plan["branch"]["name"]]
        self.assertFalse(result["passed"])
        self.assertEqual(result["tested_instances"], 2)
        self.assertEqual(result["logs"], [failure_log])

    def test_write_test_failure_log_creates_a_readable_log_file(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            log_path = curseforge_testing.write_test_failure_log(
                "minecraft/fabric-1.21.x build",
                "Gradle failed\n",
                log_dir=Path(temp_dir),
            )

            self.assertEqual(log_path.read_text(encoding="utf-8"), "Gradle failed\n")
            self.assertEqual(log_path.name, "minecraft-fabric-1.21.x-build.log")

    def test_unlaunched_setup_is_reported_as_failed(self):
        failure_log = Path("build-failure.log")
        plan = {
            "branch": {
                "name": "minecraft-forge-1.20.x-fix-null-guard",
                "loader": "forge",
                "version": "1.20.x",
                "test_failure_logs": [failure_log],
            }
        }

        results = curseforge_testing.complete_branch_test_results([plan], [])

        self.assertEqual(len(results), 1)
        self.assertFalse(results[0]["passed"])
        self.assertEqual(results[0]["tested_instances"], 0)
        self.assertEqual(results[0]["logs"], [failure_log])


class RecipeResultGatingTests(unittest.TestCase):
    def run_recipe_flow(self, advancement_result):
        recipe_tests = [{"advancement": "dissolver_enhanced:test", "trigger_items": []}]
        with (
            mock.patch.dict("sys.modules", {"pyautogui": object()}),
            mock.patch.object(curseforge_testing, "load_recipe_unlock_tests", return_value=recipe_tests),
            mock.patch.object(curseforge_testing, "wait_for_recipe_advancements", return_value=advancement_result),
            mock.patch.object(curseforge_testing, "minecraft_processes_for_instance", return_value=[("123", "java")]),
            mock.patch.object(curseforge_testing, "send_chat_line"),
            mock.patch.object(curseforge_testing, "pause_unpause_to_save"),
            mock.patch.object(curseforge_testing, "place_smoke_test_blocks"),
            mock.patch.object(curseforge_testing.time, "sleep"),
        ):
            return curseforge_testing.type_testing_started_message(
                {"path": Path("unused")},
                verbose=False,
            )

    def test_incomplete_recipe_check_fails_instance_setup(self):
        self.assertFalse(self.run_recipe_flow((0, 1, [])))

    def test_complete_recipe_check_passes_instance_setup(self):
        self.assertTrue(self.run_recipe_flow((1, 1, ["dissolver_enhanced:test"])))

    def test_early_instance_exit_stops_typing_remaining_steps(self):
        recipe_tests = [{"advancement": "dissolver_enhanced:test", "trigger_items": []}]
        with (
            mock.patch.dict("sys.modules", {"pyautogui": object()}),
            mock.patch.object(curseforge_testing, "load_recipe_unlock_tests", return_value=recipe_tests),
            mock.patch.object(
                curseforge_testing,
                "minecraft_processes_for_instance",
                side_effect=[[('123', 'java')], []],
            ),
            mock.patch.object(curseforge_testing, "send_chat_line") as send_chat_line,
            mock.patch.object(curseforge_testing, "pause_unpause_to_save"),
            mock.patch.object(curseforge_testing, "place_smoke_test_blocks"),
            mock.patch.object(curseforge_testing.time, "sleep"),
        ):
            passed = curseforge_testing.type_testing_started_message(
                {"path": Path("unused")},
                verbose=False,
            )

        self.assertFalse(passed)
        self.assertEqual(send_chat_line.call_count, 1)


class RecipeAdvancementDiscoveryTests(unittest.TestCase):
    def test_finds_legacy_world_advancement_file(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            instance_path = Path(temp_dir) / "instance"
            expected = instance_path / "saves" / "New World" / "advancements" / "player.json"
            expected.parent.mkdir(parents=True)
            expected.write_text("{}", encoding="utf-8")

            self.assertEqual(
                curseforge_testing.latest_advancement_file({"path": instance_path}),
                expected,
            )

    def test_finds_minecraft_26_player_advancement_file(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            instance_path = Path(temp_dir) / "instance"
            expected = instance_path / "saves" / "New World" / "players" / "advancements" / "player.json"
            expected.parent.mkdir(parents=True)
            expected.write_text("{}", encoding="utf-8")

            self.assertEqual(
                curseforge_testing.latest_advancement_file({"path": instance_path}),
                expected,
            )


class BuildTestCacheTests(unittest.TestCase):
    def test_gradle_build_cleans_stale_outputs_before_assemble(self):
        command = curseforge_testing.gradle_clean_command(
            ["sh", "gradlew", "assemble"]
        )

        self.assertEqual(
            command,
            [
                "sh",
                "gradlew",
                "clean",
                "assemble",
                "--console=plain",
                "--warning-mode=summary",
            ],
        )

    def test_artifact_discovery_ignores_icloud_conflict_copy_jar(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            libs = Path(temp_dir) / "build" / "libs"
            libs.mkdir(parents=True)
            canonical = libs / "dissolver-enhanced.jar"
            conflict_copy = libs / "dissolver-enhanced 2.jar"
            canonical.write_bytes(b"canonical")
            conflict_copy.write_bytes(b"stale conflict copy")

            artifact = curseforge_testing.find_branch_artifact(Path(temp_dir))

        self.assertEqual(artifact, canonical)

    def test_common_archive_is_configured_for_reproducible_sha256(self):
        common_build = SCRIPT_PATH.parents[1] / "minecraft" / "build.gradle"
        build_text = common_build.read_text(encoding="utf-8")

        self.assertIn("tasks.withType(AbstractArchiveTask).configureEach", build_text)
        self.assertIn("preserveFileTimestamps = false", build_text)
        self.assertIn("reproducibleFileOrder = true", build_text)

    def test_artifact_sha256_changes_when_jar_bytes_change(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            artifact = Path(temp_dir) / "dissolver.jar"
            artifact.write_bytes(b"first build")
            first_hash = curseforge_testing.artifact_sha256(artifact)

            artifact.write_bytes(b"second build")
            second_hash = curseforge_testing.artifact_sha256(artifact)

        self.assertEqual(len(first_hash), 64)
        self.assertNotEqual(first_hash, second_hash)

    def test_cached_pass_requires_the_same_jar_hash_and_test_count(self):
        plan = {
            "branch": {
                "name": "minecraft-fabric-1.21.x-fix-null-guard",
                "loader": "fabric",
                "version": "1.21.x",
            },
            "instances": [{}, {}],
        }
        with tempfile.TemporaryDirectory() as temp_dir:
            cache_path = Path(temp_dir) / "cache.data"
            cache = {}
            curseforge_testing.record_cached_pass(
                cache,
                plan,
                jar_hash="a" * 64,
                test_count=37,
                cache_path=cache_path,
            )
            loaded = curseforge_testing.load_test_cache(cache_path)

        cached = curseforge_testing.cached_pass_result(loaded, plan, "a" * 64, 37)
        changed_hash = curseforge_testing.cached_pass_result(loaded, plan, "b" * 64, 37)
        changed_count = curseforge_testing.cached_pass_result(loaded, plan, "a" * 64, 38)

        self.assertTrue(cached["passed"])
        self.assertTrue(cached["cached"])
        self.assertEqual(cached["passed_tests"], 37)
        self.assertEqual(cached["tested_instances"], 2)
        self.assertIsNone(changed_hash)
        self.assertIsNone(changed_count)
        self.assertEqual(
            loaded["fabric-1.21.x"]["note"],
            "Fabric-1.21.x = passed 37 tests",
        )

    def test_branch_test_count_recalculates_from_current_test_definition(self):
        plan = {"instances": [{"name": "one"}, {"name": "two"}]}

        old_count = curseforge_testing.branch_test_count(plan, instance_counter=lambda _instance: 10)
        expanded_count = curseforge_testing.branch_test_count(plan, instance_counter=lambda _instance: 11)

        self.assertEqual(old_count, 20)
        self.assertEqual(expanded_count, 22)

    def test_successful_game_version_is_written_to_cache_immediately(self):
        instance = {"de_game_version": "1.21.1"}
        plan = {
            "branch": {
                "name": "minecraft-fabric-1.21.x-fix-null-guard",
                "loader": "fabric",
                "version": "1.21.x",
            },
            "instances": [instance],
        }

        with tempfile.TemporaryDirectory() as temp_dir:
            cache_path = Path(temp_dir) / "cache.data"
            cache = {}
            curseforge_testing.record_cached_instance_pass(
                cache,
                plan,
                instance,
                jar_hash="d" * 64,
                test_count=41,
                cache_path=cache_path,
            )
            disk_cache = curseforge_testing.load_test_cache(cache_path)

        cached = curseforge_testing.cached_instance_pass(
            disk_cache,
            plan,
            instance,
            jar_hash="d" * 64,
            test_count=41,
        )
        self.assertTrue(cached)
        self.assertEqual(
            disk_cache["fabric-1.21.x"]["instances"]["1.21.1"]["passed_tests"],
            41,
        )

    def test_partial_cache_skips_only_versions_that_already_passed(self):
        first = {"de_game_version": "1.21.1"}
        second = {"de_game_version": "1.21.2"}
        plan = {
            "branch": {
                "name": "minecraft-fabric-1.21.x-fix-null-guard",
                "loader": "fabric",
                "version": "1.21.x",
            },
            "instances": [first, second],
        }
        with tempfile.TemporaryDirectory() as temp_dir:
            cache_path = Path(temp_dir) / "cache.data"
            cache = {}
            curseforge_testing.record_cached_instance_pass(
                cache,
                plan,
                first,
                jar_hash="e" * 64,
                test_count=41,
                cache_path=cache_path,
            )

        cached, pending = curseforge_testing.partition_cached_instances(
            cache,
            plan,
            jar_hash="e" * 64,
            instance_counter=lambda _instance: 41,
        )
        self.assertEqual(cached, [first])
        self.assertEqual(pending, [second])

    def test_success_is_cached_before_branch_pr_is_published(self):
        plan = {
            "branch": {
                "name": "minecraft-fabric-1.21.x-fix-null-guard",
                "loader": "fabric",
                "version": "1.21.x",
            },
            "instances": [{}],
            "jar_sha256": "c" * 64,
            "test_count": 37,
            "cache_enabled": True,
        }
        result = {
            "branch": plan["branch"],
            "passed": True,
            "tested_instances": 1,
            "logs": [],
        }
        cache = {}
        published = []

        with tempfile.TemporaryDirectory() as temp_dir:
            cache_path = Path(temp_dir) / "cache.data"

            def publisher(published_result, _repo_root):
                disk_cache = curseforge_testing.load_test_cache(cache_path)
                published.append((published_result, disk_cache))
                return True

            curseforge_testing.finalize_branch_test_result(
                result,
                plan,
                cache,
                Path("/repo"),
                cache_path=cache_path,
                publisher=publisher,
            )

        self.assertEqual(len(published), 1)
        self.assertEqual(published[0][1]["fabric-1.21.x"]["passed_tests"], 37)
        self.assertEqual(published[0][0]["passed_tests"], 37)

    def test_branch_completion_callback_runs_for_each_finished_plan(self):
        plans = [
            {"branch": {"name": "first"}},
            {"branch": {"name": "second"}},
        ]
        results = curseforge_testing.initialize_branch_test_results(plans)
        completed = []

        curseforge_testing.emit_branch_test_result(results, plans[0], lambda result, _plan: completed.append(result["branch"]["name"]))
        self.assertEqual(completed, ["first"])

        curseforge_testing.emit_branch_test_result(results, plans[1], lambda result, _plan: completed.append(result["branch"]["name"]))
        self.assertEqual(completed, ["first", "second"])

    def test_instance_completion_callback_runs_before_the_next_version(self):
        plan = {
            "branch": {"name": "first", "loader": "fabric"},
            "loader_version": "Fabric 1.21.x",
            "instances": [
                {"folder": "one", "de_game_version": "1.21.1"},
                {"folder": "two", "de_game_version": "1.21.2"},
            ],
        }
        events = []
        originals = {
            "launch_instance": curseforge_testing.launch_instance,
            "estimated_instance_steps": curseforge_testing.estimated_instance_steps,
            "append_test_result": curseforge_testing.append_test_result,
        }

        def launch(instance, *_args, **_kwargs):
            events.append(f"launch:{instance['de_game_version']}")
            return True

        try:
            setattr(curseforge_testing, "launch_instance", launch)
            setattr(curseforge_testing, "estimated_instance_steps", lambda _instance: 1)
            setattr(curseforge_testing, "append_test_result", lambda _line: None)
            with contextlib.redirect_stdout(io.StringIO()):
                curseforge_testing.launch_all_instances_compact(
                    [plan],
                    True,
                    True,
                    on_instance_complete=lambda _result, _plan, instance, _passed: events.append(
                        f"cached:{instance['de_game_version']}"
                    ),
                )
        finally:
            for name, value in originals.items():
                setattr(curseforge_testing, name, value)

        self.assertEqual(
            events,
            [
                "launch:1.21.1",
                "cached:1.21.1",
                "launch:1.21.2",
                "cached:1.21.2",
            ],
        )


if __name__ == "__main__":
    unittest.main()
