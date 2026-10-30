#!/usr/bin/env bash
# Copyright (C) 2026 Valentin Mesmer
# This file is part of EmberOne.
# SPDX-License-Identifier: AGPL-3.0-or-later

set -euo pipefail

ROOT="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"

if ! git -C "$ROOT" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "Not a git repository." >&2
  exit 1
fi

chmod +x "$ROOT/.githooks/pre-commit" "$ROOT/.githooks/commit-msg"
git -C "$ROOT" config core.hooksPath .githooks

echo "Git hooks enabled (core.hooksPath=.githooks)."
echo "  pre-commit - Biome (frontend) + google-java-format AOSP (backend)"
echo "  commit-msg - Conventional Commits"
