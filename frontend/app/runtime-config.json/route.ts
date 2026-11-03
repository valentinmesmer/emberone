/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { publicRuntimeConfigFromEnv } from '@/lib/runtimeConfig';

/** Dev/local: Docker still writes `public/runtime-config.json` at container start. */
export function GET() {
  return Response.json(publicRuntimeConfigFromEnv(), {
    headers: { 'Cache-Control': 'no-store' },
  });
}
