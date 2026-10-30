/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { type TimeEntryListRequest, timeEntryApi } from '@/lib/api/timeEntry';
import type { ListQuery } from '@/lib/list/types';
import { usePagedList } from '@/lib/store/usePagedList';
import type {
  CreateUserTimeEntryRequest,
  UpdateUserTimeEntryRequest,
  UserTimeEntryDto,
} from '@/types/timeEntry';

export const timeEntryKeys = {
  all: ['user-time-entries'] as const,
  list: (listQuery: ListQuery) => [...timeEntryKeys.all, 'list', listQuery] as const,
  organization: (organizationId: number, listQuery: ListQuery) =>
    [...timeEntryKeys.all, 'organization', organizationId, listQuery] as const,
  selfDetail: (entryId: number) => [...timeEntryKeys.all, 'detail', 'self', entryId] as const,
  organizationDetail: (organizationId: number, entryId: number) =>
    [...timeEntryKeys.all, 'detail', 'organization', organizationId, entryId] as const,
};

type UseUserTimeEntriesOptions = {
  listQuery: ListQuery;
  enabled?: boolean;
  fetchAll?: boolean;
};

export function useUserTimeEntries({
  listQuery,
  enabled = true,
  fetchAll = false,
}: UseUserTimeEntriesOptions) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return usePagedList<UserTimeEntryDto, TimeEntryListRequest>({
    queryKey: [...timeEntryKeys.all, 'list'],
    listQuery: listQuery as TimeEntryListRequest,
    queryFn: (query) => timeEntryApi.listSelf(query),
    enabled: hasEmberOneUserId && enabled,
    staleTime: 60 * 1000,
    fetchAll,
  });
}

type UseOrganizationTimeEntriesOptions = {
  organizationId: number | null | undefined;
  listQuery: ListQuery;
  enabled?: boolean;
  fetchAll?: boolean;
};

export function useUserTimeEntry(
  entryId: number | null | undefined,
  options?: { enabled?: boolean },
) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  const enabled = (options?.enabled ?? true) && hasEmberOneUserId && entryId != null;
  return useQuery({
    queryKey: timeEntryKeys.selfDetail(entryId ?? 0),
    queryFn: () => timeEntryApi.getSelf(entryId ?? 0),
    enabled,
    staleTime: 60 * 1000,
  });
}

export function useOrganizationTimeEntry(
  organizationId: number | null | undefined,
  entryId: number | null | undefined,
  options?: { enabled?: boolean },
) {
  const enabled = (options?.enabled ?? true) && organizationId != null && entryId != null;
  return useQuery({
    queryKey: timeEntryKeys.organizationDetail(organizationId ?? 0, entryId ?? 0),
    queryFn: () => timeEntryApi.getForOrganization(organizationId ?? 0, entryId ?? 0),
    enabled,
    staleTime: 60 * 1000,
  });
}

export function useOrganizationTimeEntries({
  organizationId,
  listQuery,
  enabled = true,
  fetchAll = false,
}: UseOrganizationTimeEntriesOptions) {
  const orgId = organizationId ?? 0;
  return usePagedList<UserTimeEntryDto, TimeEntryListRequest>({
    queryKey: [...timeEntryKeys.all, 'organization', orgId],
    listQuery: listQuery as TimeEntryListRequest,
    queryFn: (query) => timeEntryApi.listForOrganization(orgId, query),
    enabled: !!organizationId && enabled,
    staleTime: 60 * 1000,
    fetchAll,
  });
}

export function useCreateUserTimeEntry() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateUserTimeEntryRequest) => timeEntryApi.create(data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: timeEntryKeys.all });
    },
  });
}

export function useUpdateUserTimeEntry() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ entryId, data }: { entryId: number; data: UpdateUserTimeEntryRequest }) =>
      timeEntryApi.update(entryId, data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: timeEntryKeys.all });
    },
  });
}

export function useDeleteUserTimeEntry() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (entryId: number) => timeEntryApi.delete(entryId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: timeEntryKeys.all });
    },
  });
}
