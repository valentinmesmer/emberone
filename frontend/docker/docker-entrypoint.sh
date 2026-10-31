#!/bin/sh

# Copyright (C) 2026 Valentin Mesmer
# This file is part of EmberOne.
# SPDX-License-Identifier: Elastic-2.0

set -e
cd /app
bun /write-runtime-config.cjs
exec su-exec nextjs "$@"
