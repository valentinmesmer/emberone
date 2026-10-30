#!/usr/bin/env python3
# Copyright (C) 2026 Valentin Mesmer
# This file is part of EmberOne.
# SPDX-License-Identifier: AGPL-3.0-or-later

"""Add or verify AGPL copyright headers on backend and frontend source files.

Usage:
  python3 support/scripts/add-copyright-headers.py           # write missing/outdated headers
  python3 support/scripts/add-copyright-headers.py --check   # exit 1 if any file is missing a header
  python3 support/scripts/add-copyright-headers.py --dry-run # print actions without writing
"""

from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path


def find_repo_root() -> Path:
    script_dir = Path(__file__).resolve().parent
    try:
        top = subprocess.run(
            ["git", "rev-parse", "--show-toplevel"],
            check=True,
            capture_output=True,
            text=True,
            cwd=script_dir,
        ).stdout.strip()
        if top:
            return Path(top)
    except (OSError, subprocess.CalledProcessError):
        pass
    path = script_dir
    for _ in range(12):
        if (
            (path / "backend").is_dir()
            and (path / "frontend").is_dir()
            and (path / ".git").exists()
        ):
            return path
        if path.parent == path:
            break
        path = path.parent
    raise RuntimeError("Could not find repository root")


ROOT = find_repo_root()
SCAN_DIRS = ("backend", "frontend")

COPYRIGHT_HOLDER = "Valentin Mesmer"
COPYRIGHT_YEAR = "2026"
SPDX_ID = "AGPL-3.0-or-later"

HEADER_LINES = (
    f"Copyright (C) {COPYRIGHT_YEAR} {COPYRIGHT_HOLDER}",
    "This file is part of EmberOne.",
    f"SPDX-License-Identifier: {SPDX_ID}",
)

# Stable identity marker used to recognize an existing header block, independent
# of the exact copyright-holder wording (which may change over time). Using
# COPYRIGHT_HOLDER here would fail to match older headers after a wording
# change, causing a second header to be inserted instead of replacing the old
# one.
PROJECT_MARKER = "This file is part of EmberOne."

SKIP_DIR_NAMES = {
    ".git",
    ".gradle",
    ".next",
    "bin",
    "build",
    "coverage",
    "dist",
    "graphify-out",
    "node_modules",
    "out",
    "target",
}

SKIP_FILE_NAMES = {
    ".dockerignore",
    ".env",
    ".env.example",
    ".gitignore",
    ".npmrc",
    "gradle-wrapper.jar",
    "gradle-wrapper.properties",
    "gradlew",
    "gradlew.bat",
    "next-env.d.ts",
    "package-lock.json",
    "bun.lock",
    "tsconfig.tsbuildinfo",
}

# Third-party files that are not EmberOne original work (relative to the repo root).
SKIP_RELATIVE_PATHS: set[str] = set()

BLOCK_EXTS = {
    ".cjs",
    ".css",
    ".gradle",
    ".java",
    ".js",
    ".jsx",
    ".kts",
    ".mjs",
    ".scss",
    ".ts",
    ".tsx",
}
HASH_EXTS = {".properties", ".sh", ".yaml", ".yml"}
HTML_EXTS = {".html", ".svg"}
SQL_EXTS = {".sql"}
HASH_NAMES = {"dockerfile", "lombok.config"}

MARKER = f"SPDX-License-Identifier: {SPDX_ID}"


def comment_style_for(path: Path) -> str | None:
    if path.name.lower() in HASH_NAMES:
        return "hash"
    suffix = path.suffix.lower()
    if suffix in BLOCK_EXTS:
        return "block"
    if suffix in HASH_EXTS:
        return "hash"
    if suffix in HTML_EXTS:
        return "html"
    if suffix in SQL_EXTS:
        return "sql"
    return None


def format_header(style: str) -> str:
    if style == "block":
        inner = "\n".join(f" * {line}" if line else " *" for line in HEADER_LINES)
        return f"/*\n{inner}\n */\n"
    if style == "hash":
        return "\n".join(f"# {line}" if line else "#" for line in HEADER_LINES) + "\n"
    if style == "html":
        inner = "\n".join(f"  {line}" if line else "" for line in HEADER_LINES)
        return f"<!--\n{inner}\n-->\n"
    if style == "sql":
        return "\n".join(f"-- {line}" if line else "--" for line in HEADER_LINES) + "\n"
    raise ValueError(f"unknown comment style: {style}")


def read_file(path: Path) -> tuple[str, str]:
    data = path.read_bytes()
    newline = "\r\n" if b"\r\n" in data else "\n"
    text = data.decode("utf-8-sig").replace("\r\n", "\n").replace("\r", "\n")
    return text, newline


def write_file(path: Path, text: str, newline: str) -> None:
    if not text.endswith("\n"):
        text += "\n"
    path.write_bytes(text.replace("\n", newline).encode("utf-8"))


