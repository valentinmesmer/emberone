/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { resolveYearListQueryParam } from '@/lib/api/listQueryParams';
import type { AppointmentListPeriodParam } from '@/lib/list/appointmentList';
import type { ListQuery } from '@/lib/list/types';
import type {
  AppointmentDto,
  AppointmentParticipationStatsDto,
  AppointmentResponseDto,
  CreateAppointmentRequest,
  SeriesEditScope,
  SubmitAppointmentResponseRequest,
  UpcomingAppointmentsDto,
  UpdateAppointmentRequest,
} from '@/types/appointment';
import type { PageResponse } from '@/types/page';
import { apiClient } from './client';
import { buildQuery } from './paged';

export type AppointmentListRequest = ListQuery & {
  page?: number;
  period?: AppointmentListPeriodParam | readonly AppointmentListPeriodParam[];
  year?: readonly string[] | string;
  viewed?: readonly string[];
  response?: readonly string[];
  /** Explicit overlap window; when set, the backend ignores `period`. */
  from?: string;
  to?: string;
};

export type AppointmentPageDto = PageResponse<AppointmentDto>;

const appointmentsBasePath = (organizationId: number) =>
  `/api/v1/auth/organizations/${organizationId}/appointments`;

export const appointmentApi = {
  async list(organizationId: number, query: AppointmentListRequest): Promise<AppointmentPageDto> {
    const qs = buildQuery({
      page: query.page ?? 0,
      size: query.size,
      search: query.search,
      sort: query.sort,
      period: query.period,
      year: resolveYearListQueryParam(query.year),
      viewed: query.viewed,
      response: query.response,
      from: query.from,
      to: query.to,
    });
    const path = appointmentsBasePath(organizationId);
    return apiClient.get<AppointmentPageDto>(qs ? `${path}?${qs}` : path);
  },

  async get(organizationId: number, appointmentId: number): Promise<AppointmentDto> {
    return apiClient.get<AppointmentDto>(
      `/api/v1/auth/organizations/${organizationId}/appointments/${appointmentId}`,
    );
  },

  async create(organizationId: number, data: CreateAppointmentRequest): Promise<AppointmentDto> {
    return apiClient.post<AppointmentDto>(
      `/api/v1/auth/organizations/${organizationId}/appointments`,
      data,
    );
  },

  async update(
    organizationId: number,
    appointmentId: number,
    data: UpdateAppointmentRequest,
    scope?: SeriesEditScope,
  ): Promise<AppointmentDto> {
    const query = scope ? `?scope=${scope}` : '';
    return apiClient.put<AppointmentDto>(
      `/api/v1/auth/organizations/${organizationId}/appointments/${appointmentId}${query}`,
      data,
    );
  },

  async delete(
    organizationId: number,
    appointmentId: number,
    scope?: SeriesEditScope,
  ): Promise<void> {
    const query = scope ? `?scope=${scope}` : '';
    return apiClient.delete(
      `/api/v1/auth/organizations/${organizationId}/appointments/${appointmentId}${query}`,
    );
  },

  async submitResponse(
    organizationId: number,
    appointmentId: number,
    data: SubmitAppointmentResponseRequest,
  ): Promise<AppointmentResponseDto> {
    return apiClient.post<AppointmentResponseDto>(
      `/api/v1/self/organizations/${organizationId}/appointments/${appointmentId}/responses`,
      data,
    );
  },

  async getParticipationStats(
    organizationId: number,
    appointmentId: number,
  ): Promise<AppointmentParticipationStatsDto> {
    return apiClient.get<AppointmentParticipationStatsDto>(
      `/api/v1/auth/organizations/${organizationId}/appointments/${appointmentId}/participation-stats`,
    );
  },

  async getUpcoming(limit = 5): Promise<UpcomingAppointmentsDto> {
    return apiClient.get<UpcomingAppointmentsDto>(
      `/api/v1/self/appointments/upcoming?limit=${limit}`,
    );
  },

  async getUnreadCounts(): Promise<Record<string, number>> {
    return apiClient.get<Record<string, number>>(`/api/v1/self/appointments/my-unread-counts`);
  },

  async markRead(organizationId: number, appointmentId: number): Promise<void> {
    return apiClient.post<void>(
      `/api/v1/self/organizations/${organizationId}/appointments/${appointmentId}/mark-read`,
      undefined,
    );
  },

  async markAllRead(organizationId: number): Promise<void> {
    return apiClient.post<void>(
      `/api/v1/self/organizations/${organizationId}/appointments/mark-all-read`,
      undefined,
    );
  },
};
