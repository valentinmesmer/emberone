/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi, type ReplaceCourseOfferingTypesRequest } from '@/lib/api/admin';
import { tenantTrainingCourseTypesApi } from '@/lib/api/tenantTrainingCourseTypes';

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
  });
}

export function useAdminCourseOfferingTypeCatalog(tenantId: number, enabled: boolean) {
  return useQuery({
    queryKey: adminCourseOfferingTypeKeys.catalog(tenantId),
    queryFn: async () =>
      (await tenantTrainingCourseTypesApi.list(tenantId)).filter((t) => !t.archived),
    enabled,
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
