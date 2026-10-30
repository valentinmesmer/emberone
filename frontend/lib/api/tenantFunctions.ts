/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { apiClient } from './client';

export interface TenantFunctionDto {
  configKey: string;
  name: string;
  abbreviation: string;
  sortOrder: number;
  active: boolean;
  archived: boolean;
  archivedAt?: string | null;
}

export interface CreateTenantFunctionRequest {
  configKey: string;
  name: string;
  abbreviation: string;
  sortOrder: number;
  active: boolean;
}

export type UpdateTenantFunctionRequest = CreateTenantFunctionRequest;

function base(tenantId: number) {
  return `/api/v1/auth/admin/tenants/${tenantId}/functions`;
}

export const tenantFunctionsApi = {
  list(tenantId: number): Promise<TenantFunctionDto[]> {
    return apiClient.get<TenantFunctionDto[]>(base(tenantId));
  },

  create(tenantId: number, data: CreateTenantFunctionRequest): Promise<TenantFunctionDto> {
    return apiClient.post<TenantFunctionDto>(base(tenantId), data);
  },

  update(
    tenantId: number,
    configKey: string,
    data: UpdateTenantFunctionRequest,
  ): Promise<TenantFunctionDto> {
    return apiClient.put<TenantFunctionDto>(
      `${base(tenantId)}/${encodeURIComponent(configKey)}`,
      data,
    );
  },

  archive(tenantId: number, configKey: string): Promise<void> {
    return apiClient.post(`${base(tenantId)}/${encodeURIComponent(configKey)}/archive`, {});
  },

  unarchive(tenantId: number, configKey: string): Promise<void> {
    return apiClient.post(`${base(tenantId)}/${encodeURIComponent(configKey)}/unarchive`, {});
  },
};
