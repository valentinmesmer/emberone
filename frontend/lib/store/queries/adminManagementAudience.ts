/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi, type ReplaceManagementAudiencesRequest } from '@/lib/api/admin';

export const adminManagementAudienceKeys = {
  all: ['admin', 'managementAudience'] as const,
  settings: (tenantId: number, organizationId: number) =>
    [...adminManagementAudienceKeys.all, 'settings', tenantId, organizationId] as const,
};

export function useAdminManagementAudiences(tenantId: number, organizationId: number) {
  return useQuery({
    queryKey: adminManagementAudienceKeys.settings(tenantId, organizationId),
    queryFn: () => adminApi.getManagementAudiences(tenantId, organizationId),
  });
}

export function useReplaceAdminManagementAudiences(tenantId: number, organizationId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: ReplaceManagementAudiencesRequest) =>
      adminApi.replaceManagementAudiences(tenantId, organizationId, data),
    onSuccess: (settings) => {
      queryClient.setQueryData(
        adminManagementAudienceKeys.settings(tenantId, organizationId),
        settings,
      );
    },
  });
}
