/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { type QueryClient, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi, type CreateAdminLeadershipGroupMembershipRequest } from '@/lib/api/admin';
import type { UpdateUserOrganizationGroupMembershipRequest } from '@/types/user';
import { invalidateAfterOrgGroupMembershipChange } from './userOrganizationGroupMembership';

export const adminLeadershipGroupKeys = {
  all: ['admin', 'leadershipGroup'] as const,
  memberships: (tenantId: number, organizationId: number) =>
    [...adminLeadershipGroupKeys.all, 'memberships', tenantId, organizationId] as const,
  organizationUsers: (tenantId: number, organizationId: number) =>
    [...adminLeadershipGroupKeys.all, 'organizationUsers', tenantId, organizationId] as const,
};

export function invalidateAdminLeadershipGroupMemberships(
  queryClient: QueryClient,
  tenantId: number,
  organizationId: number,
  affectedUserId?: number,
): void {
  void queryClient.invalidateQueries({
    queryKey: adminLeadershipGroupKeys.memberships(tenantId, organizationId),
  });
  void queryClient.invalidateQueries({
    queryKey: adminLeadershipGroupKeys.organizationUsers(tenantId, organizationId),
  });
  invalidateAfterOrgGroupMembershipChange(queryClient, organizationId, affectedUserId);
}

export function useAdminLeadershipGroupMemberships(tenantId: number, organizationId: number) {
  return useQuery({
    queryKey: adminLeadershipGroupKeys.memberships(tenantId, organizationId),
    queryFn: () => adminApi.getLeadershipGroupMemberships(tenantId, organizationId),
  });
}

export function useAdminOrganizationUsers(tenantId: number, organizationId: number) {
  return useQuery({
    queryKey: adminLeadershipGroupKeys.organizationUsers(tenantId, organizationId),
    queryFn: () => adminApi.getAdminOrganizationUsers(tenantId, organizationId),
  });
}

export function useCreateAdminLeadershipGroupMembership(tenantId: number, organizationId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateAdminLeadershipGroupMembershipRequest) =>
      adminApi.createLeadershipGroupMembership(tenantId, organizationId, data),
    onSuccess: (data) => {
      invalidateAdminLeadershipGroupMemberships(queryClient, tenantId, organizationId, data.userId);
    },
  });
}

export function useUpdateAdminLeadershipGroupMembership(tenantId: number, organizationId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      membershipId,
      data,
    }: {
      membershipId: number;
      data: UpdateUserOrganizationGroupMembershipRequest;
    }) => adminApi.updateLeadershipGroupMembership(tenantId, organizationId, membershipId, data),
    onSuccess: (data) => {
      invalidateAdminLeadershipGroupMemberships(queryClient, tenantId, organizationId, data.userId);
    },
  });
}
