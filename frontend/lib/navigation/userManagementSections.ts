/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { MyAreaHubGroupId } from '@/lib/my-area/myAreaSections';
import { getMyAreaHubGroupTitle } from '@/lib/my-area/myAreaSections';
import { PermissionTypeEnum } from '@/types/permission';

/** Hub grouping aligned with my-area (see `common.hub.groups`). */
export type UserManagementHubGroupId = Extract<
  MyAreaHubGroupId,
  'personal' | 'inbox' | 'timeAttendance'
>;

export const USER_MANAGEMENT_HUB_GROUP_ORDER: readonly UserManagementHubGroupId[] = [
  'personal',
  'inbox',
  'timeAttendance',
] as const;

export function getUserManagementHubGroupTitle(
  groupId: UserManagementHubGroupId,
  t: (key: string) => string,
): string {
  return getMyAreaHubGroupTitle(groupId, t);
}

export type UserManagementSectionId =
  | 'master-data'
  | 'memberships'
  | 'emergency-contacts'
  | 'documents'
  | 'certifications'
  | 'qualifications'
  | 'training-courses'
  | 'course-registrations'
  | 'driving-licenses'
  | 'rank'
  | 'onboarding'
  | 'workflows'
  | 'absences'
  | 'time-entries';

export type UserManagementSectionConfig = {
  id: UserManagementSectionId;
  labelKey: string;
  hubGroupId: UserManagementHubGroupId;
  readPermission: PermissionTypeEnum;
  writePermission: PermissionTypeEnum;
};

export const USER_MANAGEMENT_SECTIONS: readonly UserManagementSectionConfig[] = [
  {
    id: 'master-data',
    labelKey: 'userManagement.sections.masterData',
    hubGroupId: 'personal',
    readPermission: PermissionTypeEnum.USER_READ,
    writePermission: PermissionTypeEnum.USER_WRITE,
  },
  {
    id: 'memberships',
    labelKey: 'userManagement.sections.memberships',
    hubGroupId: 'personal',
    readPermission: PermissionTypeEnum.USER_READ,
    writePermission: PermissionTypeEnum.USER_WRITE,
  },
  {
    id: 'emergency-contacts',
    labelKey: 'userManagement.sections.emergencyContacts',
    hubGroupId: 'personal',
    readPermission: PermissionTypeEnum.EMERGENCY_CONTACT_READ,
    writePermission: PermissionTypeEnum.EMERGENCY_CONTACT_WRITE,
  },
  {
    id: 'documents',
    labelKey: 'userManagement.sections.documents',
    hubGroupId: 'personal',
    readPermission: PermissionTypeEnum.DOCUMENT_READ,
    writePermission: PermissionTypeEnum.DOCUMENT_WRITE,
  },
  {
    id: 'training-courses',
    labelKey: 'userManagement.sections.trainingCourses',
    hubGroupId: 'personal',
    readPermission: PermissionTypeEnum.TRAINING_COURSE_READ,
    writePermission: PermissionTypeEnum.TRAINING_COURSE_WRITE,
  },
  {
    id: 'course-registrations',
    labelKey: 'userManagement.sections.courseRegistrations',
    hubGroupId: 'personal',
    readPermission: PermissionTypeEnum.TRAINING_COURSE_READ,
    writePermission: PermissionTypeEnum.TRAINING_COURSE_READ,
  },
  {
    id: 'driving-licenses',
    labelKey: 'userManagement.sections.drivingLicenses',
    hubGroupId: 'personal',
    readPermission: PermissionTypeEnum.DRIVING_LICENSE_READ,
    writePermission: PermissionTypeEnum.DRIVING_LICENSE_WRITE,
  },
  {
    id: 'certifications',
    labelKey: 'userManagement.sections.certifications',
    hubGroupId: 'personal',
    readPermission: PermissionTypeEnum.CERTIFICATION_READ,
    writePermission: PermissionTypeEnum.CERTIFICATION_WRITE,
  },
  {
    id: 'qualifications',
    labelKey: 'userManagement.sections.qualifications',
    hubGroupId: 'personal',
    readPermission: PermissionTypeEnum.QUALIFICATION_READ,
    writePermission: PermissionTypeEnum.QUALIFICATION_WRITE,
  },
  {
    id: 'rank',
    labelKey: 'userManagement.sections.rank',
    hubGroupId: 'personal',
    readPermission: PermissionTypeEnum.RANK_READ,
    writePermission: PermissionTypeEnum.RANK_WRITE,
  },
  {
    id: 'workflows',
    labelKey: 'userManagement.sections.workflows',
    hubGroupId: 'inbox',
    readPermission: PermissionTypeEnum.WORKFLOW_READ,
    writePermission: PermissionTypeEnum.WORKFLOW_READ,
  },
  {
    id: 'onboarding',
    labelKey: 'userManagement.sections.onboarding',
    hubGroupId: 'inbox',
    readPermission: PermissionTypeEnum.USER_READ,
    writePermission: PermissionTypeEnum.USER_WRITE,
  },
  {
    id: 'absences',
    labelKey: 'userManagement.sections.absences',
    hubGroupId: 'timeAttendance',
    readPermission: PermissionTypeEnum.ABSENCE_READ,
    writePermission: PermissionTypeEnum.ABSENCE_WRITE,
  },
  {
    id: 'time-entries',
    labelKey: 'userManagement.sections.timeEntries',
    hubGroupId: 'timeAttendance',
    readPermission: PermissionTypeEnum.TIME_ENTRY_READ,
    writePermission: PermissionTypeEnum.TIME_ENTRY_WRITE,
  },
] as const;

