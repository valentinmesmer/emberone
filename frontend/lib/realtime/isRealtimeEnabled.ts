/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import type { PublicRuntimeConfig } from '@/lib/runtimeConfig';

/** Realtime is on by default; set `NEXT_PUBLIC_REALTIME_ENABLED=false` to disable. */
export function isRealtimeEnabled(config: PublicRuntimeConfig): boolean {
  return config.NEXT_PUBLIC_REALTIME_ENABLED !== 'false';
}
