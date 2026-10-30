/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { resolveYearListQueryParam } from '@/lib/api/listQueryParams';
import type { AbsenceListPeriodParam } from '@/lib/list/absenceList';
import type { ListQuery } from '@/lib/list/types';
import type {
  CreateUserAbsenceRequest,
  UpdateUserAbsenceRequest,
  UserAbsenceDto,
  UserAbsencePageDto,
} from '@/types/absence';
import { apiClient } from './client';
import { buildQuery } from './paged';

const BASE_PATH = '/api/v1/self/absences';

export type AbsenceListRequest = ListQuery & {
  page?: number;
  period?: AbsenceListPeriodParam | readonly AbsenceListPeriodParam[];
  reason?: readonly string[];
  organizationId?: readonly string[];
  year?: readonly string[] | string;
};

function organizationAbsencesPath(organizationId: number): string {
  return `/api/v1/auth/organizations/${organizationId}/user-absences`;
}

function organizationActiveAbsencesPath(organizationId: number): string {
  return `${organizationAbsencesPath(organizationId)}/active`;
}

function userAbsencesPath(organizationId: number, userId: number): string {
  return `/api/v1/auth/organizations/${organizationId}/users/${userId}/absences`;
}

function listUrl(path: string, query: AbsenceListRequest): string {
  const qs = buildQuery({
    page: query.page ?? 0,
    size: query.size,
    search: query.search,
    sort: query.sort,
    period: query.period,
    reason: query.reason,
    organizationId: query.organizationId,
    year: resolveYearListQueryParam(query.year),
  });
  return qs ? `${path}?${qs}` : path;
}

export const absenceApi = {
  async list(query: AbsenceListRequest): Promise<UserAbsencePageDto> {
    return apiClient.get<UserAbsencePageDto>(listUrl(BASE_PATH, query));
  },

  async getSelf(absenceId: number): Promise<UserAbsenceDto> {
    return apiClient.get<UserAbsenceDto>(`${BASE_PATH}/${absenceId}`);
  },

  async listActiveForOrganization(
    organizationId: number,
    query: AbsenceListRequest,
  ): Promise<UserAbsencePageDto> {
    return apiClient.get<UserAbsencePageDto>(
      listUrl(organizationActiveAbsencesPath(organizationId), query),
    );
  },

  async getForOrganization(organizationId: number, absenceId: number): Promise<UserAbsenceDto> {
    return apiClient.get<UserAbsenceDto>(
      `${organizationAbsencesPath(organizationId)}/${absenceId}`,
    );
  },

  async listForOrganizationUser(
    organizationId: number,
    userId: number,
    query: AbsenceListRequest,
  ): Promise<UserAbsencePageDto> {
    return apiClient.get<UserAbsencePageDto>(
      listUrl(userAbsencesPath(organizationId, userId), query),
    );
  },

  async createForOrganizationUser(
    organizationId: number,
    userId: number,
    data: CreateUserAbsenceRequest,
  ): Promise<UserAbsenceDto> {
    return apiClient.post<UserAbsenceDto>(userAbsencesPath(organizationId, userId), data);
  },

  async updateForOrganizationUser(
    organizationId: number,
    userId: number,
    absenceId: number,
    data: UpdateUserAbsenceRequest,
  ): Promise<UserAbsenceDto> {
    return apiClient.put<UserAbsenceDto>(
      `${userAbsencesPath(organizationId, userId)}/${absenceId}`,
      data,
    );
  },

  async deleteForOrganizationUser(
    organizationId: number,
    userId: number,
    absenceId: number,
  ): Promise<void> {
    return apiClient.delete<void>(`${userAbsencesPath(organizationId, userId)}/${absenceId}`);
  },

  async create(data: CreateUserAbsenceRequest): Promise<UserAbsenceDto> {
    return apiClient.post<UserAbsenceDto>(BASE_PATH, data);
  },

  async update(absenceId: number, data: UpdateUserAbsenceRequest): Promise<UserAbsenceDto> {
    return apiClient.put<UserAbsenceDto>(`${BASE_PATH}/${absenceId}`, data);
  },

  async delete(absenceId: number): Promise<void> {
    return apiClient.delete<void>(`${BASE_PATH}/${absenceId}`);
  },
};
