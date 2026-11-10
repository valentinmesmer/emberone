/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { useQuery } from '@tanstack/react-query';
import {
  type AdminListRequest,
  adminApi,
  type ManagementActivationLogItemDto,
} from '@/lib/api/admin';
import type { ListQuery } from '@/lib/list/types';
import { STALE_TIME } from '@/lib/store/staleTime';
import { usePagedList } from '@/lib/store/usePagedList';

export const adminManagementActivationKeys = {
  all: ['admin', 'managementActivations'] as const,
  list: () => [...adminManagementActivationKeys.all, 'list'] as const,
  detail: (activationId: number) =>
    [...adminManagementActivationKeys.all, 'detail', activationId] as const,
};

export function useAdminManagementActivations({
  listQuery,
  enabled = true,
}: {
  listQuery: ListQuery;
  enabled?: boolean;
}) {
  return usePagedList<ManagementActivationLogItemDto, AdminListRequest>({
    queryKey: adminManagementActivationKeys.list(),
    listQuery: listQuery as AdminListRequest,
    queryFn: (query) => adminApi.listManagementActivationsPage(query),
    enabled,
    staleTime: STALE_TIME.LIVE,
  });
}

/** Single entry for the admin detail page (`GET /api/v1/auth/admin/management-activations/{id}`). */
export function useAdminManagementActivation(activationId: number) {
  return useQuery({
    queryKey: adminManagementActivationKeys.detail(activationId),
    queryFn: () => adminApi.getManagementActivation(activationId),
    enabled: activationId > 0,
    staleTime: STALE_TIME.LIVE,
  });
}
