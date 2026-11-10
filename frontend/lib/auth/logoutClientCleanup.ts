/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { queryClient } from '@/lib/store/query-client';

const OIDC_LOCAL_STORAGE_PREFIX = 'oidc.';

function clearOidcLocalStorage(): void {
  if (typeof window === 'undefined') {
    return;
  }
  try {
    const keys: string[] = [];
    for (let index = 0; index < localStorage.length; index++) {
      const key = localStorage.key(index);
      if (key?.startsWith(OIDC_LOCAL_STORAGE_PREFIX)) {
        keys.push(key);
      }
    }
    for (const key of keys) {
      localStorage.removeItem(key);
    }
  } catch {
    // ignore
  }
}

export async function clearServiceWorkerCaches(): Promise<void> {
  if (typeof window === 'undefined' || !('caches' in window)) {
    return;
  }
  try {
    const cacheNames = await caches.keys();
    await Promise.all(cacheNames.map((name) => caches.delete(name)));
  } catch {
    // ignore
  }
}

/**
 * Runs before `signoutRedirect()` so cached server state (including permissions) and shell/assets do not
 * survive logout. Clearing the query cache also frees profile image blobs. OIDC user keys stay until sign-out (id_token_hint) or error cleanup.
 */
export async function clearClientCachesBeforeLogout(): Promise<void> {
  queryClient.clear();
  await clearServiceWorkerCaches();
}

/** When sign-out fails or as a fallback after `removeUser()`. */
export function clearAuthLocalStorageAfterLogoutFailure(): void {
  queryClient.clear();
  clearOidcLocalStorage();
}
