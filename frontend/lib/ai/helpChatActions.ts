/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { DEPARTMENT_SECTION, getDepartmentSectionPath } from '@/lib/department/sections';
import {
  getMyAreaCourseRegistrationsPath,
  MY_AREA_BASE_PATH,
  MY_AREA_NOTIFICATIONS_PATH,
} from '@/lib/my-area/myAreaSections';
import {
  getAdminStandardsPath,
  getAdminTenantDetailPath,
  getAdminTenantOrganizationsPath,
  getAdminTenantRetentionPath,
  getAdminTenantStandardsPath,
} from '@/lib/navigation/adminWorkspace';
import { withCreateIntent } from '@/lib/navigation/createIntent';
import { getOrganizationManagementPath } from '@/lib/navigation/organizationNavItems';
import { getOrganizationUsersPath } from '@/lib/navigation/userManagementSections';
import { getAdminUsersPath } from '@/lib/utils/adminPaths';
import {
  getOrganizationCourseOfferingsPath,
  getOrganizationCourseRegistrationListPath,
} from '@/lib/utils/courseOffering';
import {
  getOrganizationAbsencesPath,
  getOrganizationCertificationsPath,
  getOrganizationEmergencyContactsPath,
  getOrganizationGroupsPath,
  getOrganizationQualificationsPath,
  getOrganizationRanksPath,
  getOrganizationTimeEntriesPath,
  getOrganizationWorkflowsPath,
} from '@/lib/utils/organization';
import type { HelpChatActionDto } from '@/types/ai';
import { WORKFLOW_TYPE_IDS } from '@/types/workflow';

export type HelpChatActionTarget =
  | { kind: 'modal'; workflowTypeId: number }
  | { kind: 'route'; href: string }
  | { kind: 'chooseOrganization'; options: { id: number; name: string; href: string }[] };

type StartableTypeKey = keyof typeof WORKFLOW_TYPE_IDS;

const MODAL_TYPES: readonly StartableTypeKey[] = [
  'NAME_CHANGE',
  'ADDRESS_CHANGE',
  'CERTIFICATION_RECORD',
  'TRAINING_COURSE_RECORD',
  'DRIVING_LICENSE_RECORD',
  'ORGANIZATION_JOIN_REQUEST',
  'MAIN_ORGANIZATION_CHANGE',
  'ORGANIZATION_LEAVE_REQUEST',
  'ORGANIZATION_GROUP_LEAVE_REQUEST',
  'ORGANIZATION_GROUP_JOIN_REQUEST',
  'USER_DOCUMENT_SUBMISSION',
  'RANK_REQUEST',
];

const ROUTE_TYPES: Partial<Record<StartableTypeKey, () => string>> = {
  COURSE_OFFERING_REGISTRATION: getMyAreaCourseRegistrationsPath,
  COURSE_REGISTRATION_CANCELLATION: getMyAreaCourseRegistrationsPath,
};

/** Fixed routes per help topic (must match `HelpTopic` in the backend). */
const SELF_TOPICS: Record<string, string> = {
  MY_MASTER_DATA: `${MY_AREA_BASE_PATH}/master-data`,
  MY_EMERGENCY_CONTACTS: `${MY_AREA_BASE_PATH}/emergency-contact`,
  MY_ABSENCES: `${MY_AREA_BASE_PATH}/absence`,
  MY_TIME_ENTRIES: `${MY_AREA_BASE_PATH}/time-entries`,
  MY_DOCUMENTS: `${MY_AREA_BASE_PATH}/documents`,
  MY_QUALIFICATIONS: `${MY_AREA_BASE_PATH}/qualifications`,
  MY_CERTIFICATIONS: `${MY_AREA_BASE_PATH}/certifications`,
  MY_TRAINING_COURSES: `${MY_AREA_BASE_PATH}/training-courses`,
  MY_DRIVING_LICENSES: `${MY_AREA_BASE_PATH}/driving-licenses`,
  MY_RANK: `${MY_AREA_BASE_PATH}/rank`,
  MY_COURSE_REGISTRATIONS: getMyAreaCourseRegistrationsPath(),
  MY_WORKFLOWS: `${MY_AREA_BASE_PATH}/workflows`,
  MY_TODOS: `${MY_AREA_BASE_PATH}/todos`,
  MY_NOTIFICATIONS: MY_AREA_NOTIFICATIONS_PATH,
  MY_NOTIFICATION_SETTINGS: `${MY_AREA_BASE_PATH}/settings/notifications`,
};

