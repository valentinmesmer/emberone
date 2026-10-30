/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { buildQuery } from '@/lib/api/paged';
import type { ListQuery } from '@/lib/list/types';
import { parseContentDispositionFileName } from '@/lib/utils/contentDisposition';
import { openAttachmentBlob } from '@/lib/utils/openAttachmentBlob';
import type { EntityAttachmentDto } from '@/types/attachment';
import type {
  AddressChangeWorkflowPayload,
  CertificationRecordWorkflowPayload,
  CourseOfferingRegistrationWorkflowPayload,
  CourseRegistrationCancellationWorkflowPayload,
  CreateWorkflowCommentRequest,
  DrivingLicenseRecordWorkflowPayload,
  MainOrganizationChangeWorkflowPayload,
  NameChangeWorkflowPayload,
  OpenOrganizationUserMainOrganizationChangeWorkflowRequest,
  OrganizationGroupJoinRequestWorkflowPayload,
  OrganizationGroupLeaveRequestWorkflowPayload,
  OrganizationJoinRequestWorkflowPayload,
  OrganizationLeaveRequestWorkflowPayload,
  RankRequestWorkflowPayload,
  TenantWorkflowDefinitionDto,
  TrainingCourseRecordWorkflowPayload,
  UpdateWorkflowDefinitionRequest,
  UserDocumentSubmissionWorkflowPayload,
  WorkflowCommentDto,
  WorkflowDecisionRequestBody,
  WorkflowDocumentPromotionPreviewDto,
  WorkflowDocumentPromotionSelection,
  WorkflowDto,
  WorkflowOpenConcurrencyKeysDto,
  WorkflowOpenStateDto,
  WorkflowPageDto,
  WorkflowTypeCatalogDto,
  WorkflowTypeId,
} from '@/types/workflow';
import { apiClient } from './client';

export type WorkflowListRequest = ListQuery & {
  status?: readonly string[];
  type?: readonly string[];
};

function workflowListPath(basePath: string, request: WorkflowListRequest): string {
  const { status, type, ...listQuery } = request;
  const query = buildQuery({
    ...listQuery,
    ...(status != null && status.length > 0 ? { status } : {}),
    ...(type != null && type.length > 0 ? { type } : {}),
  });
  return query ? `${basePath}?${query}` : basePath;
}

