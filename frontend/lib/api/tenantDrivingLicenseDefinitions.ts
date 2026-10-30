/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { apiClient } from './client';

export interface TenantDrivingLicenseDefinitionDto {
  id: number;
  configKey: string;
  abbreviation: string;
  name: string;
  active: boolean;
  archived: boolean;
  deletable: boolean;
  archivable: boolean;
}

export interface CreateTenantDrivingLicenseDefinitionRequest {
  abbreviation: string;
  name: string;
  active: boolean;
}

export type UpdateTenantDrivingLicenseDefinitionRequest = Pick<
  CreateTenantDrivingLicenseDefinitionRequest,
  'name' | 'abbreviation' | 'active'
>;

function base(tenantId: number) {
  return `/api/v1/auth/admin/tenants/${tenantId}/driving-license-definitions`;
}

export const tenantDrivingLicenseDefinitionsApi = {
  list(tenantId: number): Promise<TenantDrivingLicenseDefinitionDto[]> {
    return apiClient.get<TenantDrivingLicenseDefinitionDto[]>(base(tenantId));
  },

  get(tenantId: number, configKey: string): Promise<TenantDrivingLicenseDefinitionDto> {
    return apiClient.get<TenantDrivingLicenseDefinitionDto>(
      `${base(tenantId)}/${encodeURIComponent(configKey)}`,
    );
  },

  create(
    tenantId: number,
    data: CreateTenantDrivingLicenseDefinitionRequest,
  ): Promise<TenantDrivingLicenseDefinitionDto> {
    return apiClient.post<TenantDrivingLicenseDefinitionDto>(base(tenantId), data);
  },

  update(
    tenantId: number,
    configKey: string,
    data: UpdateTenantDrivingLicenseDefinitionRequest,
  ): Promise<TenantDrivingLicenseDefinitionDto> {
    return apiClient.put<TenantDrivingLicenseDefinitionDto>(
      `${base(tenantId)}/${encodeURIComponent(configKey)}`,
      data,
    );
  },

  delete(tenantId: number, configKey: string): Promise<void> {
    return apiClient.delete(`${base(tenantId)}/${encodeURIComponent(configKey)}`);
  },

  archive(tenantId: number, configKey: string): Promise<void> {
    return apiClient.post(`${base(tenantId)}/${encodeURIComponent(configKey)}/archive`, {});
  },

  unarchive(tenantId: number, configKey: string): Promise<void> {
    return apiClient.post(`${base(tenantId)}/${encodeURIComponent(configKey)}/unarchive`, {});
  },
};
