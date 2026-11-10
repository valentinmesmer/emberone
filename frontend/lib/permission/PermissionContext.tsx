/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import type React from 'react';
import { createContext, useCallback, useContext, useMemo } from 'react';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { useAuth } from '@/lib/auth/AuthContext';
import { managedPermissionsByOrganization } from '@/lib/management/workspaceOrganizations';
import { useMyManagementActivations } from '@/lib/store/queries/management';
import { useSelfPermissions } from '@/lib/store/queries/permissions';
import type { PermissionTypeEnum, UserPermissionDto } from '@/types/permission';

interface PermissionContextType {
  permissions: UserPermissionDto | null;
  isLoading: boolean;
  error: Error | null;
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

export function PermissionProvider({ children }: PermissionProviderProps) {
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const hasEmberOneUserId = useHasEmberOneUserId();
  const permissionsQuery = useSelfPermissions(hasEmberOneUserId);
  const permissions = isAuthenticated && hasEmberOneUserId ? (permissionsQuery.data ?? null) : null;
  const error = isAuthenticated ? permissionsQuery.error : null;
  const { data: activations = [] } = useMyManagementActivations();
  const managedPermissions = useMemo(
    () => managedPermissionsByOrganization(activations, Date.now()),
    [activations],
  );

  const hasPermission = useCallback(
    (organizationId: number, permission: PermissionTypeEnum): boolean => {
      // Direct rights first; management rights only add to them, never replace them.
      const orgPermissions = permissions?.permissions?.[organizationId];
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
      if (!permissions?.permissions) {
        return false;
      }
      return Object.values(permissions.permissions).some((orgPermissions) =>
        orgPermissions.includes(permission),
      );
    },
    [permissions],
  );

  const value = useMemo<PermissionContextType>(
    () => ({
      permissions,
      isLoading: permissionsQuery.isLoading || authLoading,
      error,
      hasPermission,
      hasPermissionInAnyOrganization,
    }),
    [
      authLoading,
      error,
      hasPermission,
      hasPermissionInAnyOrganization,
      permissions,
      permissionsQuery.isLoading,
    ],
  );

  return <PermissionContext.Provider value={value}>{children}</PermissionContext.Provider>;
}
