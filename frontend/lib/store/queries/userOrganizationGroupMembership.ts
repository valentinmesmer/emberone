/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { QueryClient } from '@tanstack/react-query';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { userOrganizationGroupMembershipApi } from '@/lib/api/userOrganizationGroupMembership';
import type {
  CreateUserOrganizationGroupMembershipRequest,
  UpdateUserOrganizationGroupMembershipRequest,
} from '@/types/user';
import { organizationKeys } from './organizations';
import { todoKeys } from './todos';
import { userMembershipOverviewKeys } from './userMembershipOverview';
import { userKeys } from './users';
import { workflowKeys } from './workflows';

/** Keep user lists and org-group member queries aligned after membership writes. */
export function invalidateAfterOrgGroupMembershipChange(
  queryClient: QueryClient,
  organizationId: number,
  affectedUserId?: number,
): void {
  void queryClient.invalidateQueries({ queryKey: userKeys.byOrganization(organizationId) });
  void queryClient.invalidateQueries({
    queryKey: userKeys.byOrganizationIncludingChildren(organizationId),
  });
  void queryClient.invalidateQueries({
    predicate: (q) =>
      Array.isArray(q.queryKey) &&
      q.queryKey[0] === organizationKeys.all[0] &&
      q.queryKey[1] === 'groups' &&
      q.queryKey[3] === 'members',
  });
  if (affectedUserId != null) {
    void queryClient.invalidateQueries({
      queryKey: [...organizationKeys.all, organizationId, 'users', affectedUserId, 'groups'],
    });
    void queryClient.invalidateQueries({
      queryKey: userMembershipOverviewKeys.byUser(organizationId, affectedUserId),
    });
    void queryClient.invalidateQueries({ queryKey: userKeys.self() });
    void queryClient.invalidateQueries({ queryKey: workflowKeys.all });
    void queryClient.invalidateQueries({ queryKey: todoKeys.all });
  }
}

export function useCreateUserOrganizationGroupMembership(organizationId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateUserOrganizationGroupMembershipRequest) =>
      userOrganizationGroupMembershipApi.createMembership(data),
    onSuccess: (data) => {
      invalidateAfterOrgGroupMembershipChange(queryClient, organizationId, data.userId);
    },
  });
}

export function useUpdateUserOrganizationGroupMembership(organizationId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      membershipId,
      data,
    }: {
      membershipId: number;
      data: UpdateUserOrganizationGroupMembershipRequest;
    }) => userOrganizationGroupMembershipApi.updateMembership(membershipId, data),
    onSuccess: (data) => {
      invalidateAfterOrgGroupMembershipChange(queryClient, organizationId, data.userId);
    },
  });
}
