/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { calendarYearMenubarFilter } from './calendarYearFilter';
import { defineList, toListQuery } from './defineList';
import type { ListQuery } from './types';

export const EventListAttendanceParam = {
  ATTENDING: 'ATTENDING',
  NOT_ATTENDING: 'NOT_ATTENDING',
} as const;

export type EventListAttendanceParam =
  (typeof EventListAttendanceParam)[keyof typeof EventListAttendanceParam];

const attendanceFilter = {
  kind: 'multiSelect',
  key: 'attendance',
  labelKey: 'events.list.attendance.label',
  options: [
    { value: EventListAttendanceParam.ATTENDING, labelKey: 'events.list.attendance.attending' },
    {
      value: EventListAttendanceParam.NOT_ATTENDING,
      labelKey: 'events.list.attendance.notAttending',
    },
  ],
} as const;

function organizationEventListDefinition(
  id: string,
  filterOptionsSource: 'org/service-events' | 'org/operation-events',
) {
  return defineList({
    id,
    filterOptionsSource,
    search: {},
    sort: {
      options: [
        { value: 'startAt,asc', labelKey: 'events.list.sort.startAtAsc' },
        { value: 'startAt,desc', labelKey: 'events.list.sort.startAtDesc' },
      ],
    },
    sheetLayout: 'menubar',
    filters: [calendarYearMenubarFilter('events.list.year.label'), attendanceFilter],
  });
}

/** Org department service events - menubar filters + search. */
export const serviceEventListDefinition = organizationEventListDefinition(
  'service-event',
  'org/service-events',
);

/** Org department operation events - menubar filters + search. */
export const operationEventListDefinition = organizationEventListDefinition(
  'operation-event',
  'org/operation-events',
);

const unfilteredListState = {
  search: '',
  debouncedSearch: '',
  filters: {
    year: [] as string[],
    attendance: [] as string[],
  },
};

/** Unscoped list query for aggregate fetches (e.g. event detail lookup). */
export const serviceEventListUnfilteredQuery: ListQuery = toListQuery(
  serviceEventListDefinition,
  unfilteredListState,
);

export const operationEventListUnfilteredQuery: ListQuery = toListQuery(
  operationEventListDefinition,
  unfilteredListState,
);
