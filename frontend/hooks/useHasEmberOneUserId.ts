/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useAuth } from '@/lib/auth/AuthContext';
import { getUserIdFromToken } from '@/lib/utils/tokenUserId';

/** True when the access token carries a linked EmberOne user id. */
export function useHasEmberOneUserId(): boolean {
  const { accessToken, isAuthenticated, isLoading } = useAuth();
  if (isLoading || !isAuthenticated) {
    return false;
  }
  return getUserIdFromToken(accessToken) != null;
}
