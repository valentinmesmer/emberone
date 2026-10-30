/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type {
  AddressChangeWorkflowPayload,
  CertificationRecordWorkflowPayload,
  CourseOfferingRegistrationWorkflowPayload,
  CourseRegistrationCancellationWorkflowPayload,
  DrivingLicenseRecordWorkflowPayload,
  MainOrganizationChangeWorkflowPayload,
  NameChangeWorkflowPayload,
  OnboardingSubmissionWorkflowPayload,
  OrganizationGroupJoinRequestWorkflowPayload,
  OrganizationGroupLeaveRequestWorkflowPayload,
  OrganizationJoinRequestWorkflowPayload,
  OrganizationLeaveRequestWorkflowPayload,
  OrganizationMembershipEndRequestWorkflowPayload,
  RankRequestWorkflowPayload,
  TrainingCourseRecordWorkflowPayload,
  UserDocumentSubmissionWorkflowPayload,
} from '@/types/workflow';

export function parseUserDocumentSubmissionPayload(
  payload: string | null | undefined,
): UserDocumentSubmissionWorkflowPayload | null {
  if (!payload?.trim()) return null;
  try {
    const o = JSON.parse(payload) as UserDocumentSubmissionWorkflowPayload;
    if (typeof o.title !== 'string') {
      return null;
    }
    if (!Array.isArray(o.attachmentIds) || !o.attachmentIds.every((id) => typeof id === 'number')) {
      return null;
    }
    return o;
  } catch {
    return null;
  }
}

export function parseNameChangePayload(
  payload: string | null | undefined,
): NameChangeWorkflowPayload | null {
  if (!payload?.trim()) return null;
  try {
    const o = JSON.parse(payload) as NameChangeWorkflowPayload & { proofAttachmentId?: number };
    if (typeof o.firstName !== 'string' || typeof o.lastName !== 'string') {
      return null;
    }
    const previous =
      typeof o.previousFirstName === 'string' && typeof o.previousLastName === 'string'
        ? { previousFirstName: o.previousFirstName, previousLastName: o.previousLastName }
        : {};
    if (Array.isArray(o.attachmentIds) && o.attachmentIds.every((id) => typeof id === 'number')) {
      return {
        firstName: o.firstName,
        lastName: o.lastName,
        attachmentIds: o.attachmentIds,
        ...previous,
      };
    }
    if (typeof o.proofAttachmentId === 'number') {
      return {
        firstName: o.firstName,
        lastName: o.lastName,
        attachmentIds: [o.proofAttachmentId],
        ...previous,
      };
    }
    return null;
  } catch {
    return null;
  }
}

export function parseAddressChangePayload(
  payload: string | null | undefined,
): AddressChangeWorkflowPayload | null {
  if (!payload?.trim()) return null;
  try {
    const o = JSON.parse(payload) as AddressChangeWorkflowPayload;
    if (
      typeof o.street !== 'string' ||
      typeof o.city !== 'string' ||
      typeof o.postalCode !== 'string' ||
      typeof o.country !== 'string'
    ) {
      return null;
    }
    return o;
  } catch {
    return null;
  }
}

export function parseCertificationRecordPayload(
  payload: string | null | undefined,
): CertificationRecordWorkflowPayload | null {
  if (!payload?.trim()) return null;
  try {
    const o = JSON.parse(payload) as CertificationRecordWorkflowPayload;
    if (typeof o.tenantCertDefinitionId !== 'number' || typeof o.completedDate !== 'string') {
      return null;
    }
    return o;
  } catch {
    return null;
  }
}

export function parseTrainingCourseRecordPayload(
  payload: string | null | undefined,
): TrainingCourseRecordWorkflowPayload | null {
  if (!payload?.trim()) return null;
  try {
    const o = JSON.parse(payload) as TrainingCourseRecordWorkflowPayload;
    if (
      !Array.isArray(o.tenantTrainingCourseTypeIds) ||
      typeof o.startDate !== 'string' ||
      typeof o.endDate !== 'string' ||
      typeof o.location !== 'string'
    ) {
      return null;
    }
    return o;
  } catch {
    return null;
  }
}

export function parseDrivingLicenseRecordPayload(
  payload: string | null | undefined,
): DrivingLicenseRecordWorkflowPayload | null {
  if (!payload?.trim()) return null;
  try {
    const o = JSON.parse(payload) as DrivingLicenseRecordWorkflowPayload;
    if (typeof o.issueDate !== 'string' || !Array.isArray(o.classes) || o.classes.length === 0) {
      return null;
    }
    if (
      !o.classes.every(
        (licenseClass) => typeof licenseClass.tenantDrivingLicenseDefinitionId === 'number',
      )
    ) {
      return null;
    }
    return o;
  } catch {
    return null;
  }
}

