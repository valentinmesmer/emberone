/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useRealtime } from './context';

/** Poll only when realtime is disabled or disconnected. */
export function useRealtimePollingFallback(intervalMs: number): number | false {
  const { isEnabled, isConnected } = useRealtime();
  return isEnabled && isConnected ? false : intervalMs;
}
