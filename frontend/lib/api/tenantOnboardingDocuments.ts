/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { authorizedFetch } from '@/lib/api/authorizedFetch';
import { getRuntimeConfig } from '@/lib/runtimeConfig';
import { apiClient } from './client';

export type OnboardingDocumentFulfillmentMode = 'CONFIRMED' | 'SIGNED_UPLOAD';

export interface TenantOnboardingDocumentDefinitionDto {
  id: number;
  configKey: string;
  abbreviation: string;
  name: string;
  description: string;
  fulfillmentMode: OnboardingDocumentFulfillmentMode;
  mandatory: boolean;
  active: boolean;
  archived: boolean;
  deletable: boolean;
  archivable: boolean;
  hasTemplate: boolean;
  templateFileName?: string | null;
  templateSizeBytes?: number | null;
}

export interface CreateTenantOnboardingDocumentDefinitionRequest {
  abbreviation: string;
  name: string;
  description: string;
  fulfillmentMode: OnboardingDocumentFulfillmentMode;
  mandatory: boolean;
  active: boolean;
}

export type UpdateTenantOnboardingDocumentDefinitionRequest =
  CreateTenantOnboardingDocumentDefinitionRequest;

function base(tenantId: number) {
  return `/api/v1/auth/admin/tenants/${tenantId}/onboarding-documents`;
}

export const tenantOnboardingDocumentsApi = {
  list(tenantId: number): Promise<TenantOnboardingDocumentDefinitionDto[]> {
    return apiClient.get<TenantOnboardingDocumentDefinitionDto[]>(base(tenantId));
  },

  create(
    tenantId: number,
    data: CreateTenantOnboardingDocumentDefinitionRequest,
  ): Promise<TenantOnboardingDocumentDefinitionDto> {
    return apiClient.post<TenantOnboardingDocumentDefinitionDto>(base(tenantId), data);
  },

  update(
    tenantId: number,
    configKey: string,
    data: UpdateTenantOnboardingDocumentDefinitionRequest,
  ): Promise<TenantOnboardingDocumentDefinitionDto> {
    return apiClient.put<TenantOnboardingDocumentDefinitionDto>(
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

  uploadTemplate(
    tenantId: number,
    configKey: string,
    file: File,
  ): Promise<TenantOnboardingDocumentDefinitionDto> {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.postFormData<TenantOnboardingDocumentDefinitionDto>(
      `${base(tenantId)}/${encodeURIComponent(configKey)}/template`,
      formData,
    );
  },

  async downloadTemplate(tenantId: number, configKey: string): Promise<Blob> {
    const cfg = await getRuntimeConfig();
    const url = `${cfg.NEXT_PUBLIC_API_BASE_URL || ''}${base(tenantId)}/${encodeURIComponent(configKey)}/template/download`;
    const response = await authorizedFetch(url);
    if (!response.ok) {
      throw new Error('Failed to download template');
    }
    return response.blob();
  },

  deleteTemplate(
    tenantId: number,
    configKey: string,
  ): Promise<TenantOnboardingDocumentDefinitionDto> {
    return apiClient.delete<TenantOnboardingDocumentDefinitionDto>(
      `${base(tenantId)}/${encodeURIComponent(configKey)}/template`,
    );
  },
};
