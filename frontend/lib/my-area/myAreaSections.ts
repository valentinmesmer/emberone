/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/** URL segment for "Mein Bereich" (personal workspace). */
export const MY_AREA_BASE_PATH = '/my-area';

/** Unified workflow inbox (own requests + approver participations). */
export const MY_AREA_WORKFLOW_INBOX_PATH = `${MY_AREA_BASE_PATH}/workflows`;

export const MY_AREA_NOTIFICATIONS_PATH = `${MY_AREA_BASE_PATH}/notifications`;

export const MY_AREA_TODOS_PATH = `${MY_AREA_BASE_PATH}/todos`;

export const MY_AREA_ONBOARDING_PATH = `${MY_AREA_BASE_PATH}/onboarding`;

/** Open todos by default (matches {@link todoListDefinition} default filter). */
export function getMyAreaTodosPath(): string {
  return `${MY_AREA_TODOS_PATH}?status=OPEN`;
}

export function getMyAreaTodoDetailPath(todoId: number): string {
  return `${MY_AREA_TODOS_PATH}/${todoId}`;
}

export function getMyAreaOnboardingPath(): string {
  return MY_AREA_ONBOARDING_PATH;
}

export function getMyAreaOnboardingDetailPath(invitationId: number): string {
  return `${MY_AREA_ONBOARDING_PATH}/${invitationId}`;
}

export function getMyAreaNotificationsPath(): string {
  return MY_AREA_NOTIFICATIONS_PATH;
}

export function getMyAreaNotificationDetailPath(notificationId: number): string {
  return `${MY_AREA_NOTIFICATIONS_PATH}/${notificationId}`;
}

export function getMyAreaWorkflowInboxPath(): string {
  return MY_AREA_WORKFLOW_INBOX_PATH;
}

/** Canonical shareable workflow URL (no list context in the path). */
export function getMyAreaWorkflowDetailPath(workflowId: number): string {
  return `${MY_AREA_BASE_PATH}/workflows/${workflowId}`;
}

export function getMyAreaCertificationDetailPath(definitionId: number): string {
  return `${MY_AREA_BASE_PATH}/certifications/${definitionId}`;
}

export function getMyAreaCertificationRecordDetailPath(
  definitionId: number,
  recordId: number,
): string {
  return `${MY_AREA_BASE_PATH}/certifications/${definitionId}/records/${recordId}`;
}

export function getMyAreaQualificationsPath(): string {
  return `${MY_AREA_BASE_PATH}/qualifications`;
}

export function getMyAreaQualificationDetailPath(definitionId: number): string {
  return `${MY_AREA_BASE_PATH}/qualifications/${definitionId}`;
}

export function getMyAreaTrainingCourseRecordDetailPath(recordId: number): string {
  return `${MY_AREA_BASE_PATH}/training-courses/${recordId}`;
}

export function getMyAreaCourseRegistrationsPath(): string {
  return `${MY_AREA_BASE_PATH}/course-registrations`;
}

export function getMyAreaCourseOfferingDetailPath(offeringId: number): string {
  return `${MY_AREA_BASE_PATH}/course-registrations/offerings/${offeringId}`;
}

export function getMyAreaDrivingLicensesPath(): string {
  return `${MY_AREA_BASE_PATH}/driving-licenses`;
}

/** Hub subsection for scanning many my-area sections (see `common.hub.groups`). */
export type MyAreaHubGroupId = 'personal' | 'timeAttendance' | 'inbox' | 'account';

/** Same order as my-area hub tiles / {@link HubNavPanel} `groups` on `/my-area`. */
export const MY_AREA_HUB_GROUP_ORDER: readonly MyAreaHubGroupId[] = [
  'personal',
  'inbox',
  'timeAttendance',
  'account',
];

export function getMyAreaHubGroupTitle(
  groupId: MyAreaHubGroupId,
  t: (key: string) => string,
): string {
  switch (groupId) {
    case 'personal':
      return t('common.hub.groups.myAreaPersonal');
    case 'timeAttendance':
      return t('common.hub.groups.myAreaTimeAttendance');
    case 'inbox':
      return t('common.hub.groups.myAreaInbox');
    case 'account':
      return t('common.hub.groups.myAreaAccount');
  }
}

