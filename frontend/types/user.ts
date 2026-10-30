/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { GroupMembershipType } from './enums/groupMembershipType';
import type { Salutation } from './enums/salutation';
import type { OrganizationDto } from './organization';
import type { OrganizationGroupDto } from './organizationGroup';
import type { PageResponse } from './page';

/**
 * Request DTO for creating a user in an organization
 */
export interface CreateUserRequest {
  salutation: Salutation;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  privateEmail: string;
  street?: string;
  city?: string;
  postalCode?: string;
  country?: string;
}

/**
 * Request DTO for updating user personal data (admin only)
 */
export interface UpdatePersonalDataRequest {
  salutation: Salutation;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
}

/**
 * User contact information DTO (notification email + address)
 */
export interface UserContactInformationDto {
  id: number;
  userId: number;
  privateEmail: string;
  pendingEmail?: string | null;
  street?: string;
  city?: string;
  postalCode?: string;
  country?: string;
}

/**
 * User organization membership DTO
 */
export interface UserOrganizationMembershipDto {
  id: number;
  userId: number;
  main: boolean;
  startDate: string;
  endDate?: string;
  organization?: OrganizationDto;
}

/**
 * Request DTO for updating a user organization membership
 */
export interface UpdateUserOrganizationMembershipRequest {
  startDate: string;
}

/**
 * Request DTO for ending a user organization membership
 */
export interface EndUserOrganizationMembershipRequest {
  reason?: string;
}

/**
 * User organization group membership DTO
 */
export interface UserOrganizationGroupMembershipDto {
  id: number;
  userId: number;
  organizationGroupId: number;
  startDate: string;
  endDate?: string;
  membershipType: GroupMembershipType;
}

/**
 * Request DTO for creating a user organization group membership
 */
export interface CreateUserOrganizationGroupMembershipRequest {
  userId: number;
  organizationGroupId: number;
  startDate: string;
  endDate?: string;
  membershipType: GroupMembershipType;
}

/**
 * Request DTO for updating a user organization group membership
 */
export interface UpdateUserOrganizationGroupMembershipRequest {
  startDate: string;
  endDate?: string;
  membershipType: GroupMembershipType;
}

/**
 * Group memberships for one organization group (active + history rows).
 */
export interface GroupMembershipSectionDto {
  group: OrganizationGroupDto;
  memberships: UserOrganizationGroupMembershipDto[];
}

/**
 * Membership overview for a user within one organization.
 */
export interface OrganizationMembershipSectionDto {
  organization: OrganizationDto;
  canWrite: boolean;
  inLeadershipGroup: boolean;
  canRequestOrganizationMembershipEnd: boolean;
  canRequestOrganizationUserMainOrganizationChange: boolean;
  organizationMemberships: UserOrganizationMembershipDto[];
  groups: GroupMembershipSectionDto[];
}

/**
 * Response DTO for user data
 */
export interface UserDto {
  id: number;
  personalNumber: number;
  salutation?: Salutation;
  firstName: string;
  lastName: string;
  username?: string;
  dateOfBirth?: string;
  contactInformation?: UserContactInformationDto;
  organizationMemberships?: UserOrganizationMembershipDto[];
  organizationGroupMemberships?: UserOrganizationGroupMembershipDto[];
}

export type UserPageDto = PageResponse<UserDto>;
