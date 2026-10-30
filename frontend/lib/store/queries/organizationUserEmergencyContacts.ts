/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { emergencyContactApi } from '@/lib/api/emergencyContact';
import type {
  CreateEmergencyContactRequest,
  UpdateEmergencyContactRequest,
} from '@/types/emergencyContact';

export const organizationUserEmergencyContactKeys = {
  all: ['organization-user-emergency-contacts'] as const,
  list: (organizationId: number, userId: number) =>
    [...organizationUserEmergencyContactKeys.all, organizationId, userId] as const,
};

export function useOrganizationUserEmergencyContacts(
  organizationId: number | null | undefined,
  userId: number | null | undefined,
) {
  return useQuery({
    queryKey: organizationUserEmergencyContactKeys.list(organizationId ?? 0, userId ?? 0),
    queryFn: () => emergencyContactApi.getForOrganizationUser(organizationId ?? 0, userId ?? 0),
    enabled: !!organizationId && !!userId,
    staleTime: 60 * 1000,
  });
}

export function useCreateOrganizationUserEmergencyContact(organizationId: number, userId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateEmergencyContactRequest) =>
      emergencyContactApi.createForOrganizationUser(organizationId, userId, data),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: organizationUserEmergencyContactKeys.list(organizationId, userId),
      });
    },
  });
}

export function useUpdateOrganizationUserEmergencyContact(organizationId: number, userId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ contactId, data }: { contactId: number; data: UpdateEmergencyContactRequest }) =>
      emergencyContactApi.updateForOrganizationUser(organizationId, userId, contactId, data),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: organizationUserEmergencyContactKeys.list(organizationId, userId),
      });
    },
  });
}

export function useDeleteOrganizationUserEmergencyContact(organizationId: number, userId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (contactId: number) =>
      emergencyContactApi.deleteForOrganizationUser(organizationId, userId, contactId),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: organizationUserEmergencyContactKeys.list(organizationId, userId),
      });
    },
  });
}

export function useReorderOrganizationUserEmergencyContacts(
  organizationId: number,
  userId: number,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (contactIds: number[]) =>
      emergencyContactApi.reorderForOrganizationUser(organizationId, userId, { contactIds }),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: organizationUserEmergencyContactKeys.list(organizationId, userId),
      });
    },
  });
}
