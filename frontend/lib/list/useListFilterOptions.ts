/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useQuery } from '@tanstack/react-query';
import { useEffect, useMemo } from 'react';
import type { ListControlsState } from '@/components/ui/list-filter';
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
  type ListFilterOptionsResponse,
  type ListFilterOptionsSourceId,
  listFilterOptionsQueryKey,
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
import { listRuntimeOptionKeys, pruneFiltersToRuntimeOptions } from './runtimeOptions';
import type { ListDefinition } from './types';

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
    queryKey: listFilterOptionsQueryKey(queryKeySource),
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
    staleTime: 60 * 1000,
  });
}

/** Merges API options with optional overrides; prunes stale filter values when options arrive. */
export function usePaginatedListRuntimeOptions({
  definition,
  state,
  organizationFilterOptions,
  organizationUserDocumentFilterOptions,
  organizationUserTimeEntryFilterOptions,
  organizationUserAbsenceFilterOptions,
  organizationUserWorkflowFilterOptions,
  organizationAppointmentFilterOptions,
  organizationAbsenceFilterOptions,
  organizationWorkflowFilterOptions,
  organizationCertificationFilterOptions,
  organizationCourseOfferingFilterOptions,
  organizationEventFilterOptions,
  runtimeOptions,
  enabled = true,
}: {
  definition: ListDefinition;
  state: ListControlsState;
  organizationFilterOptions?: OrganizationListFilterOptionsContext;
  organizationUserDocumentFilterOptions?: OrganizationUserDocumentListFilterOptionsContext;
  organizationUserTimeEntryFilterOptions?: OrganizationUserTimeEntryListFilterOptionsContext;
  organizationUserAbsenceFilterOptions?: OrganizationUserAbsenceListFilterOptionsContext;
  organizationUserWorkflowFilterOptions?: OrganizationUserWorkflowListFilterOptionsContext;
  organizationAppointmentFilterOptions?: OrganizationAppointmentListFilterOptionsContext;
  organizationAbsenceFilterOptions?: OrganizationAbsenceListFilterOptionsContext;
  organizationWorkflowFilterOptions?: OrganizationWorkflowListFilterOptionsContext;
  organizationCertificationFilterOptions?: OrganizationCertificationListFilterOptionsContext;
  organizationCourseOfferingFilterOptions?: OrganizationCourseOfferingListFilterOptionsContext;
  organizationEventFilterOptions?: OrganizationEventListFilterOptionsContext;
  runtimeOptions?: Record<string, readonly { value: string; label: string }[]>;
  enabled?: boolean;
}) {
  const { data } = useListFilterOptions(
    definition,
    {
      organization: organizationFilterOptions,
      organizationUserDocuments: organizationUserDocumentFilterOptions,
      organizationUserTimeEntries: organizationUserTimeEntryFilterOptions,
      organizationUserAbsences: organizationUserAbsenceFilterOptions,
      organizationUserWorkflows: organizationUserWorkflowFilterOptions,
      organizationAppointments: organizationAppointmentFilterOptions,
      organizationAbsences: organizationAbsenceFilterOptions,
      organizationWorkflows: organizationWorkflowFilterOptions,
      organizationCertifications: organizationCertificationFilterOptions,
      organizationCourseOfferings: organizationCourseOfferingFilterOptions,
      organizationEvents: organizationEventFilterOptions,
    },
    enabled,
  );

  const resolved = useMemo(() => {
    const fromApi: ListFilterOptionsResponse['options'] = data?.options ?? {};
    const merged: Record<string, readonly { value: string; label: string }[]> = {};
    for (const key of Object.keys(fromApi)) {
      merged[key] = fromApi[key] ?? [];
    }
    if (runtimeOptions) {
      for (const [key, options] of Object.entries(runtimeOptions)) {
        merged[key] = options;
      }
    }
    return Object.keys(merged).length > 0 ? merged : undefined;
  }, [data?.options, runtimeOptions]);

  useEffect(() => {
    if (!resolved) {
      return;
    }
    const pruned = pruneFiltersToRuntimeOptions(definition, state.filters, resolved);
    if (JSON.stringify(pruned) === JSON.stringify(state.filters)) {
      return;
    }
    for (const filter of definition.filters ?? []) {
      if (JSON.stringify(pruned[filter.key]) !== JSON.stringify(state.filters[filter.key])) {
        state.setFilter(filter.key, pruned[filter.key]);
      }
    }
  }, [definition, resolved, state.filters, state.setFilter]);

  return resolved;
}

export type { ListFilterOptionsSourceId };
