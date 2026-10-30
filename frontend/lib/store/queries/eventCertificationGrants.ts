/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  type CreateCertificationEventGrantRequest,
  eventCertificationGrantsApi,
  type UpdateCertificationEventGrantRequest,
} from '@/lib/api/eventCertificationGrants';
import { certificationKeys } from '@/lib/store/queries/certifications';
import type { EventType } from '@/types/event';

export const eventCertificationGrantKeys = {
  all: ['eventCertificationGrants'] as const,
  list: (organizationId: number, eventType: EventType, eventId: number) =>
    [...eventCertificationGrantKeys.all, 'list', organizationId, eventType, eventId] as const,
  detail: (organizationId: number, eventType: EventType, eventId: number, grantId: number) =>
    [
      ...eventCertificationGrantKeys.all,
      'detail',
      organizationId,
      eventType,
      eventId,
      grantId,
    ] as const,
};

export function useEventCertificationGrants(
  organizationId: number,
  eventType: EventType,
  eventId: number,
  options?: { enabled?: boolean },
) {
  return useQuery({
    queryKey: eventCertificationGrantKeys.list(organizationId, eventType, eventId),
    queryFn: () => eventCertificationGrantsApi.list(organizationId, eventType, eventId),
    enabled: options?.enabled ?? true,
    staleTime: 2 * 60 * 1000,
  });
}

export function useEventCertificationGrantDetail(
  organizationId: number,
  eventType: EventType,
  eventId: number,
  grantId: number | null,
  options?: { enabled?: boolean },
) {
  return useQuery({
    queryKey:
      grantId != null
        ? eventCertificationGrantKeys.detail(organizationId, eventType, eventId, grantId)
        : eventCertificationGrantKeys.all,
    queryFn: () =>
      eventCertificationGrantsApi.get(organizationId, eventType, eventId, grantId ?? 0),
    enabled: (options?.enabled ?? true) && grantId != null,
    staleTime: 60 * 1000,
  });
}

function invalidateEventCertificationGrantQueries(
  queryClient: ReturnType<typeof useQueryClient>,
  organizationId: number,
  eventType: EventType,
  eventId: number,
) {
  queryClient.invalidateQueries({
    queryKey: eventCertificationGrantKeys.list(organizationId, eventType, eventId),
  });
  queryClient.invalidateQueries({
    queryKey: [...eventCertificationGrantKeys.all, 'detail', organizationId, eventType, eventId],
  });
  queryClient.invalidateQueries({ queryKey: certificationKeys.all });
}

export function useCreateEventCertificationGrant(
  organizationId: number,
  eventType: EventType,
  eventId: number,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateCertificationEventGrantRequest) =>
      eventCertificationGrantsApi.create(organizationId, eventType, eventId, data),
    onSuccess: () => {
      invalidateEventCertificationGrantQueries(queryClient, organizationId, eventType, eventId);
    },
  });
}

export function useUpdateEventCertificationGrant(
  organizationId: number,
  eventType: EventType,
  eventId: number,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      grantId,
      data,
    }: {
      grantId: number;
      data: UpdateCertificationEventGrantRequest;
    }) => eventCertificationGrantsApi.update(organizationId, eventType, eventId, grantId, data),
    onSuccess: () => {
      invalidateEventCertificationGrantQueries(queryClient, organizationId, eventType, eventId);
    },
  });
}

export function useDeleteEventCertificationGrant(
  organizationId: number,
  eventType: EventType,
  eventId: number,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (grantId: number) =>
      eventCertificationGrantsApi.delete(organizationId, eventType, eventId, grantId),
    onSuccess: () => {
      invalidateEventCertificationGrantQueries(queryClient, organizationId, eventType, eventId);
    },
  });
}