def split_preamble(text: str) -> tuple[str, str]:
    """Keep lines that must remain the first physical lines of a file."""
    remaining = text
    preamble_parts: list[str] = []

    if remaining.startswith("#!"):
        line, sep, remaining = remaining.partition("\n")
        preamble_parts.append(line + sep)
        remaining = remaining.lstrip("\n")

    if remaining.startswith("# syntax="):
        line, sep, remaining = remaining.partition("\n")
        preamble_parts.append(line + sep)
        remaining = remaining.lstrip("\n")

    if remaining.startswith("<?xml"):
        end = remaining.find("?>")
        if end != -1:
            chunk = remaining[: end + 2]
            remaining = remaining[end + 2 :]
            if remaining.startswith("\n"):
                chunk += "\n"
                remaining = remaining[1:]
            preamble_parts.append(chunk)
            remaining = remaining.lstrip("\n")

    if remaining[:200].lower().startswith("<!doctype"):
        end = remaining.find(">")
        if end != -1:
            chunk = remaining[: end + 1]
            remaining = remaining[end + 1 :]
            if remaining.startswith("\n"):
                chunk += "\n"
                remaining = remaining[1:]
            preamble_parts.append(chunk)
            remaining = remaining.lstrip("\n")

    return "".join(preamble_parts), remaining


def is_our_header_block(block: str) -> bool:
    return MARKER in block and PROJECT_MARKER in block


def strip_our_header(body: str, style: str) -> str:
    stripped = body.lstrip("\n")
    probe = stripped[:800]
    if MARKER not in probe and PROJECT_MARKER not in probe:
        return body

    if style == "block" and stripped.startswith("/*"):
        end = stripped.find("*/")
        if end != -1:
            block = stripped[: end + 2]
            if is_our_header_block(block):
                return stripped[end + 2 :].lstrip("\n")
        return body

    if style == "html" and stripped.startswith("<!--"):
        end = stripped.find("-->")
        if end != -1:
            block = stripped[: end + 3]
            if is_our_header_block(block):
                return stripped[end + 3 :].lstrip("\n")
        return body

    prefix = "#" if style == "hash" else "--" if style == "sql" else None
    if prefix is None:
        return body

    lines = stripped.split("\n")
    comment_lines: list[str] = []
    i = 0
    while i < len(lines) and (lines[i].startswith(prefix) or lines[i] == ""):
        if lines[i] == "" and comment_lines:
            break
        comment_lines.append(lines[i])
        i += 1
    block = "\n".join(comment_lines)
    if is_our_header_block(block):
        return "\n".join(lines[i:]).lstrip("\n")
    return body


def apply_header(text: str, style: str) -> str:
    preamble, body = split_preamble(text)
    body_without_old = strip_our_header(body, style)
    header = format_header(style)
    new_body = header + "\n" + body_without_old.lstrip("\n")
    if not preamble:
        return new_body
    if not preamble.endswith("\n"):
        preamble += "\n"
    return preamble + "\n" + new_body


def should_skip(path: Path) -> bool:
    rel = path.relative_to(ROOT).as_posix()
    if rel in SKIP_RELATIVE_PATHS:
        return True
    if path.name in SKIP_FILE_NAMES:
        return True
    if path.name.startswith("."):
        return True
    return comment_style_for(path) is None


def iter_source_files() -> list[Path]:
    files: list[Path] = []
    for dirname in SCAN_DIRS:
        root = ROOT / dirname
        if not root.is_dir():
            continue
        for path in root.rglob("*"):
            if not path.is_file():
                continue
            if any(part in SKIP_DIR_NAMES for part in path.parts):
                continue
            if should_skip(path):
                continue
            files.append(path)
    return sorted(files)


def has_our_header(text: str, style: str) -> bool:
    _, body = split_preamble(text)
    expected = format_header(style)
    return body.lstrip("\n").startswith(expected.strip())


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="Do not write files; fail if any header is missing or outdated.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print files that would change without writing.",
    )
    args = parser.parse_args()

    files = iter_source_files()
    missing: list[Path] = []
    updated = 0

    for path in files:
        style = comment_style_for(path)
        assert style is not None
        try:
            text, newline = read_file(path)
        except UnicodeDecodeError:
            print(f"skip (not utf-8): {path.relative_to(ROOT)}", file=sys.stderr)
            continue

        if has_our_header(text, style):
            continue

        missing.append(path)
        new_text = apply_header(text, style)
        rel = path.relative_to(ROOT).as_posix()

        if args.check or args.dry_run:
            print(rel)
            continue

        if new_text != text:
            write_file(path, new_text, newline)
            updated += 1
            print(rel)

    if args.check:
        if missing:
            print(
                f"\n{len(missing)} file(s) missing an up-to-date copyright header.\n"
                "Fix: python3 support/scripts/add-copyright-headers.py",
                file=sys.stderr,
            )
            return 1
        print(f"ok: {len(files)} file(s) have a copyright header.")
        return 0

    if args.dry_run:
        print(f"\n{len(missing)} file(s) would be updated.")
        return 0

    print(f"\nupdated {updated} file(s); scanned {len(files)}.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
