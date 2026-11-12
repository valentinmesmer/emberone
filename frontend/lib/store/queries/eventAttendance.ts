/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { type QueryClient, useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { type EventAttendanceListRequest, eventApi } from '@/lib/api/event';
import { useListState } from '@/lib/list';
import {
  eventAttendanceListDefaultQuery,
  eventAttendanceListDefinition,
} from '@/lib/list/eventAttendanceList';
import type { ListQuery } from '@/lib/list/types';
import { STALE_TIME } from '@/lib/store/staleTime';
import { usePagedList } from '@/lib/store/usePagedList';
import { eventTypeToUrlSegment } from '@/lib/utils/event';
import type { EventType } from '@/types/event';
import { certificationKeys } from './certifications';
import { eventCertificationGrantKeys } from './eventCertificationGrants';
import { userKeys } from './users';

export const eventAttendanceKeys = {
  all: ['eventAttendances'] as const,
  /** Attendances of one event (the paged list appends its `listQuery`). */
  list: (organizationId: number, eventCategory: string, eventId: number) =>
    [...eventAttendanceKeys.all, 'list', organizationId, eventCategory, eventId] as const,
  meBatchRoot: (organizationId: number, eventCategory: string) =>
    [...eventAttendanceKeys.all, 'me-batch', organizationId, eventCategory] as const,
  meBatch: (organizationId: number, eventCategory: string, eventIds: readonly number[]) =>
    [...eventAttendanceKeys.meBatchRoot(organizationId, eventCategory), eventIds] as const,
};

export function invalidateAfterEventAttendanceChange(
  queryClient: QueryClient,
  organizationId: number,
  eventType: EventType,
  eventId: number,
) {
  const category = eventTypeToUrlSegment(eventType);
  void queryClient.invalidateQueries({
    queryKey: eventAttendanceKeys.list(organizationId, category, eventId),
  });
  void queryClient.invalidateQueries({
    queryKey: eventAttendanceKeys.meBatchRoot(organizationId, category),
  });
  void queryClient.invalidateQueries({ queryKey: userKeys.operationAlarmAttendanceStats() });
  // ALL_ATTENDEES grants create/remove Nachweise when attendance changes
  void queryClient.invalidateQueries({
    queryKey: eventCertificationGrantKeys.event(organizationId, eventType, eventId),
  });
  void queryClient.invalidateQueries({ queryKey: certificationKeys.all });
}

/** Local (non-URL) list state for the attendance section of an event detail page. */
export function useEventAttendanceListState() {
  return useListState(eventAttendanceListDefinition);
}

/**
 * Server-paged attendance list (search by name) for the event detail section. Consumers that need
 * every attendee (modals, grants) use {@link useEventAttendances} instead.
 */
export function useEventAttendanceList(
  organizationId: number | null | undefined,
  eventType: EventType | null | undefined,
  eventId: number | null | undefined,
  listQuery: ListQuery,
) {
  return useEventAttendances(organizationId, eventType, eventId, {
    listQuery,
    fetchAll: false,
  });
}

export function useEventAttendances(
  organizationId: number | null | undefined,
  eventType: EventType | null | undefined,
  eventId: number | null | undefined,
  options?: { enabled?: boolean; listQuery?: EventAttendanceListRequest; fetchAll?: boolean },
) {
  const category = eventType ? eventTypeToUrlSegment(eventType) : '';
  const enabled = !!organizationId && !!eventType && eventId != null && (options?.enabled ?? true);
  const listQuery = options?.listQuery ?? eventAttendanceListDefaultQuery;

  return usePagedList({
    queryKey: eventAttendanceKeys.list(organizationId ?? 0, category, eventId ?? 0),
    listQuery,
    queryFn: (query) =>
      eventApi.listEventAttendances(organizationId ?? 0, category, eventId ?? 0, query),
    enabled,
    staleTime: STALE_TIME.LIST,
    fetchAll: options?.fetchAll ?? true,
  });
}

/**
 * For each event id: whether the logged-in user is on the attendance list (one batch request).
 */
export function useMyEventAttendanceForEventIds(
  organizationId: number | null | undefined,
  eventType: EventType | null | undefined,
  eventIds: readonly number[],
  options?: { enabled?: boolean },
) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  const category = eventType ? eventTypeToUrlSegment(eventType) : '';
  const baseEnabled = options?.enabled ?? true;
  const enabled =
    hasEmberOneUserId &&
    baseEnabled &&
    organizationId != null &&
    !!eventType &&
    eventIds.length > 0;

  const { data, isPending, isLoading, error } = useQuery({
    queryKey: eventAttendanceKeys.meBatch(organizationId ?? 0, category, eventIds),
    queryFn: () => eventApi.getMyEventAttendances(organizationId ?? 0, category, [...eventIds]),
    enabled,
    staleTime: STALE_TIME.LIST,
  });

  return useMemo(() => {
    const m = new Map<number, boolean | undefined>();
    if (!enabled) {
      return m;
    }
    if (isPending || isLoading) {
      eventIds.forEach((id) => {
        m.set(id, undefined);
      });
      return m;
    }
    if (error) {
      eventIds.forEach((id) => {
        m.set(id, undefined);
      });
      return m;
    }
    const byId = new Map((data ?? []).map((item) => [item.eventId, item.attending]));
    eventIds.forEach((id) => {
      m.set(id, byId.has(id) ? !!byId.get(id) : false);
    });
    return m;
  }, [data, enabled, error, eventIds, isLoading, isPending]);
}
