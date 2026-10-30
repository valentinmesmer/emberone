/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { QueryClient } from '@tanstack/react-query';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { userOrganizationMembershipApi } from '@/lib/api/userOrganizationMembership';
import { workflowsApi } from '@/lib/api/workflows';
import type {
  EndUserOrganizationMembershipRequest,
  UpdateUserOrganizationMembershipRequest,
} from '@/types/user';
import type { OpenOrganizationUserMainOrganizationChangeWorkflowRequest } from '@/types/workflow';
import { organizationKeys } from './organizations';
import { userMembershipOverviewKeys } from './userMembershipOverview';
import { userKeys } from './users';

export function invalidateAfterOrgMembershipChange(
  queryClient: QueryClient,
  organizationId: number,
  userId?: number,
): void {
  void queryClient.invalidateQueries({ queryKey: userKeys.byOrganization(organizationId) });
  void queryClient.invalidateQueries({
    queryKey: userKeys.byOrganizationIncludingChildren(organizationId),
  });
  void queryClient.invalidateQueries({ queryKey: organizationKeys.myOrganizations() });
  if (userId != null) {
    void queryClient.invalidateQueries({
      queryKey: userMembershipOverviewKeys.byUser(organizationId, userId),
    });
  }
}

export function useUpdateUserOrganizationMembership(organizationId: number, userId?: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      membershipId,
      data,
    }: {
      membershipId: number;
      data: UpdateUserOrganizationMembershipRequest;
    }) => userOrganizationMembershipApi.updateMembership(membershipId, data),
    onSuccess: () => {
      invalidateAfterOrgMembershipChange(queryClient, organizationId, userId);
    },
  });
}

export function useOpenOrganizationUserMainOrganizationChangeWorkflow(
  organizationId: number,
  userId: number,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: OpenOrganizationUserMainOrganizationChangeWorkflowRequest) =>
      workflowsApi.openOrganizationUserMainOrganizationChangeWorkflow(organizationId, userId, body),
    onSuccess: () => {
      invalidateAfterOrgMembershipChange(queryClient, organizationId, userId);
    },
  });
}

export function useOpenOrganizationMembershipEndWorkflow(organizationId: number, userId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: EndUserOrganizationMembershipRequest) =>
      workflowsApi.openOrganizationMembershipEndWorkflow(organizationId, userId, {
        reason: data.reason,
      }),
    onSuccess: () => {
      invalidateAfterOrgMembershipChange(queryClient, organizationId, userId);
    },
  });
}
