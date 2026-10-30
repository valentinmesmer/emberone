/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

// User Organization Group Membership API endpoints

import type {
  CreateUserOrganizationGroupMembershipRequest,
  UpdateUserOrganizationGroupMembershipRequest,
  UserOrganizationGroupMembershipDto,
} from '@/types/user';
import { apiClient } from './client';

export const userOrganizationGroupMembershipApi = {
  /**
   * Create a user organization group membership
   */
  async createMembership(
    data: CreateUserOrganizationGroupMembershipRequest,
  ): Promise<UserOrganizationGroupMembershipDto> {
    return apiClient.post<UserOrganizationGroupMembershipDto>(
      '/api/v1/auth/users/organization-group-memberships',
      data,
    );
  },

  /**
   * Update a membership
   */
  async updateMembership(
    membershipId: number,
    data: UpdateUserOrganizationGroupMembershipRequest,
  ): Promise<UserOrganizationGroupMembershipDto> {
    return apiClient.put<UserOrganizationGroupMembershipDto>(
      `/api/v1/auth/users/organization-group-memberships/${membershipId}`,
      data,
    );
  },
};