export const workflowsApi = {
  async uploadWorkflowDraftAttachment(
    workflowTypeId: WorkflowTypeId,
    file: File,
  ): Promise<EntityAttachmentDto> {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.postFormData<EntityAttachmentDto>(
      `/api/v1/auth/workflows/drafts/${workflowTypeId}/attachments`,
      formData,
    );
  },

  async listWorkflowAttachments(workflowId: number): Promise<EntityAttachmentDto[]> {
    return apiClient.get<EntityAttachmentDto[]>(`/api/v1/auth/workflows/${workflowId}/attachments`);
  },

  async openNameChangeWorkflow(payload: NameChangeWorkflowPayload): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>('/api/v1/auth/workflows/name-change', payload);
  },

  async openAddressChangeWorkflow(payload: AddressChangeWorkflowPayload): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>('/api/v1/auth/workflows/address-change', payload);
  },

  async openOrganizationJoinRequestWorkflow(
    payload: OrganizationJoinRequestWorkflowPayload,
  ): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>('/api/v1/auth/workflows/organization-join-request', payload);
  },

  async openMainOrganizationChangeWorkflow(
    payload: MainOrganizationChangeWorkflowPayload,
  ): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>('/api/v1/auth/workflows/main-organization-change', payload);
  },

  async openOrganizationLeaveRequestWorkflow(
    payload: OrganizationLeaveRequestWorkflowPayload,
  ): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>(
      '/api/v1/auth/workflows/organization-leave-request',
      payload,
    );
  },

  async openOrganizationGroupLeaveRequestWorkflow(
    payload: OrganizationGroupLeaveRequestWorkflowPayload,
  ): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>(
      '/api/v1/auth/workflows/organization-group-leave-request',
      payload,
    );
  },

  async openOrganizationGroupJoinRequestWorkflow(
    payload: OrganizationGroupJoinRequestWorkflowPayload,
  ): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>(
      '/api/v1/auth/workflows/organization-group-join-request',
      payload,
    );
  },

  async openCertificationRecordWorkflow(
    payload: CertificationRecordWorkflowPayload,
  ): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>('/api/v1/auth/workflows/certification-record', payload);
  },

  async openTrainingCourseRecordWorkflow(
    payload: TrainingCourseRecordWorkflowPayload,
  ): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>('/api/v1/auth/workflows/training-course-record', payload);
  },

  async openDrivingLicenseRecordWorkflow(
    payload: DrivingLicenseRecordWorkflowPayload,
  ): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>('/api/v1/auth/workflows/driving-license-record', payload);
  },

  async openRankRequestWorkflow(payload: RankRequestWorkflowPayload): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>('/api/v1/auth/workflows/rank-request', payload);
  },

  async openCourseOfferingRegistration(
    payload: Pick<CourseOfferingRegistrationWorkflowPayload, 'offeringId'>,
  ): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>(
      '/api/v1/auth/workflows/course-offering-registration',
      payload,
    );
  },

  async openCourseRegistrationCancellation(
    payload: Pick<CourseRegistrationCancellationWorkflowPayload, 'registrationId'>,
  ): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>(
      '/api/v1/auth/workflows/course-registration-cancellation',
      payload,
    );
  },

  async openUserDocumentSubmissionWorkflow(
    payload: UserDocumentSubmissionWorkflowPayload,
  ): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>('/api/v1/auth/workflows/user-document-submission', payload);
  },

  async openOnboardingSubmissionWorkflow(invitationId: number): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>(
      `/api/v1/auth/workflows/onboarding-submission/${invitationId}`,
      {},
    );
  },

  async openOrganizationUserMainOrganizationChangeWorkflow(
    organizationId: number,
    userId: number,
    body: OpenOrganizationUserMainOrganizationChangeWorkflowRequest,
  ): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>(
      `/api/v1/auth/organizations/${organizationId}/users/${userId}/workflows/organization-user-main-organization-change`,
      body,
    );
  },

  async openOrganizationMembershipEndWorkflow(
    organizationId: number,
    userId: number,
    body: { reason?: string } = {},
  ): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>(
      `/api/v1/auth/organizations/${organizationId}/users/${userId}/workflows/organization-membership-end`,
      body,
    );
  },

  async listMyOpenedWorkflows(request: WorkflowListRequest): Promise<WorkflowPageDto> {
    return apiClient.get<WorkflowPageDto>(
      workflowListPath('/api/v1/self/workflows/opened', request),
    );
  },

  async listMyWorkflowParticipations(request: WorkflowListRequest): Promise<WorkflowPageDto> {
    return apiClient.get<WorkflowPageDto>(
      workflowListPath('/api/v1/self/workflows/participations', request),
    );
  },

  async getMyWorkflowPendingCount(): Promise<{ count: number }> {
    return apiClient.get<{ count: number }>('/api/v1/self/workflows/pending-count');
  },

  async getMyBeginnableWorkflowTypes(): Promise<WorkflowTypeCatalogDto[]> {
    return apiClient.get<WorkflowTypeCatalogDto[]>('/api/v1/self/workflows/beginnable-types');
  },

  async getWorkflowOpenState(workflowTypeId: WorkflowTypeId): Promise<WorkflowOpenStateDto> {
    return apiClient.get<WorkflowOpenStateDto>(
      `/api/v1/self/workflows/open-state/${workflowTypeId}`,
    );
  },

  async getWorkflowOpenConcurrencyKeys(
    workflowTypeId?: WorkflowTypeId,
  ): Promise<WorkflowOpenConcurrencyKeysDto[]> {
    const query = workflowTypeId != null ? `?workflowTypeId=${workflowTypeId}` : '';
    return apiClient.get<WorkflowOpenConcurrencyKeysDto[]>(
      `/api/v1/self/workflows/open-concurrency-keys${query}`,
    );
  },

  async getWorkflow(workflowId: number): Promise<WorkflowDto> {
    return apiClient.get<WorkflowDto>(`/api/v1/auth/workflows/${workflowId}`);
  },

  async listForOrganizationUser(
    organizationId: number,
    userId: number,
    request: WorkflowListRequest,
  ): Promise<WorkflowPageDto> {
    return apiClient.get<WorkflowPageDto>(
      workflowListPath(
        `/api/v1/auth/organizations/${organizationId}/users/${userId}/workflows`,
        request,
      ),
    );
  },

  async listOpenForOrganization(
    organizationId: number,
    request: WorkflowListRequest,
  ): Promise<WorkflowPageDto> {
    return apiClient.get<WorkflowPageDto>(
      workflowListPath(`/api/v1/auth/organizations/${organizationId}/workflows/open`, request),
    );
  },

  async getDocumentPromotionPreview(
    workflowId: number,
  ): Promise<WorkflowDocumentPromotionPreviewDto> {
    return apiClient.get<WorkflowDocumentPromotionPreviewDto>(
      `/api/v1/auth/workflows/${workflowId}/document-promotion-preview`,
    );
  },

  async updateDocumentPromotions(
    workflowId: number,
    selections: WorkflowDocumentPromotionSelection[],
  ): Promise<WorkflowDocumentPromotionPreviewDto> {
    return apiClient.put<WorkflowDocumentPromotionPreviewDto>(
      `/api/v1/auth/workflows/${workflowId}/document-promotions`,
      { selections },
    );
  },

  async claimWorkflowParticipant(workflowId: number, participantId: number): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>(
      `/api/v1/auth/workflows/${workflowId}/participants/${participantId}/claim`,
      {},
    );
  },

  async unclaimWorkflowParticipant(
    workflowId: number,
    participantId: number,
  ): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>(
      `/api/v1/auth/workflows/${workflowId}/participants/${participantId}/unclaim`,
      {},
    );
  },

  async withdrawWorkflowApproval(workflowId: number, participantId: number): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>(
      `/api/v1/auth/workflows/${workflowId}/participants/${participantId}/withdraw-approval`,
      {},
    );
  },

  async submitWorkflowDecision(
    workflowId: number,
    body: WorkflowDecisionRequestBody,
  ): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>(`/api/v1/auth/workflows/${workflowId}/decisions`, body);
  },

  async cancelWorkflow(workflowId: number): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>(`/api/v1/auth/workflows/${workflowId}/cancel`, {});
  },

  async updateWorkflowPayload(workflowId: number, payload: unknown): Promise<WorkflowDto> {
    return apiClient.put<WorkflowDto>(`/api/v1/auth/workflows/${workflowId}/payload`, payload);
  },

  async requestWorkflowInputChange(
    workflowId: number,
    body: { message: string },
  ): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>(
      `/api/v1/auth/workflows/${workflowId}/input-change-request`,
      body,
    );
  },

  async withdrawWorkflowInputChange(workflowId: number): Promise<WorkflowDto> {
    return apiClient.post<WorkflowDto>(
      `/api/v1/auth/workflows/${workflowId}/input-change-request/withdraw`,
      {},
    );
  },

  async listWorkflowComments(workflowId: number): Promise<WorkflowCommentDto[]> {
    return apiClient.get<WorkflowCommentDto[]>(`/api/v1/auth/workflows/${workflowId}/comments`);
  },

  async createWorkflowComment(
    workflowId: number,
    body: CreateWorkflowCommentRequest,
  ): Promise<WorkflowCommentDto> {
    return apiClient.post<WorkflowCommentDto>(
      `/api/v1/auth/workflows/${workflowId}/comments`,
      body,
    );
  },

  async uploadWorkflowCommentAttachment(
    workflowId: number,
    file: File,
  ): Promise<EntityAttachmentDto> {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.postFormData<EntityAttachmentDto>(
      `/api/v1/auth/workflows/${workflowId}/comment-attachments`,
      formData,
    );
  },

  /**
   * Opens a workflow attachment in a new browser tab for inline viewing.
   * Pass a window opened synchronously from the click handler so popup blockers
   * allow navigation after the async download.
   */
  async viewWorkflowAttachment(
    workflowId: number,
    attachmentId: number,
    targetWindow: Window,
    fallbackFileName = 'attachment',
  ): Promise<void> {
    const { blob, contentDisposition } = await apiClient.getBlob(
      `/api/v1/auth/workflows/${workflowId}/attachments/${attachmentId}/download`,
    );
    const fileName = parseContentDispositionFileName(contentDisposition) ?? fallbackFileName;
    openAttachmentBlob(blob, fileName, targetWindow);
  },

  async getWorkflowTypeCatalog(tenantId: number): Promise<WorkflowTypeCatalogDto[]> {
    return apiClient.get<WorkflowTypeCatalogDto[]>(
      `/api/v1/auth/admin/tenants/${tenantId}/workflow-types`,
    );
  },

  async getTenantWorkflowDefinition(
    tenantId: number,
    workflowTypeId: number,
  ): Promise<TenantWorkflowDefinitionDto> {
    return apiClient.get<TenantWorkflowDefinitionDto>(
      `/api/v1/auth/admin/tenants/${tenantId}/workflow-definitions/${workflowTypeId}`,
    );
  },

  async updateTenantWorkflowDefinition(
    tenantId: number,
    workflowTypeId: number,
    body: UpdateWorkflowDefinitionRequest,
  ): Promise<TenantWorkflowDefinitionDto> {
    return apiClient.put<TenantWorkflowDefinitionDto>(
      `/api/v1/auth/admin/tenants/${tenantId}/workflow-definitions/${workflowTypeId}`,
      body,
    );
  },

  async restoreTenantWorkflowDefinition(
    tenantId: number,
    workflowTypeId: number,
  ): Promise<TenantWorkflowDefinitionDto> {
    return apiClient.post<TenantWorkflowDefinitionDto>(
      `/api/v1/auth/admin/tenants/${tenantId}/workflow-definitions/${workflowTypeId}/restore`,
      {},
    );
  },
};
