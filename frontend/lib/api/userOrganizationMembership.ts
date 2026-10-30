/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

// User Organization Membership API endpoints

import type {
  UpdateUserOrganizationMembershipRequest,
  UserOrganizationMembershipDto,
} from '@/types/user';
import { apiClient } from './client';

export const userOrganizationMembershipApi = {
  async updateMembership(
    membershipId: number,
    data: UpdateUserOrganizationMembershipRequest,
  ): Promise<UserOrganizationMembershipDto> {
    return apiClient.put<UserOrganizationMembershipDto>(
      `/api/v1/auth/users/organization-memberships/${membershipId}`,
      data,
    );
  },
};
