/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { ListQuery } from '@/lib/list/types';
import type { PageResponse } from '@/types/page';
import { apiClient } from './client';
import { buildQuery } from './paged';
import type { TenantRankDto } from './tenantRanks';

export interface UserRankSummaryDto {
  tenantRankId: number;
  configKey: string;
  name: string;
  abbreviation: string;
  sortOrder: number;
  startDate: string;
}

export interface UserRankCurrentDto {
  tenantRankId: number;
  configKey: string;
  name: string;
  abbreviation: string;
  startDate: string;
  reason: string | null;
}

export interface UserRankHistoryItemDto {
  tenantRankId: number;
  configKey: string | null;
  name: string | null;
  abbreviation: string | null;
  startDate: string;
  endDate: string | null;
  reason: string | null;
}

export interface UserRankSelfDto {
  current: UserRankCurrentDto | null;
  history: UserRankHistoryItemDto[];
}

export interface OrganizationFunctionOverviewMemberDto {
  userId: number;
  firstName: string;
  lastName: string;
  mainOrganization: boolean;
  membershipType: GroupMembershipType;
  currentRank: UserRankSummaryDto | null;
}

export interface OrganizationFunctionOverviewGroupDto {
  groupKey: string;
  functionConfigKey: string;
  functionName: string;
  functionAbbreviation: string;
  functionSortOrder: number;
  organizationGroupName: string;
  organizationGroupConfigKey: string;
  grantMembershipType: GroupMembershipType | null;
  members: OrganizationFunctionOverviewMemberDto[];
}

export interface OrganizationFunctionOverviewDto {
  groups: OrganizationFunctionOverviewGroupDto[];
}

export interface OrganizationRankMemberDto {
  userId: number;
  firstName: string;
  lastName: string;
  mainOrganization: boolean;
  currentRank: UserRankSummaryDto | null;
}

export type OrganizationRankMemberPageDto = PageResponse<OrganizationRankMemberDto>;

export type OrganizationRankMemberListRequest = ListQuery;

export interface SetUserRankRequest {
  tenantRankId: number;
  startDate: string;
  reason?: string;
}

export type GroupMembershipType = 'LEADER' | 'DEPUTY_LEADER' | 'MEMBER';

export interface ResolvedDutyFunctionDto {
  organizationId: number;
  organizationName: string;
  functionConfigKey: string;
  functionName: string;
  functionAbbreviation: string;
  organizationGroupName: string;
  organizationGroupConfigKey: string;
  membershipType: GroupMembershipType;
  membershipStartDate: string;
  membershipEndDate: string | null;
  active: boolean;
}

const selfBase = '/api/v1/self/rank';
const selfFunctionsBase = '/api/v1/self/functions';
const orgRanksBase = (organizationId: number) =>
  `/api/v1/auth/organizations/${organizationId}/ranks`;
const orgUserBase = (organizationId: number, userId: number) =>
  `/api/v1/auth/organizations/${organizationId}/users/${userId}`;

export const ranksApi = {
  getMyRank(): Promise<UserRankSelfDto> {
    return apiClient.get<UserRankSelfDto>(selfBase);
  },

  getMyAvailableRanks(): Promise<TenantRankDto[]> {
    return apiClient.get<TenantRankDto[]>(`${selfBase}/available-ranks`);
  },

  getMyFunctions(): Promise<ResolvedDutyFunctionDto[]> {
    return apiClient.get<ResolvedDutyFunctionDto[]>(selfFunctionsBase);
  },

  async listOrganizationMembers(
    organizationId: number,
    query: OrganizationRankMemberListRequest,
  ): Promise<OrganizationRankMemberPageDto> {
    const qs = buildQuery({
      page: query.page ?? 0,
      size: query.size,
      search: query.search,
      sort: query.sort,
    });
    const path = orgRanksBase(organizationId);
    return apiClient.get<OrganizationRankMemberPageDto>(qs ? `${path}?${qs}` : path);
  },

  getOrganizationFunctionsOverview(
    organizationId: number,
    search?: string,
  ): Promise<OrganizationFunctionOverviewDto> {
    const qs = buildQuery({ search });
    const path = `${orgRanksBase(organizationId)}/functions/overview`;
    return apiClient.get<OrganizationFunctionOverviewDto>(qs ? `${path}?${qs}` : path);
  },

  getOrganizationAvailableRanks(organizationId: number): Promise<TenantRankDto[]> {
    return apiClient.get<TenantRankDto[]>(`${orgRanksBase(organizationId)}/available-ranks`);
  },

  getUserRank(organizationId: number, userId: number): Promise<UserRankSelfDto> {
    return apiClient.get<UserRankSelfDto>(`${orgUserBase(organizationId, userId)}/rank`);
  },

  setUserRank(
    organizationId: number,
    userId: number,
    data: SetUserRankRequest,
  ): Promise<UserRankCurrentDto> {
    return apiClient.post<UserRankCurrentDto>(`${orgUserBase(organizationId, userId)}/rank`, data);
  },

  getUserFunctions(organizationId: number, userId: number): Promise<ResolvedDutyFunctionDto[]> {
    return apiClient.get<ResolvedDutyFunctionDto[]>(
      `${orgUserBase(organizationId, userId)}/functions`,
    );
  },
};
