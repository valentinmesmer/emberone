/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type {
  GroupMembershipType,
  OrganizationFunctionOverviewDto,
  OrganizationRankMemberDto,
} from '@/lib/api/ranks';
import type { TenantRankDto } from '@/lib/api/tenantRanks';
import { compareText } from '@/lib/i18n/locale';

const NO_RANK_GROUP_ID = 'rank:none';
const NO_RANK_SORT_ORDER = Number.MAX_SAFE_INTEGER;

export interface RankOverviewGroup {
  id: string;
  title: string;
  sortOrder: number;
  members: OrganizationRankMemberDto[];
}

export interface FunctionOverviewMember {
  userId: number;
  firstName: string;
  lastName: string;
  mainOrganization: boolean;
  currentRank: OrganizationRankMemberDto['currentRank'];
  membershipType: GroupMembershipType;
  functionAbbreviation: string;
}

export interface FunctionOverviewGroup {
  id: string;
  title: string;
  subtitle: string;
  members: FunctionOverviewMember[];
}

export function buildRankOverviewGroups(
  members: OrganizationRankMemberDto[],
  catalogRanks: TenantRankDto[] | undefined,
  includeEmptyCatalogGroups: boolean,
  noRankLabel: string,
): RankOverviewGroup[] {
  const groups = new Map<string, RankOverviewGroup>();

  const upsertGroup = (id: string, title: string, sortOrder: number) => {
    const existing = groups.get(id);
    if (existing) return existing;
    const group: RankOverviewGroup = { id, title, sortOrder, members: [] };
    groups.set(id, group);
    return group;
  };

  if (includeEmptyCatalogGroups && catalogRanks?.length) {
    for (const rank of catalogRanks) {
      if (!rank.active || rank.archived) continue;
      upsertGroup(`rank:${rank.id}`, `${rank.name} (${rank.abbreviation})`, rank.sortOrder);
    }
  }

  for (const member of members) {
    if (member.currentRank) {
      const rank = member.currentRank;
      upsertGroup(
        `rank:${rank.tenantRankId}`,
        `${rank.name} (${rank.abbreviation})`,
        rank.sortOrder,
      ).members.push(member);
    } else {
      upsertGroup(NO_RANK_GROUP_ID, noRankLabel, NO_RANK_SORT_ORDER).members.push(member);
    }
  }

  return [...groups.values()].sort(
    (a, b) => a.sortOrder - b.sortOrder || compareText(a.title, b.title),
  );
}

export function mapFunctionOverviewGroups(
  overview: OrganizationFunctionOverviewDto | undefined,
): FunctionOverviewGroup[] {
  if (!overview?.groups?.length) return [];

  return overview.groups.map((group) => ({
    id: group.groupKey,
    title: group.functionName,
    subtitle: group.organizationGroupName,
    members: group.members.map((member) => ({
      userId: member.userId,
      firstName: member.firstName,
      lastName: member.lastName,
      mainOrganization: member.mainOrganization,
      currentRank: member.currentRank,
      membershipType: member.membershipType,
      functionAbbreviation: group.functionAbbreviation,
    })),
  }));
}

export function defaultExpandedGroupIds(groups: { id: string; members: unknown[] }[]): Set<string> {
  return new Set(groups.filter((group) => group.members.length > 0).map((group) => group.id));
}
