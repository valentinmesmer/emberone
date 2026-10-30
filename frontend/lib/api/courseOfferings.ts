/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { buildQuery } from '@/lib/api/paged';
import type { TrainingCourseTypeDto } from '@/lib/api/trainingCourses';
import type { ListQuery } from '@/lib/list/types';
import type {
  CourseAudienceEntryRequest,
  CourseAudienceOptionsDto,
  CourseCompletionDto,
  CourseOfferingDto,
  CourseOfferingPageDto,
  CoursePrerequisiteCheckDto,
  CourseRegistrationDto,
  CourseRegistrationPageDto,
  CourseRegistrationSummaryDto,
  CourseSessionRequest,
  CreateCourseOfferingRequest,
  OrganizationCourseRegistrationPageDto,
  ReplaceCoursePrerequisitesRequest,
  SelfCourseOfferingDto,
  SelfCourseOfferingPageDto,
  UpdateCourseOfferingRequest,
  UserCourseRegistrationPageDto,
} from '@/types/courseOffering';
import { apiClient } from './client';

export type OrgCourseOfferingListRequest = ListQuery & {
  status?: string[];
  trainingCourseTypeId?: string[];
};

export type CourseRegistrationListRequest = ListQuery;

export type SelfCourseOfferingListRequest = ListQuery & {
  includeHeld?: boolean;
  includeUnfulfilled?: boolean;
};

function orgBase(organizationId: number): string {
  return `/api/v1/auth/organizations/${organizationId}/course-offerings`;
}

function withQuery(path: string, query: string): string {
  return query ? `${path}?${query}` : path;
}

