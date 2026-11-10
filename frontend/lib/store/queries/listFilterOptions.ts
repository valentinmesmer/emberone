/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import {
  fetchListFilterOptionsForSource,
  fetchOrganizationAbsenceListFilterOptions,
  fetchOrganizationAppointmentListFilterOptions,
  fetchOrganizationCertificationListFilterOptions,
  fetchOrganizationCourseOfferingListFilterOptions,
  fetchOrganizationEventListFilterOptions,
  fetchOrganizationListFilterOptions,
  fetchOrganizationUserAbsenceListFilterOptions,
  fetchOrganizationUserDocumentListFilterOptions,
  fetchOrganizationUserTimeEntryListFilterOptions,
  fetchOrganizationUserWorkflowListFilterOptions,
  fetchOrganizationWorkflowListFilterOptions,
  type ListFilterOptionsQueryKeySource,
  type OrganizationAbsenceListFilterOptionsContext,
  type OrganizationAppointmentListFilterOptionsContext,
  type OrganizationCertificationListFilterOptionsContext,
  type OrganizationCourseOfferingListFilterOptionsContext,
  type OrganizationEventListFilterOptionsContext,
  type OrganizationListFilterOptionsContext,
  type OrganizationUserAbsenceListFilterOptionsContext,
  type OrganizationUserDocumentListFilterOptionsContext,
  type OrganizationUserTimeEntryListFilterOptionsContext,
  type OrganizationUserWorkflowListFilterOptionsContext,
  type OrganizationWorkflowListFilterOptionsContext,
} from '@/lib/api/listFilterOptions';
import { listRuntimeOptionKeys } from '@/lib/list/runtimeOptions';
import type { ListDefinition } from '@/lib/list/types';
import { STALE_TIME } from '@/lib/store/staleTime';

// React Query keys and hook for list filter options (server state)

export const listFilterOptionsKeys = {
  all: ['list-filter-options'] as const,
  source: (source: ListFilterOptionsQueryKeySource) => {
    if (typeof source === 'string') {
      return [...listFilterOptionsKeys.all, source] as const;
    }
    switch (source.scope) {
      case 'organizationUserDocuments':
      case 'organizationUserTimeEntries':
      case 'organizationUserAbsences':
      case 'organizationUserWorkflows':
        return [
          ...listFilterOptionsKeys.all,
          source.scope,
          source.organizationId,
          source.userId,
        ] as const;
      case 'organizationAppointments':
      case 'organizationAbsences':
      case 'organizationWorkflows':
      case 'organizationCertifications':
      case 'organizationCourseOfferings':
      case 'organization':
        return [...listFilterOptionsKeys.all, source.scope, source.organizationId] as const;
      case 'organizationEvents':
        return [
          ...listFilterOptionsKeys.all,
          source.scope,
          source.organizationId,
          source.category,
        ] as const;
    }
  },
};

export type ListFilterOptionsLoadContext = {
  organization?: OrganizationListFilterOptionsContext;
  organizationUserDocuments?: OrganizationUserDocumentListFilterOptionsContext;
  organizationUserTimeEntries?: OrganizationUserTimeEntryListFilterOptionsContext;
  organizationUserAbsences?: OrganizationUserAbsenceListFilterOptionsContext;
  organizationUserWorkflows?: OrganizationUserWorkflowListFilterOptionsContext;
  organizationAppointments?: OrganizationAppointmentListFilterOptionsContext;
  organizationAbsences?: OrganizationAbsenceListFilterOptionsContext;
  organizationWorkflows?: OrganizationWorkflowListFilterOptionsContext;
  organizationCertifications?: OrganizationCertificationListFilterOptionsContext;
  organizationCourseOfferings?: OrganizationCourseOfferingListFilterOptionsContext;
  organizationEvents?: OrganizationEventListFilterOptionsContext;
};

/**
 * Loads filter options for a list with {@link ListDefinition.filterOptionsSource} (or org
 * aggregate context when no source is set).
 */
