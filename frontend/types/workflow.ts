/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { EntityAttachmentDto } from './attachment';

/** Mirrors backend {@code WorkflowValidationException} error codes. */
export const WORKFLOW_CLIENT_ERROR_CODES = {
  TYPE_ALREADY_OPEN: 'WORKFLOW_TYPE_ALREADY_OPEN',
  CERTIFICATION_DEFINITION_ALREADY_OPEN: 'CERTIFICATION_DEFINITION_ALREADY_OPEN',
  TRAINING_COURSE_TYPE_ALREADY_OPEN: 'TRAINING_COURSE_TYPE_ALREADY_OPEN',
  ORGANIZATION_JOIN_ALREADY_OPEN: 'ORGANIZATION_JOIN_ALREADY_OPEN',
  MAIN_ORGANIZATION_CHANGE_ALREADY_OPEN: 'MAIN_ORGANIZATION_CHANGE_ALREADY_OPEN',
  ORGANIZATION_LEAVE_ALREADY_OPEN: 'ORGANIZATION_LEAVE_ALREADY_OPEN',
  ORGANIZATION_GROUP_LEAVE_ALREADY_OPEN: 'ORGANIZATION_GROUP_LEAVE_ALREADY_OPEN',
  ORGANIZATION_GROUP_JOIN_ALREADY_OPEN: 'ORGANIZATION_GROUP_JOIN_ALREADY_OPEN',
  ONBOARDING_SUBMISSION_ALREADY_OPEN: 'ONBOARDING_SUBMISSION_ALREADY_OPEN',
  USER_ALREADY_DEACTIVATED: 'USER_ALREADY_DEACTIVATED',
  ORGANIZATION_MEMBERSHIP_END_ALREADY_OPEN: 'ORGANIZATION_MEMBERSHIP_END_ALREADY_OPEN',
  ORGANIZATION_MEMBERSHIP_EXIT_ALREADY_OPEN: 'ORGANIZATION_MEMBERSHIP_EXIT_ALREADY_OPEN',
  OPEN_EXCLUSIVITY_ALREADY_OPEN: 'OPEN_EXCLUSIVITY_ALREADY_OPEN',
  OPEN_CONCURRENCY_KEY_ALREADY_OPEN: 'OPEN_CONCURRENCY_KEY_ALREADY_OPEN',
  LEADERSHIP_MEMBERSHIP_BLOCKS_LEAVE: 'LEADERSHIP_MEMBERSHIP_BLOCKS_LEAVE',
  LEADERSHIP_GROUP_LEAVE_NOT_ALLOWED: 'LEADERSHIP_GROUP_LEAVE_NOT_ALLOWED',
  LEADERSHIP_GROUP_JOIN_NOT_ALLOWED: 'LEADERSHIP_GROUP_JOIN_NOT_ALLOWED',
  USER_ACCOUNT_NOT_SUSTAINABLE: 'USER_ACCOUNT_NOT_SUSTAINABLE',
} as const;

/** Mirrors backend {@link com.emberone.workflow.domain.ParticipantRole}. */
export type WorkflowParticipantRole = 'APPROVER' | 'VIEWER';

/** Mirrors backend {@link com.emberone.workflow.domain.PrincipalType}. */
export type WorkflowPrincipalType = 'USER' | 'ORGANIZATION_GROUP';

/** Mirrors backend {@link com.emberone.workflow.domain.ParticipantSlotStatus}. */
export type WorkflowParticipantSlotStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

/** Mirrors backend {@link com.emberone.workflow.domain.DecisionType}. */
export type WorkflowDecisionType = 'APPROVED' | 'REJECTED';

/** Mirrors backend {@link com.emberone.workflow.domain.WorkflowStatus}. */
export type WorkflowStatus =
  | 'OPEN'
  | 'AWAITING_INPUT_CHANGE'
  | 'APPROVED'
  | 'REJECTED'
  | 'CANCELLED';

/** Mirrors backend {@code com.emberone.workflow.domain.WorkflowType} ids. */
export const WORKFLOW_TYPE_IDS = {
  NAME_CHANGE: 1,
  ADDRESS_CHANGE: 2,
  CERTIFICATION_RECORD: 3,
  TRAINING_COURSE_RECORD: 4,
  DRIVING_LICENSE_RECORD: 5,
  ORGANIZATION_JOIN_REQUEST: 6,
  MAIN_ORGANIZATION_CHANGE: 7,
  ORGANIZATION_LEAVE_REQUEST: 8,
  ORGANIZATION_GROUP_LEAVE_REQUEST: 9,
  ORGANIZATION_GROUP_JOIN_REQUEST: 10,
  ONBOARDING_SUBMISSION: 11,
  ORGANIZATION_MEMBERSHIP_END_REQUEST: 13,
  ORGANIZATION_USER_MAIN_ORGANIZATION_CHANGE: 14,
  USER_DOCUMENT_SUBMISSION: 15,
  RANK_REQUEST: 16,
  COURSE_OFFERING_REGISTRATION: 17,
  COURSE_REGISTRATION_CANCELLATION: 18,
} as const;

