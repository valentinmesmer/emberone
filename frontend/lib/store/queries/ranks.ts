/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { ranksApi, type SetUserRankRequest } from '@/lib/api/ranks';
import { organizationRankMemberListDefinition } from '@/lib/list/organizationRankMemberList';
import type { ListQuery } from '@/lib/list/types';
import { useUrlSyncedListState } from '@/lib/list/useListState';
import { usePagedList } from '@/lib/store/usePagedList';

export const rankKeys = {
  all: ['ranks'] as const,
  myRank: () => [...rankKeys.all, 'my-rank'] as const,
  myAvailableRanks: () => [...rankKeys.all, 'my-available-ranks'] as const,
  myFunctions: () => [...rankKeys.all, 'my-functions'] as const,
  orgMembersRoot: (orgId: number) => [...rankKeys.all, 'org-members', orgId] as const,
  orgMembers: (orgId: number, listQuery: ListQuery) =>
    [...rankKeys.orgMembersRoot(orgId), listQuery] as const,
  orgFunctionsOverviewRoot: (orgId: number) =>
    [...rankKeys.all, 'org-functions-overview', orgId] as const,
  orgFunctionsOverview: (orgId: number, search: string) =>
    [...rankKeys.orgFunctionsOverviewRoot(orgId), search] as const,
  orgAvailableRanks: (orgId: number) => [...rankKeys.all, 'org-available-ranks', orgId] as const,
  userRank: (orgId: number, userId: number) =>
    [...rankKeys.all, 'user-rank', orgId, userId] as const,
  userFunctions: (orgId: number, userId: number) =>
    [...rankKeys.all, 'user-functions', orgId, userId] as const,
};

export function useMyRank(enabled = true) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: rankKeys.myRank(),
    queryFn: () => ranksApi.getMyRank(),
    enabled: hasEmberOneUserId && enabled,
    staleTime: 60 * 1000,
  });
}

export function useMyAvailableRanks(enabled = true) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: rankKeys.myAvailableRanks(),
    queryFn: () => ranksApi.getMyAvailableRanks(),
    enabled: hasEmberOneUserId && enabled,
    staleTime: 5 * 60 * 1000,
  });
}

export function useMyDutyFunctions(enabled = true) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: rankKeys.myFunctions(),
    queryFn: () => ranksApi.getMyFunctions(),
    enabled: hasEmberOneUserId && enabled,
    staleTime: 60 * 1000,
  });
}

export function useUrlSyncedOrganizationRankMemberListState() {
  return useUrlSyncedListState(organizationRankMemberListDefinition);
}

type UseOrganizationRankMembersOptions = {
  organizationId: number;
  listQuery: ListQuery;
  enabled?: boolean;
  /** Loads all pages for grouped overview rendering. */
  fetchAll?: boolean;
};

export function useOrganizationRankMembers({
  organizationId,
  listQuery,
  enabled = true,
  fetchAll = false,
}: UseOrganizationRankMembersOptions) {
  return usePagedList({
    queryKey: rankKeys.orgMembersRoot(organizationId),
    listQuery,
    queryFn: (query) => ranksApi.listOrganizationMembers(organizationId, query),
    enabled: enabled && organizationId > 0,
    staleTime: 30 * 1000,
    fetchAll,
  });
}

export function useOrganizationFunctionsOverview(
  organizationId: number,
  search: string,
  enabled = true,
) {
  return useQuery({
    queryKey: rankKeys.orgFunctionsOverview(organizationId, search),
    queryFn: () => ranksApi.getOrganizationFunctionsOverview(organizationId, search),
    enabled: enabled && organizationId > 0,
    staleTime: 30 * 1000,
  });
}

export function useOrganizationAvailableRanks(organizationId: number, enabled = true) {
  return useQuery({
    queryKey: rankKeys.orgAvailableRanks(organizationId),
    queryFn: () => ranksApi.getOrganizationAvailableRanks(organizationId),
    enabled: enabled && organizationId > 0,
    staleTime: 5 * 60 * 1000,
  });
}

export function useUserRank(organizationId: number, userId: number, enabled = true) {
  return useQuery({
    queryKey: rankKeys.userRank(organizationId, userId),
    queryFn: () => ranksApi.getUserRank(organizationId, userId),
    enabled: enabled && organizationId > 0 && userId > 0,
    staleTime: 60 * 1000,
  });
}

export function useUserDutyFunctions(organizationId: number, userId: number, enabled = true) {
  return useQuery({
    queryKey: rankKeys.userFunctions(organizationId, userId),
    queryFn: () => ranksApi.getUserFunctions(organizationId, userId),
    enabled: enabled && organizationId > 0 && userId > 0,
    staleTime: 60 * 1000,
  });
}

export function useSetUserRank(organizationId: number, userId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: SetUserRankRequest) => ranksApi.setUserRank(organizationId, userId, data),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: rankKeys.userRank(organizationId, userId) }),
        queryClient.invalidateQueries({ queryKey: rankKeys.orgMembersRoot(organizationId) }),
        queryClient.invalidateQueries({
          queryKey: rankKeys.orgFunctionsOverviewRoot(organizationId),
        }),
      ]);
    },
  });
}
