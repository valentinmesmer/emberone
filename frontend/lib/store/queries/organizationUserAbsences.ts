/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { type AbsenceListRequest, absenceApi } from '@/lib/api/absence';
import { absenceOrgUserListDefinition } from '@/lib/list/absenceList';
import type { ListQuery } from '@/lib/list/types';
import { useUrlSyncedListState } from '@/lib/list/useListState';
import { usePagedList } from '@/lib/store/usePagedList';
import type { CreateUserAbsenceRequest, UpdateUserAbsenceRequest } from '@/types/absence';

export function useOrganizationUserAbsenceListState() {
  return useUrlSyncedListState(absenceOrgUserListDefinition);
}

export const organizationUserAbsenceKeys = {
  all: ['organization-user-absences'] as const,
  list: (organizationId: number, userId: number) =>
    [...organizationUserAbsenceKeys.all, organizationId, userId] as const,
};

type UseOrganizationUserAbsencesOptions = {
  organizationId: number | null | undefined;
  userId: number | null | undefined;
  listQuery: ListQuery;
  enabled?: boolean;
  fetchAll?: boolean;
};

export function useOrganizationUserAbsences({
  organizationId,
  userId,
  listQuery,
  enabled = true,
  fetchAll = false,
}: UseOrganizationUserAbsencesOptions) {
  const orgId = organizationId ?? 0;
  const uid = userId ?? 0;
  return usePagedList({
    queryKey: [...organizationUserAbsenceKeys.all, orgId, uid],
    listQuery: listQuery as AbsenceListRequest,
    queryFn: (query) => absenceApi.listForOrganizationUser(orgId, uid, query),
    enabled: !!organizationId && !!userId && enabled,
    staleTime: 60 * 1000,
    fetchAll,
  });
}

export function useCreateOrganizationUserAbsence(organizationId: number, userId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateUserAbsenceRequest) =>
      absenceApi.createForOrganizationUser(organizationId, userId, data),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: organizationUserAbsenceKeys.list(organizationId, userId),
      });
      void queryClient.invalidateQueries({ queryKey: ['user-absences'] });
    },
  });
}

export function useUpdateOrganizationUserAbsence(organizationId: number, userId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ absenceId, data }: { absenceId: number; data: UpdateUserAbsenceRequest }) =>
      absenceApi.updateForOrganizationUser(organizationId, userId, absenceId, data),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: organizationUserAbsenceKeys.list(organizationId, userId),
      });
      void queryClient.invalidateQueries({ queryKey: ['user-absences'] });
    },
  });
}

export function useDeleteOrganizationUserAbsence(organizationId: number, userId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (absenceId: number) =>
      absenceApi.deleteForOrganizationUser(organizationId, userId, absenceId),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: organizationUserAbsenceKeys.list(organizationId, userId),
      });
      void queryClient.invalidateQueries({ queryKey: ['user-absences'] });
    },
  });
}
