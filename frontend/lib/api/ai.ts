/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { apiClient } from '@/lib/api/client';
import type {
  AiCapabilitiesDto,
  AiExtractionTarget,
  AiProofType,
  DocumentExtractionResultDto,
  HelpChatMessageDto,
  HelpChatResponseDto,
} from '@/types/ai';

const EXTRACT_PATHS: Record<AiProofType, string> = {
  CERTIFICATION_RECORD: '/api/v1/auth/workflows/certification-record/extract',
  TRAINING_COURSE_RECORD: '/api/v1/auth/workflows/training-course-record/extract',
  DRIVING_LICENSE_RECORD: '/api/v1/auth/workflows/driving-license-record/extract',
  USER_DOCUMENT_SUBMISSION: '/api/v1/auth/workflows/user-document-submission/extract',
  ADDRESS_CHANGE: '/api/v1/auth/workflows/address-change/extract',
  NAME_CHANGE: '/api/v1/auth/workflows/name-change/extract',
  RANK_REQUEST: '/api/v1/auth/workflows/rank-request/extract',
};

/** Admin surface path segment per proof type (only types an admin can enter for a member). */
const ADMIN_EXTRACT_SEGMENTS: Partial<Record<AiProofType, string>> = {
  CERTIFICATION_RECORD: 'certifications',
  TRAINING_COURSE_RECORD: 'training-courses',
  DRIVING_LICENSE_RECORD: 'driving-license',
};

function extractPath(proofType: AiProofType, target?: AiExtractionTarget): string {
  const segment = ADMIN_EXTRACT_SEGMENTS[proofType];
  if (target && segment) {
    return `/api/v1/auth/organizations/${target.organizationId}/users/${target.userId}/${segment}/extract`;
  }
  return EXTRACT_PATHS[proofType];
}

export const aiApi = {
  async getCapabilities(): Promise<AiCapabilitiesDto> {
    return apiClient.get<AiCapabilitiesDto>('/api/v1/self/ai/capabilities');
  },

  async helpChat(messages: HelpChatMessageDto[]): Promise<HelpChatResponseDto> {
    return apiClient.post<HelpChatResponseDto>('/api/v1/self/ai/help-chat', { messages });
  },

  /** Sends all proof documents of one proof for suggestions; the server stores nothing. */
  async extractFromDocument(
    proofType: AiProofType,
    files: File[],
    selectionId?: number | null,
    target?: AiExtractionTarget,
  ): Promise<DocumentExtractionResultDto> {
    const formData = new FormData();
    for (const file of files) {
      formData.append('file', file);
    }
    if (proofType === 'CERTIFICATION_RECORD' && selectionId != null) {
      formData.append('tenantCertDefinitionId', String(selectionId));
    }
    return apiClient.postFormData<DocumentExtractionResultDto>(
      extractPath(proofType, target),
      formData,
    );
  },
};