export type OrganizationPermissionChecker = (
  organizationId: number,
  permission: PermissionTypeEnum,
) => boolean;

export function getOrganizationUsersPath(orgId: number): string {
  return `/organizations/${orgId}/users`;
}

export function getOrganizationUserPath(orgId: number, userId: number): string {
  return `${getOrganizationUsersPath(orgId)}/${userId}`;
}

export function getOrganizationUserSectionPath(
  orgId: number,
  userId: number,
  sectionId: UserManagementSectionId,
): string {
  return `${getOrganizationUserPath(orgId, userId)}/${sectionId}`;
}

export function getOrganizationUserEmergencyContactDetailPath(
  orgId: number,
  userId: number,
  contactId: number,
): string {
  return `${getOrganizationUserSectionPath(orgId, userId, 'emergency-contacts')}/${contactId}`;
}

export function getOrganizationUserTimeEntryDetailPath(
  orgId: number,
  userId: number,
  entryId: number,
): string {
  return `${getOrganizationUserSectionPath(orgId, userId, 'time-entries')}/${entryId}`;
}

export function getOrganizationUserAbsenceDetailPath(
  orgId: number,
  userId: number,
  absenceId: number,
): string {
  return `${getOrganizationUserSectionPath(orgId, userId, 'absences')}/${absenceId}`;
}

export function getOrganizationUserCourseRegistrationDetailPath(
  orgId: number,
  userId: number,
  registrationId: number,
): string {
  return `${getOrganizationUserSectionPath(orgId, userId, 'course-registrations')}/${registrationId}`;
}

export function getOrganizationUserDocumentDetailPath(
  orgId: number,
  userId: number,
  documentId: number,
): string {
  return `${getOrganizationUserSectionPath(orgId, userId, 'documents')}/${documentId}`;
}

export function getOrganizationUserWorkflowDetailPath(
  orgId: number,
  userId: number,
  workflowId: number,
): string {
  return `${getOrganizationUserSectionPath(orgId, userId, 'workflows')}/${workflowId}`;
}

export function getOrganizationUserCertificationDetailPath(
  orgId: number,
  userId: number,
  definitionId: number,
): string {
  return `${getOrganizationUserSectionPath(orgId, userId, 'certifications')}/${definitionId}`;
}

export function getOrganizationUserCertificationRecordDetailPath(
  orgId: number,
  userId: number,
  definitionId: number,
  recordId: number,
): string {
  return `${getOrganizationUserCertificationDetailPath(orgId, userId, definitionId)}/records/${recordId}`;
}

export function getOrganizationUserCertificationAssignmentDetailPath(
  orgId: number,
  userId: number,
  assignmentId: number,
): string {
  return `${getOrganizationUserSectionPath(orgId, userId, 'certifications')}/assignments/${assignmentId}`;
}

export function getOrganizationUserTrainingCourseRecordDetailPath(
  orgId: number,
  userId: number,
  recordId: number,
): string {
  return `${getOrganizationUserSectionPath(orgId, userId, 'training-courses')}/${recordId}`;
}

export function getOrganizationUserQualificationDetailPath(
  orgId: number,
  userId: number,
  definitionId: number,
): string {
  return `${getOrganizationUserSectionPath(orgId, userId, 'qualifications')}/${definitionId}`;
}

export function canAccessUserManagement(
  organizationId: number,
  hasPermission: OrganizationPermissionChecker,
): boolean {
  return USER_MANAGEMENT_SECTIONS.some((section) =>
    hasPermission(organizationId, section.readPermission),
  );
}

export function getVisibleUserManagementSections(
  organizationId: number,
  hasPermission: OrganizationPermissionChecker,
): UserManagementSectionConfig[] {
  return USER_MANAGEMENT_SECTIONS.filter((section) =>
    hasPermission(organizationId, section.readPermission),
  );
}
