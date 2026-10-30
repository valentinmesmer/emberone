/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { resolveYearListQueryParam } from '@/lib/api/listQueryParams';
import type { EventListAttendanceParam } from '@/lib/list/eventList';
import type { ListQuery } from '@/lib/list/types';
import type {
  CreateEventAttendanceRequest,
  CreateOperationEventRequest,
  CreateServiceEventRequest,
  EventAttendanceDto,
  EventAttendancePageDto,
  EventAttendanceSelfItemDto,
  OperationEventDto,
  OperationEventPageDto,
  ServiceEventDto,
  ServiceEventPageDto,
  UpdateOperationEventRequest,
  UpdateServiceEventRequest,
} from '@/types/event';
import { apiClient } from './client';
import { buildQuery } from './paged';

export type EventListRequest = ListQuery & {
  year?: readonly string[] | string;
  attendance?: readonly EventListAttendanceParam[];
  /** Explicit start range (import lookups). */
  from?: string;
  to?: string;
};

export type EventAttendanceListRequest = ListQuery;

function organizationEventsPath(organizationId: number, category: 'service' | 'operation'): string {
  return `/api/v1/auth/organizations/${organizationId}/events/${category}`;
}

function listUrl(path: string, query: EventListRequest | EventAttendanceListRequest): string {
  const eventQuery = query as EventListRequest;
  const qs = buildQuery({
    page: query.page ?? 0,
    size: query.size,
    search: query.search,
    sort: query.sort,
    year: resolveYearListQueryParam(eventQuery.year),
    attendance: eventQuery.attendance,
    from: eventQuery.from,
    to: eventQuery.to,
  });
  return qs ? `${path}?${qs}` : path;
}

export const eventApi = {
  async listServiceEvents(
    organizationId: number,
    query: EventListRequest,
  ): Promise<ServiceEventPageDto> {
    return apiClient.get<ServiceEventPageDto>(
      listUrl(organizationEventsPath(organizationId, 'service'), query),
    );
  },

  async getServiceEvent(organizationId: number, eventId: number): Promise<ServiceEventDto> {
    return apiClient.get<ServiceEventDto>(
      `${organizationEventsPath(organizationId, 'service')}/${eventId}`,
    );
  },

  async listOperationEvents(
    organizationId: number,
    query: EventListRequest,
  ): Promise<OperationEventPageDto> {
    return apiClient.get<OperationEventPageDto>(
      listUrl(organizationEventsPath(organizationId, 'operation'), query),
    );
  },

  async getOperationEvent(organizationId: number, eventId: number): Promise<OperationEventDto> {
    return apiClient.get<OperationEventDto>(
      `${organizationEventsPath(organizationId, 'operation')}/${eventId}`,
    );
  },

  async createServiceEvent(data: CreateServiceEventRequest): Promise<ServiceEventDto> {
    if (!data.organizationId) {
      throw new Error('Organization ID is required');
    }
    return apiClient.post<ServiceEventDto>(
      `/api/v1/auth/organizations/${data.organizationId}/events/service`,
      data,
    );
  },

  async createOperationEvent(data: CreateOperationEventRequest): Promise<OperationEventDto> {
    if (!data.organizationId) {
      throw new Error('Organization ID is required');
    }
    return apiClient.post<OperationEventDto>(
      `/api/v1/auth/organizations/${data.organizationId}/events/operation`,
      data,
    );
  },

  async updateServiceEvent(
    organizationId: number,
    eventId: number,
    data: UpdateServiceEventRequest,
  ): Promise<ServiceEventDto> {
    return apiClient.put<ServiceEventDto>(
      `/api/v1/auth/organizations/${organizationId}/events/service/${eventId}`,
      data,
    );
  },

  async deleteServiceEvent(organizationId: number, eventId: number): Promise<void> {
    return apiClient.delete(
      `/api/v1/auth/organizations/${organizationId}/events/service/${eventId}`,
    );
  },

  async updateOperationEvent(
    organizationId: number,
    eventId: number,
    data: UpdateOperationEventRequest,
  ): Promise<OperationEventDto> {
    return apiClient.put<OperationEventDto>(
      `/api/v1/auth/organizations/${organizationId}/events/operation/${eventId}`,
      data,
    );
  },

  async deleteOperationEvent(organizationId: number, eventId: number): Promise<void> {
    return apiClient.delete(
      `/api/v1/auth/organizations/${organizationId}/events/operation/${eventId}`,
    );
  },

  async listEventAttendances(
    organizationId: number,
    eventCategory: string,
    eventId: number,
    query: EventAttendanceListRequest,
  ): Promise<EventAttendancePageDto> {
    const path = `/api/v1/auth/organizations/${organizationId}/events/${eventCategory}/${eventId}/attendances`;
    return apiClient.get<EventAttendancePageDto>(listUrl(path, query));
  },

  async getMyEventAttendances(
    organizationId: number,
    eventCategory: string,
    eventIds: number[],
  ): Promise<EventAttendanceSelfItemDto[]> {
    if (eventIds.length === 0) return [];
    const query = eventIds.map((id) => `eventIds=${id}`).join('&');
    return apiClient.get<EventAttendanceSelfItemDto[]>(
      `/api/v1/self/organizations/${organizationId}/events/${eventCategory}/attendances/me?${query}`,
    );
  },

  async createEventAttendance(
    organizationId: number,
    eventCategory: string,
    eventId: number,
    data: CreateEventAttendanceRequest,
  ): Promise<EventAttendanceDto> {
    return apiClient.post<EventAttendanceDto>(
      `/api/v1/auth/organizations/${organizationId}/events/${eventCategory}/${eventId}/attendances`,
      data,
    );
  },

  async deleteEventAttendance(
    organizationId: number,
    eventCategory: string,
    eventId: number,
    attendanceId: number,
  ): Promise<void> {
    return apiClient.delete(
      `/api/v1/auth/organizations/${organizationId}/events/${eventCategory}/${eventId}/attendances/${attendanceId}`,
    );
  },
};
