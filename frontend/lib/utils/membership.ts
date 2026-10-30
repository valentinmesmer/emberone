/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { GroupMembershipType } from '@/types/enums/groupMembershipType';
import type { OrganizationGroupDto, OrganizationGroupType } from '@/types/organizationGroup';
import type { UserDto, UserOrganizationGroupMembershipDto } from '@/types/user';
import { getOrganizationWallClockNow } from './date';
import { isActiveGroupMembership } from './membershipPartition';
import { validateDateRange, validateStartNotInFuture } from './validation';

export type TranslationFunction = (key: string, params?: Record<string, string | number>) => string;
export type ShowErrorFunction = (message: string) => void;

export {
  formatMembershipPeriod,
  isActiveGroupMembership,
  isActiveOrganizationMembership,
  partitionGroupMemberships,
  partitionOrganizationMemberships,
} from './membershipPartition';

/**
 * Returns the membership type options for select dropdowns
 */
export function getMembershipTypeOptions(t: TranslationFunction) {
  return [
    { value: GroupMembershipType.LEADER, label: t('userManagement.groupMembership.types.LEADER') },
    {
      value: GroupMembershipType.DEPUTY_LEADER,
      label: t('userManagement.groupMembership.types.DEPUTY_LEADER'),
    },
    { value: GroupMembershipType.MEMBER, label: t('userManagement.groupMembership.types.MEMBER') },
  ];
}

function parseWallClock(value: string): number {
  return new Date(value).getTime();
}

/**
 * Checks if two wall-clock ranges overlap (yyyy-MM-ddTHH:mm:ss).
 */
export function datesOverlap(
  start1: string,
  end1: string | null | undefined,
  start2: string,
  end2: string | null | undefined,
): boolean {
  const start1Ms = parseWallClock(start1);
  const end1Ms = end1 ? parseWallClock(end1) : Number.POSITIVE_INFINITY;
  const start2Ms = parseWallClock(start2);
  const end2Ms = end2 ? parseWallClock(end2) : Number.POSITIVE_INFINITY;
  return start1Ms <= end2Ms && end1Ms >= start2Ms;
}

/**
 * Checks if a membership overlaps with the given date range
 * @param membership The existing membership to check
 * @param newStartDate Start date of new membership
 * @param newEndDate End date of new membership (null/undefined means no end)
 * @returns true if the membership overlaps with the date range
 */
export function membershipOverlaps(
  membership: UserOrganizationGroupMembershipDto,
  newStartDate: string,
  newEndDate: string | null | undefined,
): boolean {
  return datesOverlap(membership.startDate, membership.endDate, newStartDate, newEndDate);
}

export function isActiveMemberOfOrganizationGroup(
  memberships: UserOrganizationGroupMembershipDto[] | undefined,
  organizationGroupId: number | null | undefined,
  at: string = getOrganizationWallClockNow(),
): boolean {
  if (organizationGroupId == null || !memberships?.length) {
    return false;
  }
  return memberships.some(
    (m) => m.organizationGroupId === organizationGroupId && isActiveGroupMembership(m, at),
  );
}

/** Mirrors backend requireNoActiveLeader - one active LEADER membership per organization. */
export function hasActiveLeadershipLeader(
  memberships: UserOrganizationGroupMembershipDto[],
  at: string = getOrganizationWallClockNow(),
): boolean {
  return memberships.some(
    (m) => m.membershipType === GroupMembershipType.LEADER && isActiveGroupMembership(m, at),
  );
}

/**
 * Translates backend error messages to frontend translation keys
 * @param errorMessage The error message from the backend
 * @param t Translation function
 * @returns Translated error message or original if no translation found
 */
