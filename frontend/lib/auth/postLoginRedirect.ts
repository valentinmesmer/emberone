/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/**
 * Preserves the in-app URL across the OIDC redirect round-trip so deep links
 * (e.g. appointment detail) work after login instead of always landing on /.
 */

export const POST_LOGIN_REDIRECT_STORAGE_KEY = 'emberone_post_login_redirect';

function isSafeRelativeReturnPath(path: string): boolean {
  if (!path.startsWith('/')) {
    return false;
  }
  // Block protocol-relative URLs used in open redirects
  if (path.startsWith('//')) {
    return false;
  }
  // Avoid sending users back into the auth flow
  if (path === '/auth/callback' || path.startsWith('/auth/callback/')) {
    return false;
  }
  return true;
}

/** Call immediately before signinRedirect(). */
export function rememberReturnPathForSignIn(): void {
  if (typeof window === 'undefined') {
    return;
  }
  try {
    const full = `${window.location.pathname}${window.location.search}${window.location.hash}`;
    if (!isSafeRelativeReturnPath(full)) {
      return;
    }
    sessionStorage.setItem(POST_LOGIN_REDIRECT_STORAGE_KEY, full);
  } catch {
    // Quota or private mode - ignore
  }
}

/** Remember an explicit in-app path for the next OIDC round-trip. */
export function rememberExplicitReturnPath(path: string): void {
  if (typeof window === 'undefined') {
    return;
  }
  try {
    if (!isSafeRelativeReturnPath(path)) {
      return;
    }
    sessionStorage.setItem(POST_LOGIN_REDIRECT_STORAGE_KEY, path);
  } catch {
    // Quota or private mode - ignore
  }
}

/** Returns the remembered path or fallback, and clears storage (one-time use). */
export function consumeRememberedReturnPath(fallback: string): string {
  if (typeof window === 'undefined') {
    return fallback;
  }
  try {
    const raw = sessionStorage.getItem(POST_LOGIN_REDIRECT_STORAGE_KEY);
    sessionStorage.removeItem(POST_LOGIN_REDIRECT_STORAGE_KEY);
    if (!raw || !isSafeRelativeReturnPath(raw)) {
      return fallback;
    }
    return raw;
  } catch {
    return fallback;
  }
}

export function clearRememberedReturnPath(): void {
  if (typeof window === 'undefined') {
    return;
  }
  try {
    sessionStorage.removeItem(POST_LOGIN_REDIRECT_STORAGE_KEY);
  } catch {
    // ignore
  }
}
