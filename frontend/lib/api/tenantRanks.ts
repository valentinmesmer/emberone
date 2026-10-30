/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { apiClient } from './client';

export interface TenantRankDto {
  id: number;
  configKey: string;
  name: string;
  abbreviation: string;
  sortOrder: number;
  active: boolean;
  archived: boolean;
  archivedAt?: string | null;
}

export interface CreateTenantRankRequest {
  configKey: string;
  name: string;
  abbreviation: string;
  sortOrder: number;
  active: boolean;
}

export interface UpdateTenantRankRequest {
  name: string;
  abbreviation: string;
  sortOrder: number;
  active: boolean;
}

function base(tenantId: number) {
  return `/api/v1/auth/admin/tenants/${tenantId}/ranks`;
}

export const tenantRanksApi = {
  list(tenantId: number): Promise<TenantRankDto[]> {
    return apiClient.get<TenantRankDto[]>(base(tenantId));
  },

  createRank(tenantId: number, data: CreateTenantRankRequest): Promise<TenantRankDto> {
    return apiClient.post<TenantRankDto>(base(tenantId), data);
  },

  updateRank(
    tenantId: number,
    configKey: string,
    data: UpdateTenantRankRequest,
  ): Promise<TenantRankDto> {
    return apiClient.put<TenantRankDto>(`${base(tenantId)}/${encodeURIComponent(configKey)}`, data);
  },

  archive(tenantId: number, configKey: string): Promise<void> {
    return apiClient.post(`${base(tenantId)}/${encodeURIComponent(configKey)}/archive`, {});
  },

  unarchive(tenantId: number, configKey: string): Promise<void> {
    return apiClient.post(`${base(tenantId)}/${encodeURIComponent(configKey)}/unarchive`, {});
  },
};
