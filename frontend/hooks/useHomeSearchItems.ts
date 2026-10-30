/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { useTranslation } from '@/hooks/useTranslation';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { useAuth } from '@/hooks/useAuth';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { buildHomeSearchItems, type HomeSearchItem } from '@/lib/home/homeSearchItems';
import { useWorkspaceOrganizations } from '@/hooks/useWorkspaceOrganizations';

/**
 * Navigation-only home search index. Reuses org membership from the home page cache;
 * no additional API calls for search content.
 */
export function useHomeSearchItems(): { items: HomeSearchItem[]; isLoading: boolean } {
  const t = useTranslation();
  const hasEmberOneUserId = useHasEmberOneUserId();
  const { canAccessAdministration, appRole, tenantId: authTenantId } = useAuth();
  const { hasPermission, isLoading: isPermissionsLoading } = usePermissions();

  const {
    organizations,
    isPending: isPendingMyOrgs,
    isFetching: isFetchingMyOrgs,
  } = useWorkspaceOrganizations();
  const isLoadingMyOrgs = isPendingMyOrgs && isFetchingMyOrgs;

  const isLoading = isPermissionsLoading || (hasEmberOneUserId && isLoadingMyOrgs);

  const items = useMemo(() => {
    if (isLoading) {
      return [];
    }

    return buildHomeSearchItems({
      t,
      hasEmberOneUserId,
      organizations,
      hasPermission,
      canAccessAdministration,
      appRole,
      authTenantId,
    });
  }, [
    isLoading,
    t,
    hasEmberOneUserId,
    organizations,
    hasPermission,
    canAccessAdministration,
    appRole,
    authTenantId,
  ]);

  return { items, isLoading };
}
