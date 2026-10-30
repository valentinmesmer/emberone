/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { apiClient } from './client';

export interface OrganizationTypeDto {
  id: number;
  name: string;
  active: boolean;
}

export interface CreateOrganizationTypeRequest {
  name: string;
}

export interface UpdateOrganizationTypeRequest {
  name?: string;
  active?: boolean;
}

const base = (tenantId: number) => `/api/v1/auth/admin/tenants/${tenantId}/organization-types`;

export const organizationTypesApi = {
  list(tenantId: number): Promise<OrganizationTypeDto[]> {
    return apiClient.get<OrganizationTypeDto[]>(base(tenantId));
  },

  create(tenantId: number, data: CreateOrganizationTypeRequest): Promise<OrganizationTypeDto> {
    return apiClient.post<OrganizationTypeDto>(base(tenantId), data);
  },

  update(
    tenantId: number,
    id: number,
    data: UpdateOrganizationTypeRequest,
  ): Promise<OrganizationTypeDto> {
    return apiClient.patch<OrganizationTypeDto>(`${base(tenantId)}/${id}`, data);
  },

  remove(tenantId: number, id: number): Promise<void> {
    return apiClient.delete<void>(`${base(tenantId)}/${id}`);
  },
};
