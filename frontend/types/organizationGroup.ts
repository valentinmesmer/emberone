/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { PageResponse } from './page';

export type OrganizationGroupType = 'LEADERSHIP' | 'ALL_MEMBERS' | 'CUSTOM';

export interface OrganizationGroupDto {
  id: number;
  organizationId: number;
  name: string;
  description?: string;
  type: OrganizationGroupType;
  archived?: boolean;
  archivedAt?: string | null;
}

export interface OrganizationGroupMemberDto {
  id: number;
  firstName: string;
  lastName: string;
}

export type OrganizationGroupPageDto = PageResponse<OrganizationGroupDto>;
export type OrganizationGroupMemberPageDto = PageResponse<OrganizationGroupMemberDto>;

export interface OrganizationGroupLeaveTargetDto {
  organizationId: number;
  organizationName: string;
  organizationGroupId: number;
  organizationGroupName: string;
  organizationGroupType?: OrganizationGroupType;
}

export interface OrganizationGroupJoinTargetDto {
  organizationId: number;
  organizationName: string;
  organizationGroupId: number;
  organizationGroupName: string;
  organizationGroupType?: OrganizationGroupType;
}

/** Groups that may appear as self-service leave-request options. Leadership cannot be left this way. */
export function isLeaveRequestableOrganizationGroup(
  type: OrganizationGroupType | undefined,
): boolean {
  return type !== 'LEADERSHIP' && type !== 'ALL_MEMBERS';
}

/** Groups that may appear as self-service join-request options. Leadership cannot be joined this way. */
export function isJoinRequestableOrganizationGroup(
  type: OrganizationGroupType | undefined,
): boolean {
  return type !== 'LEADERSHIP' && type !== 'ALL_MEMBERS';
}
