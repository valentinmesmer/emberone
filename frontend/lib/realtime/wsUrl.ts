/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/** Builds the native WebSocket URL for the STOMP endpoint (`/ws`). */
export function buildRealtimeWebSocketUrl(apiBaseUrl: string): string {
  const trimmed = apiBaseUrl.replace(/\/$/, '');
  if (trimmed.startsWith('https://')) {
    return `wss://${trimmed.slice('https://'.length)}/ws`;
  }
  if (trimmed.startsWith('http://')) {
    return `ws://${trimmed.slice('http://'.length)}/ws`;
  }
  return `${trimmed}/ws`;
}
