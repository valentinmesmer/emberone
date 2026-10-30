/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { CertificationDefinitionDto } from '@/lib/api/certifications';
import type {
  DrivingLicenseClassInput,
  DrivingLicenseDefinitionDto,
} from '@/lib/api/drivingLicenses';
import type { OnboardingStepType } from '@/lib/api/standards';
import type { OnboardingDocumentFulfillmentMode } from '@/lib/api/tenantOnboardingDocuments';
import type { TenantRankDto } from '@/lib/api/tenantRanks';
import type { TrainingCourseTypeDto } from '@/lib/api/trainingCourses';
import type { EmergencyContactRelationship } from './enums/emergencyContactRelationship';
import type { Salutation } from './enums/salutation';
import type { PageResponse } from './page';

export type { OnboardingStepType };

export type OnboardingInvitationStatus = 'OPEN' | 'SUBMITTED' | 'APPROVED';

/** Org-API shape: pending invitees for an organization. */
export interface OnboardingInvitationDto {
  id: number;
  userId: number;
  personalNumber: number;
  salutation?: Salutation | null;
  firstName: string;
  lastName: string;
  dateOfBirth?: string | null;
  privateEmail?: string | null;
  street?: string | null;
  city?: string | null;
  postalCode?: string | null;
  country?: string | null;
  status: OnboardingInvitationStatus;
  invitedAt: string;
}

export type OnboardingInvitationPageDto = PageResponse<OnboardingInvitationDto>;

/** Self-API shape: own pending onboardings (one per target org). */
export interface SelfOnboardingInvitationDto {
  id: number;
  targetOrganizationId: number;
  targetOrganizationName: string;
  status: OnboardingInvitationStatus;
  invitedAt: string;
}

export interface OnboardingDocumentCatalogItemDto {
  configKey: string;
  name: string;
  description: string;
  fulfillmentMode: OnboardingDocumentFulfillmentMode;
  mandatory: boolean;
  hasTemplate: boolean;
  templateFileName?: string | null;
  templateVersion?: number | null;
  templateUploadedAt?: string | null;
  alreadyFulfilled: boolean;
  promotedUserDocumentId?: number | null;
}

export interface OnboardingInvitationCatalogDto {
  documents: OnboardingDocumentCatalogItemDto[];
  organizationDocuments?: OnboardingDocumentCatalogItemDto[];
  trainingCourseTypes: TrainingCourseTypeDto[];
  certifications: CertificationDefinitionDto[];
  drivingLicenseClasses: DrivingLicenseDefinitionDto[];
  ranks?: TenantRankDto[];
}

export interface OnboardingInvitationStepDto {
  stepType: OnboardingStepType;
  sortOrder: number;
  config: Record<string, unknown>;
  fulfilled: boolean;
  payload: Record<string, unknown>;
}

export interface SelfOnboardingInvitationDetailDto {
  id: number;
  subjectUserId: number;
  targetOrganizationId: number;
  targetOrganizationName: string;
  status: OnboardingInvitationStatus;
  invitedAt: string;
  editable: boolean;
  allStepsFulfilled: boolean;
  completionEnabled: boolean;
  alreadyFulfilledStepTypes: OnboardingStepType[];
  inputChangeMessage?: string | null;
  steps: OnboardingInvitationStepDto[];
  catalog: OnboardingInvitationCatalogDto;
}

export interface OnboardingEmergencyContactDraft {
  relationship: EmergencyContactRelationship;
  firstName: string;
  lastName: string;
  street: string;
  city: string;
  postalCode: string;
  country: string;
  phone: string;
  email: string;
}

export interface OnboardingEntryDocumentDraft {
  configKey: string;
  confirmed?: boolean;
  declined?: boolean;
  fileName?: string | null;
  uploaded?: boolean;
  templateOpened?: boolean;
}

export interface OnboardingTrainingCourseDraft {
  recordKey: string;
  tenantTrainingCourseTypeIds: number[];
  name?: string | null;
  startDate: string;
  endDate: string;
  location: string;
  hours?: number | null;
  notes?: string | null;
  fileName?: string | null;
  fileNames?: string[];
  uploaded?: boolean;
}

export interface OnboardingCertificationDraft {
  recordKey: string;
  tenantCertDefinitionId: number;
  completedDate: string;
  notes?: string | null;
  fileName?: string | null;
  fileNames?: string[];
  uploaded?: boolean;
}

export interface OnboardingDrivingLicenseDraft {
  issueDate: string;
  documentExpirationDate?: string | null;
  licenseNumber: string;
  issuingAuthority: string;
  classes: DrivingLicenseClassInput[];
  notes?: string | null;
  fileName?: string | null;
  fileNames?: string[];
  uploaded?: boolean;
}

export interface SaveOnboardingStepRequest {
  payload: Record<string, unknown>;
}

/** How invitation files and wizard data are loaded: Self, org USER_READ, or admin capture. */
export type OnboardingInvitationAccess =
  | { kind: 'self'; invitationId: number }
  | { kind: 'organization'; organizationId: number; invitationId: number }
  | { kind: 'admin'; tenantId: number; organizationId: number; invitationId: number };
