#!/usr/bin/env python3
# Copyright (C) 2026 Valentin Mesmer
# This file is part of EmberOne.
# SPDX-License-Identifier: AGPL-3.0-or-later

import importlib.util
import unittest
from pathlib import Path

MODULE_PATH = Path(__file__).with_name("check-conventional-commit.py")
spec = importlib.util.spec_from_file_location("check_conventional_commit", MODULE_PATH)
module = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(module)


class ValidateHeaderTest(unittest.TestCase):
    def test_accepts_common_types(self) -> None:
        for header in (
            "feat: add hooks",
            "fix(api): reject missing tenant header",
            "docs: describe local git hooks",
            "feat!: drop legacy endpoint",
            "chore(deps): bump spring boot",
        ):
            with self.subTest(header=header):
                self.assertIsNone(module.validate_header(header))

    def test_rejects_invalid_subject(self) -> None:
        for header in (
            "added hooks",
            "feat add hooks",
            "feature: add hooks",
            "feat:",
            "feat: ",
        ):
            with self.subTest(header=header):
                self.assertEqual(
                    module.validate_header(header), "not a Conventional Commit"
                )

    def test_skips_merge_and_revert(self) -> None:
        for header in (
            "Merge pull request #35 from org/branch",
            "Revert \"feat: add hooks\"",
            "fixup! feat: add hooks",
            "squash! feat: add hooks",
        ):
            with self.subTest(header=header):
                self.assertIsNone(module.validate_header(header))

    def test_rejects_long_subject(self) -> None:
        header = "feat: " + ("x" * 95)
        error = module.validate_header(header)
        self.assertIsNotNone(error)
        assert error is not None
        self.assertIn("max 100", error)


class ExtractHeaderTest(unittest.TestCase):
    def test_ignores_comments_and_uses_first_line(self) -> None:
        text = "# Please enter the commit message.\nfeat: add hooks\n\nBody."
        self.assertEqual(module.extract_header(text), "feat: add hooks")

    def test_returns_none_for_empty_message(self) -> None:
        self.assertIsNone(module.extract_header("# comment only\n"))


if __name__ == "__main__":
    unittest.main()
