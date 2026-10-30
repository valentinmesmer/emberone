/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { emergencyContactApi } from '@/lib/api/emergencyContact';
import type {
  CreateEmergencyContactRequest,
  UpdateEmergencyContactRequest,
} from '@/types/emergencyContact';

export const emergencyContactKeys = {
  all: ['emergency-contacts'] as const,
  list: () => [...emergencyContactKeys.all, 'list'] as const,
  organization: (organizationId: number) =>
    [...emergencyContactKeys.all, 'organization', organizationId] as const,
};

export function useEmergencyContacts() {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: emergencyContactKeys.list(),
    queryFn: () => emergencyContactApi.getAll(),
    enabled: hasEmberOneUserId,
    staleTime: 5 * 60 * 1000,
  });
}

export function useOrganizationEmergencyContacts(organizationId: number | null | undefined) {
  return useQuery({
    queryKey: emergencyContactKeys.organization(organizationId ?? 0),
    queryFn: () => emergencyContactApi.getForOrganization(organizationId ?? 0),
    enabled: !!organizationId,
    staleTime: 60 * 1000,
  });
}

export function useCreateEmergencyContact() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateEmergencyContactRequest) => emergencyContactApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: emergencyContactKeys.list() });
    },
  });
}

export function useUpdateEmergencyContact() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ contactId, data }: { contactId: number; data: UpdateEmergencyContactRequest }) =>
      emergencyContactApi.update(contactId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: emergencyContactKeys.list() });
    },
  });
}

export function useDeleteEmergencyContact() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (contactId: number) => emergencyContactApi.delete(contactId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: emergencyContactKeys.list() });
    },
  });
}

export function useReorderEmergencyContacts() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (contactIds: number[]) => emergencyContactApi.reorder({ contactIds }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: emergencyContactKeys.list() });
    },
  });
}