export function parseRankRequestPayload(
  payload: string | null | undefined,
): RankRequestWorkflowPayload | null {
  if (!payload?.trim()) return null;
  try {
    const o = JSON.parse(payload) as RankRequestWorkflowPayload;
    if (typeof o.tenantRankId !== 'number') return null;
    return o;
  } catch {
    return null;
  }
}

export function parseOrganizationJoinRequestPayload(
  payload: string | null | undefined,
): OrganizationJoinRequestWorkflowPayload | null {
  if (!payload?.trim()) return null;
  try {
    const o = JSON.parse(payload) as OrganizationJoinRequestWorkflowPayload;
    if (typeof o.targetOrganizationId !== 'number') {
      return null;
    }
    return o;
  } catch {
    return null;
  }
}

export function parseMainOrganizationChangePayload(
  payload: string | null | undefined,
): MainOrganizationChangeWorkflowPayload | null {
  if (!payload?.trim()) return null;
  try {
    const o = JSON.parse(payload) as MainOrganizationChangeWorkflowPayload;
    if (typeof o.newMainOrganizationId !== 'number') {
      return null;
    }
    return o;
  } catch {
    return null;
  }
}

export function parseOrganizationLeaveRequestPayload(
  payload: string | null | undefined,
): OrganizationLeaveRequestWorkflowPayload | null {
  if (!payload?.trim()) return null;
  try {
    const o = JSON.parse(payload) as OrganizationLeaveRequestWorkflowPayload;
    if (typeof o.organizationId !== 'number') {
      return null;
    }
    return o;
  } catch {
    return null;
  }
}

export function parseOrganizationGroupLeaveRequestPayload(
  payload: string | null | undefined,
): OrganizationGroupLeaveRequestWorkflowPayload | null {
  if (!payload?.trim()) return null;
  try {
    const o = JSON.parse(payload) as OrganizationGroupLeaveRequestWorkflowPayload;
    if (typeof o.organizationId !== 'number' || typeof o.organizationGroupId !== 'number') {
      return null;
    }
    return o;
  } catch {
    return null;
  }
}

export function parseOrganizationGroupJoinRequestPayload(
  payload: string | null | undefined,
): OrganizationGroupJoinRequestWorkflowPayload | null {
  if (!payload?.trim()) return null;
  try {
    const o = JSON.parse(payload) as OrganizationGroupJoinRequestWorkflowPayload;
    if (typeof o.organizationId !== 'number' || typeof o.organizationGroupId !== 'number') {
      return null;
    }
    return o;
  } catch {
    return null;
  }
}

export function parseOnboardingSubmissionPayload(
  payload: string | null | undefined,
): OnboardingSubmissionWorkflowPayload | null {
  if (!payload?.trim()) return null;
  try {
    const o = JSON.parse(payload) as OnboardingSubmissionWorkflowPayload;
    if (typeof o.invitationId !== 'number') {
      return null;
    }
    return o;
  } catch {
    return null;
  }
}

export function parseOrganizationUserMainOrganizationChangePayload(
  payload: string | null | undefined,
): import('@/types/workflow').OrganizationUserMainOrganizationChangeWorkflowPayload | null {
  if (!payload?.trim()) return null;
  try {
    const o = JSON.parse(
      payload,
    ) as import('@/types/workflow').OrganizationUserMainOrganizationChangeWorkflowPayload;
    if (typeof o.organizationId !== 'number' || typeof o.targetUserId !== 'number') {
      return null;
    }
    return o;
  } catch {
    return null;
  }
}

export function parseOrganizationMembershipEndRequestPayload(
  payload: string | null | undefined,
): OrganizationMembershipEndRequestWorkflowPayload | null {
  if (!payload?.trim()) return null;
  try {
    const o = JSON.parse(payload) as OrganizationMembershipEndRequestWorkflowPayload;
    if (typeof o.organizationId !== 'number' || typeof o.targetUserId !== 'number') {
      return null;
    }
    return o;
  } catch {
    return null;
  }
}

export function parseCourseOfferingRegistrationPayload(
  payload: string | null | undefined,
): CourseOfferingRegistrationWorkflowPayload | null {
  if (!payload?.trim()) return null;
  try {
    const o = JSON.parse(payload) as CourseOfferingRegistrationWorkflowPayload;
    if (typeof o.offeringId !== 'number') {
      return null;
    }
    return o;
  } catch {
    return null;
  }
}

export function parseCourseRegistrationCancellationPayload(
  payload: string | null | undefined,
): CourseRegistrationCancellationWorkflowPayload | null {
  if (!payload?.trim()) return null;
  try {
    const o = JSON.parse(payload) as CourseRegistrationCancellationWorkflowPayload;
    if (typeof o.registrationId !== 'number') {
      return null;
    }
    return o;
  } catch {
    return null;
  }
}
