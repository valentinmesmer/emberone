/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { buildQuery } from '@/lib/api/paged';
import { apiClient } from './client';

export type ListFilterOptionDto = {
  value: string;
  label: string;
};

export type ListFilterOptionsResponse = {
  options: Record<string, ListFilterOptionDto[]>;
};

/** Registered per-list filter-options sources (fixed path or scoped; see fetch helpers). */
export type ListFilterOptionsSourceId =
  | 'admin/users'
  | 'admin/management-activations'
  | 'admin/tenants'
  | 'self/documents'
  | 'self/todos'
  | 'self/notifications'
  | 'self/workflows'
  | 'self/absences'
  | 'self/time-entries'
  | 'org/user-time-entries'
  | 'org/user-absences'
  | 'org/absences'
  | 'org/user-documents'
  | 'org/appointments'
  | 'org/service-events'
  | 'org/operation-events'
  | 'org/user-workflows'
  | 'org/workflows'
  | 'org/certifications'
  | 'org/course-offerings';

export type FixedListFilterOptionsSourceId = Exclude<
  ListFilterOptionsSourceId,
  | 'org/user-time-entries'
  | 'org/user-absences'
  | 'org/absences'
  | 'org/user-documents'
  | 'org/appointments'
  | 'org/service-events'
  | 'org/operation-events'
  | 'org/user-workflows'
  | 'org/workflows'
  | 'org/certifications'
  | 'org/course-offerings'
>;

const LIST_FILTER_OPTIONS_PATH: Record<FixedListFilterOptionsSourceId, string> = {
  'admin/users': '/api/v1/auth/admin/users/list-filter-options',
  'admin/management-activations': '/api/v1/auth/admin/management-activations/list-filter-options',
  'admin/tenants': '/api/v1/auth/admin/tenants/list-filter-options',
  'self/documents': '/api/v1/self/documents/list-filter-options',
  'self/todos': '/api/v1/self/todos/list-filter-options',
  'self/notifications': '/api/v1/self/notifications/list-filter-options',
  'self/workflows': '/api/v1/self/workflows/list-filter-options',
  'self/absences': '/api/v1/self/absences/list-filter-options',
  'self/time-entries': '/api/v1/self/time-entries/list-filter-options',
};

/** Legacy org workspace aggregate endpoint (until each org list has its own source). */
export type OrganizationListFilterOptionsContext = {
  scope: 'organization';
  organizationId: number;
};

/** Org user-management documents list (`…/users/{userId}/documents`). */
export type OrganizationUserDocumentListFilterOptionsContext = {
  scope: 'organizationUserDocuments';
  organizationId: number;
  userId: number;
};

export type OrganizationUserTimeEntryListFilterOptionsContext = {
  scope: 'organizationUserTimeEntries';
  organizationId: number;
  userId: number;
};

export type OrganizationUserAbsenceListFilterOptionsContext = {
  scope: 'organizationUserAbsences';
  organizationId: number;
  userId: number;
};

export type OrganizationUserWorkflowListFilterOptionsContext = {
  scope: 'organizationUserWorkflows';
  organizationId: number;
  userId: number;
};

/** Org department appointments list (`…/appointments`). */
export type OrganizationAppointmentListFilterOptionsContext = {
  scope: 'organizationAppointments';
  organizationId: number;
};

export type OrganizationAbsenceListFilterOptionsContext = {
  scope: 'organizationAbsences';
  organizationId: number;
};

export type OrganizationWorkflowListFilterOptionsContext = {
  scope: 'organizationWorkflows';
  organizationId: number;
};

export type OrganizationCertificationListFilterOptionsContext = {
  scope: 'organizationCertifications';
  organizationId: number;
};

export type OrganizationCourseOfferingListFilterOptionsContext = {
  scope: 'organizationCourseOfferings';
  organizationId: number;
};

export type OrganizationEventListFilterOptionsContext = {
  scope: 'organizationEvents';
  organizationId: number;
  category: 'service' | 'operation';
};

export type ListFilterOptionsQueryKeySource =
  | ListFilterOptionsSourceId
  | OrganizationListFilterOptionsContext
  | OrganizationUserDocumentListFilterOptionsContext
  | OrganizationUserTimeEntryListFilterOptionsContext
  | OrganizationUserAbsenceListFilterOptionsContext
  | OrganizationUserWorkflowListFilterOptionsContext
  | OrganizationAppointmentListFilterOptionsContext
  | OrganizationAbsenceListFilterOptionsContext
  | OrganizationWorkflowListFilterOptionsContext
  | OrganizationCertificationListFilterOptionsContext
  | OrganizationCourseOfferingListFilterOptionsContext
  | OrganizationEventListFilterOptionsContext;

