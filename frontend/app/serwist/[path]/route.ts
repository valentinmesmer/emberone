/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { spawnSync } from 'node:child_process';
import { createSerwistRoute } from '@serwist/turbopack';

/**
 * Revision for extra precache entries (offline shell). Prefer CI commit SHA;
 * fall back to git HEAD locally; last resort a build-time UUID (Docker without .git).
 */
const revision =
  process.env.SOURCE_COMMIT?.trim() ||
  process.env.GIT_COMMIT?.trim() ||
  spawnSync('git', ['rev-parse', 'HEAD'], { encoding: 'utf-8' }).stdout?.trim() ||
  crypto.randomUUID();

export const { dynamic, dynamicParams, revalidate, generateStaticParams, GET } = createSerwistRoute(
  {
    additionalPrecacheEntries: [{ url: '/~offline', revision }],
    // Written at container start, so its build-time hash would pin a stale copy forever.
    globIgnores: ['public/runtime-config.json'],
    swSrc: 'app/sw.ts',
    useNativeEsbuild: true,
  },
);
