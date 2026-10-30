/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import {
  AlertCircle,
  Building2,
  Calendar,
  CalendarClock,
  CalendarOff,
  ClipboardList,
  GraduationCap,
  Inbox,
  Medal,
  ShieldCheck,
  Star,
  User,
  Users as UsersIcon,
} from 'lucide-react';
import type { ReactNode } from 'react';
import {
  DEPARTMENT_EVENT_SECTIONS,
  DEPARTMENT_SECTION,
  type DepartmentSection,
  getDepartmentSectionPath,
  getEventTypeForSection,
} from '@/lib/department/sections';
import { canAccessUserManagement } from '@/lib/navigation/userManagementSections';
import {
  cn,
  getOrganizationAbsencesPath,
  getOrganizationCertificationsPath,
  getOrganizationCourseOfferingsPath,
  getOrganizationCourseRegistrationListPath,
  getOrganizationGroupsPath,
  getOrganizationQualificationsPath,
  getOrganizationRanksPath,
  getOrganizationUsersPath,
  getOrganizationWorkflowsPath,
  getReadPermissionForEventType,
  isOrganizationCertificationsPathActive,
  isOrganizationCourseOfferingsPathActive,
  isOrganizationCourseRegistrationListPathActive,
  isOrganizationGroupsPathActive,
  isOrganizationQualificationsPathActive,
  isOrganizationRanksPathActive,
  isOrganizationUserManagementPathActive,
  isOrganizationWorkflowsPathActive,
} from '@/lib/utils';
import { EventType } from '@/types/event';
import { PermissionTypeEnum } from '@/types/permission';

/** Hub grouping for many org tiles (see `common.hub.groups`). */
export type OrgHubGroupId = 'department' | 'membersAndAbsences';

/** Same order as org hub tiles / {@link HubNavPanel} `groups` on `/organizations/[id]`. */
export const ORG_HUB_GROUP_ORDER: readonly OrgHubGroupId[] = [
  'department',
  'membersAndAbsences',
] as const;

export function getOrganizationHubGroupTitle(
  groupId: OrgHubGroupId,
  t: (key: string) => string,
): string {
  switch (groupId) {
    case 'department':
      return t('common.hub.groups.orgDepartment');
    case 'membersAndAbsences':
      return t('common.hub.groups.orgMembersAndAbsences');
  }
}

export type OrgNavListItem = {
  id: string;
  href: string;
  label: string;
  hint: string;
  active: boolean;
  icon: ReactNode;
  hubGroupId?: OrgHubGroupId;
};

type OrgRef = { id: number; name: string };

type OrgNavIconVariant = 'main' | 'workspace' | 'hub';

function orgHubTileLabel(
  iconVariant: OrgNavIconVariant,
  hubLabelKey: string,
  fallbackLabelKey: string,
  t: (key: string) => string,
): string {
  if (iconVariant === 'hub') {
    const resolved = t(hubLabelKey);
    if (resolved !== hubLabelKey) return resolved;
  }
  return t(fallbackLabelKey);
}

function orgSubnavIconClass(active: boolean, variant: OrgNavIconVariant) {
  if (variant === 'hub') return '';
  if (!active) return 'text-[var(--text-secondary)]';
  if (variant === 'workspace') return 'text-white';
  return 'text-[var(--fire-red)] dark:text-[var(--fire-red-light)]';
}

/** Hub tiles size icons via {@link HubNavTile}; sidebar/workspace nav keeps explicit dimensions. */
function orgNavIconProps(active: boolean, variant: OrgNavIconVariant): { className?: string } {
  if (variant === 'hub') return {};
  return { className: cn('h-4 w-4 shrink-0', orgSubnavIconClass(active, variant)) };
}

const EVENT_NAV_META: Record<
  EventType,
  {
    labelKey: string;
    hubLabelKey: string;
    hintKey: string;
    icon: (active: boolean, variant: OrgNavIconVariant) => React.ReactNode;
    section: DepartmentSection;
  }
> = {
  [EventType.SERVICE]: {
    labelKey: 'events.menu.service',
    hubLabelKey: 'organization.hub.tileLabels.service',
    hintKey: 'organization.hub.tiles.hintEventService',
    section: DEPARTMENT_SECTION.SERVICE,
    icon: (active, variant) => <Calendar {...orgNavIconProps(active, variant)} />,
  },
  [EventType.OPERATION]: {
    labelKey: 'events.menu.operation',
    hubLabelKey: 'organization.hub.tileLabels.operation',
    hintKey: 'organization.hub.tiles.hintEventOperation',
    section: DEPARTMENT_SECTION.OPERATION,
    icon: (active, variant) => <AlertCircle {...orgNavIconProps(active, variant)} />,
  },
};

