/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import {
  type InfiniteData,
  type QueryClient,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { useMemo } from 'react';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import {
  type AppointmentListRequest,
  type AppointmentPageDto,
  appointmentApi,
} from '@/lib/api/appointment';
import type { ListDefinition, ListQuery } from '@/lib/list/types';
import { useUrlSyncedListState } from '@/lib/list/useListState';
import { useRealtimePollingFallback } from '@/lib/realtime/useRealtimePollingFallback';
import { STALE_TIME } from '@/lib/store/staleTime';
import { usePagedList } from '@/lib/store/usePagedList';
import { formatLocalDateTimeFromDate } from '@/lib/utils/date';
import type {
  AppointmentDto,
  AppointmentParticipationStatsDto,
  CreateAppointmentRequest,
  SeriesEditScope,
  SubmitAppointmentResponseRequest,
  UpdateAppointmentRequest,
} from '@/types/appointment';

export const appointmentKeys = {
  all: ['appointments'] as const,
  lists: () => [...appointmentKeys.all, 'list'] as const,
  list: (orgId: number, listQuery: ListQuery) =>
    [...appointmentKeys.lists(), orgId, listQuery] as const,
  details: () => [...appointmentKeys.all, 'detail'] as const,
  detail: (orgId: number, id: number) => [...appointmentKeys.details(), orgId, id] as const,
  upcomingRoot: () => [...appointmentKeys.all, 'upcoming'] as const,
  upcoming: (limit: number) => [...appointmentKeys.upcomingRoot(), limit] as const,
  participationStats: (orgId: number, appointmentId: number) =>
    [...appointmentKeys.all, 'participation-stats', orgId, appointmentId] as const,
  unreadCounts: () => [...appointmentKeys.all, 'unread-counts'] as const,
};

/** Lists, details, upcoming and unread counts; the records themselves (incl. embedded response and attachment count). */
export function invalidateAppointmentRecords(queryClient: QueryClient) {
  void queryClient.invalidateQueries({ queryKey: appointmentKeys.lists() });
  void queryClient.invalidateQueries({ queryKey: appointmentKeys.details() });
  void queryClient.invalidateQueries({ queryKey: appointmentKeys.upcomingRoot() });
  void queryClient.invalidateQueries({ queryKey: appointmentKeys.unreadCounts() });
}

function isAppointmentListQueryKey(queryKey: readonly unknown[]): boolean {
  return (
    queryKey[0] === appointmentKeys.all[0] &&
    queryKey[1] === 'list' &&
    typeof queryKey[2] === 'number'
  );
}

function forEachCachedAppointmentList(
  queryClient: QueryClient,
  organizationId: number,
  updater: (page: AppointmentPageDto) => AppointmentPageDto,
) {
  for (const query of queryClient.getQueryCache().findAll({
    queryKey: [...appointmentKeys.all, 'list', organizationId],
    exact: false,
  })) {
    if (!isAppointmentListQueryKey(query.queryKey)) {
      continue;
    }
    const current = query.state.data;
    if (current == null) {
      continue;
    }
    if (Array.isArray(current)) {
      queryClient.setQueryData(
        query.queryKey,
        current.map((appointment) => ({ ...appointment, unread: false })),
      );
      continue;
    }
    if (typeof current === 'object' && 'pages' in current) {
      const infinite = current as InfiniteData<AppointmentPageDto>;
      queryClient.setQueryData(query.queryKey, {
        ...infinite,
        pages: infinite.pages.map(updater),
      });
    }
  }
}

export function useUrlSyncedAppointmentListState(definition: ListDefinition) {
  return useUrlSyncedListState(definition);
}

type UseAppointmentListOptions = {
  organizationId: number | null;
  listQuery: ListQuery;
  enabled?: boolean;
  fetchAll?: boolean;
};

export function useAppointmentList({
  organizationId,
  listQuery,
  enabled = true,
  fetchAll = false,
}: UseAppointmentListOptions) {
  const orgId = organizationId ?? 0;
  return usePagedList<AppointmentDto, AppointmentListRequest>({
    queryKey: appointmentKeys.list(orgId, listQuery),
    listQuery: listQuery as AppointmentListRequest,
    queryFn: (query) => appointmentApi.list(orgId, query),
    enabled: organizationId != null && enabled,
    staleTime: STALE_TIME.LIST,
    fetchAll,
  });
}

/**
 * Loads all appointments from one month back to eleven months ahead (for lookups / import
 * flows). Sends an explicit `from`/`to` window, which the backend uses instead of `period`.
 */
export function useAppointments(organizationId: number | null) {
  const listQuery = useMemo<ListQuery>(() => {
    const from = new Date();
    from.setHours(0, 0, 0, 0);
    from.setMonth(from.getMonth() - 1);
    const to = new Date(from);
    to.setMonth(to.getMonth() + 12);
    // Backend parses from/to as organization wall clock (no offset) and caps the span at 12 months.
    return { from: formatLocalDateTimeFromDate(from), to: formatLocalDateTimeFromDate(to) };
  }, []);
  return useAppointmentList({
    organizationId,
    listQuery,
    fetchAll: true,
  });
}

export function useUpcomingAppointments(limit = 5) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: appointmentKeys.upcoming(limit),
    queryFn: () => appointmentApi.getUpcoming(limit),
    enabled: hasEmberOneUserId,
    // Carries the unread badge total; realtime events refresh it.
    staleTime: STALE_TIME.LIVE,
  });
}

