/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { useQuery } from '@tanstack/react-query';
import {
  operationEventListDefinition,
  operationEventListUnfilteredQuery,
  serviceEventListDefinition,
  serviceEventListUnfilteredQuery,
} from '@/lib/list/eventList';
import type { ListQuery } from '@/lib/list/types';
import type { EventDto } from '@/types/event';
import { EventType } from '@/types/event';

export { operationEventListUnfilteredQuery, serviceEventListUnfilteredQuery };

import { type EventListRequest, eventApi } from '@/lib/api/event';
import { createInitialListQueryState, toListQuery, useUrlSyncedListState } from '@/lib/list';
import { usePagedList } from '@/lib/store/usePagedList';

export const eventKeys = {
  all: ['events'] as const,
  service: (organizationId: number, listQuery: ListQuery) =>
    [...eventKeys.all, 'service', organizationId, listQuery] as const,
  operation: (organizationId: number, listQuery: ListQuery) =>
    [...eventKeys.all, 'operation', organizationId, listQuery] as const,
  serviceDetail: (organizationId: number, eventId: number) =>
    [...eventKeys.all, 'service', organizationId, 'detail', eventId] as const,
  operationDetail: (organizationId: number, eventId: number) =>
    [...eventKeys.all, 'operation', organizationId, 'detail', eventId] as const,
};

const defaultServiceListQuery = toListQuery(
  serviceEventListDefinition,
  createInitialListQueryState(serviceEventListDefinition),
);

const defaultOperationListQuery = toListQuery(
  operationEventListDefinition,
  createInitialListQueryState(operationEventListDefinition),
);

type UseOrganizationEventsOptions = {
  listQuery?: EventListRequest;
  enabled?: boolean;
  fetchAll?: boolean;
};

export function useServiceEvents(
  organizationId: number | null | undefined,
  options?: UseOrganizationEventsOptions,
) {
  const canFetch = options?.enabled ?? true;
  const listQuery = options?.listQuery ?? defaultServiceListQuery;
  return usePagedList<EventDto, EventListRequest>({
    queryKey: [...eventKeys.all, 'service', organizationId ?? 0],
    listQuery,
    queryFn: (query) => eventApi.listServiceEvents(organizationId ?? 0, query),
    enabled: !!organizationId && canFetch,
    staleTime: 2 * 60 * 1000,
    fetchAll: options?.fetchAll,
  });
}

export function useOperationEvents(
  organizationId: number | null | undefined,
  options?: UseOrganizationEventsOptions,
) {
  const canFetch = options?.enabled ?? true;
  const listQuery = options?.listQuery ?? defaultOperationListQuery;
  return usePagedList<EventDto, EventListRequest>({
    queryKey: [...eventKeys.all, 'operation', organizationId ?? 0],
    listQuery,
    queryFn: (query) => eventApi.listOperationEvents(organizationId ?? 0, query),
    enabled: !!organizationId && canFetch,
    staleTime: 2 * 60 * 1000,
    fetchAll: options?.fetchAll,
  });
}

type UseOrganizationEventOptions = {
  enabled?: boolean;
};

export function useServiceEvent(
  organizationId: number | null | undefined,
  eventId: number | null | undefined,
  options?: UseOrganizationEventOptions,
) {
  const enabled = (options?.enabled ?? true) && organizationId != null && eventId != null;
  return useQuery({
    queryKey: eventKeys.serviceDetail(organizationId ?? 0, eventId ?? 0),
    queryFn: () => eventApi.getServiceEvent(organizationId ?? 0, eventId ?? 0),
    enabled,
    staleTime: 2 * 60 * 1000,
  });
}

export function useOperationEvent(
  organizationId: number | null | undefined,
  eventId: number | null | undefined,
  options?: UseOrganizationEventOptions,
) {
  const enabled = (options?.enabled ?? true) && organizationId != null && eventId != null;
  return useQuery({
    queryKey: eventKeys.operationDetail(organizationId ?? 0, eventId ?? 0),
    queryFn: () => eventApi.getOperationEvent(organizationId ?? 0, eventId ?? 0),
    enabled,
    staleTime: 2 * 60 * 1000,
  });
}

export function useOrganizationEvent(
  eventType: EventType,
  organizationId: number | null | undefined,
  eventId: number | null | undefined,
  options?: UseOrganizationEventOptions,
) {
  const canFetch = options?.enabled ?? true;
  const serviceQuery = useServiceEvent(
    eventType === EventType.SERVICE ? organizationId : null,
    eventType === EventType.SERVICE ? eventId : null,
    { enabled: canFetch && eventType === EventType.SERVICE },
  );
  const operationQuery = useOperationEvent(
    eventType === EventType.OPERATION ? organizationId : null,
    eventType === EventType.OPERATION ? eventId : null,
    { enabled: canFetch && eventType === EventType.OPERATION },
  );
  switch (eventType) {
    case EventType.SERVICE:
      return serviceQuery;
    case EventType.OPERATION:
      return operationQuery;
  }
}

export function eventListDefinitionForType(eventType: EventType) {
  return eventType === EventType.SERVICE
    ? serviceEventListDefinition
    : operationEventListDefinition;
}

export function useUrlSyncedServiceEventListState() {
  return useUrlSyncedListState(serviceEventListDefinition);
}

export function useUrlSyncedOperationEventListState() {
  return useUrlSyncedListState(operationEventListDefinition);
}