export type WorkflowTypeId = (typeof WORKFLOW_TYPE_IDS)[keyof typeof WORKFLOW_TYPE_IDS];

export type WorkflowOpenScope = 'SELF' | 'ORGANIZATION';

export interface WorkflowTypeCatalogDto {
  workflowTypeId: number;
  workflowTypeKey: string;
  mode: string;
  allowsMultipleConcurrentOpen: boolean;
  openScope: WorkflowOpenScope;
  /** When false, start only from a dedicated UI - omitted from Anträge → Hinzufügen. */
  listedInBeginnableCatalog: boolean;
}

export interface WorkflowOpenStateDto {
  workflowTypeId: number;
  typeBlocked: boolean;
  openConcurrencyKeys: string[];
}

export interface WorkflowOpenConcurrencyKeysDto {
  workflowTypeId: number;
  keys: string[];
}

export interface TenantWorkflowDefinitionDto {
  id: number;
  workflowTypeId: number;
  staticApproversExclusive: boolean;
  staticApproverOrganizationIds: number[];
  customized: boolean;
}

export interface UpdateWorkflowDefinitionRequest {
  staticApproversExclusive: boolean;
  staticApproverOrganizationIds: number[];
}

export interface NameChangeWorkflowPayload {
  firstName: string;
  lastName: string;
  /** Set by the server when the workflow is opened. */
  previousFirstName?: string;
  previousLastName?: string;
  attachmentIds: number[];
}

export interface UserDocumentSubmissionWorkflowPayload {
  title: string;
  description?: string | null;
  tags?: string[];
  attachmentIds: number[];
}

export interface AddressChangeWorkflowPayload {
  street: string;
  city: string;
  postalCode: string;
  country: string;
  /** Set by the server when the workflow is opened. */
  previousStreet?: string;
  previousCity?: string;
  previousPostalCode?: string;
  previousCountry?: string;
  /** Optional proof documents. */
  attachmentIds?: number[];
}

export interface CertificationRecordWorkflowPayload {
  tenantCertDefinitionId: number;
  completedDate: string;
  notes?: string | null;
  attachmentIds?: number[];
}

export interface TrainingCourseRecordWorkflowPayload {
  tenantTrainingCourseTypeIds: number[];
  name?: string | null;
  startDate: string;
  endDate: string;
  location: string;
  hours?: number | null;
  notes?: string | null;
  attachmentIds?: number[];
}

export interface DrivingLicenseClassWorkflowPayload {
  tenantDrivingLicenseDefinitionId: number;
  grantDate?: string | null;
  expirationDate?: string | null;
}

export interface DrivingLicenseRecordWorkflowPayload {
  issueDate: string;
  documentExpirationDate?: string | null;
  licenseNumber: string;
  issuingAuthority: string;
  classes: DrivingLicenseClassWorkflowPayload[];
  notes?: string | null;
  attachmentIds?: number[];
}

export interface RankRequestWorkflowPayload {
  tenantRankId: number;
  attachmentIds?: number[];
}

export interface CourseOfferingRegistrationWorkflowPayload {
  offeringId: number;
  trainingCourseTypeId?: number;
  trainingCourseTypeName?: string;
  offeringOrganizationId?: number;
  offeringOrganizationName?: string;
  homeOrganizationId?: number | null;
  homeOrganizationName?: string | null;
  alreadyHeld?: boolean;
}

export interface CourseRegistrationCancellationWorkflowPayload {
  registrationId: number;
  offeringId?: number;
  trainingCourseTypeId?: number;
  trainingCourseTypeName?: string;
  firstSessionStart?: string;
  offeringOrganizationId?: number;
  offeringOrganizationName?: string;
  homeOrganizationId?: number;
  homeOrganizationName?: string;
}

export interface OrganizationJoinRequestWorkflowPayload {
  targetOrganizationId: number;
  /** Set by the server when the workflow is opened. */
  targetOrganizationName?: string;
  reason?: string | null;
  attachmentIds?: number[];
}

export interface OnboardingSubmissionWorkflowPayload {
  invitationId: number;
}

export interface MainOrganizationChangeWorkflowPayload {
  newMainOrganizationId: number;
  /** Set by the server when the workflow is opened. */
  newMainOrganizationName?: string;
  reason?: string | null;
  attachmentIds?: number[];
}

export interface OrganizationLeaveRequestWorkflowPayload {
  organizationId: number;
  /** Set by the server when the workflow is opened. */
  organizationName?: string;
  reason?: string | null;
  attachmentIds?: number[];
}

/** POST body when opening the org user-management main organization change workflow. */
export interface OpenOrganizationUserMainOrganizationChangeWorkflowRequest {
  newMainOrganizationId: number;
  reason?: string;
}

export interface OrganizationUserMainOrganizationChangeWorkflowPayload {
  organizationId: number;
  targetUserId: number;
  newMainOrganizationId: number;
  newMainOrganizationName?: string;
  organizationName?: string;
  targetUserDisplayName?: string;
  reason?: string | null;
}

