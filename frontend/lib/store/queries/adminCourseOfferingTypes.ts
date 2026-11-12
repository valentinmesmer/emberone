/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi, type ReplaceCourseOfferingTypesRequest } from '@/lib/api/admin';
import { tenantTrainingCourseTypesApi } from '@/lib/api/tenantTrainingCourseTypes';
import { STALE_TIME } from '@/lib/store/staleTime';

export const adminCourseOfferingTypeKeys = {
  all: ['admin', 'courseOfferingTypes'] as const,
  settings: (tenantId: number, organizationId: number) =>
    [...adminCourseOfferingTypeKeys.all, 'settings', tenantId, organizationId] as const,
  catalog: (tenantId: number) => [...adminCourseOfferingTypeKeys.all, 'catalog', tenantId] as const,
};

export function useAdminCourseOfferingTypes(tenantId: number, organizationId: number) {
  return useQuery({
    queryKey: adminCourseOfferingTypeKeys.settings(tenantId, organizationId),
    queryFn: () => adminApi.getCourseOfferingTypes(tenantId, organizationId),
    staleTime: STALE_TIME.LIST,
  });
}

export function useAdminCourseOfferingTypeCatalog(tenantId: number, enabled: boolean) {
  return useQuery({
    queryKey: adminCourseOfferingTypeKeys.catalog(tenantId),
    queryFn: async () =>
      (await tenantTrainingCourseTypesApi.list(tenantId)).filter((t) => !t.archived),
    enabled,
    staleTime: STALE_TIME.STANDARD,
  });
}

export function useReplaceAdminCourseOfferingTypes(tenantId: number, organizationId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: ReplaceCourseOfferingTypesRequest) =>
      adminApi.replaceCourseOfferingTypes(tenantId, organizationId, data),
    onSuccess: (settings) => {
      queryClient.setQueryData(
        adminCourseOfferingTypeKeys.settings(tenantId, organizationId),
        settings,
      );
    },
  });
}