export const courseOfferingsApi = {
  listForOrganization(
    organizationId: number,
    request: OrgCourseOfferingListRequest,
  ): Promise<CourseOfferingPageDto> {
    const query = buildQuery(request);
    return apiClient.get<CourseOfferingPageDto>(withQuery(orgBase(organizationId), query));
  },

  listTemplates(organizationId: number, request: ListQuery): Promise<CourseOfferingPageDto> {
    const query = buildQuery(request);
    return apiClient.get<CourseOfferingPageDto>(
      withQuery(`${orgBase(organizationId)}/templates`, query),
    );
  },

  listOfferableTypes(organizationId: number): Promise<TrainingCourseTypeDto[]> {
    return apiClient.get<TrainingCourseTypeDto[]>(`${orgBase(organizationId)}/offerable-types`);
  },

  listAudienceOptions(organizationId: number): Promise<CourseAudienceOptionsDto> {
    return apiClient.get<CourseAudienceOptionsDto>(`${orgBase(organizationId)}/audience-options`);
  },

  getForOrganization(organizationId: number, offeringId: number): Promise<CourseOfferingDto> {
    return apiClient.get<CourseOfferingDto>(`${orgBase(organizationId)}/${offeringId}`);
  },

  create(organizationId: number, data: CreateCourseOfferingRequest): Promise<CourseOfferingDto> {
    return apiClient.post<CourseOfferingDto>(orgBase(organizationId), data);
  },

  saveAsTemplate(
    organizationId: number,
    offeringId: number,
    templateName: string,
  ): Promise<CourseOfferingDto> {
    return apiClient.post<CourseOfferingDto>(
      `${orgBase(organizationId)}/${offeringId}/save-as-template`,
      { templateName },
    );
  },

  instantiateFromTemplate(
    organizationId: number,
    offeringId: number,
    firstSessionDate: string,
  ): Promise<CourseOfferingDto> {
    return apiClient.post<CourseOfferingDto>(
      `${orgBase(organizationId)}/${offeringId}/instantiate`,
      { firstSessionDate },
    );
  },

  update(
    organizationId: number,
    offeringId: number,
    data: UpdateCourseOfferingRequest,
  ): Promise<CourseOfferingDto> {
    return apiClient.put<CourseOfferingDto>(`${orgBase(organizationId)}/${offeringId}`, data);
  },

  delete(organizationId: number, offeringId: number): Promise<void> {
    return apiClient.delete<void>(`${orgBase(organizationId)}/${offeringId}`);
  },

  publish(organizationId: number, offeringId: number): Promise<CourseOfferingDto> {
    return apiClient.post<CourseOfferingDto>(
      `${orgBase(organizationId)}/${offeringId}/publish`,
      {},
    );
  },

  cancel(organizationId: number, offeringId: number, reason: string): Promise<CourseOfferingDto> {
    return apiClient.post<CourseOfferingDto>(`${orgBase(organizationId)}/${offeringId}/cancel`, {
      reason,
    });
  },

  replaceAudiences(
    organizationId: number,
    offeringId: number,
    entries: CourseAudienceEntryRequest[],
  ): Promise<CourseOfferingDto> {
    return apiClient.put<CourseOfferingDto>(`${orgBase(organizationId)}/${offeringId}/audiences`, {
      entries,
    });
  },

  replacePrerequisites(
    organizationId: number,
    offeringId: number,
    data: ReplaceCoursePrerequisitesRequest,
  ): Promise<CourseOfferingDto> {
    return apiClient.put<CourseOfferingDto>(
      `${orgBase(organizationId)}/${offeringId}/prerequisites`,
      data,
    );
  },

  addSession(
    organizationId: number,
    offeringId: number,
    data: CourseSessionRequest,
  ): Promise<CourseOfferingDto> {
    return apiClient.post<CourseOfferingDto>(
      `${orgBase(organizationId)}/${offeringId}/sessions`,
      data,
    );
  },

  updateSession(
    organizationId: number,
    offeringId: number,
    sessionId: number,
    data: CourseSessionRequest,
  ): Promise<CourseOfferingDto> {
    return apiClient.put<CourseOfferingDto>(
      `${orgBase(organizationId)}/${offeringId}/sessions/${sessionId}`,
      data,
    );
  },

  deleteSession(
    organizationId: number,
    offeringId: number,
    sessionId: number,
  ): Promise<CourseOfferingDto> {
    return apiClient.delete<CourseOfferingDto>(
      `${orgBase(organizationId)}/${offeringId}/sessions/${sessionId}`,
    );
  },

  listRegistrations(
    organizationId: number,
    offeringId: number,
    request: CourseRegistrationListRequest,
  ): Promise<CourseRegistrationPageDto> {
    const query = buildQuery(request);
    return apiClient.get<CourseRegistrationPageDto>(
      withQuery(`${orgBase(organizationId)}/${offeringId}/registrations`, query),
    );
  },

  getRegistration(
    organizationId: number,
    offeringId: number,
    registrationId: number,
  ): Promise<CourseRegistrationDto> {
    return apiClient.get<CourseRegistrationDto>(
      `${orgBase(organizationId)}/${offeringId}/registrations/${registrationId}`,
    );
  },

  registrationSummary(
    organizationId: number,
    offeringId: number,
  ): Promise<CourseRegistrationSummaryDto> {
    return apiClient.get<CourseRegistrationSummaryDto>(
      `${orgBase(organizationId)}/${offeringId}/registrations/summary`,
    );
  },

  promoteRegistration(
    organizationId: number,
    offeringId: number,
    registrationId: number,
  ): Promise<CourseRegistrationDto> {
    return apiClient.post<CourseRegistrationDto>(
      `${orgBase(organizationId)}/${offeringId}/registrations/${registrationId}/promote`,
      {},
    );
  },

  removeRegistration(
    organizationId: number,
    offeringId: number,
    registrationId: number,
    reason: string,
  ): Promise<CourseRegistrationDto> {
    return apiClient.post<CourseRegistrationDto>(
      `${orgBase(organizationId)}/${offeringId}/registrations/${registrationId}/remove`,
      { reason },
    );
  },

  getCompletion(organizationId: number, offeringId: number): Promise<CourseCompletionDto> {
    return apiClient.get<CourseCompletionDto>(
      `${orgBase(organizationId)}/${offeringId}/completion`,
    );
  },

  setFinalResult(
    organizationId: number,
    offeringId: number,
    registrationId: number,
    passed: boolean | null,
  ): Promise<CourseCompletionDto> {
    return apiClient.put<CourseCompletionDto>(
      `${orgBase(organizationId)}/${offeringId}/registrations/${registrationId}/final-result`,
      { passed },
    );
  },

  completeOffering(organizationId: number, offeringId: number): Promise<CourseCompletionDto> {
    return apiClient.post<CourseCompletionDto>(
      `${orgBase(organizationId)}/${offeringId}/complete`,
      {},
    );
  },

  listSelfOfferings(request: SelfCourseOfferingListRequest): Promise<SelfCourseOfferingPageDto> {
    const query = buildQuery(request);
    return apiClient.get<SelfCourseOfferingPageDto>(
      withQuery('/api/v1/self/course-offerings', query),
    );
  },

  getSelfOffering(offeringId: number): Promise<SelfCourseOfferingDto> {
    return apiClient.get(`/api/v1/self/course-offerings/${offeringId}`);
  },

  checkSelfPrerequisites(offeringId: number): Promise<CoursePrerequisiteCheckDto> {
    return apiClient.get<CoursePrerequisiteCheckDto>(
      `/api/v1/self/course-offerings/${offeringId}/prerequisites`,
    );
  },

  withdrawFromWaitlist(registrationId: number): Promise<void> {
    return apiClient.post<void>(`/api/v1/self/course-registrations/${registrationId}/withdraw`, {});
  },

  listOrganizationRegistrations(
    organizationId: number,
    request: CourseRegistrationListRequest,
  ): Promise<OrganizationCourseRegistrationPageDto> {
    const query = buildQuery(request);
    return apiClient.get<OrganizationCourseRegistrationPageDto>(
      withQuery(`/api/v1/auth/organizations/${organizationId}/course-registrations`, query),
    );
  },

  listUserRegistrations(
    organizationId: number,
    userId: number,
    request: CourseRegistrationListRequest,
  ): Promise<UserCourseRegistrationPageDto> {
    const query = buildQuery(request);
    return apiClient.get<UserCourseRegistrationPageDto>(
      withQuery(
        `/api/v1/auth/organizations/${organizationId}/users/${userId}/course-registrations`,
        query,
      ),
    );
  },
};
