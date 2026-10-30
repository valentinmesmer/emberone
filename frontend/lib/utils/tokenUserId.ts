/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { USER_ID } from '@/lib/auth/claims';

/**
 * Decodes the JWT token payload.
 */
function decodeTokenPayload(token: string | null): Record<string, unknown> | null {
  if (!token) {
    return null;
  }

  try {
    const parts = token.split('.');
    if (parts.length !== 3) {
      return null;
    }

    const payload = parts[1];
    return JSON.parse(atob(payload));
  } catch {
    return null;
  }
}

/**
 * Extracts the EmberOne tenant-user id from a JWT token.
 * Platform admin ({@code userId=0}) has no {@code users} row and is treated as absent.
 */
export function getUserIdFromToken(token: string | null): string | null {
  const decoded = decodeTokenPayload(token);
  if (!decoded) {
    return null;
  }

  const userId = decoded[USER_ID];
  let normalized: string | null = null;
  if (typeof userId === 'string' && userId.trim() !== '') {
    normalized = userId.trim();
  } else if (typeof userId === 'number' && Number.isFinite(userId)) {
    normalized = String(userId);
  }
  if (normalized == null || normalized === '0') {
    return null;
  }
  return normalized;
}
