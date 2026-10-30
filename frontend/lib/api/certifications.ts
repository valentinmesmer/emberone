/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { apiClient } from '@/lib/api/client';
import { buildQuery } from '@/lib/api/paged';
import type { CertEvidenceType } from '@/lib/api/tenantCertifications';
import type { ListQuery } from '@/lib/list/types';
import type { PageResponse } from '@/types/page';

export type CertificationStatus =
  | 'MISSING'
  | 'IN_REVIEW'
  | 'EXPIRED'
  | 'EXPIRING_SOON'
  | 'VALID'
  | 'ARCHIVED';

export interface CertificationStatusItemDto {
  tenantCertDefinitionId: number;
  certDefinitionName: string;
  status: CertificationStatus;
  mandatory: boolean;
  responsibleOrganizationId: number | null;
  completedDate: string | null;
  expiryDate: string | null;
  recordId: number | null;
  archived: boolean;
  assigned: boolean;
  evidenceTypes?: CertEvidenceType[];
}

export interface UserCertificationStatusDto {
  userId: number;
  items: CertificationStatusItemDto[];
  missingCount: number;
  expiredCount: number;
  expiringSoonCount: number;
  validCount: number;
}

export interface CertificationOverviewRowDto {
  userId: number;
  userDisplayName: string;
  tenantCertDefinitionId: number;
  certDefinitionName: string;
  status: CertificationStatus;
  expiryDate: string | null;
}

export interface CertificationOverviewCountsDto {
  all: number;
  missing: number;
  expired: number;
  expiringSoon: number;
  valid: number;
}

export type CertificationOverviewRequest = ListQuery;

export interface UserCertificationRecordDto {
  id: number;
  userId: number;
  tenantCertDefinitionId: number;
  certDefinitionName: string | null;
  definitionArchived: boolean;
  completedDate: string;
  expiryDate: string | null;
  userDocumentIds: number[];
  notes: string | null;
  appLinkType: string | null;
  appLinkId: number | null;
  submissionMethod?: 'MANUAL' | 'WORKFLOW' | 'EVENT' | 'ONBOARDING' | null;
  /** Present when submissionMethod is EVENT: SERVICE or OPERATION. */
  eventType?: 'SERVICE' | 'OPERATION' | null;
  /** Organization of the linked event when submissionMethod is EVENT. */
  eventOrganizationId?: number | null;
  navigationPath?: string | null;
}

export interface UserCertificationAssignmentDto {
  id: number;
  userId: number;
  responsibleOrganizationId: number;
  tenantCertDefinitionId: number;
  certDefinitionName: string | null;
  source: string;
  startDate: string | null;
  endDate: string | null;
}

export interface CertificationDefinitionDto {
  id: number;
  name: string;
  description: string | null;
  intervalDays: number | null;
  reminderDaysBeforeExpiry: number | null;
  active: boolean;
  configKey: string | null;
  evidenceTypes?: CertEvidenceType[];
}

export interface OrgCertificationCatalogDto {
  organizationId: number;
  definitions: CertificationDefinitionDto[];
}

export interface CreateUserCertificationAssignmentRequest {
  tenantCertDefinitionId: number;
  responsibleOrganizationId?: number | null;
  startDate?: string | null;
  endDate?: string | null;
}

export interface CreateUserCertificationRecordRequest {
  tenantCertDefinitionId: number;
  completedDate: string;
  expiryDate?: string | null;
  userDocumentIds?: number[];
  notes?: string | null;
}

const selfBase = '/api/v1/self/certifications';
const orgBase = (organizationId: number, userId: number) =>
  `/api/v1/auth/organizations/${organizationId}/users/${userId}/certifications`;

export const certificationsApi = {
  // --- self ---
  getMyStatus(): Promise<UserCertificationStatusDto> {
    return apiClient.get<UserCertificationStatusDto>(`${selfBase}/status`);
  },
  getMyRecords(): Promise<UserCertificationRecordDto[]> {
    return apiClient.get<UserCertificationRecordDto[]>(`${selfBase}/records`);
  },
  getMyRequestableDefinitions(): Promise<CertificationDefinitionDto[]> {
    return apiClient.get<CertificationDefinitionDto[]>(`${selfBase}/requestable-definitions`);
  },

  // --- org/admin scoped ---
  getUserStatus(organizationId: number, userId: number): Promise<UserCertificationStatusDto> {
    return apiClient.get<UserCertificationStatusDto>(`${orgBase(organizationId, userId)}/status`);
  },
  getStatusOverview(
    organizationId: number,
    query: CertificationOverviewRequest = {},
  ): Promise<PageResponse<CertificationOverviewRowDto>> {
    const qs = buildQuery({
      page: query.page ?? 0,
      size: query.size,
      search: query.search,
      sort: query.sort,
      status: query.status,
      type: query.type,
    });
    return apiClient.get<PageResponse<CertificationOverviewRowDto>>(
      `/api/v1/auth/organizations/${organizationId}/certifications/status-overview?${qs}`,
    );
  },
  getStatusOverviewCounts(organizationId: number): Promise<CertificationOverviewCountsDto> {
    return apiClient.get<CertificationOverviewCountsDto>(
      `/api/v1/auth/organizations/${organizationId}/certifications/status-overview/counts`,
    );
  },
  getUserRecords(organizationId: number, userId: number): Promise<UserCertificationRecordDto[]> {
    return apiClient.get<UserCertificationRecordDto[]>(
      `${orgBase(organizationId, userId)}/records`,
    );
  },
  listAssignments(
    organizationId: number,
    userId: number,
  ): Promise<UserCertificationAssignmentDto[]> {
    return apiClient.get<UserCertificationAssignmentDto[]>(
      `${orgBase(organizationId, userId)}/assignments`,
    );
  },
  createAssignment(
    organizationId: number,
    userId: number,
    data: CreateUserCertificationAssignmentRequest,
  ): Promise<UserCertificationAssignmentDto> {
    return apiClient.post<UserCertificationAssignmentDto>(
      `${orgBase(organizationId, userId)}/assignments`,
      data,
    );
  },
  deleteAssignment(organizationId: number, userId: number, assignmentId: number): Promise<void> {
    return apiClient.delete<void>(`${orgBase(organizationId, userId)}/assignments/${assignmentId}`);
  },
  createRecord(
    organizationId: number,
    userId: number,
    data: CreateUserCertificationRecordRequest,
  ): Promise<UserCertificationRecordDto> {
    return apiClient.post<UserCertificationRecordDto>(
      `${orgBase(organizationId, userId)}/records`,
      data,
    );
  },
  getCatalog(organizationId: number, userId: number): Promise<OrgCertificationCatalogDto> {
    return apiClient.get<OrgCertificationCatalogDto>(
      `${orgBase(organizationId, userId)}/available`,
    );
  },
};
