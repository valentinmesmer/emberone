/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { resolveYearListQueryParam } from '@/lib/api/listQueryParams';
import type { ListQuery } from '@/lib/list/types';
import type {
  CreateUserTimeEntryRequest,
  UpdateUserTimeEntryRequest,
  UserTimeEntryDto,
  UserTimeEntryPageDto,
} from '@/types/timeEntry';
import { apiClient } from './client';
import { buildQuery } from './paged';

const BASE_PATH = '/api/v1/self/time-entries';

function organizationTimeEntriesPath(organizationId: number): string {
  return `/api/v1/auth/organizations/${organizationId}/user-time-entries`;
}

function userTimeEntriesPath(organizationId: number, userId: number): string {
  return `/api/v1/auth/organizations/${organizationId}/users/${userId}/time-entries`;
}

export type TimeEntryListRequest = ListQuery & {
  from?: string;
  to?: string;
  organizationId?: readonly string[];
  year?: readonly string[] | string;
};

function listUrl(path: string, query: TimeEntryListRequest): string {
  const qs = buildQuery({
    page: query.page ?? 0,
    size: query.size,
    search: query.search,
    sort: query.sort,
    from: query.from,
    to: query.to,
    organizationId: query.organizationId,
    year: resolveYearListQueryParam(query.year),
  });
  return qs ? `${path}?${qs}` : path;
}

export const timeEntryApi = {
  async listSelf(query: TimeEntryListRequest): Promise<UserTimeEntryPageDto> {
    return apiClient.get<UserTimeEntryPageDto>(listUrl(BASE_PATH, query));
  },

  async getSelf(entryId: number): Promise<UserTimeEntryDto> {
    return apiClient.get<UserTimeEntryDto>(`${BASE_PATH}/${entryId}`);
  },

  async listForOrganization(
    organizationId: number,
    query: TimeEntryListRequest,
  ): Promise<UserTimeEntryPageDto> {
    return apiClient.get<UserTimeEntryPageDto>(
      listUrl(organizationTimeEntriesPath(organizationId), query),
    );
  },

  async getForOrganization(organizationId: number, entryId: number): Promise<UserTimeEntryDto> {
    return apiClient.get<UserTimeEntryDto>(
      `${organizationTimeEntriesPath(organizationId)}/${entryId}`,
    );
  },

  async listForOrganizationUser(
    organizationId: number,
    userId: number,
    query: TimeEntryListRequest,
  ): Promise<UserTimeEntryPageDto> {
    return apiClient.get<UserTimeEntryPageDto>(
      listUrl(userTimeEntriesPath(organizationId, userId), query),
    );
  },

  async createForOrganizationUser(
    organizationId: number,
    userId: number,
    data: CreateUserTimeEntryRequest,
  ): Promise<UserTimeEntryDto> {
    return apiClient.post<UserTimeEntryDto>(userTimeEntriesPath(organizationId, userId), data);
  },

  async updateForOrganizationUser(
    organizationId: number,
    userId: number,
    entryId: number,
    data: UpdateUserTimeEntryRequest,
  ): Promise<UserTimeEntryDto> {
    return apiClient.put<UserTimeEntryDto>(
      `${userTimeEntriesPath(organizationId, userId)}/${entryId}`,
      data,
    );
  },

  async deleteForOrganizationUser(
    organizationId: number,
    userId: number,
    entryId: number,
  ): Promise<void> {
    return apiClient.delete<void>(`${userTimeEntriesPath(organizationId, userId)}/${entryId}`);
  },

  async create(data: CreateUserTimeEntryRequest): Promise<UserTimeEntryDto> {
    return apiClient.post<UserTimeEntryDto>(BASE_PATH, data);
  },

  async update(entryId: number, data: UpdateUserTimeEntryRequest): Promise<UserTimeEntryDto> {
    return apiClient.put<UserTimeEntryDto>(`${BASE_PATH}/${entryId}`, data);
  },

  async delete(entryId: number): Promise<void> {
    return apiClient.delete<void>(`${BASE_PATH}/${entryId}`);
  },
};
