/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { OnboardingStepType } from '@/lib/api/standards';
import type { ListQuery } from '@/lib/list/types';
import { parseContentDispositionFileName } from '@/lib/utils/contentDisposition';
import { openAttachmentBlob } from '@/lib/utils/openAttachmentBlob';
import type {
  OnboardingInvitationAccess,
  OnboardingInvitationPageDto,
  SaveOnboardingStepRequest,
  SelfOnboardingInvitationDetailDto,
  SelfOnboardingInvitationDto,
} from '@/types/onboardingInvitation';
import { apiClient } from './client';
import { buildQuery } from './paged';

function organizationInvitationsPath(organizationId: number): string {
  return `/api/v1/auth/organizations/${organizationId}/onboarding-invitations`;
}

const SELF_PATH = '/api/v1/self/onboarding-invitations';

function adminInvitationPath(
  tenantId: number,
  organizationId: number,
  invitationId: number,
): string {
  return `/api/v1/auth/admin/tenants/${tenantId}/organizations/${organizationId}/onboarding-invitations/${invitationId}`;
}

export function invitationPath(access: OnboardingInvitationAccess): string {
  if (access.kind === 'self') {
    return `${SELF_PATH}/${access.invitationId}`;
  }
  if (access.kind === 'admin') {
    return adminInvitationPath(access.tenantId, access.organizationId, access.invitationId);
  }
  return `${organizationInvitationsPath(access.organizationId)}/${access.invitationId}`;
}

async function downloadInvitationFile(
  path: string,
  fallbackFileName: string,
): Promise<{ blob: Blob; fileName: string }> {
  const { blob, contentDisposition } = await apiClient.getBlob(path);
  return {
    blob,
    fileName: parseContentDispositionFileName(contentDisposition) ?? fallbackFileName,
  };
}

async function viewInvitationFile(
  path: string,
  targetWindow: Window,
  fallbackFileName: string,
): Promise<void> {
  const { blob, fileName } = await downloadInvitationFile(path, fallbackFileName);
  openAttachmentBlob(blob, fileName, targetWindow);
}

