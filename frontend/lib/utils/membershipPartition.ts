/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type {
  UserOrganizationGroupMembershipDto,
  UserOrganizationMembershipDto,
} from '@/types/user';
import { formatDateTime, getOrganizationWallClockNow } from './date';

function parseWallClock(value: string): number {
  return new Date(value).getTime();
}

/** Mirrors the backend check at an instant: half-open `[start, end)`, the end instant is not active. */
export function isActiveOrganizationMembership(
  membership: UserOrganizationMembershipDto,
  at: string = getOrganizationWallClockNow(),
): boolean {
  const atMs = parseWallClock(at);
  const started = !membership.startDate || parseWallClock(membership.startDate) <= atMs;
  const notEnded = !membership.endDate || parseWallClock(membership.endDate) > atMs;
  return started && notEnded;
}

/** Mirrors the backend check at an instant: half-open `[start, end)`, the end instant is not active. */
export function isActiveGroupMembership(
  membership: UserOrganizationGroupMembershipDto,
  at: string = getOrganizationWallClockNow(),
): boolean {
  const atMs = parseWallClock(at);
  const started = !membership.startDate || parseWallClock(membership.startDate) <= atMs;
  const notEnded = !membership.endDate || parseWallClock(membership.endDate) > atMs;
  return started && notEnded;
}

export function partitionOrganizationMemberships(memberships: UserOrganizationMembershipDto[]): {
  active: UserOrganizationMembershipDto[];
  history: UserOrganizationMembershipDto[];
} {
  const active: UserOrganizationMembershipDto[] = [];
  const history: UserOrganizationMembershipDto[] = [];
  for (const membership of memberships) {
    if (isActiveOrganizationMembership(membership)) {
      active.push(membership);
    } else {
      history.push(membership);
    }
  }
  return { active, history };
}

export function partitionGroupMemberships(memberships: UserOrganizationGroupMembershipDto[]): {
  active: UserOrganizationGroupMembershipDto[];
  history: UserOrganizationGroupMembershipDto[];
} {
  const active: UserOrganizationGroupMembershipDto[] = [];
  const history: UserOrganizationGroupMembershipDto[] = [];
  for (const membership of memberships) {
    if (isActiveGroupMembership(membership)) {
      active.push(membership);
    } else {
      history.push(membership);
    }
  }
  return { active, history };
}

/** Newest first, using end date when the membership has ended (falls back to start date). */
export function sortMembershipsByRecency<T extends { startDate: string; endDate?: string | null }>(
  memberships: T[],
): T[] {
  return [...memberships].sort((a, b) => {
    const aTime = parseWallClock(a.endDate ?? a.startDate);
    const bTime = parseWallClock(b.endDate ?? b.startDate);
    return bTime - aTime;
  });
}

export function formatMembershipPeriod(
  startDate: string,
  endDate: string | null | undefined,
): string {
  const start = formatDateTime(startDate);
  if (!endDate) {
    return start;
  }
  return `${start} - ${formatDateTime(endDate)}`;
}