const ORGANIZATION_TOPICS: Record<string, (orgId: number) => string> = {
  ORG_APPOINTMENTS: (orgId) => getDepartmentSectionPath(orgId, DEPARTMENT_SECTION.APPOINTMENTS),
  ORG_SERVICE_EVENTS: (orgId) => getDepartmentSectionPath(orgId, DEPARTMENT_SECTION.SERVICE),
  ORG_OPERATION_EVENTS: (orgId) => getDepartmentSectionPath(orgId, DEPARTMENT_SECTION.OPERATION),
  ORG_COURSE_OFFERINGS: getOrganizationCourseOfferingsPath,
  ORG_COURSE_REGISTRATIONS: getOrganizationCourseRegistrationListPath,
  ORG_USERS: getOrganizationUsersPath,
  ORG_ABSENCES: getOrganizationAbsencesPath,
  ORG_EMERGENCY_CONTACTS: getOrganizationEmergencyContactsPath,
  ORG_TIME_ENTRIES: getOrganizationTimeEntriesPath,
  ORG_CERTIFICATIONS: getOrganizationCertificationsPath,
  ORG_QUALIFICATIONS: getOrganizationQualificationsPath,
  ORG_RANKS: getOrganizationRanksPath,
  ORG_GROUPS: getOrganizationGroupsPath,
  ORG_WORKFLOWS: getOrganizationWorkflowsPath,
  ORG_MANAGEMENT: getOrganizationManagementPath,
};

const ADMIN_TOPICS: Record<string, (tenantId: number | null) => string | null> = {
  ADMIN_USERS: () => getAdminUsersPath(),
  ADMIN_TENANT: (tenantId) => (tenantId ? getAdminTenantDetailPath(tenantId) : null),
  ADMIN_ORGANIZATIONS: (tenantId) => (tenantId ? getAdminTenantOrganizationsPath(tenantId) : null),
  ADMIN_STANDARDS: (tenantId) => (tenantId ? getAdminStandardsPath(tenantId) : null),
  ADMIN_CATALOGS: (tenantId) => (tenantId ? getAdminTenantStandardsPath(tenantId) : null),
  ADMIN_RETENTION: (tenantId) => (tenantId ? getAdminTenantRetentionPath(tenantId) : null),
};

function resolveWorkflowTarget(action: HelpChatActionDto): HelpChatActionTarget | null {
  const key = action.workflowTypeKey as StartableTypeKey;
  const knownId = WORKFLOW_TYPE_IDS[key];
  // Reject anything where the server's id and our own table disagree.
  if (knownId === undefined || knownId !== action.workflowTypeId) {
    return null;
  }
  if (MODAL_TYPES.includes(key)) {
    return { kind: 'modal', workflowTypeId: knownId };
  }
  const route = ROUTE_TYPES[key];
  return route ? { kind: 'route', href: route() } : null;
}

function resolveTopicTarget(action: HelpChatActionDto): HelpChatActionTarget | null {
  const key = action.topicKey ?? '';
  const finish = (href: string) => (action.create ? withCreateIntent(href) : href);
  switch (action.area) {
    case 'SELF': {
      const href = SELF_TOPICS[key];
      return href ? { kind: 'route', href: finish(href) } : null;
    }
    case 'ORGANIZATION': {
      const build = ORGANIZATION_TOPICS[key];
      if (!build || action.organizations.length === 0) return null;
      const options = action.organizations.map((org) => ({
        id: org.id,
        name: org.name,
        href: finish(build(org.id)),
      }));
      return options.length === 1
        ? { kind: 'route', href: options[0].href }
        : { kind: 'chooseOrganization', options };
    }
    case 'ADMIN': {
      const href = ADMIN_TOPICS[key]?.(action.tenantId);
      return href ? { kind: 'route', href } : null;
    }
    default:
      return null;
  }
}

/** Maps a validated server action to a fixed in-app target; unknown actions resolve to null. */
export function resolveHelpChatActionTarget(
  action: HelpChatActionDto,
): HelpChatActionTarget | null {
  if (action.kind === 'WORKFLOW') return resolveWorkflowTarget(action);
  if (action.kind === 'TOPIC') return resolveTopicTarget(action);
  return null;
}
