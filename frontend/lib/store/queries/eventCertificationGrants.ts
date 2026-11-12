/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  type CreateCertificationEventGrantRequest,
  eventCertificationGrantsApi,
  type UpdateCertificationEventGrantRequest,
} from '@/lib/api/eventCertificationGrants';
import { certificationKeys } from '@/lib/store/queries/certifications';
import { STALE_TIME } from '@/lib/store/staleTime';
import type { EventType } from '@/types/event';

export const eventCertificationGrantKeys = {
  all: ['eventCertificationGrants'] as const,
  /** Lists and details of the grants of one event. */
  event: (organizationId: number, eventType: EventType, eventId: number) =>
    [...eventCertificationGrantKeys.all, organizationId, eventType, eventId] as const,
  list: (organizationId: number, eventType: EventType, eventId: number) =>
    [...eventCertificationGrantKeys.event(organizationId, eventType, eventId), 'list'] as const,
  detail: (organizationId: number, eventType: EventType, eventId: number, grantId: number) =>
    [
      ...eventCertificationGrantKeys.event(organizationId, eventType, eventId),
      'detail',
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
    staleTime: STALE_TIME.LIST,
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
    queryKey: eventCertificationGrantKeys.detail(organizationId, eventType, eventId, grantId ?? 0),
    queryFn: () =>
      eventCertificationGrantsApi.get(organizationId, eventType, eventId, grantId ?? 0),
    enabled: (options?.enabled ?? true) && grantId != null,
    staleTime: STALE_TIME.LIST,
  });
}

function invalidateEventCertificationGrantQueries(
  queryClient: ReturnType<typeof useQueryClient>,
  organizationId: number,
  eventType: EventType,
  eventId: number,
) {
  void queryClient.invalidateQueries({
    queryKey: eventCertificationGrantKeys.event(organizationId, eventType, eventId),
  });
  void queryClient.invalidateQueries({ queryKey: certificationKeys.all });
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
