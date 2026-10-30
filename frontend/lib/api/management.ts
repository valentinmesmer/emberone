/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

// Self-service management endpoints (activations of the current user)

import type {
  ManagementActivationDto,
  ManagementActivationRequest,
  ManagementTargetOrganizationDto,
} from '@/types/management';
import type { OrganizationDto } from '@/types/organization';
import { apiClient } from './client';

export const managementApi = {
  async getTargets(): Promise<ManagementTargetOrganizationDto[]> {
    return apiClient.get<ManagementTargetOrganizationDto[]>(
      '/api/v1/self/management/organizations',
    );
  },

  /** Master data of a managed target; the backend answers "not found" without live rights. */
  async getTargetOrganization(organizationId: number): Promise<OrganizationDto> {
    return apiClient.get<OrganizationDto>(
      `/api/v1/self/management/organizations/${organizationId}`,
    );
  },

  async getActivations(): Promise<ManagementActivationDto[]> {
    return apiClient.get<ManagementActivationDto[]>('/api/v1/self/management/activations');
  },

  async activate(request: ManagementActivationRequest): Promise<ManagementActivationDto> {
    return apiClient.post<ManagementActivationDto>('/api/v1/self/management/activations', request);
  },

  async endActivation(activationId: number): Promise<void> {
    await apiClient.delete<void>(`/api/v1/self/management/activations/${activationId}`);
  },
};
