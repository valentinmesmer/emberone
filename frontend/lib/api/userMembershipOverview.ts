/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { OrganizationDto } from '@/types/organization';
import type { OrganizationMembershipSectionDto } from '@/types/user';
import { apiClient } from './client';

export const userMembershipOverviewApi = {
  async getMembershipOverview(
    organizationId: number,
    userId: number,
  ): Promise<OrganizationMembershipSectionDto> {
    return apiClient.get<OrganizationMembershipSectionDto>(
      `/api/v1/auth/organizations/${organizationId}/users/${userId}/membership-overview`,
    );
  },

  async getMainOrganizationChangeTargets(
    organizationId: number,
    userId: number,
  ): Promise<OrganizationDto[]> {
    return apiClient.get<OrganizationDto[]>(
      `/api/v1/auth/organizations/${organizationId}/users/${userId}/main-change-targets`,
    );
  },
};