export function useAppointment(organizationId: number | null, appointmentId: number | null) {
  return useQuery({
    queryKey: appointmentKeys.detail(organizationId ?? 0, appointmentId ?? 0),
    queryFn: () => appointmentApi.get(organizationId ?? 0, appointmentId ?? 0),
    enabled: organizationId != null && appointmentId != null,
    staleTime: STALE_TIME.LIST,
  });
}

export function useAppointmentParticipationStats(
  organizationId: number,
  appointmentId: number | null | undefined,
  options?: { enabled?: boolean },
) {
  const enabled = (options?.enabled ?? true) && appointmentId != null;
  return useQuery<AppointmentParticipationStatsDto>({
    queryKey: appointmentKeys.participationStats(organizationId, appointmentId ?? 0),
    queryFn: () => appointmentApi.getParticipationStats(organizationId, appointmentId ?? 0),
    enabled,
    staleTime: STALE_TIME.LIST,
  });
}

export function useAppointmentUnreadCount(
  organizationId: number | null,
  options?: { enabled?: boolean },
) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  const refetchInterval = useRealtimePollingFallback(120 * 1000);
  return useQuery({
    queryKey: appointmentKeys.unreadCounts(),
    queryFn: () => appointmentApi.getUnreadCounts(),
    enabled: hasEmberOneUserId && organizationId != null && (options?.enabled ?? true),
    staleTime: STALE_TIME.LIVE,
    refetchInterval,
    select: (data) => ({
      count: organizationId != null ? Number(data[String(organizationId)] ?? 0) : 0,
    }),
  });
}

export function useAppointmentUnreadCountsByOrganization(organizationIds: number[]) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  const refetchInterval = useRealtimePollingFallback(120 * 1000);
  const { data } = useQuery({
    queryKey: appointmentKeys.unreadCounts(),
    queryFn: () => appointmentApi.getUnreadCounts(),
    enabled: hasEmberOneUserId && organizationIds.length > 0,
    staleTime: STALE_TIME.LIVE,
    refetchInterval,
  });

  return useMemo(
    () =>
      organizationIds.reduce<Record<number, number>>((counts, organizationId) => {
        counts[organizationId] = Number(data?.[String(organizationId)] ?? 0);
        return counts;
      }, {}),
    [data, organizationIds],
  );
}

