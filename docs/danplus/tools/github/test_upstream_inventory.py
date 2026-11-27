import unittest

from upstream_inventory import inventory, risk_areas


class InventoryTest(unittest.TestCase):
    def test_deduplication_and_risk(self):
        base, head = "a" * 40, "b" * 40
        patch = {"sha": head, "commit": {"message": "Fix player\nDetails"}}
        first = {"status": "ahead", "commits": [patch], "total_commits": 1,
                 "files": [{"filename": "app/player/Playback.kt", "status": "modified"}]}
        releases = [{"id": 3, "draft": False, "tag_name": "v3", "prerelease": True},
                    {"id": 2, "draft": False}, {"id": 4, "draft": True}]

        def api(path, paginate=False):
            if "/compare/" in path:
                return [first, {"commits": [patch]}]
            if "/releases" in path:
                return [releases]
            return {"sha": head}

        result = inventory("owner/repo", {"commit": base, "releases": [2]}, api)
        self.assertEqual({head: "Fix player"}, result["commits"])
        self.assertEqual({3: {"tag": "v3", "prerelease": True}}, result["releases"])
        self.assertEqual(["player"], result["risk_areas"])
        self.assertFalse(result["file_list_may_be_truncated"])

    def test_divergent_upstream_requires_review(self):
        def api(path, paginate=False):
            return [{"status": "diverged"}] if paginate else {"sha": "b" * 40}

        with self.assertRaisesRegex(ValueError, "no longer an ancestor"):
            inventory("owner/repo", {"commit": "a" * 40, "releases": []}, api)

    def test_risk_assignment_is_path_based(self):
        self.assertEqual(["build", "security"], risk_areas(["build.gradle.kts", "auth/Login.kt"]))
