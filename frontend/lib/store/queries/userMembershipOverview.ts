/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { useQuery } from '@tanstack/react-query';
import { userMembershipOverviewApi } from '@/lib/api/userMembershipOverview';

export const userMembershipOverviewKeys = {
  all: ['user-membership-overview'] as const,
  byUser: (organizationId: number, userId: number) =>
    [...userMembershipOverviewKeys.all, organizationId, userId] as const,
  mainChangeTargets: (organizationId: number, userId: number) =>
    [...userMembershipOverviewKeys.all, 'main-change-targets', organizationId, userId] as const,
};

export function useUserMembershipOverview(
  organizationId: number | null | undefined,
  userId: number | null | undefined,
) {
  return useQuery({
    queryKey: userMembershipOverviewKeys.byUser(organizationId ?? 0, userId ?? 0),
    queryFn: () =>
      userMembershipOverviewApi.getMembershipOverview(organizationId ?? 0, userId ?? 0),
    enabled: !!organizationId && !!userId,
    staleTime: 60 * 1000,
  });
}

export function useMemberMainOrganizationChangeTargets(
  organizationId: number | null | undefined,
  userId: number | null | undefined,
  enabled = true,
) {
  return useQuery({
    queryKey: userMembershipOverviewKeys.mainChangeTargets(organizationId ?? 0, userId ?? 0),
    queryFn: () =>
      userMembershipOverviewApi.getMainOrganizationChangeTargets(organizationId ?? 0, userId ?? 0),
    enabled: enabled && !!organizationId && !!userId,
    staleTime: 60 * 1000,
  });
}