export const MY_AREA_SECTIONS = [
  {
    id: 'master-data',
    path: `${MY_AREA_BASE_PATH}/master-data`,
    labelKey: 'myArea.menu.masterData',
    hubGroupId: 'personal' satisfies MyAreaHubGroupId,
  },
  {
    id: 'emergency-contact',
    path: `${MY_AREA_BASE_PATH}/emergency-contact`,
    labelKey: 'myArea.menu.emergencyContact',
    hubGroupId: 'personal' satisfies MyAreaHubGroupId,
  },
  {
    id: 'documents',
    path: `${MY_AREA_BASE_PATH}/documents`,
    labelKey: 'myArea.menu.documents',
    hubGroupId: 'personal' satisfies MyAreaHubGroupId,
  },
  {
    id: 'training-courses',
    path: `${MY_AREA_BASE_PATH}/training-courses`,
    labelKey: 'myArea.menu.trainingCourses',
    hubGroupId: 'personal' satisfies MyAreaHubGroupId,
  },
  {
    id: 'course-registrations',
    path: `${MY_AREA_BASE_PATH}/course-registrations`,
    labelKey: 'myArea.menu.courseRegistrations',
    hubGroupId: 'personal' satisfies MyAreaHubGroupId,
    requiresOrganizationMembership: true,
  },
  {
    id: 'driving-licenses',
    path: `${MY_AREA_BASE_PATH}/driving-licenses`,
    labelKey: 'myArea.menu.drivingLicenses',
    hubGroupId: 'personal' satisfies MyAreaHubGroupId,
  },
  {
    id: 'certifications',
    path: `${MY_AREA_BASE_PATH}/certifications`,
    labelKey: 'myArea.menu.certifications',
    hubGroupId: 'personal' satisfies MyAreaHubGroupId,
  },
  {
    id: 'qualifications',
    path: `${MY_AREA_BASE_PATH}/qualifications`,
    labelKey: 'myArea.menu.qualifications',
    hubGroupId: 'personal' satisfies MyAreaHubGroupId,
  },
  {
    id: 'rank',
    path: `${MY_AREA_BASE_PATH}/rank`,
    labelKey: 'myArea.menu.rank',
    hubGroupId: 'personal' satisfies MyAreaHubGroupId,
  },
  {
    id: 'absence',
    path: `${MY_AREA_BASE_PATH}/absence`,
    labelKey: 'myArea.menu.absence',
    hubGroupId: 'timeAttendance' satisfies MyAreaHubGroupId,
    requiresOrganizationMembership: true,
  },
  {
    id: 'time-entries',
    path: `${MY_AREA_BASE_PATH}/time-entries`,
    labelKey: 'myArea.menu.timeEntries',
    hubGroupId: 'timeAttendance' satisfies MyAreaHubGroupId,
    requiresOrganizationMembership: true,
  },
  {
    id: 'todos',
    path: MY_AREA_TODOS_PATH,
    labelKey: 'myArea.menu.todos',
    hubGroupId: 'inbox' satisfies MyAreaHubGroupId,
  },
  {
    id: 'onboarding',
    path: MY_AREA_ONBOARDING_PATH,
    labelKey: 'myArea.menu.onboarding',
    hubGroupId: 'inbox' satisfies MyAreaHubGroupId,
  },
  {
    id: 'notifications',
    path: MY_AREA_NOTIFICATIONS_PATH,
    labelKey: 'myArea.menu.notifications',
    hubGroupId: 'inbox' satisfies MyAreaHubGroupId,
  },
  {
    id: 'workflows',
    path: MY_AREA_WORKFLOW_INBOX_PATH,
    labelKey: 'myArea.menu.workflowsInbox',
    hubGroupId: 'inbox' satisfies MyAreaHubGroupId,
  },
  {
    id: 'settings',
    path: `${MY_AREA_BASE_PATH}/settings`,
    labelKey: 'myArea.menu.settings',
    hubGroupId: 'account' satisfies MyAreaHubGroupId,
  },
] as const;

export type MyAreaSectionId = (typeof MY_AREA_SECTIONS)[number]['id'];

export function isMyAreaSectionAccessible(
  section: (typeof MY_AREA_SECTIONS)[number],
  hasOrganizationMembership: boolean,
): boolean {
  if (
    'requiresOrganizationMembership' in section &&
    section.requiresOrganizationMembership &&
    !hasOrganizationMembership
  ) {
    return false;
  }
  return true;
}

export function getAccessibleMyAreaSections(hasOrganizationMembership: boolean) {
  return MY_AREA_SECTIONS.filter((section) =>
    isMyAreaSectionAccessible(section, hasOrganizationMembership),
  );
}
