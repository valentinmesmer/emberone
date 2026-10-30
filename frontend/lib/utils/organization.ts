/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { getFirstAccessibleSection } from '@/lib/department/sections';
import type { PermissionTypeEnum } from '@/types/permission';

export { normalizeOrganizationCode } from '@/lib/validation/organization';

export type OrganizationPermissionChecker = (
  organizationId: number,
  permission: PermissionTypeEnum,
) => boolean;

export function getOrganizationUsersPath(orgId: number): string {
  return `/organizations/${orgId}/users`;
}

export function getOrganizationGroupsPath(orgId: number): string {
  return `/organizations/${orgId}/groups`;
}

export function getOrganizationGroupDetailPath(orgId: number, groupId: number): string {
  return `${getOrganizationGroupsPath(orgId)}/${groupId}`;
}

export function getOrganizationGroupMembershipDetailPathFromGroup(
  orgId: number,
  groupId: number,
  membershipId: number,
): string {
  return `${getOrganizationGroupDetailPath(orgId, groupId)}/memberships/${membershipId}`;
}

export function isOrganizationUserManagementPathActive(orgId: number, pathname: string): boolean {
  const usersPath = getOrganizationUsersPath(orgId);
  const groupMembershipsBase = `/organizations/${orgId}/group-memberships`;
  const inUserGroupMemberships =
    pathname === groupMembershipsBase || pathname.startsWith(`${groupMembershipsBase}/`);
  return pathname === usersPath || pathname.startsWith(`${usersPath}/`) || inUserGroupMemberships;
}

export function isOrganizationGroupsPathActive(orgId: number, pathname: string): boolean {
  const groupsPath = getOrganizationGroupsPath(orgId);
  if (pathname === groupsPath || pathname.startsWith(`${groupsPath}/`)) {
    return true;
  }
  return false;
}

export function getOrganizationAbsencesPath(orgId: number): string {
  return `/organizations/${orgId}/absences`;
}

export function getOrganizationCertificationsPath(orgId: number): string {
  return `/organizations/${orgId}/certifications`;
}

export function getOrganizationQualificationsPath(orgId: number): string {
  return `/organizations/${orgId}/qualifications`;
}

export {
  getOrganizationCourseAudienceDetailPath,
  getOrganizationCourseCompletionParticipantPath,
  getOrganizationCourseCompletionPath,
  getOrganizationCourseOfferingDetailPath,
  getOrganizationCourseOfferingsPath,
  getOrganizationCourseRegistrationListPath,
  getOrganizationCourseRegistrationsPath,
  getOrganizationCourseSessionDetailPath,
  isOrganizationCourseOfferingsPathActive,
  isOrganizationCourseRegistrationListPathActive,
} from './courseOffering';

export function getOrganizationRanksPath(orgId: number): string {
  return `/organizations/${orgId}/ranks`;
}

export function getOrganizationWorkflowsPath(orgId: number): string {
  return `/organizations/${orgId}/workflows`;
}

export function isOrganizationCertificationsPathActive(orgId: number, pathname: string): boolean {
  const certificationsPath = getOrganizationCertificationsPath(orgId);
  return pathname === certificationsPath || pathname.startsWith(`${certificationsPath}/`);
}

export function isOrganizationQualificationsPathActive(orgId: number, pathname: string): boolean {
  const qualificationsPath = getOrganizationQualificationsPath(orgId);
  return pathname === qualificationsPath || pathname.startsWith(`${qualificationsPath}/`);
}

export function isOrganizationRanksPathActive(orgId: number, pathname: string): boolean {
  const ranksPath = getOrganizationRanksPath(orgId);
  return pathname === ranksPath || pathname.startsWith(`${ranksPath}/`);
}

export function isOrganizationWorkflowsPathActive(orgId: number, pathname: string): boolean {
  const workflowsPath = getOrganizationWorkflowsPath(orgId);
  return pathname === workflowsPath || pathname.startsWith(`${workflowsPath}/`);
}

export function getOrganizationEmergencyContactDetailPath(
  orgId: number,
  contactId: number,
): string {
  return `/organizations/${orgId}/emergency-contacts/${contactId}`;
}

export function getOrganizationTimeEntryDetailPath(orgId: number, entryId: number): string {
  return `/organizations/${orgId}/time-entries/${entryId}`;
}

/**
 * Organization overview hub - the curated landing page for an organization.
 */
export function getOrganizationOverviewPath(orgId: number): string {
  return `/organizations/${orgId}`;
}

/**
 * Default route when opening an organization from the menu or home.
 * Members always land on the overview hub; modules are shown based on specific permissions.
 */
export function getOrganizationWorkspaceEntryPath(orgId: number): string {
  return getOrganizationOverviewPath(orgId);
}

/** True when the user may open at least one department section (appointments or events). */
export function canAccessDepartment(
  organizationId: number,
  hasPermission: OrganizationPermissionChecker,
): boolean {
  return getFirstAccessibleSection(organizationId, hasPermission) !== null;
}

/** Where the user opened the group-membership detail from (drives back navigation). */
export type OrganizationGroupMembershipDetailReferrer =
  | { kind: 'group'; groupId: number }
  | { kind: 'user'; userId: number };

export function getOrganizationGroupMembershipDetailPath(
  orgId: number,
  membershipId: number,
  referrer?: OrganizationGroupMembershipDetailReferrer,
): string {
  const base = `/organizations/${orgId}/group-memberships/${membershipId}`;
  if (!referrer) return base;
  if (referrer.kind === 'group') {
    return getOrganizationGroupMembershipDetailPathFromGroup(orgId, referrer.groupId, membershipId);
  }
  return `${base}?from=user&userId=${referrer.userId}`;
}

export function getOrganizationGroupMembershipBackPath(
  orgId: number,
  referrer?: OrganizationGroupMembershipDetailReferrer,
): string {
  if (referrer?.kind === 'group') {
    return getOrganizationGroupDetailPath(orgId, referrer.groupId);
  }
  if (referrer?.kind === 'user') {
    return `${getOrganizationUsersPath(orgId)}/${referrer.userId}/memberships`;
  }
  return getOrganizationGroupsPath(orgId);
}

export function parseOrganizationGroupMembershipDetailReferrer(searchParams: {
  get(name: string): string | null;
}): OrganizationGroupMembershipDetailReferrer | undefined {
  const from = searchParams.get('from');
  if (from === 'group') {
    const groupId = Number.parseInt(searchParams.get('groupId') ?? '', 10);
    if (Number.isFinite(groupId) && groupId > 0) {
      return { kind: 'group', groupId };
    }
  }
  if (from === 'user') {
    const userId = Number.parseInt(searchParams.get('userId') ?? '', 10);
    if (Number.isFinite(userId) && userId > 0) {
      return { kind: 'user', userId };
    }
  }
  return undefined;
}