export function translateBackendError(errorMessage: string, t: TranslationFunction): string {
  // Check for common backend error patterns and map them to translation keys
  if (
    errorMessage.includes('must have at least one leader') ||
    errorMessage.includes('must have at least one leader when it has members')
  ) {
    return t('userManagement.groupMembership.errors.groupMustHaveLeader');
  }

  if (
    errorMessage.includes('Cannot remove last member from leadership group') ||
    errorMessage.includes('last member from leadership groups')
  ) {
    return t('userManagement.groupMembership.errors.cannotRemoveLastLeadershipMember');
  }

  if (
    errorMessage.includes('already has a leader') ||
    errorMessage.includes('Organization already has an active leader') ||
    errorMessage.includes('leader already exists')
  ) {
    return t('userManagement.groupMembership.errors.leaderAlreadyExists');
  }

  if (
    errorMessage.includes('already a member of organization group') ||
    errorMessage.includes('is already a member')
  ) {
    return t('userManagement.groupMembership.errors.alreadyMember');
  }

  if (
    errorMessage.includes('already a member of organization') ||
    errorMessage.includes('organization membership already exists')
  ) {
    return t('userManagement.organizationMembership.errors.alreadyMember');
  }

  if (
    errorMessage.includes('Ending organization memberships') ||
    errorMessage.includes('not supported yet')
  ) {
    return t('userManagement.organizationMembership.errors.endingNotSupported');
  }

  if (errorMessage.includes('Membership is already ended')) {
    return t('userManagement.groupMembership.errors.alreadyEnded');
  }

  if (
    errorMessage.includes('Membership is already ended:') ||
    errorMessage.includes('Organisationsmitgliedschaft ist bereits beendet')
  ) {
    return t('userManagement.organizationMembership.errors.alreadyEnded');
  }

  if (
    errorMessage.includes(
      'Cannot change organization membership while the user is in the leadership group',
    ) ||
    errorMessage.includes(
      'Cannot end organization membership while the user is in the leadership group',
    )
  ) {
    return t('userManagement.organizationMembership.errors.blockedByLeadership');
  }

  if (
    errorMessage.includes('USER_ALREADY_DEACTIVATED') ||
    errorMessage.includes('already deactivated')
  ) {
    return t('userManagement.organizationMembership.userAlreadyDeactivated');
  }

  if (
    errorMessage.includes('USER_ACCOUNT_NOT_SUSTAINABLE') ||
    errorMessage.includes('without an organization anchor')
  ) {
    return t('userManagement.organizationMembership.accountNotSustainable');
  }

  if (errorMessage.includes('Start date cannot be in the future')) {
    return t('common.validation.startNotInFuture');
  }

  if (
    errorMessage.includes('Cannot manually modify memberships in the Leadership organization group')
  ) {
    return t('organizationManagement.organizationGroups.leadershipReadOnly');
  }

  // Return original message if no translation found
  return errorMessage;
}

export function isHiddenOrganizationGroup(group: { type?: OrganizationGroupType }): boolean {
  return group.type === 'ALL_MEMBERS';
}

export function isLeadershipOrganizationGroup(group: { type?: OrganizationGroupType }): boolean {
  return group.type === 'LEADERSHIP';
}

export function filterVisibleOrganizationGroups<T extends { type?: OrganizationGroupType }>(
  groups: T[],
): T[] {
  return groups.filter((group) => !isHiddenOrganizationGroup(group));
}

/** User-visible organization group label from API (localized name stored at creation). */
export function getGroupDisplayName(group: { name: string }): string {
  return group.name;
}

/**
 * Gets the translated label for a membership type
 * @param type The membership type
 * @param t Translation function
 * @returns The translated label
 */
export function getMembershipTypeLabel(type: GroupMembershipType, t: TranslationFunction): string {
  return t(`userManagement.groupMembership.types.${type}`);
}

/**
 * Validates that a user doesn't already have an overlapping membership in the same group
 * @param userMemberships All memberships of the user
 * @param groupId The group ID to check
 * @param startDate The start date of the new membership
 * @param endDate The end date of the new membership
 * @param excludeMembershipId Optional membership ID to exclude from check (for updates)
 * @returns The overlapping membership if found, null otherwise
 */
export function findOverlappingMembership(
  userMemberships: UserOrganizationGroupMembershipDto[],
  groupId: number,
  startDate: string,
  endDate: string | null | undefined,
  excludeMembershipId?: number,
): UserOrganizationGroupMembershipDto | null {
  return (
    userMemberships.find(
      (m) =>
        m.id !== excludeMembershipId &&
        m.organizationGroupId === groupId &&
        membershipOverlaps(m, startDate, endDate),
    ) || null
  );
}

/**
 * Validates group membership assignment/update (checks overlaps)
 * @param user The user being assigned/updated
 * @param users All users in the organization
 * @param organizationGroups All organization groups
 * @param groupId The target group ID
 * @param startDate Start date of the membership
 * @param endDate End date of the membership
 * @param membershipType The membership type
 * @param excludeMembershipId Optional membership ID to exclude (for updates)
 * @param t Translation function
 * @param showError Error display function
 * @returns true if validation passes, false otherwise
 */
export function validateGroupMembershipAssignment(
  user: UserDto,
  _users: UserDto[],
  organizationGroups: OrganizationGroupDto[],
  groupId: number,
  startDate: string,
  endDate: string | null | undefined,
  _membershipType: GroupMembershipType,
  excludeMembershipId: number | undefined,
  t: TranslationFunction,
  showError: ShowErrorFunction,
  options?: { allowLeadershipGroups?: boolean },
): boolean {
  if (!validateDateRange(startDate, endDate ?? undefined, t, showError)) {
    return false;
  }
  if (!validateStartNotInFuture(startDate, t, showError)) {
    return false;
  }

  const targetGroup = organizationGroups.find((g) => g.id === groupId);
  if (
    targetGroup &&
    isLeadershipOrganizationGroup(targetGroup) &&
    !options?.allowLeadershipGroups
  ) {
    showError(t('organizationManagement.organizationGroups.leadershipReadOnly'));
    return false;
  }

  const userMemberships = user.organizationGroupMemberships || [];

  // Check for overlapping memberships
  const overlappingMembership = findOverlappingMembership(
    userMemberships,
    groupId,
    startDate,
    endDate,
    excludeMembershipId,
  );

  if (overlappingMembership) {
    showError(t('userManagement.groupMembership.errors.alreadyMember'));
    return false;
  }

  return true;
}
