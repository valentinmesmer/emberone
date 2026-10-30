/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type { ReactNode } from 'react';
import {
  ListControls,
  type ListControlsProps,
  type ListControlsState,
  ListLoadMore,
  ListNoResults,
} from '@/components/ui/list-filter';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import type {
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
import type { ListDefinition } from '@/lib/list';
import { usePaginatedListRuntimeOptions } from '@/lib/list/useListFilterOptions';
import { cn } from '@/lib/utils/cn';

export type PaginatedListViewProps = {
  definition: ListDefinition;
  state: ListControlsState;
  /** Flattened pages from `usePagedList` (or `data` when not infinite). */
  totalItems: number;
  /** First load; search and filters stay visible. */
  isPending?: boolean;
  /** Shown below controls when `isPending` is false and the query failed. */
  error?: ReactNode;
  /** When there are no rows and search/sheet filters are inactive. */
  emptyState: ReactNode;
  hasNextPage?: boolean;
  isFetchingNextPage?: boolean;
  onLoadMore?: () => void;
  children: ReactNode;
  className?: string;
  /**
   * Until each org list has {@link ListDefinition.filterOptionsSource}, use the aggregate org
   * options endpoint (same permission as the list GET will use).
   */
  organizationFilterOptions?: OrganizationListFilterOptionsContext;
  /** Required when {@link ListDefinition.filterOptionsSource} is `org/user-documents`. */
  organizationUserDocumentFilterOptions?: OrganizationUserDocumentListFilterOptionsContext;
  /** Required when {@link ListDefinition.filterOptionsSource} is `org/user-time-entries`. */
  organizationUserTimeEntryFilterOptions?: OrganizationUserTimeEntryListFilterOptionsContext;
  /** Required when {@link ListDefinition.filterOptionsSource} is `org/user-absences`. */
  organizationUserAbsenceFilterOptions?: OrganizationUserAbsenceListFilterOptionsContext;
  /** Required when {@link ListDefinition.filterOptionsSource} is `org/user-workflows`. */
  organizationUserWorkflowFilterOptions?: OrganizationUserWorkflowListFilterOptionsContext;
  /** Required when {@link ListDefinition.filterOptionsSource} is `org/appointments`. */
  organizationAppointmentFilterOptions?: OrganizationAppointmentListFilterOptionsContext;
  /** Required when {@link ListDefinition.filterOptionsSource} is `org/absences`. */
  organizationAbsenceFilterOptions?: OrganizationAbsenceListFilterOptionsContext;
  /** Required when {@link ListDefinition.filterOptionsSource} is `org/workflows`. */
  organizationWorkflowFilterOptions?: OrganizationWorkflowListFilterOptionsContext;
  /** Required when {@link ListDefinition.filterOptionsSource} is `org/certifications`. */
  organizationCertificationFilterOptions?: OrganizationCertificationListFilterOptionsContext;
  /** Required when {@link ListDefinition.filterOptionsSource} is `org/course-offerings`. */
  organizationCourseOfferingFilterOptions?: OrganizationCourseOfferingListFilterOptionsContext;
  /** Required when {@link ListDefinition.filterOptionsSource} is `org/service-events` or `org/operation-events`. */
  organizationEventFilterOptions?: OrganizationEventListFilterOptionsContext;
} & Pick<ListControlsProps, 'runtimeOptions' | 'selectCounts'>;

/**
 * Standard shell for paginated lists: {@link ListControls} always on top, then loading /
 * error / empty / filtered-empty / content + {@link ListLoadMore}. Matches `/admin/users`.
 */
export function PaginatedListView({
  definition,
  state,
  runtimeOptions,
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
  selectCounts,
  totalItems,
  isPending = false,
  error,
  emptyState,
  hasNextPage = false,
  isFetchingNextPage = false,
  onLoadMore,
  children,
  className,
}: PaginatedListViewProps) {
  const resolvedRuntimeOptions = usePaginatedListRuntimeOptions({
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
  });

  const controls = (
    <ListControls
      definition={definition}
      state={state}
      runtimeOptions={resolvedRuntimeOptions}
      selectCounts={selectCounts}
    />
  );

  let body: ReactNode;
  if (isPending) {
    body = <LoadingState variant="embedded" size="sm" />;
  } else if (error) {
    body = error;
  } else if (totalItems === 0) {
    body = state.hasActiveFilters ? <ListNoResults onReset={state.reset} /> : emptyState;
  } else {
    body = (
      <>
        {children}
        {onLoadMore ? (
          <ListLoadMore
            hasNextPage={hasNextPage}
            isFetchingNextPage={isFetchingNextPage}
            onLoadMore={onLoadMore}
          />
        ) : null}
      </>
    );
  }

  return (
    <div className={cn(className)}>
      {controls}
      {body}
    </div>
  );
}

/** Convenience when the empty copy is a single translated string. */
export function paginatedListEmptyCard(message: ReactNode) {
  return <EmptyStateCard>{message}</EmptyStateCard>;
}
