/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { type AbsenceListRequest, absenceApi } from '@/lib/api/absence';
import { absenceListDefinition, orgAbsenceListDefinition } from '@/lib/list/absenceList';
import type { ListQuery } from '@/lib/list/types';
import { useUrlSyncedListState } from '@/lib/list/useListState';
import { usePagedList } from '@/lib/store/usePagedList';
import type { CreateUserAbsenceRequest, UpdateUserAbsenceRequest } from '@/types/absence';

export const absenceKeys = {
  all: ['user-absences'] as const,
  listRoot: () => [...absenceKeys.all, 'list'] as const,
  organizationActive: (organizationId: number) =>
    [...absenceKeys.all, 'organization-active', organizationId] as const,
  selfDetail: (absenceId: number) => [...absenceKeys.all, 'detail', 'self', absenceId] as const,
  organizationDetail: (organizationId: number, absenceId: number) =>
    [...absenceKeys.all, 'detail', 'organization', organizationId, absenceId] as const,
};

export function useUrlSyncedAbsenceListState() {
  return useUrlSyncedListState(absenceListDefinition);
}

export function useUrlSyncedOrgAbsenceListState() {
  return useUrlSyncedListState(orgAbsenceListDefinition);
}

type UseUserAbsencesOptions = {
  listQuery: ListQuery;
  enabled?: boolean;
  fetchAll?: boolean;
};

export function useUserAbsence(
  absenceId: number | null | undefined,
  options?: { enabled?: boolean },
) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  const enabled = (options?.enabled ?? true) && hasEmberOneUserId && absenceId != null;
  return useQuery({
    queryKey: absenceKeys.selfDetail(absenceId ?? 0),
    queryFn: () => absenceApi.getSelf(absenceId ?? 0),
    enabled,
    staleTime: 60 * 1000,
  });
}

export function useOrganizationAbsence(
  organizationId: number | null | undefined,
  absenceId: number | null | undefined,
  options?: { enabled?: boolean },
) {
  const enabled = (options?.enabled ?? true) && organizationId != null && absenceId != null;
  return useQuery({
    queryKey: absenceKeys.organizationDetail(organizationId ?? 0, absenceId ?? 0),
    queryFn: () => absenceApi.getForOrganization(organizationId ?? 0, absenceId ?? 0),
    enabled,
    staleTime: 60 * 1000,
  });
}

export function useUserAbsences({
  listQuery,
  enabled = true,
  fetchAll = false,
}: UseUserAbsencesOptions) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return usePagedList({
    queryKey: absenceKeys.listRoot(),
    listQuery: listQuery as AbsenceListRequest,
    queryFn: (query) => absenceApi.list(query),
    enabled: enabled && hasEmberOneUserId,
    staleTime: 60 * 1000,
    fetchAll,
  });
}

type UseOrganizationActiveAbsencesOptions = {
  listQuery: ListQuery;
  enabled?: boolean;
  fetchAll?: boolean;
};

export function useOrganizationActiveAbsences({
  organizationId,
  listQuery,
  enabled = true,
  fetchAll = false,
}: UseOrganizationActiveAbsencesOptions & {
  organizationId: number | null | undefined;
}) {
  const orgId = organizationId ?? 0;
  return usePagedList({
    queryKey: [...absenceKeys.organizationActive(orgId)],
    listQuery: listQuery as AbsenceListRequest,
    queryFn: (query) => absenceApi.listActiveForOrganization(orgId, query),
    enabled: !!organizationId && enabled,
    staleTime: 60 * 1000,
    fetchAll,
  });
}

export function useCreateUserAbsence() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateUserAbsenceRequest) => absenceApi.create(data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: absenceKeys.all });
    },
  });
}

export function useUpdateUserAbsence() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ absenceId, data }: { absenceId: number; data: UpdateUserAbsenceRequest }) =>
      absenceApi.update(absenceId, data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: absenceKeys.all });
    },
  });
}

export function useDeleteUserAbsence() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (absenceId: number) => absenceApi.delete(absenceId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: absenceKeys.all });
    },
  });
}
