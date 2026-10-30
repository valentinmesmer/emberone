/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { apiClient } from '@/lib/api/client';
import { buildQuery } from '@/lib/api/paged';
import type { ListQuery } from '@/lib/list/types';
import type { PageResponse } from '@/types/page';

export interface TrainingCourseTypeDto {
  id: number;
  name: string;
  active: boolean;
  configKey: string;
}

export interface TrainingCourseGrantedTypeDto {
  tenantTrainingCourseTypeId: number;
  courseTypeName: string | null;
  typeArchived: boolean;
}

export interface TrainingCourseUserRecordDto {
  id: number;
  userId: number;
  name: string;
  startDate: string;
  endDate: string;
  location: string | null;
  hours: number | null;
  userDocumentIds: number[];
  grantedTypes: TrainingCourseGrantedTypeDto[];
  notes: string | null;
  appLinkType: string | null;
  appLinkId: number | null;
  navigationPath: string | null;
}

export interface OrgTrainingCourseCatalogDto {
  organizationId: number;
  types: TrainingCourseTypeDto[];
}

export interface CreateTrainingCourseUserRecordRequest {
  tenantTrainingCourseTypeIds: number[];
  name?: string | null;
  startDate: string;
  endDate: string;
  location?: string | null;
  hours?: number | null;
  userDocumentIds?: number[];
  notes?: string | null;
}

export interface UpdateTrainingCourseUserRecordRequest {
  name?: string | null;
  startDate: string;
  endDate: string;
  location: string;
  hours?: number | null;
  notes?: string | null;
}

export type TrainingCourseUserRecordPageDto = PageResponse<TrainingCourseUserRecordDto>;

export type TrainingCourseRecordListRequest = ListQuery;

export interface TrainingCoursePendingSubmissionDto {
  workflowId: number;
  name: string | null;
  tenantTrainingCourseTypeIds: number[];
  courseTypeNames: string[];
  startDate: string;
  endDate: string;
  location: string | null;
  hours: number | null;
}

const selfBase = '/api/v1/self/training-courses';
const orgBase = (organizationId: number, userId: number) =>
  `/api/v1/auth/organizations/${organizationId}/users/${userId}/training-courses`;

export const trainingCoursesApi = {
  getMyRecords(
    query: TrainingCourseRecordListRequest = {},
  ): Promise<TrainingCourseUserRecordPageDto> {
    const qs = buildQuery({
      page: query.page ?? 0,
      size: query.size,
      search: query.search,
      sort: query.sort,
    });
    const base = `${selfBase}/records`;
    return apiClient.get<TrainingCourseUserRecordPageDto>(qs ? `${base}?${qs}` : base);
  },

  getMyAvailableTypes(): Promise<TrainingCourseTypeDto[]> {
    return apiClient.get<TrainingCourseTypeDto[]>(`${selfBase}/available-types`);
  },

  getUserRecords(
    organizationId: number,
    userId: number,
    query: TrainingCourseRecordListRequest = {},
  ): Promise<TrainingCourseUserRecordPageDto> {
    const qs = buildQuery({
      page: query.page ?? 0,
      size: query.size,
      search: query.search,
      sort: query.sort,
    });
    const base = `${orgBase(organizationId, userId)}/records`;
    return apiClient.get<TrainingCourseUserRecordPageDto>(qs ? `${base}?${qs}` : base);
  },

  getPendingSubmissions(
    organizationId: number,
    userId: number,
  ): Promise<TrainingCoursePendingSubmissionDto[]> {
    return apiClient.get<TrainingCoursePendingSubmissionDto[]>(
      `${orgBase(organizationId, userId)}/pending-submissions`,
    );
  },

  createRecord(
    organizationId: number,
    userId: number,
    data: CreateTrainingCourseUserRecordRequest,
  ): Promise<TrainingCourseUserRecordDto> {
    return apiClient.post<TrainingCourseUserRecordDto>(
      `${orgBase(organizationId, userId)}/records`,
      data,
    );
  },

  updateRecord(
    organizationId: number,
    userId: number,
    recordId: number,
    data: UpdateTrainingCourseUserRecordRequest,
  ): Promise<TrainingCourseUserRecordDto> {
    return apiClient.put<TrainingCourseUserRecordDto>(
      `${orgBase(organizationId, userId)}/records/${recordId}`,
      data,
    );
  },

  deleteRecord(organizationId: number, userId: number, recordId: number): Promise<void> {
    return apiClient.delete<void>(`${orgBase(organizationId, userId)}/records/${recordId}`);
  },

  getAvailableTypes(organizationId: number, userId: number): Promise<OrgTrainingCourseCatalogDto> {
    return apiClient.get<OrgTrainingCourseCatalogDto>(
      `${orgBase(organizationId, userId)}/available-types`,
    );
  },
};
