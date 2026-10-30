/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { useQueries } from '@tanstack/react-query';
import { certificationsApi, type UserCertificationRecordDto } from '@/lib/api/certifications';
import { type DrivingLicenseDto, drivingLicensesApi } from '@/lib/api/drivingLicenses';
import { type TrainingCourseUserRecordDto, trainingCoursesApi } from '@/lib/api/trainingCourses';
import type {
  OnboardingCertificationDraft,
  OnboardingDrivingLicenseDraft,
  OnboardingInvitationAccess,
  OnboardingTrainingCourseDraft,
} from '@/types/onboardingInvitation';

export interface OnboardingApprovedEvidence {
  trainingRecords: TrainingCourseUserRecordDto[];
  certificationRecords: UserCertificationRecordDto[];
  drivingLicense: DrivingLicenseDto | null;
  isLoading: boolean;
}

function isOnboardingPromotedRecord(
  record: { appLinkType: string | null; appLinkId: number | null },
  invitationId: number,
): boolean {
  return record.appLinkType === 'ONBOARDING_INVITATION' && record.appLinkId === invitationId;
}

export function matchPromotedTrainingCourseRecord(
  draft: OnboardingTrainingCourseDraft,
  invitationId: number,
  records: TrainingCourseUserRecordDto[],
): TrainingCourseUserRecordDto | undefined {
  const draftTypeKey = [...draft.tenantTrainingCourseTypeIds].sort((a, b) => a - b).join(',');
  return records
    .filter((record) => isOnboardingPromotedRecord(record, invitationId))
    .find((record) => {
      const recordTypeKey = record.grantedTypes
        .map((entry) => entry.tenantTrainingCourseTypeId)
        .sort((a, b) => a - b)
        .join(',');
      return (
        recordTypeKey === draftTypeKey &&
        record.startDate === draft.startDate &&
        record.endDate === draft.endDate
      );
    });
}

export function matchPromotedCertificationRecord(
  draft: OnboardingCertificationDraft,
  invitationId: number,
  records: UserCertificationRecordDto[],
): UserCertificationRecordDto | undefined {
  return records
    .filter((record) => isOnboardingPromotedRecord(record, invitationId))
    .find(
      (record) =>
        record.tenantCertDefinitionId === draft.tenantCertDefinitionId &&
        record.completedDate === draft.completedDate,
    );
}

export function matchPromotedDrivingLicense(
  draft: OnboardingDrivingLicenseDraft,
  invitationId: number,
  license: DrivingLicenseDto | null,
): DrivingLicenseDto | null {
  if (license == null || !isOnboardingPromotedRecord(license, invitationId)) {
    return null;
  }
  if (license.issueDate !== draft.issueDate) {
    return null;
  }
  return license;
}

export function promotedUserDocumentIdForFileIndex(
  userDocumentIds: number[] | undefined,
  fileIndex: number,
): number | undefined {
  const id = userDocumentIds?.[fileIndex];
  return id != null && id > 0 ? id : undefined;
}

export function useOnboardingApprovedEvidence(params: {
  enabled: boolean;
  invitationId: number;
  organizationId: number;
  subjectUserId: number;
  scopeKind: OnboardingInvitationAccess['kind'];
}): OnboardingApprovedEvidence {
  const orgScoped = params.scopeKind !== 'self';
  const organizationId = params.organizationId;
  const subjectUserId = params.subjectUserId;
  const baseEnabled =
    params.enabled &&
    params.invitationId > 0 &&
    params.subjectUserId > 0 &&
    params.organizationId > 0;

  const [trainingQuery, certificationQuery, drivingLicenseQuery] = useQueries({
    queries: [
      {
        queryKey: [
          'onboarding-approved-evidence',
          params.invitationId,
          'training',
          orgScoped ? organizationId : 'self',
          subjectUserId,
        ],
        queryFn: async () => {
          const { fetchAllPageContent } = await import('@/lib/api/paged');
          return fetchAllPageContent((page) =>
            orgScoped
              ? trainingCoursesApi.getUserRecords(organizationId, subjectUserId, { page })
              : trainingCoursesApi.getMyRecords({ page }),
          );
        },
        enabled: baseEnabled,
        staleTime: 60 * 1000,
      },
      {
        queryKey: [
          'onboarding-approved-evidence',
          params.invitationId,
          'certifications',
          orgScoped ? organizationId : 'self',
          subjectUserId,
        ],
        queryFn: () =>
          orgScoped
            ? certificationsApi.getUserRecords(organizationId, subjectUserId)
            : certificationsApi.getMyRecords(),
        enabled: baseEnabled,
        staleTime: 60 * 1000,
      },
      {
        queryKey: [
          'onboarding-approved-evidence',
          params.invitationId,
          'driving-license',
          orgScoped ? organizationId : 'self',
          subjectUserId,
        ],
        queryFn: () =>
          orgScoped
            ? drivingLicensesApi.getUserLicense(organizationId, subjectUserId)
            : drivingLicensesApi.getMyLicense(),
        enabled: baseEnabled,
        staleTime: 60 * 1000,
      },
    ],
  });

  return {
    trainingRecords: trainingQuery.data ?? [],
    certificationRecords: certificationQuery.data ?? [],
    drivingLicense: drivingLicenseQuery.data ?? null,
    isLoading:
      trainingQuery.isLoading || certificationQuery.isLoading || drivingLicenseQuery.isLoading,
  };
}
