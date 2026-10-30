/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { ensureUserManager } from './oidcConfig';
import { rememberExplicitReturnPath, rememberReturnPathForSignIn } from './postLoginRedirect';

let silentRenewInFlight: Promise<string | null> | null = null;
let loginRedirectInFlight: Promise<void> | null = null;

function isAuthCallbackPath(): boolean {
  if (typeof window === 'undefined') {
    return true;
  }
  const path = window.location.pathname;
  return (
    path === '/auth/callback' ||
    path.startsWith('/auth/callback/') ||
    path === '/auth/silent-callback' ||
    path.startsWith('/auth/silent-callback/')
  );
}

/**
 * Force a silent OIDC renew (prompt=none iframe). Concurrent callers share one
 * in-flight renew so a burst of 401s does not stampede the IdP.
 */
export function forceSilentRenew(): Promise<string | null> {
  if (silentRenewInFlight) {
    return silentRenewInFlight;
  }

  silentRenewInFlight = (async () => {
    const userManager = await ensureUserManager();
    if (!userManager) {
      return null;
    }
    try {
      const renewed = await userManager.signinSilent();
      const token = renewed?.access_token?.trim();
      return token ? token : null;
    } catch {
      try {
        await userManager.removeUser();
      } catch {
        // Storage may already be clear
      }
      return null;
    }
  })().finally(() => {
    silentRenewInFlight = null;
  });

  return silentRenewInFlight;
}

/**
 * Single-flight redirect to the IdP login.
 * Optional `returnPath` overrides the current location for the post-login return.
 */
export function redirectToLogin(returnPath?: string): Promise<void> {
  if (loginRedirectInFlight) {
    return loginRedirectInFlight;
  }
  if (typeof window === 'undefined' || isAuthCallbackPath()) {
    return Promise.resolve();
  }

  loginRedirectInFlight = (async () => {
    const userManager = await ensureUserManager();
    if (!userManager) {
      return;
    }
    if (returnPath) {
      rememberExplicitReturnPath(returnPath);
    } else {
      rememberReturnPathForSignIn();
    }
    await userManager.signinRedirect();
  })().catch((error) => {
    loginRedirectInFlight = null;
    throw error;
  });

  return loginRedirectInFlight;
}
