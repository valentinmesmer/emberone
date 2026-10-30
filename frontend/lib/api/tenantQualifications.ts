/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { apiClient } from './client';

export type QualificationRequirementMode = 'ALL' | 'ANY';

export interface TenantQualificationDefinitionDto {
  id: number;
  configKey: string;
  abbreviation: string;
  name: string;
  description?: string | null;
  requiresOrgDecision: boolean;
  trainingCourseRequirementMode: QualificationRequirementMode;
  certRequirementMode: QualificationRequirementMode;
  drivingLicenseRequirementMode: QualificationRequirementMode;
  trainingCourseConfigKeys: string[];
  certDefinitionConfigKeys: string[];
  drivingLicenseDefinitionConfigKeys: string[];
  active: boolean;
  archived: boolean;
  deletable: boolean;
  archivable: boolean;
}

export interface CreateTenantQualificationDefinitionRequest {
  abbreviation: string;
  name: string;
  description?: string;
  requiresOrgDecision: boolean;
  trainingCourseRequirementMode: QualificationRequirementMode;
  certRequirementMode: QualificationRequirementMode;
  drivingLicenseRequirementMode: QualificationRequirementMode;
  trainingCourseConfigKeys: string[];
  certDefinitionConfigKeys: string[];
  drivingLicenseDefinitionConfigKeys: string[];
  active: boolean;
}

export type UpdateTenantQualificationDefinitionRequest = CreateTenantQualificationDefinitionRequest;

function base(tenantId: number) {
  return `/api/v1/auth/admin/tenants/${tenantId}/qualification-definitions`;
}

export const tenantQualificationsApi = {
  list(tenantId: number): Promise<TenantQualificationDefinitionDto[]> {
    return apiClient.get<TenantQualificationDefinitionDto[]>(base(tenantId));
  },

  create(
    tenantId: number,
    data: CreateTenantQualificationDefinitionRequest,
  ): Promise<TenantQualificationDefinitionDto> {
    return apiClient.post<TenantQualificationDefinitionDto>(base(tenantId), data);
  },

  update(
    tenantId: number,
    configKey: string,
    data: UpdateTenantQualificationDefinitionRequest,
  ): Promise<TenantQualificationDefinitionDto> {
    return apiClient.put<TenantQualificationDefinitionDto>(
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
