/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

// React Query keys and hooks for appointment attachments (server state)

import { type QueryClient, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { appointmentAttachmentApi } from '@/lib/api/appointmentAttachment';
import { appointmentKeys } from '@/lib/store/queries/appointments';
import { STALE_TIME } from '@/lib/store/staleTime';
import type { EntityAttachmentDto } from '@/types/attachment';

export const appointmentAttachmentKeys = {
  all: ['appointment-attachments'] as const,
  list: (organizationId: number, appointmentId: number) =>
    [...appointmentAttachmentKeys.all, organizationId, appointmentId] as const,
};

/** A change affects this list and the `attachmentCount` of the appointment records. */
export function invalidateAppointmentAttachments(
  queryClient: QueryClient,
  organizationId: number,
  appointmentId: number,
): Promise<void> {
  void queryClient.invalidateQueries({ queryKey: appointmentKeys.lists() });
  void queryClient.invalidateQueries({ queryKey: appointmentKeys.details() });
  void queryClient.invalidateQueries({ queryKey: appointmentKeys.upcomingRoot() });
  return queryClient.invalidateQueries({
    queryKey: appointmentAttachmentKeys.list(organizationId, appointmentId),
  });
}

export function useAppointmentAttachments(organizationId: number, appointmentId: number) {
  return useQuery<EntityAttachmentDto[]>({
    queryKey: appointmentAttachmentKeys.list(organizationId, appointmentId),
    queryFn: () => appointmentAttachmentApi.list(organizationId, appointmentId),
    staleTime: STALE_TIME.LIST,
  });
}

export function useUploadAppointmentAttachment(organizationId: number, appointmentId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (file: File) =>
      appointmentAttachmentApi.upload(organizationId, appointmentId, file),
    onSuccess: () => invalidateAppointmentAttachments(queryClient, organizationId, appointmentId),
  });
}

export function useDeleteAppointmentAttachment(organizationId: number, appointmentId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (attachmentId: number) =>
      appointmentAttachmentApi.delete(organizationId, appointmentId, attachmentId),
    onSuccess: () => invalidateAppointmentAttachments(queryClient, organizationId, appointmentId),
  });
}
