/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

// No static imports from oidc-client-ts (breaks SSR)

import { resolveOidcClientSettings } from '@/lib/idp';

type OidcUserManager = {
  getUser: () => Promise<OidcUser | null>;
  removeUser: () => Promise<void>;
  signinRedirect: (args?: { prompt?: string }) => Promise<void>;
  signinRedirectCallback: () => Promise<OidcUser>;
  signinSilent: () => Promise<OidcUser | null>;
  signinSilentCallback: () => Promise<void>;
  signoutRedirect: () => Promise<void>;
  events: {
    addUserLoaded: (cb: (user: OidcUser) => void) => void;
    removeUserLoaded: (cb: (user: OidcUser) => void) => void;
    addUserUnloaded: (cb: () => void) => void;
    removeUserUnloaded: (cb: () => void) => void;
    addSilentRenewError: (cb: (error: Error) => void) => void;
    removeSilentRenewError: (cb: (error: Error) => void) => void;
  };
};

export type OidcUser = {
  expired: boolean;
  access_token: string;
  profile: Record<string, unknown>;
};

let userManagerInstance: OidcUserManager | null = null;
let initPromise: Promise<OidcUserManager | null> | null = null;

export async function ensureUserManager(): Promise<OidcUserManager | null> {
  if (typeof window === 'undefined') {
    return null;
  }

  if (userManagerInstance) {
    return userManagerInstance;
  }

  if (!initPromise) {
    initPromise = (async () => {
      const { UserManager, WebStorageStateStore } = await import('oidc-client-ts');
      const { getRuntimeConfig } = await import('@/lib/runtimeConfig');

      const cfg = await getRuntimeConfig();
      const oidc = resolveOidcClientSettings(cfg);
      if (!oidc) {
        return null;
      }

      userManagerInstance = new UserManager({
        authority: oidc.authority,
        metadata: oidc.metadata,
        client_id: oidc.clientId,
        redirect_uri: `${window.location.origin}/auth/callback`,
        silent_redirect_uri: `${window.location.origin}/auth/silent-callback`,
        response_type: 'code',
        scope: oidc.scope,
        automaticSilentRenew: true,
        userStore: new WebStorageStateStore({ store: window.localStorage }),
      }) as unknown as OidcUserManager;

      return userManagerInstance;
    })();
  }

  return initPromise;
}
