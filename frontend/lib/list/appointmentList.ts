/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { defineList } from './defineList';

export const AppointmentListPeriodParam = {
  UPCOMING: 'UPCOMING',
  PAST: 'PAST',
} as const;

export type AppointmentListPeriodParam =
  (typeof AppointmentListPeriodParam)[keyof typeof AppointmentListPeriodParam];

export const AppointmentListResponseParam = {
  YES: 'YES',
  NO: 'NO',
  UNCERTAIN: 'UNCERTAIN',
  NO_RESPONSE: 'NO_RESPONSE',
} as const;

export type AppointmentListResponseParam =
  (typeof AppointmentListResponseParam)[keyof typeof AppointmentListResponseParam];

export const AppointmentListViewedParam = {
  READ: 'READ',
  UNREAD: 'UNREAD',
} as const;

export type AppointmentListViewedParam =
  (typeof AppointmentListViewedParam)[keyof typeof AppointmentListViewedParam];

const periodFilter = {
  kind: 'multiSelect',
  key: 'period',
  labelKey: 'appointments.list.period.label',
  defaultValue: [AppointmentListPeriodParam.UPCOMING],
  countsInMenubarWhenSelected: true,
  options: [
    { value: AppointmentListPeriodParam.UPCOMING, labelKey: 'appointments.list.period.upcoming' },
    { value: AppointmentListPeriodParam.PAST, labelKey: 'appointments.list.period.past' },
  ],
} as const;

const responseFilter = {
  kind: 'multiSelect',
  key: 'response',
  labelKey: 'appointments.list.response.label',
  options: [
    { value: AppointmentListResponseParam.YES, labelKey: 'appointments.list.response.yes' },
    { value: AppointmentListResponseParam.NO, labelKey: 'appointments.list.response.no' },
    {
      value: AppointmentListResponseParam.UNCERTAIN,
      labelKey: 'appointments.list.response.uncertain',
    },
    {
      value: AppointmentListResponseParam.NO_RESPONSE,
      labelKey: 'appointments.list.response.noResponse',
    },
  ],
} as const;

const viewedFilter = {
  kind: 'multiSelect',
  key: 'viewed',
  labelKey: 'appointments.list.viewed.label',
  options: [
    { value: AppointmentListViewedParam.READ, labelKey: 'appointments.list.viewed.read' },
    { value: AppointmentListViewedParam.UNREAD, labelKey: 'appointments.list.viewed.unread' },
  ],
} as const;

/** Optional calendar year - no default; list default is period UPCOMING only. */
const yearFilter = {
  kind: 'multiSelect',
  key: 'year',
  labelKey: 'appointments.list.year.label',
  countsInMenubarWhenSelected: true,
  options: [],
  runtimeOptions: true,
} as const;

const appointmentListBase = {
  id: 'appointment',
  filterOptionsSource: 'org/appointments',
  search: {},
  sort: {
    options: [
      { value: 'start,asc', labelKey: 'appointments.list.sort.startAsc' },
      { value: 'start,desc', labelKey: 'appointments.list.sort.startDesc' },
      { value: 'name,asc', labelKey: 'appointments.list.sort.nameAsc' },
      { value: 'name,desc', labelKey: 'appointments.list.sort.nameDesc' },
    ],
  },
  sheetLayout: 'menubar',
} as const;

/** Org department `/organizations/.../department/appointments` for members - with own state. */
export const appointmentListDefinition = defineList({
  ...appointmentListBase,
  filters: [periodFilter, yearFilter, responseFilter, viewedFilter],
});

/** Same list for a managed organization: only the entries, no own response or read state. */
export const managedAppointmentListDefinition = defineList({
  ...appointmentListBase,
  filters: [periodFilter, yearFilter],
});
