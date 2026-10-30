/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { authorizedFetch } from '@/lib/api/authorizedFetch';
import type {
  CreateTenantOnboardingDocumentDefinitionRequest,
  OnboardingDocumentFulfillmentMode,
  TenantOnboardingDocumentDefinitionDto,
  UpdateTenantOnboardingDocumentDefinitionRequest,
} from '@/lib/api/tenantOnboardingDocuments';
import { getRuntimeConfig } from '@/lib/runtimeConfig';
import { apiClient } from './client';

export type OrganizationOnboardingDocumentDefinitionDto = TenantOnboardingDocumentDefinitionDto & {
  organizationId: number;
};

export type CreateOrganizationOnboardingDocumentDefinitionRequest =
  CreateTenantOnboardingDocumentDefinitionRequest;

export type UpdateOrganizationOnboardingDocumentDefinitionRequest =
  UpdateTenantOnboardingDocumentDefinitionRequest;

export type { OnboardingDocumentFulfillmentMode };

function base(tenantId: number, organizationId: number) {
  return `/api/v1/auth/admin/tenants/${tenantId}/organizations/${organizationId}/onboarding-documents`;
}

export const adminOrganizationOnboardingDocumentsApi = {
  list(
    tenantId: number,
    organizationId: number,
  ): Promise<OrganizationOnboardingDocumentDefinitionDto[]> {
    return apiClient.get<OrganizationOnboardingDocumentDefinitionDto[]>(
      base(tenantId, organizationId),
    );
  },

  create(
    tenantId: number,
    organizationId: number,
    data: CreateOrganizationOnboardingDocumentDefinitionRequest,
  ): Promise<OrganizationOnboardingDocumentDefinitionDto> {
    return apiClient.post<OrganizationOnboardingDocumentDefinitionDto>(
      base(tenantId, organizationId),
      data,
    );
  },

  update(
    tenantId: number,
    organizationId: number,
    configKey: string,
    data: UpdateOrganizationOnboardingDocumentDefinitionRequest,
  ): Promise<OrganizationOnboardingDocumentDefinitionDto> {
    return apiClient.put<OrganizationOnboardingDocumentDefinitionDto>(
      `${base(tenantId, organizationId)}/${encodeURIComponent(configKey)}`,
      data,
    );
  },

  delete(tenantId: number, organizationId: number, configKey: string): Promise<void> {
    return apiClient.delete(`${base(tenantId, organizationId)}/${encodeURIComponent(configKey)}`);
  },

  archive(tenantId: number, organizationId: number, configKey: string): Promise<void> {
    return apiClient.post(
      `${base(tenantId, organizationId)}/${encodeURIComponent(configKey)}/archive`,
      {},
    );
  },

  unarchive(tenantId: number, organizationId: number, configKey: string): Promise<void> {
    return apiClient.post(
      `${base(tenantId, organizationId)}/${encodeURIComponent(configKey)}/unarchive`,
      {},
    );
  },

  uploadTemplate(
    tenantId: number,
    organizationId: number,
    configKey: string,
    file: File,
  ): Promise<OrganizationOnboardingDocumentDefinitionDto> {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.postFormData<OrganizationOnboardingDocumentDefinitionDto>(
      `${base(tenantId, organizationId)}/${encodeURIComponent(configKey)}/template`,
      formData,
    );
  },

  async downloadTemplate(
    tenantId: number,
    organizationId: number,
    configKey: string,
  ): Promise<Blob> {
    const cfg = await getRuntimeConfig();
    const url = `${cfg.NEXT_PUBLIC_API_BASE_URL || ''}${base(tenantId, organizationId)}/${encodeURIComponent(configKey)}/template/download`;
    const response = await authorizedFetch(url);
    if (!response.ok) {
      throw new Error('Failed to download template');
    }
    return response.blob();
  },

  deleteTemplate(
    tenantId: number,
    organizationId: number,
    configKey: string,
  ): Promise<OrganizationOnboardingDocumentDefinitionDto> {
    return apiClient.delete<OrganizationOnboardingDocumentDefinitionDto>(
      `${base(tenantId, organizationId)}/${encodeURIComponent(configKey)}/template`,
    );
  },
};