export interface OrganizationMembershipEndRequestWorkflowPayload {
  organizationId: number;
  targetUserId: number;
  organizationName?: string;
  targetUserDisplayName?: string;
  reason?: string | null;
}

export interface OrganizationGroupLeaveRequestWorkflowPayload {
  organizationId: number;
  /** Set by the server when the workflow is opened. */
  organizationName?: string;
  organizationGroupId: number;
  /** Set by the server when the workflow is opened. */
  organizationGroupName?: string;
  reason?: string | null;
  attachmentIds?: number[];
}

export interface OrganizationGroupJoinRequestWorkflowPayload {
  organizationId: number;
  /** Set by the server when the workflow is opened. */
  organizationName?: string;
  organizationGroupId: number;
  /** Set by the server when the workflow is opened. */
  organizationGroupName?: string;
  reason?: string | null;
  attachmentIds?: number[];
}

export interface WorkflowParticipantDto {
  id: number;
  role: WorkflowParticipantRole;
  principalType: WorkflowPrincipalType;
  principalId: number | null;
  organizationId: number | null;
  organizationDisplayName?: string | null;
  slotStatus: WorkflowParticipantSlotStatus | null;
  claimedByUserId: number | null;
  claimedAt?: string | null;
  principalDisplayName?: string | null;
  claimedByDisplayName?: string | null;
}

export interface WorkflowDecisionDto {
  id: number;
  participantId: number;
  userId: number | null;
  userDisplayName?: string | null;
  decisionType: WorkflowDecisionType;
  comment?: string | null;
  decidedAt?: string | null;
}

export type WorkflowInputChangeStatus = 'OPEN' | 'FULFILLED' | 'CANCELLED';

/** Mirrors backend {@link com.emberone.workflow.dto.WorkflowInputChangeDto}. */
export interface WorkflowInputChangeDto {
  id: number;
  requestedAt?: string | null;
  message?: string | null;
  status: WorkflowInputChangeStatus | string;
  fulfilledAt?: string | null;
}

export interface WorkflowDto {
  id: number;
  workflowTypeId: number;
  status: WorkflowStatus | string;
  openedByUserId: number;
  /** Set on participation lists: opener's display name from the API. */
  openedByDisplayName?: string | null;
  /** Resolved from the current admin workflow definition (not stored on the instance). */
  selfApproveAllowed?: boolean;
  payload?: string | null;
  openConcurrencyKeys?: string[] | null;
  decidedAt?: string | null;
  /** Currently open input-change request, if any. */
  activeInputChange?: WorkflowInputChangeDto | null;
  /** Full input-change history (oldest first). */
  inputChanges?: WorkflowInputChangeDto[] | null;
  /** True when the current user may update the workflow payload. */
  canEditInput?: boolean;
  /** True when the current user may request an input change from the opener. */
  canRequestInputChange?: boolean;
  /** True when the current user may withdraw the open input-change request. */
  canWithdrawInputChange?: boolean;
  /**
   * True when reject becomes an input-change request for this type (comment required)
   * instead of terminating the workflow.
   */
  mapsRejectToInputChange?: boolean;
  /** Present on {@code GET /api/v1/auth/workflows/{id}}. */
  participants?: WorkflowParticipantDto[];
  decisions?: WorkflowDecisionDto[];
}

/** Mirrors backend {@link com.emberone.workflow.dto.WorkflowPageDto}. */
export interface WorkflowPageDto {
  content: WorkflowDto[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export type WorkflowMediaSource = 'WORKFLOW' | 'COMMENT';

export interface WorkflowDocumentPromotionItemDto {
  attachmentId: number;
  source: WorkflowMediaSource;
  commentId?: number | null;
  attachment: EntityAttachmentDto;
  selected: boolean;
  defaultTitle: string;
  defaultDescription?: string | null;
  defaultTags: string[];
}

export interface WorkflowDocumentPromotionPreviewDto {
  items: WorkflowDocumentPromotionItemDto[];
}

export interface WorkflowDocumentPromotionSelection {
  attachmentId: number;
  source: WorkflowMediaSource;
  title?: string | null;
  description?: string | null;
  tags?: string[] | null;
}

export interface WorkflowDecisionRequestBody {
  participantId: number;
  decision: WorkflowDecisionType;
  comment?: string | null;
}

/** Mirrors backend {@link com.emberone.workflow.domain.WorkflowCommentAuthorRole}. */
export type WorkflowCommentAuthorRole = 'OPENER' | 'APPROVER' | 'VIEWER';

export interface WorkflowCommentDto {
  id: number;
  userId: number;
  userFirstName?: string | null;
  userLastName?: string | null;
  userDisplayName?: string | null;
  authorRole: WorkflowCommentAuthorRole;
  body: string;
  attachment?: EntityAttachmentDto | null;
}

export interface CreateWorkflowCommentRequest {
  body: string;
  attachmentId?: number | null;
}
