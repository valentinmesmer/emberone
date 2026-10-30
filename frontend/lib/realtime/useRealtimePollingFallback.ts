/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useRealtime } from './context';

/** Poll only when realtime is disabled or disconnected. */
export function useRealtimePollingFallback(intervalMs: number): number | false {
  const { isEnabled, isConnected } = useRealtime();
  return isEnabled && isConnected ? false : intervalMs;
}
