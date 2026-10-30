/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { fetchAllPageContent } from '@/lib/api/paged';
import { qualificationsApi } from '@/lib/api/qualifications';
import { organizationQualificationOverviewListDefinition } from '@/lib/list/organizationQualificationOverviewList';
import { useUrlSyncedListState } from '@/lib/list/useListState';
import { userQualificationStatusesFromSelfRows } from '@/lib/qualifications/aggregateQualifications';

export const qualificationKeys = {
  all: ['qualifications'] as const,
  userStatus: (orgId: number, userId: number) =>
    [...qualificationKeys.all, 'status', orgId, userId] as const,
  statusOverviewAll: (orgId: number) =>
    [...qualificationKeys.all, 'status-overview', orgId] as const,
  statusOverview: (orgId: number, search = '') =>
    [...qualificationKeys.all, 'status-overview', orgId, search] as const,
  selfStatus: () => [...qualificationKeys.all, 'self', 'status'] as const,
};

export function useUserQualificationStatus(organizationId: number, userId: number, enabled = true) {
  return useQuery({
    queryKey: qualificationKeys.userStatus(organizationId, userId),
    queryFn: async () => {
      const items = await fetchAllPageContent((page) =>
        qualificationsApi.getStatus(organizationId, userId, { page }),
      );
      return { userId, organizationId, items };
    },
    enabled: enabled && organizationId > 0 && userId > 0,
  });
}

export function useUrlSyncedOrganizationQualificationOverviewListState() {
  return useUrlSyncedListState(organizationQualificationOverviewListDefinition);
}

export function useOrganizationQualificationStatusOverview(
  organizationId: number,
  search = '',
  enabled = true,
) {
  return useQuery({
    queryKey: qualificationKeys.statusOverview(organizationId, search),
    queryFn: () => qualificationsApi.getStatusOverview(organizationId, search),
    enabled: enabled && organizationId > 0,
    placeholderData: keepPreviousData,
    staleTime: 60 * 1000,
  });
}

export function useSelfQualificationStatus(enabled = true) {
  return useQuery({
    queryKey: qualificationKeys.selfStatus(),
    queryFn: async () => {
      const rows = await fetchAllPageContent((page) => qualificationsApi.getSelfStatus({ page }));
      return userQualificationStatusesFromSelfRows(rows);
    },
    enabled,
  });
}

export function invalidateQualificationStatusForOrganization(
  queryClient: ReturnType<typeof useQueryClient>,
  organizationId: number,
) {
  void queryClient.invalidateQueries({ queryKey: ['qualifications', 'status', organizationId] });
  void queryClient.invalidateQueries({
    queryKey: qualificationKeys.statusOverviewAll(organizationId),
  });
  void queryClient.invalidateQueries({ queryKey: qualificationKeys.selfStatus() });
}

export function useGrantQualificationOrgDecision(organizationId: number, userId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (params: {
      qualificationConfigKey: string;
      startDate: string;
      endDate?: string;
      notes?: string;
    }) =>
      qualificationsApi.grantOrgDecision(organizationId, userId, params.qualificationConfigKey, {
        startDate: params.startDate,
        endDate: params.endDate,
        notes: params.notes,
      }),
    onSuccess: () => {
      invalidateQualificationStatusForOrganization(queryClient, organizationId);
    },
  });
}

export function useGrantQualificationOrgDecisionForOrganization(organizationId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (params: {
      userId: number;
      qualificationConfigKey: string;
      startDate: string;
      endDate?: string;
      notes?: string;
    }) =>
      qualificationsApi.grantOrgDecision(
        organizationId,
        params.userId,
        params.qualificationConfigKey,
        {
          startDate: params.startDate,
          endDate: params.endDate,
          notes: params.notes,
        },
      ),
    onSuccess: () => {
      invalidateQualificationStatusForOrganization(queryClient, organizationId);
    },
  });
}

export function useUpdateQualificationOrgDecision(organizationId: number, userId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (params: {
      qualificationConfigKey: string;
      id: number;
      startDate: string;
      endDate: string;
      notes?: string;
    }) =>
      qualificationsApi.updateOrgDecision(organizationId, userId, params.qualificationConfigKey, {
        id: params.id,
        startDate: params.startDate,
        endDate: params.endDate,
        notes: params.notes,
      }),
    onSuccess: () => {
      invalidateQualificationStatusForOrganization(queryClient, organizationId);
    },
  });
}
