/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { PermissionTypeEnum } from '@/types/permission';

/**
 * Hub grouping for a member's page (see `common.hub.groups`). The person's record comes first
 * (the manager looks someone up); requests and actions close the page.
 */
export type UserManagementHubGroupId = 'profile' | 'training' | 'timeAttendance' | 'requests';

export const USER_MANAGEMENT_HUB_GROUP_ORDER: readonly UserManagementHubGroupId[] = [
  'profile',
  'training',
  'timeAttendance',
  'requests',
] as const;

export function getUserManagementHubGroupTitle(
  groupId: UserManagementHubGroupId,
  t: (key: string) => string,
): string {
  switch (groupId) {
    case 'profile':
      return t('common.hub.groups.myAreaProfile');
    case 'training':
      return t('common.hub.groups.myAreaTraining');
    case 'timeAttendance':
      return t('common.hub.groups.myAreaTimeAttendance');
    case 'requests':
      return t('common.hub.groups.userRequestsAndActions');
  }
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
    hubGroupId: 'profile',
    readPermission: PermissionTypeEnum.USER_READ,
    writePermission: PermissionTypeEnum.USER_WRITE,
  },
  {
    id: 'memberships',
    labelKey: 'userManagement.sections.memberships',
    hubGroupId: 'profile',
    readPermission: PermissionTypeEnum.USER_READ,
    writePermission: PermissionTypeEnum.USER_WRITE,
  },
  {
    id: 'rank',
    labelKey: 'userManagement.sections.rank',
    hubGroupId: 'profile',
    readPermission: PermissionTypeEnum.RANK_READ,
    writePermission: PermissionTypeEnum.RANK_WRITE,
  },
  {
    id: 'emergency-contacts',
    labelKey: 'userManagement.sections.emergencyContacts',
    hubGroupId: 'profile',
    readPermission: PermissionTypeEnum.EMERGENCY_CONTACT_READ,
    writePermission: PermissionTypeEnum.EMERGENCY_CONTACT_WRITE,
  },
  {
    id: 'documents',
    labelKey: 'userManagement.sections.documents',
    hubGroupId: 'profile',
    readPermission: PermissionTypeEnum.DOCUMENT_READ,
    writePermission: PermissionTypeEnum.DOCUMENT_WRITE,
  },
  {
    id: 'onboarding',
    labelKey: 'userManagement.sections.onboarding',
    hubGroupId: 'profile',
    readPermission: PermissionTypeEnum.USER_READ,
    writePermission: PermissionTypeEnum.USER_WRITE,
  },
  {
    id: 'qualifications',
    labelKey: 'userManagement.sections.qualifications',
    hubGroupId: 'training',
    readPermission: PermissionTypeEnum.QUALIFICATION_READ,
    writePermission: PermissionTypeEnum.QUALIFICATION_WRITE,
  },
  {
    id: 'certifications',
    labelKey: 'userManagement.sections.certifications',
    hubGroupId: 'training',
    readPermission: PermissionTypeEnum.CERTIFICATION_READ,
    writePermission: PermissionTypeEnum.CERTIFICATION_WRITE,
  },
  {
    id: 'training-courses',
    labelKey: 'userManagement.sections.trainingCourses',
    hubGroupId: 'training',
    readPermission: PermissionTypeEnum.TRAINING_COURSE_READ,
    writePermission: PermissionTypeEnum.TRAINING_COURSE_WRITE,
  },
  {
    id: 'driving-licenses',
    labelKey: 'userManagement.sections.drivingLicenses',
    hubGroupId: 'training',
    readPermission: PermissionTypeEnum.DRIVING_LICENSE_READ,
    writePermission: PermissionTypeEnum.DRIVING_LICENSE_WRITE,
  },
  {
    id: 'course-registrations',
    labelKey: 'userManagement.sections.courseRegistrations',
    hubGroupId: 'training',
    readPermission: PermissionTypeEnum.TRAINING_COURSE_READ,
    writePermission: PermissionTypeEnum.TRAINING_COURSE_READ,
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
  {
    id: 'workflows',
    labelKey: 'userManagement.sections.workflows',
    hubGroupId: 'requests',
    readPermission: PermissionTypeEnum.WORKFLOW_READ,
    writePermission: PermissionTypeEnum.WORKFLOW_READ,
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
