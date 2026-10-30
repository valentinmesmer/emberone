/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type React from 'react';
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { setTokenGetter } from '@/lib/api/tokenGetter';
import { canAccessAdministration, isGlobalAdmin } from './claims';
import { readAppRoleFromToken, readTenantIdFromToken } from './jwtPayload';
import {
  clearAuthLocalStorageAfterLogoutFailure,
  clearClientCachesBeforeLogout,
} from './logoutClientCleanup';
import { ensureUserManager, type OidcUser } from './oidcConfig';
import { forceSilentRenew, redirectToLogin } from './sessionRecovery';

export interface AuthAccount {
  username?: string;
  name?: string;
  email?: string;
}

interface AuthContextType {
  isAuthenticated: boolean;
  isLoading: boolean;
  isLoggingOut: boolean;
  accessToken: string | null;
  /** Resolved from ID token profile or access token JWT (`app_role`). */
  appRole: string | null;
  /** Resolved from JWT / profile (`tenantId`). */
  tenantId: number | null;
  /** Global admin only: create tenants, bootstrap root organization, etc. */
  isGlobalAdmin: boolean;
  /** Global or tenant admin: access to `/admin`. */
  canAccessAdministration: boolean;
  login: (returnPath?: string) => Promise<void>;
  logout: () => Promise<void>;
  getAccessToken: () => Promise<string | null>;
  account: AuthAccount | null;
  /** `false` when OIDC env is missing/invalid (no redirect to Authentik possible). */
  isOidcConfigured: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

function toAccount(user: OidcUser | null): AuthAccount | null {
  if (!user) {
    return null;
  }
  return {
    username: user.profile.preferred_username as string | undefined,
    name: user.profile.name as string | undefined,
    email: user.profile.email as string | undefined,
  };
}

interface AuthProviderProps {
  children: React.ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<OidcUser | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [isOidcConfigured, setIsOidcConfigured] = useState(false);
  const getAccessTokenRef = useRef<(() => Promise<string | null>) | null>(null);

  const getAccessToken = useCallback(async (): Promise<string | null> => {
    const userManager = await ensureUserManager();
    if (!userManager) {
      return null;
    }
    try {
      // Always read from the OIDC store - React state can lag behind silent renew.
      const currentUser = await userManager.getUser();
      if (currentUser?.access_token && !currentUser.expired) {
        setUser(currentUser);
        setAccessToken(currentUser.access_token);
        return currentUser.access_token;
      }

      const renewedToken = await forceSilentRenew();
      if (renewedToken) {
        const renewedUser = await userManager.getUser();
        setUser(renewedUser);
        setAccessToken(renewedToken);
        return renewedToken;
      }

      setUser(null);
      setAccessToken(null);
      return null;
    } catch {
      setUser(null);
      setAccessToken(null);
      return null;
    }
  }, []);

  useEffect(() => {
    getAccessTokenRef.current = getAccessToken;
  }, [getAccessToken]);

  useLayoutEffect(() => {
    setTokenGetter(async () => {
      if (getAccessTokenRef.current) {
        return await getAccessTokenRef.current();
      }
      return null;
    });
  }, []);

  useEffect(() => {
    let cancelled = false;

    const init = async () => {
      const manager = await ensureUserManager();
      if (cancelled) {
        return;
      }
      if (!manager) {
        setIsOidcConfigured(false);
        setIsLoading(false);
        return;
      }

      setIsOidcConfigured(true);

      try {
        let currentUser = await manager.getUser();

        // Only attempt silent refresh when a stored user exists with an expired token.
        // Skipping when there's no stored user avoids a ~10-30s iframe timeout for
        // first-time visitors who have never logged in.
        if (currentUser?.expired) {
          const renewedToken = await forceSilentRenew();
          currentUser = renewedToken ? await manager.getUser() : null;
        }

        if (!cancelled) {
          setUser(currentUser);
          setAccessToken(currentUser?.access_token ?? null);
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }

      const onUserLoaded = (loadedUser: OidcUser) => {
        setUser(loadedUser);
        setAccessToken(loadedUser.access_token);
      };
      const onUserUnloaded = () => {
        setUser(null);
        setAccessToken(null);
      };
      const onSilentRenewError = () => {
        setUser(null);
        setAccessToken(null);
        void redirectToLogin();
      };

      manager.events.addUserLoaded(onUserLoaded);
      manager.events.addUserUnloaded(onUserUnloaded);
      manager.events.addSilentRenewError(onSilentRenewError);

      return () => {
        manager.events.removeUserLoaded(onUserLoaded);
        manager.events.removeUserUnloaded(onUserUnloaded);
        manager.events.removeSilentRenewError(onSilentRenewError);
      };
    };

    const cleanupPromise = init();

    return () => {
      cancelled = true;
      cleanupPromise.then((cleanup) => cleanup?.());
    };
  }, []);

  const login = useCallback(async (returnPath?: string) => {
    await redirectToLogin(returnPath);
  }, []);

  const logout = useCallback(async () => {
    const userManager = await ensureUserManager();
    if (!userManager) {
      return;
    }
    // signoutRedirect removes the user locally before navigating; without this flag
    // ProtectedRoute would immediately call login() again.
    setIsLoggingOut(true);
    setAccessToken(null);
    try {
      await clearClientCachesBeforeLogout();
      await userManager.signoutRedirect();
    } catch (error) {
      setIsLoggingOut(false);
      try {
        await userManager.removeUser();
      } catch {
        // ignore - still strip local keys below
      }
      clearAuthLocalStorageAfterLogoutFailure();
      throw error;
    }
  }, []);

  const appRole = useMemo(
    () => (user ? readAppRoleFromToken(user.access_token, user.profile) : null),
    [user],
  );
  const tenantId = useMemo(
    () => (user ? readTenantIdFromToken(user.access_token, user.profile) : null),
    [user],
  );
  const account = useMemo(() => toAccount(user), [user]);

  const value = useMemo<AuthContextType>(
    () => ({
      isAuthenticated: !!user && !user.expired,
      isLoading,
      isLoggingOut,
      accessToken,
      appRole,
      tenantId,
      isGlobalAdmin: isGlobalAdmin(appRole),
      canAccessAdministration: canAccessAdministration(appRole),
      login,
      logout,
      getAccessToken,
      account,
      isOidcConfigured,
    }),
    [
      accessToken,
      account,
      appRole,
      getAccessToken,
      isLoading,
      isLoggingOut,
      isOidcConfigured,
      login,
      logout,
      tenantId,
      user,
    ],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
