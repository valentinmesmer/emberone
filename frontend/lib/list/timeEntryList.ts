/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { calendarYearMenubarFilter } from './calendarYearFilter';
import { defineList } from './defineList';
import type { ListFilterField } from './types';

const timeEntryDateRange = {
  kind: 'dateRange',
  key: 'startAt',
  labelKey: 'timeEntry.list.startAt.label',
  fromParam: 'from',
  toParam: 'to',
} as const satisfies ListFilterField;

const organizationFilter = {
  kind: 'multiSelect',
  key: 'organizationId',
  labelKey: 'timeEntry.list.organization.label',
  options: [],
  runtimeOptions: true,
} as const;

const timeEntrySelfFilters = [
  organizationFilter,
  calendarYearMenubarFilter('timeEntry.list.year.label'),
] as const;

/** Org user-management: Mitglied ist fest, Organisation steckt in der Route - nur Kalenderjahr. */
const timeEntryOrgUserFilters = [calendarYearMenubarFilter('timeEntry.list.year.label')] as const;

/**
 * Self `/my-area/time-entries` - Organisation + Kalenderjahr (Default: aktuelles Jahr); keine Suche.
 */
export const timeEntryListDefinition = defineList({
  id: 'time-entry',
  filterOptionsSource: 'self/time-entries',
  sort: {
    options: [
      { value: 'startAt,asc', labelKey: 'timeEntry.list.sort.startAtAsc' },
      { value: 'startAt,desc', labelKey: 'timeEntry.list.sort.startAtDesc' },
    ],
  },
  sheetLayout: 'menubar',
  filters: timeEntrySelfFilters,
});

/** Org user-management `…/users/{userId}/time-entries` - filter options on the list controller. */
export const timeEntryOrgUserListDefinition = defineList({
  id: 'time-entry-org-user',
  filterOptionsSource: 'org/user-time-entries',
  sort: {
    options: [
      { value: 'startAt,asc', labelKey: 'timeEntry.list.sort.startAtAsc' },
      { value: 'startAt,desc', labelKey: 'timeEntry.list.sort.startAtDesc' },
    ],
  },
  sheetLayout: 'menubar',
  filters: timeEntryOrgUserFilters,
});

/** Organization-wide time entries: date range + search by person. */
export const orgTimeEntryListDefinition = defineList({
  id: 'org-time-entry',
  search: {},
  sort: {
    options: [
      { value: 'startAt,asc', labelKey: 'timeEntry.list.sort.startAtAsc' },
      { value: 'startAt,desc', labelKey: 'timeEntry.list.sort.startAtDesc' },
      { value: 'lastName,asc', labelKey: 'timeEntry.list.sort.lastNameAsc' },
      { value: 'lastName,desc', labelKey: 'timeEntry.list.sort.lastNameDesc' },
    ],
  },
  filters: [timeEntryDateRange],
});