export function listFilterOptionsQueryKey(
  source: ListFilterOptionsQueryKeySource,
): readonly unknown[] {
  if (typeof source === 'string') {
    return ['list-filter-options', source];
  }
  if (source.scope === 'organizationUserDocuments') {
    return [
      'list-filter-options',
      'organizationUserDocuments',
      source.organizationId,
      source.userId,
    ];
  }
  if (source.scope === 'organizationUserTimeEntries') {
    return [
      'list-filter-options',
      'organizationUserTimeEntries',
      source.organizationId,
      source.userId,
    ];
  }
  if (source.scope === 'organizationUserAbsences') {
    return [
      'list-filter-options',
      'organizationUserAbsences',
      source.organizationId,
      source.userId,
    ];
  }
  if (source.scope === 'organizationUserWorkflows') {
    return [
      'list-filter-options',
      'organizationUserWorkflows',
      source.organizationId,
      source.userId,
    ];
  }
  if (source.scope === 'organizationAppointments') {
    return ['list-filter-options', 'organizationAppointments', source.organizationId];
  }
  if (source.scope === 'organizationAbsences') {
    return ['list-filter-options', 'organizationAbsences', source.organizationId];
  }
  if (source.scope === 'organizationWorkflows') {
    return ['list-filter-options', 'organizationWorkflows', source.organizationId];
  }
  if (source.scope === 'organizationCertifications') {
    return ['list-filter-options', 'organizationCertifications', source.organizationId];
  }
  if (source.scope === 'organizationEvents') {
    return ['list-filter-options', 'organizationEvents', source.organizationId, source.category];
  }
  return ['list-filter-options', 'organization', source.organizationId];
}

export async function fetchListFilterOptionsForSource(
  source: FixedListFilterOptionsSourceId,
): Promise<ListFilterOptionsResponse> {
  return apiClient.get<ListFilterOptionsResponse>(LIST_FILTER_OPTIONS_PATH[source]);
}

export async function fetchOrganizationUserDocumentListFilterOptions(
  organizationId: number,
  userId: number,
): Promise<ListFilterOptionsResponse> {
  return apiClient.get<ListFilterOptionsResponse>(
    `/api/v1/auth/organizations/${organizationId}/users/${userId}/documents/list-filter-options`,
  );
}

export async function fetchOrganizationUserTimeEntryListFilterOptions(
  organizationId: number,
  userId: number,
): Promise<ListFilterOptionsResponse> {
  return apiClient.get<ListFilterOptionsResponse>(
    `/api/v1/auth/organizations/${organizationId}/users/${userId}/time-entries/list-filter-options`,
  );
}

export async function fetchOrganizationUserAbsenceListFilterOptions(
  organizationId: number,
  userId: number,
): Promise<ListFilterOptionsResponse> {
  return apiClient.get<ListFilterOptionsResponse>(
    `/api/v1/auth/organizations/${organizationId}/users/${userId}/absences/list-filter-options`,
  );
}

export async function fetchOrganizationUserWorkflowListFilterOptions(
  organizationId: number,
  userId: number,
): Promise<ListFilterOptionsResponse> {
  return apiClient.get<ListFilterOptionsResponse>(
    `/api/v1/auth/organizations/${organizationId}/users/${userId}/workflows/list-filter-options`,
  );
}

export async function fetchOrganizationAppointmentListFilterOptions(
  organizationId: number,
): Promise<ListFilterOptionsResponse> {
  return apiClient.get<ListFilterOptionsResponse>(
    `/api/v1/auth/organizations/${organizationId}/appointments/list-filter-options`,
  );
}

export async function fetchOrganizationAbsenceListFilterOptions(
  organizationId: number,
): Promise<ListFilterOptionsResponse> {
  return apiClient.get<ListFilterOptionsResponse>(
    `/api/v1/auth/organizations/${organizationId}/user-absences/active/list-filter-options`,
  );
}

export async function fetchOrganizationWorkflowListFilterOptions(
  organizationId: number,
): Promise<ListFilterOptionsResponse> {
  return apiClient.get<ListFilterOptionsResponse>(
    `/api/v1/auth/organizations/${organizationId}/workflows/open/list-filter-options`,
  );
}

export async function fetchOrganizationCertificationListFilterOptions(
  organizationId: number,
): Promise<ListFilterOptionsResponse> {
  return apiClient.get<ListFilterOptionsResponse>(
    `/api/v1/auth/organizations/${organizationId}/certifications/status-overview/list-filter-options`,
  );
}

export async function fetchOrganizationCourseOfferingListFilterOptions(
  organizationId: number,
): Promise<ListFilterOptionsResponse> {
  return apiClient.get<ListFilterOptionsResponse>(
    `/api/v1/auth/organizations/${organizationId}/course-offerings/list-filter-options`,
  );
}

export async function fetchOrganizationEventListFilterOptions(
  organizationId: number,
  category: 'service' | 'operation',
): Promise<ListFilterOptionsResponse> {
  return apiClient.get<ListFilterOptionsResponse>(
    `/api/v1/auth/organizations/${organizationId}/events/${category}/list-filter-options`,
  );
}

export async function fetchOrganizationListFilterOptions(
  organizationId: number,
  include: readonly string[],
): Promise<ListFilterOptionsResponse> {
  const qs = buildQuery({ include: [...include] });
  return apiClient.get<ListFilterOptionsResponse>(
    `/api/v1/auth/organizations/${organizationId}/list-filter-options?${qs}`,
  );
}
