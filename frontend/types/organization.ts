/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { GroupMembershipType } from './enums/groupMembershipType';
import type { OrganizationGroupType } from './organizationGroup';

/**
 * Response DTO for organization data
 */
export interface OrganizationDto {
  id: number;
  organizationCode: string;
  name: string;
  organizationTypeId: number;
  parentOrganizationId?: number;
  main?: boolean;
}

/**
 * Request DTO for updating organization
 */
export interface UpdateOrganizationRequest {
  name: string;
  organizationCode: string;
}

/**
 * Request DTO for creating child organization
 */
export interface CreateChildOrganizationRequest {
  organizationCode: string;
  name: string;
  organizationTypeId: number;
}

/**
 * Group tree DTO
 */
export interface GroupTreeDto {
  id: number;
  name: string;
  description?: string;
  type: OrganizationGroupType;
}

/**
 * Organization tree DTO
 */
export interface OrganizationTreeDto {
  id: number;
  name: string;
  organizationCode: string;
  main?: boolean;
  groups: GroupTreeDto[];
  children?: OrganizationTreeDto[];
}

/** One organization membership interval of the current user (`endDate` null = open). */
export interface OrganizationMembershipPeriodDto {
  organizationId: number;
  organizationName: string;
  organizationCode: string;
  main: boolean;
  startDate: string;
  endDate: string | null;
  active: boolean;
}

/** One group membership interval of the current user (`endDate` null = open). */
export interface GroupMembershipPeriodDto {
  organizationId: number;
  organizationGroupId: number;
  organizationGroupName: string;
  membershipType: GroupMembershipType;
  startDate: string;
  endDate: string | null;
  active: boolean;
}

export interface SelfMembershipHistoryDto {
  organizations: OrganizationMembershipPeriodDto[];
  groups: GroupMembershipPeriodDto[];
}