export function useMarkAppointmentRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      organizationId,
      appointmentId,
    }: {
      organizationId: number;
      appointmentId: number;
    }) => appointmentApi.markRead(organizationId, appointmentId),
    onMutate: async ({ organizationId, appointmentId }) => {
      await Promise.all([
        queryClient.cancelQueries({
          queryKey: appointmentKeys.detail(organizationId, appointmentId),
        }),
        queryClient.cancelQueries({ queryKey: appointmentKeys.unreadCounts() }),
      ]);

      const previousDetail = queryClient.getQueryData<AppointmentDto>(
        appointmentKeys.detail(organizationId, appointmentId),
      );
      const previousUnreadCounts = queryClient.getQueryData<Record<string, number>>(
        appointmentKeys.unreadCounts(),
      );
      const wasUnread = previousDetail?.unread === true;

      if (wasUnread) {
        queryClient.setQueryData<AppointmentDto>(
          appointmentKeys.detail(organizationId, appointmentId),
          (current) => (current ? { ...current, unread: false } : current),
        );
        queryClient.setQueryData<Record<string, number>>(
          appointmentKeys.unreadCounts(),
          (current) => {
            if (!current) return current;
            const key = String(organizationId);
            return {
              ...current,
              [key]: Math.max(0, Number(current[key] ?? 0) - 1),
            };
          },
        );
      }

      return { previousDetail, previousUnreadCounts };
    },
    onError: (_error, { organizationId, appointmentId }, context) => {
      if (context?.previousDetail !== undefined) {
        queryClient.setQueryData(
          appointmentKeys.detail(organizationId, appointmentId),
          context.previousDetail,
        );
      }
      if (context?.previousUnreadCounts) {
        queryClient.setQueryData(appointmentKeys.unreadCounts(), context.previousUnreadCounts);
      }
    },
    onSettled: () => {
      // Read state only shows in lists and counters; the detail is already patched optimistically.
      void queryClient.invalidateQueries({ queryKey: appointmentKeys.lists() });
      void queryClient.invalidateQueries({ queryKey: appointmentKeys.upcomingRoot() });
      void queryClient.invalidateQueries({ queryKey: appointmentKeys.unreadCounts() });
    },
  });
}

export function useMarkAllAppointmentsRead(organizationId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => appointmentApi.markAllRead(organizationId),
    onSuccess: () => {
      forEachCachedAppointmentList(queryClient, organizationId, (page) => ({
        ...page,
        content: page.content.map((appointment) => ({ ...appointment, unread: false })),
      }));
      queryClient.setQueryData<Record<string, number>>(
        appointmentKeys.unreadCounts(),
        (current) => ({
          ...(current ?? {}),
          [String(organizationId)]: 0,
        }),
      );
      void queryClient.invalidateQueries({ queryKey: appointmentKeys.lists() });
      void queryClient.invalidateQueries({ queryKey: appointmentKeys.details() });
      void queryClient.invalidateQueries({ queryKey: appointmentKeys.upcomingRoot() });
      void queryClient.invalidateQueries({ queryKey: appointmentKeys.unreadCounts() });
    },
  });
}

export function useCreateAppointment(organizationId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateAppointmentRequest) => appointmentApi.create(organizationId, data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: appointmentKeys.lists() });
      void queryClient.invalidateQueries({ queryKey: appointmentKeys.upcomingRoot() });
      void queryClient.invalidateQueries({ queryKey: appointmentKeys.unreadCounts() });
    },
  });
}

export function useUpdateAppointment(organizationId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      appointmentId,
      data,
      scope,
    }: {
      appointmentId: number;
      data: UpdateAppointmentRequest;
      scope?: SeriesEditScope;
    }) => appointmentApi.update(organizationId, appointmentId, data, scope),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: appointmentKeys.all });
    },
  });
}

export function useDeleteAppointment(organizationId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ appointmentId, scope }: { appointmentId: number; scope?: SeriesEditScope }) =>
      appointmentApi.delete(organizationId, appointmentId, scope),
    onSuccess: () => {
      invalidateAppointmentRecords(queryClient);
    },
  });
}

/** Submit RSVP / response; invalidates the records carrying `myResponse` and the participation stats. */
export function useSubmitAppointmentResponse() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      organizationId,
      appointmentId,
      data,
    }: {
      organizationId: number;
      appointmentId: number;
      data: SubmitAppointmentResponseRequest;
    }) => appointmentApi.submitResponse(organizationId, appointmentId, data),
    onSuccess: (_data, { organizationId, appointmentId }) => {
      void queryClient.invalidateQueries({ queryKey: appointmentKeys.lists() });
      void queryClient.invalidateQueries({ queryKey: appointmentKeys.details() });
      void queryClient.invalidateQueries({ queryKey: appointmentKeys.upcomingRoot() });
      void queryClient.invalidateQueries({
        queryKey: appointmentKeys.participationStats(organizationId, appointmentId),
      });
    },
  });
}
