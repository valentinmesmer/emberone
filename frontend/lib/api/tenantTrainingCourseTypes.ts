/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { apiClient } from './client';

export interface TenantTrainingCourseTypeDto {
  id: number;
  configKey: string;
  abbreviation: string;
  name: string;
  active: boolean;
  archived: boolean;
  deletable: boolean;
  archivable: boolean;
}

export interface CreateTenantTrainingCourseTypeRequest {
  abbreviation: string;
  name: string;
  active: boolean;
}

export type UpdateTenantTrainingCourseTypeRequest = Pick<
  CreateTenantTrainingCourseTypeRequest,
  'name' | 'abbreviation' | 'active'
>;

function base(tenantId: number) {
  return `/api/v1/auth/admin/tenants/${tenantId}/training-course-types`;
}

export const tenantTrainingCourseTypesApi = {
  list(tenantId: number): Promise<TenantTrainingCourseTypeDto[]> {
    return apiClient.get<TenantTrainingCourseTypeDto[]>(base(tenantId));
  },

  get(tenantId: number, configKey: string): Promise<TenantTrainingCourseTypeDto> {
    return apiClient.get<TenantTrainingCourseTypeDto>(
      `${base(tenantId)}/${encodeURIComponent(configKey)}`,
    );
  },

  create(
    tenantId: number,
    data: CreateTenantTrainingCourseTypeRequest,
  ): Promise<TenantTrainingCourseTypeDto> {
    return apiClient.post<TenantTrainingCourseTypeDto>(base(tenantId), data);
  },

  update(
    tenantId: number,
    configKey: string,
    data: UpdateTenantTrainingCourseTypeRequest,
  ): Promise<TenantTrainingCourseTypeDto> {
    return apiClient.put<TenantTrainingCourseTypeDto>(
      `${base(tenantId)}/${encodeURIComponent(configKey)}`,
      data,
    );
  },

  delete(tenantId: number, configKey: string): Promise<void> {
    return apiClient.delete(`${base(tenantId)}/${encodeURIComponent(configKey)}`);
  },

  archive(tenantId: number, configKey: string): Promise<void> {
    return apiClient.post(`${base(tenantId)}/${encodeURIComponent(configKey)}/archive`, {});
  },

  unarchive(tenantId: number, configKey: string): Promise<void> {
    return apiClient.post(`${base(tenantId)}/${encodeURIComponent(configKey)}/unarchive`, {});
  },
};
