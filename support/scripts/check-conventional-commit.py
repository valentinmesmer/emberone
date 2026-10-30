#!/usr/bin/env python3
# Copyright (C) 2026 Valentin Mesmer
# This file is part of EmberOne.
# SPDX-License-Identifier: AGPL-3.0-or-later

"""Validate commit messages against Conventional Commits.

Usage:
  python3 support/scripts/check-conventional-commit.py COMMIT_EDITMSG
  python3 support/scripts/check-conventional-commit.py --from <sha>
  python3 support/scripts/check-conventional-commit.py --message "feat: add hooks"
"""

from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys

TYPES = (
    "feat",
    "fix",
    "docs",
    "style",
    "refactor",
    "perf",
    "test",
    "build",
    "ci",
    "chore",
    "revert",
)

HEADER_RE = re.compile(
    rf"^(?:{'|'.join(TYPES)})(?:\([^)\n]+\))?!?: \S.*$"
)
SKIP_RE = re.compile(
    r"^(Merge |Revert |fixup! |squash! )",
    re.IGNORECASE,
)
MAX_HEADER_LENGTH = 100
ZERO_SHA = "0" * 40


def git_command(*args: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["git", *args],
        check=False,
        capture_output=True,
        text=True,
    )


def ref_exists(ref: str) -> bool:
    return git_command("rev-parse", "--verify", ref).returncode == 0


def is_ancestor(ancestor: str, descendant: str = "HEAD") -> bool:
    return (
        git_command("merge-base", "--is-ancestor", ancestor, descendant).returncode
        == 0
    )


def warn(message: str) -> None:
    print(message, file=sys.stderr)
    if os.environ.get("GITHUB_ACTIONS") == "true":
        escaped = message.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
        print(f"::warning::{escaped}")


def extract_header(text: str) -> str | None:
    lines: list[str] = []
    for line in text.splitlines():
        if line.startswith("#"):
            continue
        if line.startswith("diff --git"):
            break
        lines.append(line.rstrip())
    content = "\n".join(lines).strip()
    if not content:
        return None
    return content.split("\n", 1)[0].strip()


def validate_header(header: str) -> str | None:
    if SKIP_RE.match(header):
        return None
    if len(header) > MAX_HEADER_LENGTH:
        return f"subject is {len(header)} characters (max {MAX_HEADER_LENGTH})"
    if not HEADER_RE.match(header):
        return "not a Conventional Commit"
    return None


def fail(message: str, header: str | None = None) -> int:
    print("Invalid commit message:", file=sys.stderr)
    if header is not None:
        print(file=sys.stderr)
        print(f"  {header}", file=sys.stderr)
        print(file=sys.stderr)
    print(f"{message}.", file=sys.stderr)
    print(file=sys.stderr)
    print("Use Conventional Commits, for example:", file=sys.stderr)
    print("  feat: add git hooks for format and commit messages", file=sys.stderr)
    print("  fix(api): reject missing tenant header", file=sys.stderr)
    print("  docs: describe local git hooks", file=sys.stderr)
    print(file=sys.stderr)
    print(f"Allowed types: {', '.join(TYPES)}", file=sys.stderr)
    print("See https://www.conventionalcommits.org/", file=sys.stderr)
    return 1


def git_log_messages(base: str) -> list[str]:
    base = base.strip()
    if base == ZERO_SHA:
        rev_args: list[str] = []
        note = f"{base} is empty; checking all commits on HEAD"
    elif not ref_exists(base):
        rev_args = ["-1", "HEAD"]
        note = f"{base} is not available; checking HEAD only"
    elif not is_ancestor(base):
        rev_args = ["-1", "HEAD"]
        note = f"{base} is not an ancestor of HEAD; checking HEAD only"
    else:
        rev_args = [f"{base}..HEAD"]
        note = None

    if note:
        warn(f"Note: {note}.")

    result = git_command("log", "--format=%B%x00", *rev_args)
    if result.returncode != 0:
        err = result.stderr.strip() or result.stdout.strip() or "git log failed"
        raise SystemExit(f"Could not read commits from {base}: {err}")
    return [part for part in result.stdout.split("\0") if part.strip()]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("commit_file", nargs="?", help="Path to COMMIT_EDITMSG")
    parser.add_argument("--from", dest="base", help="Git SHA to check commits after")
    parser.add_argument("--message", help="Check a single message")
    args = parser.parse_args()

    if args.message is not None:
        messages = [args.message]
    elif args.base:
        if not args.base.strip():
            print("Missing --from SHA.", file=sys.stderr)
            return 1
        messages = git_log_messages(args.base)
        if not messages:
            return 0
    elif args.commit_file:
        messages = [open(args.commit_file, encoding="utf-8").read()]
    else:
        parser.print_help()
        return 2

    for raw in messages:
        header = extract_header(raw)
        if header is None:
            return fail("commit message is empty")
        error = validate_header(header)
        if error:
            return fail(error, header)
    return 0


if __name__ == "__main__":
    sys.exit(main())