/** Management list of a managing organization (`/organizations/[id]/management`). */
export function getOrganizationManagementPath(orgId: number): string {
  return `/organizations/${orgId}/management`;
}

export function isOrganizationPathActive(path: string, pathname: string): boolean {
  return pathname === path || pathname.startsWith(`${path}/`);
}

export function buildFlatOrgNavItems(
  org: OrgRef,
  pathname: string,
  t: (key: string) => string,
  hasPermission: (orgId: number, permission: PermissionTypeEnum) => boolean,
  iconVariant: OrgNavIconVariant = 'main',
): OrgNavListItem[] {
  const items: OrgNavListItem[] = [];

  if (hasPermission(org.id, PermissionTypeEnum.APPOINTMENT_READ)) {
    const href = getDepartmentSectionPath(org.id, DEPARTMENT_SECTION.APPOINTMENTS);
    const active = isOrganizationPathActive(href, pathname);
    items.push({
      id: `org-${org.id}-appointments`,
      href,
      label: orgHubTileLabel(
        iconVariant,
        'organization.hub.tileLabels.appointments',
        'appointments.title',
        t,
      ),
      hint: t('organization.hub.tiles.hintAppointments'),
      active,
      hubGroupId: 'department',
      icon: <CalendarClock {...orgNavIconProps(active, iconVariant)} />,
    });
  }

  for (const section of DEPARTMENT_EVENT_SECTIONS) {
    const eventType = getEventTypeForSection(section);
    if (!eventType) continue;
    const readPerm = getReadPermissionForEventType(eventType);
    if (!hasPermission(org.id, readPerm)) continue;
    const meta = EVENT_NAV_META[eventType];
    const href = getDepartmentSectionPath(org.id, section);
    const active = isOrganizationPathActive(href, pathname);
    items.push({
      id: `org-${org.id}-${section}`,
      href,
      label: orgHubTileLabel(iconVariant, meta.hubLabelKey, meta.labelKey, t),
      hint: t(meta.hintKey),
      active,
      hubGroupId: 'department',
      icon: meta.icon(active, iconVariant),
    });
  }

  if (canAccessUserManagement(org.id, hasPermission)) {
    const usersPath = getOrganizationUsersPath(org.id);
    const usersActive = isOrganizationUserManagementPathActive(org.id, pathname);
    items.push({
      id: `org-${org.id}-manage-users`,
      href: usersPath,
      label: orgHubTileLabel(
        iconVariant,
        'organization.hub.tileLabels.manageUsers',
        'organization.hub.navigation.manageUsers',
        t,
      ),
      hint: t('organization.hub.tiles.hintManageUsers'),
      active: usersActive,
      hubGroupId: 'membersAndAbsences',
      icon: <User {...orgNavIconProps(usersActive, iconVariant)} />,
    });
  }

  if (hasPermission(org.id, PermissionTypeEnum.MANAGEMENT_ACCESS)) {
    const href = getOrganizationManagementPath(org.id);
    const active = isOrganizationPathActive(href, pathname);
    items.push({
      id: `org-${org.id}-management`,
      href,
      label: orgHubTileLabel(
        iconVariant,
        'organization.hub.tileLabels.management',
        'management.page.title',
        t,
      ),
      hint: t('organization.hub.tiles.hintManagement'),
      active,
      hubGroupId: 'department',
      icon: <Building2 {...orgNavIconProps(active, iconVariant)} />,
    });
  }

  if (hasPermission(org.id, PermissionTypeEnum.ABSENCE_READ)) {
    const absencesPath = getOrganizationAbsencesPath(org.id);
    const absActive = isOrganizationPathActive(absencesPath, pathname);
    items.push({
      id: `org-${org.id}-absences`,
      href: absencesPath,
      label: orgHubTileLabel(
        iconVariant,
        'organization.hub.tileLabels.absences',
        'userManagement.tabs.absences',
        t,
      ),
      hint: t('organization.hub.tiles.hintOrgAbsences'),
      active: absActive,
      hubGroupId: 'membersAndAbsences',
      icon: <CalendarOff {...orgNavIconProps(absActive, iconVariant)} />,
    });
  }

  if (hasPermission(org.id, PermissionTypeEnum.WORKFLOW_READ)) {
    const workflowsPath = getOrganizationWorkflowsPath(org.id);
    const workflowsActive = isOrganizationWorkflowsPathActive(org.id, pathname);
    items.push({
      id: `org-${org.id}-workflows`,
      href: workflowsPath,
      label: orgHubTileLabel(
        iconVariant,
        'organization.hub.tileLabels.workflows',
        'userManagement.sections.workflows',
        t,
      ),
      hint: t('organization.hub.tiles.hintWorkflows'),
      active: workflowsActive,
      hubGroupId: 'membersAndAbsences',
      icon: <Inbox {...orgNavIconProps(workflowsActive, iconVariant)} />,
    });
  }

  if (hasPermission(org.id, PermissionTypeEnum.CERTIFICATION_READ)) {
    const certificationsPath = getOrganizationCertificationsPath(org.id);
    const certificationsActive = isOrganizationCertificationsPathActive(org.id, pathname);
    items.push({
      id: `org-${org.id}-certifications`,
      href: certificationsPath,
      label: orgHubTileLabel(
        iconVariant,
        'organization.hub.tileLabels.certifications',
        'userManagement.sections.certifications',
        t,
      ),
      hint: t('organization.hub.tiles.hintCertifications'),
      active: certificationsActive,
      hubGroupId: 'membersAndAbsences',
      icon: <ShieldCheck {...orgNavIconProps(certificationsActive, iconVariant)} />,
    });
  }

  if (hasPermission(org.id, PermissionTypeEnum.RANK_READ)) {
    const ranksPath = getOrganizationRanksPath(org.id);
    const ranksActive = isOrganizationRanksPathActive(org.id, pathname);
    items.push({
      id: `org-${org.id}-ranks`,
      href: ranksPath,
      label: orgHubTileLabel(
        iconVariant,
        'organization.hub.tileLabels.ranks',
        'ranks.overview.title',
        t,
      ),
      hint: t('organization.hub.tiles.hintRanks'),
      active: ranksActive,
      hubGroupId: 'membersAndAbsences',
      icon: <Star {...orgNavIconProps(ranksActive, iconVariant)} />,
    });
  }

  if (hasPermission(org.id, PermissionTypeEnum.QUALIFICATION_READ)) {
    const qualificationsPath = getOrganizationQualificationsPath(org.id);
    const qualificationsActive = isOrganizationQualificationsPathActive(org.id, pathname);
    items.push({
      id: `org-${org.id}-qualifications`,
      href: qualificationsPath,
      label: orgHubTileLabel(
        iconVariant,
        'organization.hub.tileLabels.qualifications',
        'userManagement.sections.qualifications',
        t,
      ),
      hint: t('organization.hub.tiles.hintQualifications'),
      active: qualificationsActive,
      hubGroupId: 'membersAndAbsences',
      icon: <Medal {...orgNavIconProps(qualificationsActive, iconVariant)} />,
    });
  }

  if (hasPermission(org.id, PermissionTypeEnum.TRAINING_COURSE_READ)) {
    const courseRegistrationsPath = getOrganizationCourseRegistrationListPath(org.id);
    const courseRegistrationsActive = isOrganizationCourseRegistrationListPathActive(
      org.id,
      pathname,
    );
    items.push({
      id: `org-${org.id}-course-registrations`,
      href: courseRegistrationsPath,
      label: orgHubTileLabel(
        iconVariant,
        'organization.hub.tileLabels.courseRegistrations',
        'courseOfferings.registrations.organizationList.title',
        t,
      ),
      hint: t('organization.hub.tiles.hintCourseRegistrations'),
      active: courseRegistrationsActive,
      hubGroupId: 'membersAndAbsences',
      icon: <ClipboardList {...orgNavIconProps(courseRegistrationsActive, iconVariant)} />,
    });
  }

  if (hasPermission(org.id, PermissionTypeEnum.COURSE_OFFERING_READ)) {
    const courseOfferingsPath = getOrganizationCourseOfferingsPath(org.id);
    const courseOfferingsActive = isOrganizationCourseOfferingsPathActive(org.id, pathname);
    items.push({
      id: `org-${org.id}-course-offerings`,
      href: courseOfferingsPath,
      label: orgHubTileLabel(
        iconVariant,
        'organization.hub.tileLabels.courseOfferings',
        'courseOfferings.title',
        t,
      ),
      hint: t('organization.hub.tiles.hintCourseOfferings'),
      active: courseOfferingsActive,
      hubGroupId: 'department',
      icon: <GraduationCap {...orgNavIconProps(courseOfferingsActive, iconVariant)} />,
    });
  }

  if (hasPermission(org.id, PermissionTypeEnum.ORGANIZATION_GROUP_READ)) {
    const groupsPath = getOrganizationGroupsPath(org.id);
    const groupsActive = isOrganizationGroupsPathActive(org.id, pathname);
    items.push({
      id: `org-${org.id}-groups`,
      href: groupsPath,
      label: orgHubTileLabel(
        iconVariant,
        'organization.hub.tileLabels.groups',
        'organization.groups.title',
        t,
      ),
      hint: t('organization.hub.tiles.hintGroups'),
      active: groupsActive,
      hubGroupId: 'membersAndAbsences',
      icon: <UsersIcon {...orgNavIconProps(groupsActive, iconVariant)} />,
    });
  }

  return items;
}
