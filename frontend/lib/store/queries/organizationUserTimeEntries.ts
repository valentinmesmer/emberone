/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { type TimeEntryListRequest, timeEntryApi } from '@/lib/api/timeEntry';
import { timeEntryOrgUserListDefinition } from '@/lib/list/timeEntryList';
import type { ListQuery } from '@/lib/list/types';
import { useUrlSyncedListState } from '@/lib/list/useListState';
import { usePagedList } from '@/lib/store/usePagedList';
import type {
  CreateUserTimeEntryRequest,
  UpdateUserTimeEntryRequest,
  UserTimeEntryDto,
} from '@/types/timeEntry';

export function useOrganizationUserTimeEntryListState() {
  return useUrlSyncedListState(timeEntryOrgUserListDefinition);
}

export const organizationUserTimeEntryKeys = {
  all: ['organization-user-time-entries'] as const,
  list: (organizationId: number, userId: number, listQuery: ListQuery) =>
    [...organizationUserTimeEntryKeys.all, organizationId, userId, listQuery] as const,
};

type UseOrganizationUserTimeEntriesOptions = {
  organizationId: number | null | undefined;
  userId: number | null | undefined;
  listQuery: ListQuery;
  enabled?: boolean;
  fetchAll?: boolean;
};

export function useOrganizationUserTimeEntries({
  organizationId,
  userId,
  listQuery,
  enabled = true,
  fetchAll = false,
}: UseOrganizationUserTimeEntriesOptions) {
  const orgId = organizationId ?? 0;
  const uid = userId ?? 0;
  return usePagedList<UserTimeEntryDto, TimeEntryListRequest>({
    queryKey: [...organizationUserTimeEntryKeys.all, orgId, uid],
    listQuery: listQuery as TimeEntryListRequest,
    queryFn: (query) => timeEntryApi.listForOrganizationUser(orgId, uid, query),
    enabled: !!organizationId && !!userId && enabled,
    staleTime: 60 * 1000,
    fetchAll,
  });
}

export function useCreateOrganizationUserTimeEntry(organizationId: number, userId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateUserTimeEntryRequest) =>
      timeEntryApi.createForOrganizationUser(organizationId, userId, data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: organizationUserTimeEntryKeys.all });
      void queryClient.invalidateQueries({ queryKey: ['user-time-entries'] });
    },
  });
}

export function useUpdateOrganizationUserTimeEntry(organizationId: number, userId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ entryId, data }: { entryId: number; data: UpdateUserTimeEntryRequest }) =>
      timeEntryApi.updateForOrganizationUser(organizationId, userId, entryId, data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: organizationUserTimeEntryKeys.all });
      void queryClient.invalidateQueries({ queryKey: ['user-time-entries'] });
    },
  });
}

export function useDeleteOrganizationUserTimeEntry(organizationId: number, userId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (entryId: number) =>
      timeEntryApi.deleteForOrganizationUser(organizationId, userId, entryId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: organizationUserTimeEntryKeys.all });
      void queryClient.invalidateQueries({ queryKey: ['user-time-entries'] });
    },
  });
}
