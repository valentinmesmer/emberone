/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { calendarYearMenubarFilter } from './calendarYearFilter';
import { defineList } from './defineList';
import type { ListFilterValues } from './types';

export const AbsenceListPeriodParam = {
  CURRENT: 'CURRENT',
  UPCOMING: 'UPCOMING',
  PAST: 'PAST',
} as const;

export type AbsenceListPeriodParam =
  (typeof AbsenceListPeriodParam)[keyof typeof AbsenceListPeriodParam];

const periodFilter = {
  kind: 'multiSelect',
  key: 'period',
  labelKey: 'absence.list.period.label',
  countsInMenubarWhenSelected: true,
  options: [
    { value: AbsenceListPeriodParam.UPCOMING, labelKey: 'absence.list.period.upcoming' },
    { value: AbsenceListPeriodParam.PAST, labelKey: 'absence.list.period.past' },
  ],
} as const;

const typeFilter = {
  kind: 'multiSelect',
  key: 'reason',
  labelKey: 'absence.list.type.label',
  options: [],
  runtimeOptions: true,
} as const;

const organizationFilter = {
  kind: 'multiSelect',
  key: 'organizationId',
  labelKey: 'absence.list.organization.label',
  options: [],
  runtimeOptions: true,
} as const;

const absenceSelfFilters = [
  periodFilter,
  typeFilter,
  organizationFilter,
  calendarYearMenubarFilter('absence.list.year.label'),
] as const;

/** Org user-management: Mitglied + Organisation aus der Route - ohne Organisationsfilter. */
const absenceOrgUserFilters = [
  periodFilter,
  typeFilter,
  calendarYearMenubarFilter('absence.list.year.label'),
] as const;

/**
 * Self `/my-area/absence` - Zeitraum (leer = alle), Typ, Organisation, Kalenderjahr; keine Suche.
 */
export const absenceListDefinition = defineList({
  id: 'absence',
  filterOptionsSource: 'self/absences',
  sort: {
    options: [
      { value: 'startDate,asc', labelKey: 'absence.list.sort.startDateAsc' },
      { value: 'startDate,desc', labelKey: 'absence.list.sort.startDateDesc' },
    ],
  },
  sheetLayout: 'menubar',
  filters: absenceSelfFilters,
});

/** Org user-management `…/users/{userId}/absences` - filter options on the list controller. */
export const absenceOrgUserListDefinition = defineList({
  id: 'absence-org-user',
  filterOptionsSource: 'org/user-absences',
  sort: {
    options: [
      { value: 'startDate,asc', labelKey: 'absence.list.sort.startDateAsc' },
      { value: 'startDate,desc', labelKey: 'absence.list.sort.startDateDesc' },
    ],
  },
  sheetLayout: 'menubar',
  filters: absenceOrgUserFilters,
});

const orgPeriodFilter = {
  kind: 'multiSelect',
  key: 'period',
  labelKey: 'absence.list.period.label',
  defaultValue: [AbsenceListPeriodParam.CURRENT],
  countsInMenubarWhenSelected: true,
  options: [
    { value: AbsenceListPeriodParam.CURRENT, labelKey: 'absence.list.period.current' },
    { value: AbsenceListPeriodParam.UPCOMING, labelKey: 'absence.list.period.upcoming' },
    { value: AbsenceListPeriodParam.PAST, labelKey: 'absence.list.period.past' },
  ],
} as const;

const orgAbsenceFilters = [
  orgPeriodFilter,
  typeFilter,
  calendarYearMenubarFilter('absence.list.year.label'),
] as const;

/** Organization `/organizations/.../absences` - search + menubar (Zeitraum, Typ, Jahr). */
export const orgAbsenceListDefinition = defineList({
  id: 'org-absence',
  filterOptionsSource: 'org/absences',
  search: {},
  sort: {
    options: [
      { value: 'startDate,asc', labelKey: 'absence.list.sort.startDateAsc' },
      { value: 'startDate,desc', labelKey: 'absence.list.sort.startDateDesc' },
      { value: 'lastName,asc', labelKey: 'absence.list.sort.lastNameAsc' },
      { value: 'lastName,desc', labelKey: 'absence.list.sort.lastNameDesc' },
    ],
  },
  sheetLayout: 'menubar',
  filters: orgAbsenceFilters,
});

export function orgAbsenceEmptyMessageKey(
  period: ListFilterValues[string] | undefined,
):
  | 'userManagement.absences.emptyCurrent'
  | 'userManagement.absences.emptyUpcoming'
  | 'userManagement.absences.emptyPast' {
  const periods = Array.isArray(period)
    ? period
    : typeof period === 'string' && period !== ''
      ? [period]
      : [AbsenceListPeriodParam.CURRENT];
  if (periods.length === 1 && periods[0] === AbsenceListPeriodParam.PAST) {
    return 'userManagement.absences.emptyPast';
  }
  if (periods.length === 1 && periods[0] === AbsenceListPeriodParam.UPCOMING) {
    return 'userManagement.absences.emptyUpcoming';
  }
  return 'userManagement.absences.emptyCurrent';
}