export const onboardingInvitationApi = {
  async listForOrganization(
    organizationId: number,
    query: ListQuery,
  ): Promise<OnboardingInvitationPageDto> {
    const qs = buildQuery({
      page: query.page ?? 0,
      size: query.size,
      search: query.search,
      sort: query.sort,
    });
    const path = organizationInvitationsPath(organizationId);
    return apiClient.get<OnboardingInvitationPageDto>(qs ? `${path}?${qs}` : path);
  },

  async listForSelf(): Promise<SelfOnboardingInvitationDto[]> {
    return apiClient.get<SelfOnboardingInvitationDto[]>(SELF_PATH);
  },

  async getOrganizationDetail(
    organizationId: number,
    invitationId: number,
  ): Promise<SelfOnboardingInvitationDetailDto> {
    return apiClient.get<SelfOnboardingInvitationDetailDto>(
      `${organizationInvitationsPath(organizationId)}/${invitationId}`,
    );
  },

  async getOrganizationUserOnboardingDetail(
    organizationId: number,
    userId: number,
  ): Promise<SelfOnboardingInvitationDetailDto> {
    return apiClient.get<SelfOnboardingInvitationDetailDto>(
      `/api/v1/auth/organizations/${organizationId}/users/${userId}/onboarding-invitation`,
    );
  },

  async getOrganizationOnboardingAvailable(
    organizationId: number,
  ): Promise<{ available: boolean }> {
    return apiClient.get<{ available: boolean }>(
      `/api/v1/auth/organizations/${organizationId}/onboarding-available`,
    );
  },

  async getSelfDetail(invitationId: number): Promise<SelfOnboardingInvitationDetailDto> {
    return apiClient.get<SelfOnboardingInvitationDetailDto>(`${SELF_PATH}/${invitationId}`);
  },

  async saveStep(
    access: OnboardingInvitationAccess,
    stepType: OnboardingStepType,
    request: SaveOnboardingStepRequest,
  ): Promise<SelfOnboardingInvitationDetailDto> {
    return apiClient.put<SelfOnboardingInvitationDetailDto>(
      `${invitationPath(access)}/steps/${stepType}`,
      request,
    );
  },

  async downloadDocumentTemplate(
    access: OnboardingInvitationAccess,
    configKey: string,
    fallbackFileName: string,
  ): Promise<{ blob: Blob; fileName: string }> {
    return downloadInvitationFile(
      `${invitationPath(access)}/documents/${encodeURIComponent(configKey)}/template`,
      fallbackFileName,
    );
  },

  async downloadSignedDocument(
    access: OnboardingInvitationAccess,
    configKey: string,
    fallbackFileName: string,
  ): Promise<{ blob: Blob; fileName: string }> {
    return downloadInvitationFile(
      `${invitationPath(access)}/documents/${encodeURIComponent(configKey)}/file`,
      fallbackFileName,
    );
  },

  async downloadRecordAttachment(
    access: OnboardingInvitationAccess,
    stepType: OnboardingStepType,
    recordKey: string,
    fallbackFileName: string,
    attachmentIndex = 0,
  ): Promise<{ blob: Blob; fileName: string }> {
    return downloadInvitationFile(
      `${invitationPath(access)}/steps/${stepType}/records/${encodeURIComponent(recordKey)}/attachment?attachmentIndex=${attachmentIndex}`,
      fallbackFileName,
    );
  },

  async viewDocumentTemplate(
    access: OnboardingInvitationAccess,
    configKey: string,
    fallbackFileName: string,
    targetWindow: Window,
  ): Promise<void> {
    return viewInvitationFile(
      `${invitationPath(access)}/documents/${encodeURIComponent(configKey)}/template`,
      targetWindow,
      fallbackFileName,
    );
  },

  async viewSignedDocument(
    access: OnboardingInvitationAccess,
    configKey: string,
    fallbackFileName: string,
    targetWindow: Window,
  ): Promise<void> {
    return viewInvitationFile(
      `${invitationPath(access)}/documents/${encodeURIComponent(configKey)}/file`,
      targetWindow,
      fallbackFileName,
    );
  },

  async viewRecordAttachment(
    access: OnboardingInvitationAccess,
    stepType: OnboardingStepType,
    recordKey: string,
    fallbackFileName: string,
    targetWindow: Window,
    attachmentIndex = 0,
  ): Promise<void> {
    return viewInvitationFile(
      `${invitationPath(access)}/steps/${stepType}/records/${encodeURIComponent(recordKey)}/attachment?attachmentIndex=${attachmentIndex}`,
      targetWindow,
      fallbackFileName,
    );
  },

  async viewDrivingLicenseAttachment(
    access: OnboardingInvitationAccess,
    fallbackFileName: string,
    targetWindow: Window,
    attachmentIndex = 0,
  ): Promise<void> {
    return viewInvitationFile(
      `${invitationPath(access)}/steps/DRIVING_LICENSE/attachment?attachmentIndex=${attachmentIndex}`,
      targetWindow,
      fallbackFileName,
    );
  },

  async uploadSignedDocument(
    access: OnboardingInvitationAccess,
    configKey: string,
    file: File,
  ): Promise<SelfOnboardingInvitationDetailDto> {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.postFormData<SelfOnboardingInvitationDetailDto>(
      `${invitationPath(access)}/documents/${encodeURIComponent(configKey)}`,
      formData,
    );
  },

  async downloadOrganizationDocumentTemplate(
    access: OnboardingInvitationAccess,
    configKey: string,
    fallbackFileName: string,
  ): Promise<{ blob: Blob; fileName: string }> {
    return downloadInvitationFile(
      `${invitationPath(access)}/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/${encodeURIComponent(configKey)}/template`,
      fallbackFileName,
    );
  },

  async downloadOrganizationSignedDocument(
    access: OnboardingInvitationAccess,
    configKey: string,
    fallbackFileName: string,
  ): Promise<{ blob: Blob; fileName: string }> {
    return downloadInvitationFile(
      `${invitationPath(access)}/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/${encodeURIComponent(configKey)}/file`,
      fallbackFileName,
    );
  },

  async viewOrganizationDocumentTemplate(
    access: OnboardingInvitationAccess,
    configKey: string,
    fallbackFileName: string,
    targetWindow: Window,
  ): Promise<void> {
    return viewInvitationFile(
      `${invitationPath(access)}/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/${encodeURIComponent(configKey)}/template`,
      targetWindow,
      fallbackFileName,
    );
  },

  async viewOrganizationSignedDocument(
    access: OnboardingInvitationAccess,
    configKey: string,
    fallbackFileName: string,
    targetWindow: Window,
  ): Promise<void> {
    return viewInvitationFile(
      `${invitationPath(access)}/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/${encodeURIComponent(configKey)}/file`,
      targetWindow,
      fallbackFileName,
    );
  },

  async uploadOrganizationSignedDocument(
    access: OnboardingInvitationAccess,
    configKey: string,
    file: File,
  ): Promise<SelfOnboardingInvitationDetailDto> {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.postFormData<SelfOnboardingInvitationDetailDto>(
      `${invitationPath(access)}/steps/ORGANIZATION_ENTRY_DOCUMENTS/documents/${encodeURIComponent(configKey)}`,
      formData,
    );
  },

  async uploadRecordAttachment(
    access: OnboardingInvitationAccess,
    stepType: OnboardingStepType,
    recordKey: string,
    file: File,
  ): Promise<SelfOnboardingInvitationDetailDto> {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.postFormData<SelfOnboardingInvitationDetailDto>(
      `${invitationPath(access)}/steps/${stepType}/records/${encodeURIComponent(recordKey)}/attachment`,
      formData,
    );
  },

  async uploadDrivingLicenseAttachment(
    access: OnboardingInvitationAccess,
    file: File,
  ): Promise<SelfOnboardingInvitationDetailDto> {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.postFormData<SelfOnboardingInvitationDetailDto>(
      `${invitationPath(access)}/steps/DRIVING_LICENSE/attachment`,
      formData,
    );
  },

  async uploadRankAttachment(
    access: OnboardingInvitationAccess,
    file: File,
  ): Promise<SelfOnboardingInvitationDetailDto> {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.postFormData<SelfOnboardingInvitationDetailDto>(
      `${invitationPath(access)}/steps/RANK/attachment`,
      formData,
    );
  },

  async deleteRecordAttachment(
    access: OnboardingInvitationAccess,
    stepType: OnboardingStepType,
    recordKey: string,
    attachmentIndex = 0,
  ): Promise<SelfOnboardingInvitationDetailDto> {
    return apiClient.delete<SelfOnboardingInvitationDetailDto>(
      `${invitationPath(access)}/steps/${stepType}/records/${encodeURIComponent(recordKey)}/attachment?attachmentIndex=${attachmentIndex}`,
    );
  },

  async deleteDrivingLicenseAttachment(
    access: OnboardingInvitationAccess,
    attachmentIndex = 0,
  ): Promise<SelfOnboardingInvitationDetailDto> {
    return apiClient.delete<SelfOnboardingInvitationDetailDto>(
      `${invitationPath(access)}/steps/DRIVING_LICENSE/attachment?attachmentIndex=${attachmentIndex}`,
    );
  },

  async deleteRankAttachment(
    access: OnboardingInvitationAccess,
    attachmentIndex = 0,
  ): Promise<SelfOnboardingInvitationDetailDto> {
    return apiClient.delete<SelfOnboardingInvitationDetailDto>(
      `${invitationPath(access)}/steps/RANK/attachment?attachmentIndex=${attachmentIndex}`,
    );
  },
};