export function useListFilterOptions(
  definition: ListDefinition,
  loadContext?: ListFilterOptionsLoadContext,
  enabled = true,
) {
  const keys = useMemo(() => listRuntimeOptionKeys(definition), [definition]);
  const source = definition.filterOptionsSource;
  const orgUserDocs = loadContext?.organizationUserDocuments;
  const orgUserTimeEntries = loadContext?.organizationUserTimeEntries;
  const orgUserAbsences = loadContext?.organizationUserAbsences;
  const orgUserWorkflows = loadContext?.organizationUserWorkflows;
  const orgAppointments = loadContext?.organizationAppointments;
  const orgAbsences = loadContext?.organizationAbsences;
  const orgWorkflows = loadContext?.organizationWorkflows;
  const orgCertifications = loadContext?.organizationCertifications;
  const orgCourseOfferings = loadContext?.organizationCourseOfferings;
  const orgEvents = loadContext?.organizationEvents;
  const organizationContext = loadContext?.organization;

  const resolvedEnabled = useMemo(() => {
    if (!enabled || keys.length === 0) {
      return false;
    }
    if (source === 'org/user-documents') {
      return orgUserDocs != null;
    }
    if (source === 'org/user-time-entries') {
      return orgUserTimeEntries != null;
    }
    if (source === 'org/user-absences') {
      return orgUserAbsences != null;
    }
    if (source === 'org/user-workflows') {
      return orgUserWorkflows != null;
    }
    if (source === 'org/appointments') {
      return orgAppointments != null;
    }
    if (source === 'org/absences') {
      return orgAbsences != null;
    }
    if (source === 'org/workflows') {
      return orgWorkflows != null;
    }
    if (source === 'org/certifications') {
      return orgCertifications != null;
    }
    if (source === 'org/course-offerings') {
      return orgCourseOfferings != null;
    }
    if (source === 'org/service-events' || source === 'org/operation-events') {
      return orgEvents != null;
    }
    if (source != null) {
      return true;
    }
    return organizationContext != null && organizationContext.scope === 'organization';
  }, [
    enabled,
    keys.length,
    source,
    orgUserDocs,
    orgUserTimeEntries,
    orgUserAbsences,
    orgUserWorkflows,
    orgAppointments,
    orgAbsences,
    orgWorkflows,
    orgCertifications,
    orgCourseOfferings,
    orgEvents,
    organizationContext,
  ]);

  const queryKeySource = useMemo(() => {
    if (source === 'org/user-documents' && orgUserDocs) {
      return orgUserDocs;
    }
    if (source === 'org/user-time-entries' && orgUserTimeEntries) {
      return orgUserTimeEntries;
    }
    if (source === 'org/user-absences' && orgUserAbsences) {
      return orgUserAbsences;
    }
    if (source === 'org/user-workflows' && orgUserWorkflows) {
      return orgUserWorkflows;
    }
    if (source === 'org/appointments' && orgAppointments) {
      return orgAppointments;
    }
    if (source === 'org/absences' && orgAbsences) {
      return orgAbsences;
    }
    if (source === 'org/workflows' && orgWorkflows) {
      return orgWorkflows;
    }
    if (source === 'org/certifications' && orgCertifications) {
      return orgCertifications;
    }
    if (source === 'org/course-offerings' && orgCourseOfferings) {
      return orgCourseOfferings;
    }
    if ((source === 'org/service-events' || source === 'org/operation-events') && orgEvents) {
      return orgEvents;
    }
    if (
      source &&
      source !== 'org/user-documents' &&
      source !== 'org/user-time-entries' &&
      source !== 'org/user-absences' &&
      source !== 'org/user-workflows' &&
      source !== 'org/appointments' &&
      source !== 'org/absences' &&
      source !== 'org/workflows' &&
      source !== 'org/certifications' &&
      source !== 'org/course-offerings' &&
      source !== 'org/service-events' &&
      source !== 'org/operation-events'
    ) {
      return source;
    }
    return (
      organizationContext ??
      ({
        scope: 'organization',
        organizationId: 0,
      } satisfies OrganizationListFilterOptionsContext)
    );
  }, [
    source,
    orgUserDocs,
    orgUserTimeEntries,
    orgUserAbsences,
    orgUserWorkflows,
    orgAppointments,
    orgAbsences,
    orgWorkflows,
    orgCertifications,
    orgCourseOfferings,
    orgEvents,
    organizationContext,
  ]);

  return useQuery({
    queryKey: listFilterOptionsKeys.source(queryKeySource),
    queryFn: () => {
      if (source === 'org/user-documents' && orgUserDocs) {
        return fetchOrganizationUserDocumentListFilterOptions(
          orgUserDocs.organizationId,
          orgUserDocs.userId,
        );
      }
      if (source === 'org/user-time-entries' && orgUserTimeEntries) {
        return fetchOrganizationUserTimeEntryListFilterOptions(
          orgUserTimeEntries.organizationId,
          orgUserTimeEntries.userId,
        );
      }
      if (source === 'org/user-absences' && orgUserAbsences) {
        return fetchOrganizationUserAbsenceListFilterOptions(
          orgUserAbsences.organizationId,
          orgUserAbsences.userId,
        );
      }
      if (source === 'org/user-workflows' && orgUserWorkflows) {
        return fetchOrganizationUserWorkflowListFilterOptions(
          orgUserWorkflows.organizationId,
          orgUserWorkflows.userId,
        );
      }
      if (source === 'org/appointments' && orgAppointments) {
        return fetchOrganizationAppointmentListFilterOptions(orgAppointments.organizationId);
      }
      if (source === 'org/absences' && orgAbsences) {
        return fetchOrganizationAbsenceListFilterOptions(orgAbsences.organizationId);
      }
      if (source === 'org/workflows' && orgWorkflows) {
        return fetchOrganizationWorkflowListFilterOptions(orgWorkflows.organizationId);
      }
      if (source === 'org/certifications' && orgCertifications) {
        return fetchOrganizationCertificationListFilterOptions(orgCertifications.organizationId);
      }
      if (source === 'org/course-offerings' && orgCourseOfferings) {
        return fetchOrganizationCourseOfferingListFilterOptions(orgCourseOfferings.organizationId);
      }
      if ((source === 'org/service-events' || source === 'org/operation-events') && orgEvents) {
        return fetchOrganizationEventListFilterOptions(
          orgEvents.organizationId,
          orgEvents.category,
        );
      }
      if (
        source &&
        source !== 'org/user-documents' &&
        source !== 'org/user-time-entries' &&
        source !== 'org/user-absences' &&
        source !== 'org/user-workflows' &&
        source !== 'org/appointments' &&
        source !== 'org/absences' &&
        source !== 'org/workflows' &&
        source !== 'org/certifications' &&
        source !== 'org/course-offerings' &&
        source !== 'org/service-events' &&
        source !== 'org/operation-events'
      ) {
        return fetchListFilterOptionsForSource(source);
      }
      if (organizationContext) {
        return fetchOrganizationListFilterOptions(organizationContext.organizationId, keys);
      }
      return Promise.resolve({ options: {} });
    },
    enabled: resolvedEnabled,
    staleTime: STALE_TIME.LIST,
  });
}
