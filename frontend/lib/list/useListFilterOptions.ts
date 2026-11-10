/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useEffect, useMemo } from 'react';
import type { ListControlsState } from '@/components/ui/list-filter';
import type {
  ListFilterOptionsResponse,
  ListFilterOptionsSourceId,
  OrganizationAbsenceListFilterOptionsContext,
  OrganizationAppointmentListFilterOptionsContext,
  OrganizationCertificationListFilterOptionsContext,
  OrganizationCourseOfferingListFilterOptionsContext,
  OrganizationEventListFilterOptionsContext,
  OrganizationListFilterOptionsContext,
  OrganizationUserAbsenceListFilterOptionsContext,
  OrganizationUserDocumentListFilterOptionsContext,
  OrganizationUserTimeEntryListFilterOptionsContext,
  OrganizationUserWorkflowListFilterOptionsContext,
  OrganizationWorkflowListFilterOptionsContext,
} from '@/lib/api/listFilterOptions';
import { useListFilterOptions } from '@/lib/store/queries/listFilterOptions';
import { pruneFiltersToRuntimeOptions } from './runtimeOptions';
import type { ListDefinition } from './types';

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
