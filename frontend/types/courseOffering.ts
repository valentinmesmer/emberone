/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { QualificationRequirementMode } from '@/lib/api/tenantQualifications';
import type { AppLinkFields } from '@/types/appLink';
import type { PageResponse } from '@/types/page';

export type CourseOfferingStatus = 'TEMPLATE' | 'DRAFT' | 'PUBLISHED' | 'COMPLETED' | 'CANCELLED';

export type CourseSessionType = 'THEORY' | 'PRACTICE' | 'EXAM';

export type CourseAudienceKind = 'OWN_ORG' | 'ORG' | 'ORG_TYPE' | 'ORG_SUBTREE';

export type CoursePrerequisiteKind = 'TRAINING_COURSE' | 'CERTIFICATION' | 'DRIVING_LICENSE';

export type CourseRegistrationStatus =
  | 'PENDING'
  | 'PARTICIPANT'
  | 'WAITLIST'
  | 'REJECTED'
  | 'CANCELLED';

export type CourseRegistrationEndedBy = 'USER' | 'ORGANIZATION';

export interface CourseSessionDto {
  id: number;
  type: CourseSessionType;
  start: string;
  end: string;
  location: string | null;
  notes: string | null;
}

export interface CourseAudienceEntryDto {
  id: number;
  kind: CourseAudienceKind;
  refId: number | null;
  quota: number | null;
}

/** Minimal org row from `GET …/course-offerings/audience-options`. */
export interface CourseAudienceOrganizationOptionDto {
  id: number;
  name: string;
}

/** Minimal org-type row from `GET …/course-offerings/audience-options`. */
export interface CourseAudienceOrganizationTypeOptionDto {
  id: number;
  name: string;
}

export interface CourseAudienceOptionsDto {
  organizations: CourseAudienceOrganizationOptionDto[];
  organizationTypes: CourseAudienceOrganizationTypeOptionDto[];
}

export interface CoursePrerequisitesDto {
  trainingCourseRequirementMode: QualificationRequirementMode;
  certRequirementMode: QualificationRequirementMode;
  drivingLicenseRequirementMode: QualificationRequirementMode;
  entries: { kind: CoursePrerequisiteKind; configKey: string }[];
}

export interface CourseOfferingDto {
  id: number;
  organizationId: number;
  trainingCourseTypeId: number;
  trainingCourseTypeName: string;
  content: string | null;
  notes: string | null;
  templateName: string | null;
  status: CourseOfferingStatus;
  registrationStart: string | null;
  registrationEnd: string | null;
  maxParticipants: number;
  hasExam: boolean;
  requiresHomeOrgApproval: boolean;
  firstSessionStart: string | null;
  lastSessionEnd: string | null;
  sessions: CourseSessionDto[];
  audiences: CourseAudienceEntryDto[];
  prerequisites: CoursePrerequisitesDto | null;
  cancelledAt: string | null;
  cancelReason: string | null;
}

export type CourseOfferingPageDto = PageResponse<CourseOfferingDto>;

export interface SelfCourseOfferingMyRegistrationDto {
  id: number;
  status: CourseRegistrationStatus;
  waitlistPosition: number | null;
  registeredAt: string;
  endedBy: CourseRegistrationEndedBy | null;
  endReason: string | null;
  workflowId: number | null;
  cancellationWorkflowId: number | null;
  canCancel: boolean;
  cancellationPending: boolean;
  canWithdraw: boolean;
}

export interface SelfCourseOfferingDto {
  id: number;
  organizationId: number;
  organizationName: string;
  trainingCourseTypeId: number;
  trainingCourseTypeName: string;
  content: string | null;
  notes: string | null;
  registrationStart: string | null;
  registrationEnd: string | null;
  registrationOpen: boolean;
  full: boolean;
  maxParticipants: number;
  hasExam: boolean;
  requiresHomeOrgApproval: boolean;
  alreadyHeld: boolean;
  /** The caller meets all prerequisites of the offering. */
  prerequisitesMet: boolean;
  firstSessionStart: string | null;
  lastSessionEnd: string | null;
  sessions: CourseSessionDto[];
  myRegistration: SelfCourseOfferingMyRegistrationDto | null;
}

export type SelfCourseOfferingPageDto = PageResponse<SelfCourseOfferingDto>;

