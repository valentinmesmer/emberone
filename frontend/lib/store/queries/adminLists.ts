/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { type QueryClient, useQuery } from '@tanstack/react-query';
import {
  type AdminListRequest,
  type AdminUserListItemDto,
  adminApi,
  type TenantAdminDetailDto,
  type TenantDto,
} from '@/lib/api/admin';
import type { ListQuery } from '@/lib/list/types';
import { STALE_TIME } from '@/lib/store/staleTime';
import { usePagedList } from '@/lib/store/usePagedList';

export const adminListKeys = {
  all: ['admin', 'lists'] as const,
  users: () => [...adminListKeys.all, 'users'] as const,
  user: (userId: number) => [...adminListKeys.all, 'user', userId] as const,
  tenants: () => [...adminListKeys.all, 'tenants'] as const,
  tenant: (tenantId: number) => [...adminListKeys.all, 'tenant', tenantId] as const,
  federalStates: () => [...adminListKeys.all, 'federalStates'] as const,
  retentionProfile: (tenantId: number) =>
    [...adminListKeys.all, 'retentionProfile', tenantId] as const,
};

export function invalidateAdminTenantList(queryClient: QueryClient): Promise<void> {
  return queryClient.invalidateQueries({ queryKey: adminListKeys.tenants() });
}

export function invalidateAdminTenant(queryClient: QueryClient, tenantId: number): Promise<void> {
  return queryClient.invalidateQueries({ queryKey: adminListKeys.tenant(tenantId) });
}

/** Stores a tenant returned by a mutation and refreshes the tenant list. */
export function setAdminTenantData(queryClient: QueryClient, tenant: TenantAdminDetailDto): void {
  queryClient.setQueryData(adminListKeys.tenant(tenant.id), tenant);
  void invalidateAdminTenantList(queryClient);
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
    staleTime: STALE_TIME.LIST,
  });
}

/** Single user for the admin detail view (`GET /api/v1/auth/admin/users/{userId}`). */
export function useAdminUser(userId: number, enabled = true) {
  return useQuery({
    queryKey: adminListKeys.user(userId),
    queryFn: () => adminApi.getAdminUser(userId),
    enabled: enabled && userId > 0,
    staleTime: STALE_TIME.LIST,
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
    staleTime: STALE_TIME.LIST,
  });
}

/** Tenant detail for the admin area (`GET /api/v1/auth/admin/tenants/{id}`). */
export function useAdminTenant(tenantId: number, enabled = true) {
  return useQuery({
    queryKey: adminListKeys.tenant(tenantId),
    queryFn: () => adminApi.getTenant(tenantId),
    enabled: enabled && tenantId > 0,
    // Carries root organization leader issues and the standards-configured flag.
    staleTime: STALE_TIME.LIST,
  });
}

/** Retention periods of a tenant (read-only profile). */
export function useTenantRetentionProfile(tenantId: number, enabled = true) {
  return useQuery({
    queryKey: adminListKeys.retentionProfile(tenantId),
    queryFn: () => adminApi.getTenantRetentionProfile(tenantId),
    enabled: enabled && tenantId > 0,
    staleTime: STALE_TIME.STANDARD,
  });
}

/** Federal states a tenant may be created for. */
export function useConfiguredFederalStates(enabled = true) {
  return useQuery({
    queryKey: adminListKeys.federalStates(),
    queryFn: () => adminApi.listConfiguredFederalStates(),
    enabled,
    staleTime: STALE_TIME.STANDARD,
  });
}

/** Loads a tenant detail on demand (e.g. before opening an edit dialog), reusing the cache. */
export function fetchAdminTenant(
  queryClient: QueryClient,
  tenantId: number,
): Promise<TenantAdminDetailDto> {
  return queryClient.fetchQuery({
    queryKey: adminListKeys.tenant(tenantId),
    queryFn: () => adminApi.getTenant(tenantId),
    staleTime: STALE_TIME.LIST,
  });
}
