/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { apiClient } from './client';

export type CertEvidenceType = 'DOCUMENT' | 'SERVICE_EVENT' | 'OPERATION_EVENT';

export interface TenantCertDefinitionDto {
  id: number;
  configKey: string;
  abbreviation: string;
  name: string;
  description?: string | null;
  intervalDays?: number | null;
  reminderDaysBeforeExpiry: number;
  active: boolean;
  archived: boolean;
  deletable: boolean;
  archivable: boolean;
  evidenceTypes: CertEvidenceType[];
}

export interface CreateTenantCertDefinitionRequest {
  abbreviation: string;
  name: string;
  description?: string;
  intervalDays?: number | null;
  reminderDaysBeforeExpiry: number;
  active: boolean;
  evidenceTypes: CertEvidenceType[];
}

export type UpdateTenantCertDefinitionRequest = CreateTenantCertDefinitionRequest;

function base(tenantId: number) {
  return `/api/v1/auth/admin/tenants/${tenantId}/certification-definitions`;
}

export const tenantCertificationsApi = {
  list(tenantId: number): Promise<TenantCertDefinitionDto[]> {
    return apiClient.get<TenantCertDefinitionDto[]>(base(tenantId));
  },

  get(tenantId: number, configKey: string): Promise<TenantCertDefinitionDto> {
    return apiClient.get<TenantCertDefinitionDto>(
      `${base(tenantId)}/${encodeURIComponent(configKey)}`,
    );
  },

  create(
    tenantId: number,
    data: CreateTenantCertDefinitionRequest,
  ): Promise<TenantCertDefinitionDto> {
    return apiClient.post<TenantCertDefinitionDto>(base(tenantId), data);
  },

  update(
    tenantId: number,
    configKey: string,
    data: UpdateTenantCertDefinitionRequest,
  ): Promise<TenantCertDefinitionDto> {
    return apiClient.put<TenantCertDefinitionDto>(
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
