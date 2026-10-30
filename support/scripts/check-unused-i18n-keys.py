#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
"""
Report translation keys in frontend/locales/de.json that appear unused in frontend TS/TSX.

Heuristic: exact quoted key strings plus dynamic prefixes from template literals and
key-builder return patterns. False positives are unlikely; false negatives possible for
keys built from runtime data only.
"""

from __future__ import annotations

import json
import re
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
FRONTEND = ROOT / "frontend"
DE_JSON = FRONTEND / "locales" / "de.json"


def flatten(obj: object, prefix: str = "") -> dict[str, str]:
    items: dict[str, str] = {}
    if isinstance(obj, dict):
        for key, value in obj.items():
            dotted = f"{prefix}.{key}" if prefix else key
            if isinstance(value, dict):
                items.update(flatten(value, dotted))
            elif isinstance(value, str):
                items[dotted] = value
    return items


def load_corpus() -> str:
    parts: list[str] = []
    for path in list(FRONTEND.rglob("*.ts")) + list(FRONTEND.rglob("*.tsx")):
        if "node_modules" in path.parts or ".next" in path.parts:
            continue
        parts.append(path.read_text(encoding="utf-8"))
    return "\n".join(parts)


def collect_dynamic_prefixes(text: str) -> set[str]:
    prefixes: set[str] = set()
    for match in re.finditer(r"`([a-zA-Z][a-zA-Z0-9_.-]*(?:\.[a-zA-Z0-9_.-]*)+)\.\$\{", text):
        prefixes.add(match.group(1))
    for match in re.finditer(r"t\(\s*`([a-zA-Z][a-zA-Z0-9_.-]*)\.\$\{", text, re.MULTILINE):
        prefixes.add(match.group(1))
    for match in re.finditer(
        r"""return\s+['"]([a-zA-Z][a-zA-Z0-9_.-]*(?:\.[a-zA-Z0-9_.-]+)+)\.['"]\s*\+""",
        text,
    ):
        prefixes.add(match.group(1))
    for match in re.finditer(
        r"""return\s+`([a-zA-Z][a-zA-Z0-9_.-]*(?:\.[a-zA-Z0-9_.-]*)+)\.\$\{""",
        text,
    ):
        prefixes.add(match.group(1))
    return prefixes


def main() -> int:
    data = json.loads(DE_JSON.read_text(encoding="utf-8"))
    all_keys = set(flatten(data).keys())
    corpus = load_corpus()

    exact_used = {
        key
        for key in all_keys
        if f"'{key}'" in corpus or f'"{key}"' in corpus or f"`{key}`" in corpus
    }
    dynamic_prefixes = collect_dynamic_prefixes(corpus)

    def is_used(key: str) -> bool:
        if key in exact_used:
            return True
        for prefix in dynamic_prefixes:
            if key == prefix or key.startswith(prefix + "."):
                return True
        return False

    unused = sorted(key for key in all_keys if not is_used(key))
    if unused:
        print(f"Unused translation keys ({len(unused)}):", file=sys.stderr)
        for key in unused:
            print(f"  {key}", file=sys.stderr)
        return 1

    print(f"OK: all {len(all_keys)} keys appear used.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
