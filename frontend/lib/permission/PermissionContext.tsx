/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import type React from 'react';
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { permissionApi } from '@/lib/api/permission';
import { useAuth } from '@/lib/auth/AuthContext';
import { managedPermissionsByOrganization } from '@/lib/management/workspaceOrganizations';
import { subscribePermissionsChanged } from '@/lib/permission/permissionsRealtime';
import { PERMISSIONS_STORAGE_KEY } from '@/lib/permission/permissionsStorageKey';
import { managementKeys, useMyManagementActivations } from '@/lib/store/queries/management';
import { getUserIdFromToken } from '@/lib/utils/tokenUserId';
import type { PermissionTypeEnum, UserPermissionDto } from '@/types/permission';

interface PermissionContextType {
  permissions: UserPermissionDto | null;
  isLoading: boolean;
  error: Error | null;
  refreshPermissions: () => Promise<void>;
  hasPermission: (organizationId: number, permission: PermissionTypeEnum) => boolean;
  hasPermissionInAnyOrganization: (permission: PermissionTypeEnum) => boolean;
}

const PermissionContext = createContext<PermissionContextType | undefined>(undefined);

export function usePermissions() {
  const context = useContext(PermissionContext);
  if (context === undefined) {
    throw new Error('usePermissions must be used within a PermissionProvider');
  }
  return context;
}

interface PermissionProviderProps {
  children: React.ReactNode;
}

function loadStoredPermissions(): UserPermissionDto | null {
  if (typeof window === 'undefined') {
    return null;
  }
  try {
    const stored = localStorage.getItem(PERMISSIONS_STORAGE_KEY);
    return stored ? (JSON.parse(stored) as UserPermissionDto) : null;
  } catch (e) {
    console.error('Failed to load permissions from localStorage:', e);
    return null;
  }
}

export function PermissionProvider({ children }: PermissionProviderProps) {
  const { isAuthenticated, isLoading: authLoading, accessToken, getAccessToken } = useAuth();
  const [permissions, setPermissions] = useState<UserPermissionDto | null>(loadStoredPermissions);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const queryClient = useQueryClient();
  const { data: activations = [] } = useMyManagementActivations();
  const managedPermissions = useMemo(
    () => managedPermissionsByOrganization(activations, Date.now()),
    [activations],
  );
  const refreshPermissions = useCallback(async () => {
    await Promise.resolve();

    if (!isAuthenticated) {
      setPermissions(null);
      setIsLoading(false);
      return;
    }

    const token = accessToken ?? (await getAccessToken());
    if (!getUserIdFromToken(token)) {
      setPermissions(null);
      setError(null);
      setIsLoading(false);
      if (typeof window !== 'undefined') {
        localStorage.removeItem(PERMISSIONS_STORAGE_KEY);
      }
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const data = await permissionApi.getSelfPermissions();
      setPermissions(data);

      if (typeof window !== 'undefined') {
        try {
          localStorage.setItem(PERMISSIONS_STORAGE_KEY, JSON.stringify(data));
        } catch (e) {
          console.error('Failed to save permissions to localStorage:', e);
        }
      }
    } catch (err) {
      const fetchError = err instanceof Error ? err : new Error('Failed to fetch permissions');
      setError(fetchError);
      console.error('Failed to fetch permissions:', fetchError);

      if (typeof window !== 'undefined') {
        localStorage.removeItem(PERMISSIONS_STORAGE_KEY);
      }
    } finally {
      setIsLoading(false);
    }
  }, [isAuthenticated, accessToken, getAccessToken]);

  useEffect(() => {
    if (authLoading) {
      return;
    }

    if (isAuthenticated) {
      queueMicrotask(() => {
        void refreshPermissions();
      });
    }
  }, [isAuthenticated, authLoading, refreshPermissions]);

  useEffect(() => {
    if (!authLoading && !isAuthenticated && typeof window !== 'undefined') {
      localStorage.removeItem(PERMISSIONS_STORAGE_KEY);
    }
  }, [isAuthenticated, authLoading]);

  useEffect(() => {
    if (!isAuthenticated) {
      return;
    }
    return subscribePermissionsChanged(() => {
      void refreshPermissions();
      void queryClient.invalidateQueries({ queryKey: managementKeys.all });
    });
  }, [isAuthenticated, queryClient, refreshPermissions]);

  const hasPermission = useCallback(
    (organizationId: number, permission: PermissionTypeEnum): boolean => {
      // Direct rights first; management rights only add to them, never replace them.
      const activePermissions = isAuthenticated ? permissions : null;
      const orgPermissions = activePermissions?.permissions?.[organizationId];
      if (orgPermissions?.includes(permission)) {
        return true;
      }
      return isAuthenticated
        ? (managedPermissions.get(organizationId)?.includes(permission) ?? false)
        : false;
    },
    [permissions, isAuthenticated, managedPermissions],
  );

  const hasPermissionInAnyOrganization = useCallback(
    (permission: PermissionTypeEnum): boolean => {
      const activePermissions = isAuthenticated ? permissions : null;
      if (!activePermissions?.permissions) {
        return false;
      }
      return Object.values(activePermissions.permissions).some((orgPermissions) =>
        orgPermissions.includes(permission),
      );
    },
    [permissions, isAuthenticated],
  );

  const value = useMemo<PermissionContextType>(
    () => ({
      permissions: isAuthenticated ? permissions : null,
      isLoading: isLoading || authLoading,
      error: isAuthenticated ? error : null,
      refreshPermissions,
      hasPermission,
      hasPermissionInAnyOrganization,
    }),
    [
      authLoading,
      error,
      hasPermission,
      hasPermissionInAnyOrganization,
      isAuthenticated,
      isLoading,
      permissions,
      refreshPermissions,
    ],
  );

  return <PermissionContext.Provider value={value}>{children}</PermissionContext.Provider>;
}
