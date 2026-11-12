/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import type { CertificationStatusItemDto } from '@/lib/api/certifications';
import type { DrivingLicenseDefinitionDto } from '@/lib/api/drivingLicenses';
import { buildQuery } from '@/lib/api/paged';
import type { QualificationRequirementMode } from '@/lib/api/tenantQualifications';
import type { ListQuery } from '@/lib/list/types';
import type { PageResponse } from '@/types/page';
import { apiClient } from './client';

export type QualificationExerciseStatus =
  | 'NOT_AVAILABLE'
  | 'PREREQUISITES_MISSING'
  | 'ORG_DECISION_DENIED'
  | 'ORG_DECISION_REVOKED'
  | 'CAN_EXERCISE';

export interface QualificationPrerequisiteDetailDto {
  configKey: string;
  abbreviation: string;
  name: string;
  met: boolean;
}

export interface QualificationStatusItemDto {
  tenantQualificationDefinitionId: number;
  configKey: string;
  abbreviation: string;
  name: string;
  status: QualificationExerciseStatus;
  prerequisitesMet: boolean;
  trainingCoursesMet: boolean;
  certDefinitionsMet: boolean;
  drivingLicensesMet: boolean;
  /** Present on status payloads from the API; default to ALL when absent. */
  trainingCourseRequirementMode?: QualificationRequirementMode;
  certRequirementMode?: QualificationRequirementMode;
  drivingLicenseRequirementMode?: QualificationRequirementMode;
  requiresOrgDecision: boolean;
  orgDecisionGranted: boolean;
  orgDecisions: UserQualificationOrgDecisionDto[];
  trainingCourseDetails: QualificationPrerequisiteDetailDto[];
  certDefinitionDetails: QualificationPrerequisiteDetailDto[];
  drivingLicenseDetails: QualificationPrerequisiteDetailDto[];
}

export interface UserQualificationOrgDecisionDto {
  id: number;
  status: 'GRANTED' | 'REVOKED';
  startDate: string;
  endDate: string | null;
  notes: string | null;
}

export interface UserQualificationStatusDto {
  userId: number;
  organizationId: number;
  items: QualificationStatusItemDto[];
}

export interface QualificationOverviewMemberDto {
  userId: number;
  userDisplayName: string;
}

export interface QualificationOverviewGroupDto {
  tenantQualificationDefinitionId: number;
  configKey: string;
  name: string;
  requiresOrgDecision: boolean;
  members: QualificationOverviewMemberDto[];
  appointableMembers: QualificationOverviewMemberDto[];
}

export interface OrganizationQualificationStatusOverviewDto {
  groups: QualificationOverviewGroupDto[];
}

export type QualificationStatusItemPageDto = PageResponse<QualificationStatusItemDto>;

export interface SelfQualificationStatusItemDto {
  organizationId: number;
  item: QualificationStatusItemDto;
}

export type SelfQualificationStatusItemPageDto = PageResponse<SelfQualificationStatusItemDto>;

export interface QualificationTrainingRecordRefDto {
  configKey: string;
  recordId: number;
}

export interface QualificationDrivingLicenseDto {
  documentExpirationDate: string | null;
  classes: {
    tenantDrivingLicenseDefinitionId: number;
    grantDate: string | null;
    expirationDate: string | null;
  }[];
}

/** Org view of one qualification; evidence sections are empty without the matching permission. */
export interface UserQualificationDetailDto {
  item: QualificationStatusItemDto;
  certifications: CertificationStatusItemDto[];
  trainingRecords: QualificationTrainingRecordRefDto[];
  drivingLicense: QualificationDrivingLicenseDto | null;
  drivingLicenseDefinitions: DrivingLicenseDefinitionDto[];
}

export interface SelfQualificationDetailDto {
  organizations: SelfQualificationStatusItemDto[];
  certifications: CertificationStatusItemDto[];
  trainingRecords: QualificationTrainingRecordRefDto[];
  drivingLicense: QualificationDrivingLicenseDto | null;
}

export type QualificationListRequest = ListQuery & {
  status?: QualificationExerciseStatus;
};

function orgBase(organizationId: number, userId: number) {
  return `/api/v1/auth/organizations/${organizationId}/users/${userId}/qualifications`;
}

export const qualificationsApi = {
  getStatus(
    organizationId: number,
    userId: number,
    query: QualificationListRequest = {},
  ): Promise<QualificationStatusItemPageDto> {
    const qs = buildQuery({
      page: query.page ?? 0,
      size: query.size,
      search: query.search,
      status: query.status,
    });
    const base = `${orgBase(organizationId, userId)}/status`;
    return apiClient.get<QualificationStatusItemPageDto>(qs ? `${base}?${qs}` : base);
  },

  getUserDetail(
    organizationId: number,
    userId: number,
    definitionId: number,
  ): Promise<UserQualificationDetailDto> {
    return apiClient.get<UserQualificationDetailDto>(
      `${orgBase(organizationId, userId)}/${definitionId}`,
    );
  },

  getSelfDetail(definitionId: number): Promise<SelfQualificationDetailDto> {
    return apiClient.get<SelfQualificationDetailDto>(`/api/v1/self/qualifications/${definitionId}`);
  },

  getStatusOverview(
    organizationId: number,
    search?: string,
  ): Promise<OrganizationQualificationStatusOverviewDto> {
    const base = `/api/v1/auth/organizations/${organizationId}/qualifications/status-overview`;
    return apiClient.get<OrganizationQualificationStatusOverviewDto>(
      search ? `${base}?search=${encodeURIComponent(search)}` : base,
    );
  },

  grantOrgDecision(
    organizationId: number,
    userId: number,
    qualificationConfigKey: string,
    data: { startDate: string; endDate?: string; notes?: string },
  ): Promise<void> {
    return apiClient.post(
      `${orgBase(organizationId, userId)}/${encodeURIComponent(qualificationConfigKey)}/org-decision`,
      {
        startDate: data.startDate,
        endDate: data.endDate,
        notes: data.notes,
      },
    );
  },

  updateOrgDecision(
    organizationId: number,
    userId: number,
    qualificationConfigKey: string,
    data: {
      id: number;
      startDate: string;
      endDate?: string;
      notes?: string;
    },
  ): Promise<void> {
    return apiClient.put(
      `${orgBase(organizationId, userId)}/${encodeURIComponent(qualificationConfigKey)}/org-decision`,
      {
        id: data.id,
        startDate: data.startDate,
        endDate: data.endDate,
        notes: data.notes,
      },
    );
  },

  getSelfStatus(query: QualificationListRequest = {}): Promise<SelfQualificationStatusItemPageDto> {
    const qs = buildQuery({
      page: query.page ?? 0,
      size: query.size,
      search: query.search,
      status: query.status,
    });
    const base = '/api/v1/self/qualifications/status';
    return apiClient.get<SelfQualificationStatusItemPageDto>(qs ? `${base}?${qs}` : base);
  },
};