export interface SelfCourseRegistrationDto {
  id: number;
  offeringId: number;
  trainingCourseTypeId: number;
  trainingCourseTypeName: string;
  organizationId: number;
  organizationName: string;
  firstSessionStart: string | null;
  lastSessionEnd: string | null;
  status: CourseRegistrationStatus;
  waitlistPosition: number | null;
  registeredAt: string;
  endedBy: CourseRegistrationEndedBy | null;
  endReason: string | null;
  workflowId: number | null;
  cancellationWorkflowId: number | null;
  canCancel: boolean;
  cancellationPending: boolean;
  canWithdraw: boolean;
}

export interface UserCourseRegistrationDto {
  id: number;
  offeringId: number;
  trainingCourseTypeId: number;
  trainingCourseTypeName: string;
  organizationId: number;
  organizationName: string;
  firstSessionStart: string | null;
  lastSessionEnd: string | null;
  status: CourseRegistrationStatus;
  endedBy: CourseRegistrationEndedBy | null;
}

export type UserCourseRegistrationPageDto = PageResponse<UserCourseRegistrationDto>;

/** A registration of one of the organization's offerings, for the organization-wide list. */
export interface OrganizationCourseRegistrationDto {
  id: number;
  offeringId: number;
  userId: number;
  userDisplayName: string;
  trainingCourseTypeName: string;
  firstSessionStart: string | null;
  lastSessionEnd: string | null;
  status: CourseRegistrationStatus;
}

export type OrganizationCourseRegistrationPageDto = PageResponse<OrganizationCourseRegistrationDto>;

export interface CourseRegistrationDto extends AppLinkFields {
  id: number;
  offeringId: number;
  userId: number;
  audienceId: number | null;
  userDisplayName: string;
  status: CourseRegistrationStatus;
  waitlistPosition: number | null;
  alreadyHeld: boolean;
  /** Registration request workflow; mirrored as AppLink when set. */
  workflowId: number | null;
  registeredAt: string;
  approvedAt: string | null;
  endedAt: string | null;
  endedBy: CourseRegistrationEndedBy | null;
  endReason: string | null;
}

export type CourseRegistrationPageDto = PageResponse<CourseRegistrationDto>;

export interface CourseRegistrationSummaryDto {
  maxParticipants: number;
  participants: number;
  waitlist: number;
  pending: number;
  freeSeats: number;
  quotas: {
    audienceId: number;
    kind: CourseAudienceKind;
    refId: number | null;
    quota: number;
    participants: number;
    waitlist: number;
    freeSeats: number;
  }[];
}

export interface CourseCompletionDto {
  offeringId: number;
  status: CourseOfferingStatus;
  completedAt: string | null;
  participants: {
    registrationId: number;
    userId: number;
    userDisplayName: string;
    finalPassed: boolean | null;
    decidedByUserId: number | null;
    decidedAt: string | null;
  }[];
}

export interface CoursePrerequisiteCheckDto {
  met: boolean;
  trainingCourseRequirementMode: QualificationRequirementMode;
  certRequirementMode: QualificationRequirementMode;
  drivingLicenseRequirementMode: QualificationRequirementMode;
  trainingCourseDetails: {
    configKey: string;
    abbreviation: string;
    name: string;
    met: boolean;
  }[];
  certDefinitionDetails: {
    configKey: string;
    abbreviation: string;
    name: string;
    met: boolean;
  }[];
  drivingLicenseDetails: {
    configKey: string;
    abbreviation: string;
    name: string;
    met: boolean;
  }[];
}

export interface CourseAudienceEntryRequest {
  kind: CourseAudienceKind;
  refId?: number | null;
  quota?: number | null;
}

export interface CourseAudienceQuotaRequest {
  audienceId: number;
  quota: number | null;
}

export interface UpdateCourseOfferingRequest {
  content?: string | null;
  notes?: string | null;
  templateName?: string | null;
  registrationStart?: string | null;
  registrationEnd?: string | null;
  maxParticipants: number;
  hasExam?: boolean;
  requiresHomeOrgApproval?: boolean;
  audienceQuotas?: CourseAudienceQuotaRequest[] | null;
}

export interface CreateCourseOfferingRequest extends UpdateCourseOfferingRequest {
  trainingCourseTypeId: number;
}

export interface CourseSessionRequest {
  type: CourseSessionType;
  start: string;
  end: string;
  location?: string | null;
  notes?: string | null;
}

export interface ReplaceCoursePrerequisitesRequest {
  trainingCourseRequirementMode: QualificationRequirementMode;
  certRequirementMode: QualificationRequirementMode;
  drivingLicenseRequirementMode: QualificationRequirementMode;
  entries: { kind: CoursePrerequisiteKind; configKey: string }[];
}
