/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { type QueryClient, useQuery } from '@tanstack/react-query';
import {
  type AdminListRequest,
  type AdminUserListItemDto,
  adminApi,
  type TenantDto,
} from '@/lib/api/admin';
import type { ListQuery } from '@/lib/list/types';
import { usePagedList } from '@/lib/store/usePagedList';

export const adminListKeys = {
  all: ['admin', 'lists'] as const,
  users: () => [...adminListKeys.all, 'users'] as const,
  user: (userId: number) => [...adminListKeys.all, 'user', userId] as const,
  tenants: () => [...adminListKeys.all, 'tenants'] as const,
  tenantOptions: () => [...adminListKeys.all, 'tenantOptions'] as const,
  organizationOptions: (tenantId: number) =>
    [...adminListKeys.all, 'organizationOptions', tenantId] as const,
  userFilterOrganizationOptions: (scope: string) =>
    [...adminListKeys.all, 'userFilterOrganizationOptions', scope] as const,
};

export function invalidateAdminTenantList(queryClient: QueryClient): Promise<void> {
  return queryClient.invalidateQueries({ queryKey: adminListKeys.tenants() });
}

export function useAdminUsers({
  listQuery,
  enabled = true,
}: {
  listQuery: ListQuery;
  enabled?: boolean;
}) {
  return usePagedList<AdminUserListItemDto, AdminListRequest>({
    queryKey: adminListKeys.users(),
    listQuery: listQuery as AdminListRequest,
    queryFn: (query) => adminApi.listAdminUsersPage(query),
    enabled,
    staleTime: 30 * 1000,
  });
}

/** Single user for the admin detail view (`GET /api/v1/auth/admin/users/{userId}`). */
export function useAdminUser(userId: number, enabled = true) {
  return useQuery({
    queryKey: adminListKeys.user(userId),
    queryFn: () => adminApi.getAdminUser(userId),
    enabled: enabled && userId > 0,
    staleTime: 0,
  });
}

export function useAdminTenants({
  listQuery,
  enabled = true,
}: {
  listQuery: ListQuery;
  enabled?: boolean;
}) {
  return usePagedList<TenantDto, AdminListRequest>({
    queryKey: adminListKeys.tenants(),
    listQuery: listQuery as AdminListRequest,
    queryFn: (query) => adminApi.listTenantsPage(query),
    enabled,
    staleTime: 30 * 1000,
  });
}
