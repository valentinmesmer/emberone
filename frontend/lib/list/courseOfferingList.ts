/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { CourseOfferingStatus } from '@/types/courseOffering';
import { defineList, toListQuery } from './defineList';
import type { ListQuery, ListQueryState } from './types';

export const CourseOfferingListStatusParam = {
  DRAFT: 'DRAFT',
  PUBLISHED: 'PUBLISHED',
  COMPLETED: 'COMPLETED',
  CANCELLED: 'CANCELLED',
} as const satisfies Record<Exclude<CourseOfferingStatus, 'TEMPLATE'>, CourseOfferingStatus>;

const offeringStatusFilter = {
  kind: 'multiSelect',
  key: 'status',
  labelKey: 'courseOfferings.list.filters.status',
  defaultValue: [CourseOfferingListStatusParam.DRAFT, CourseOfferingListStatusParam.PUBLISHED],
  countsInMenubarWhenSelected: true,
  options: [
    { value: CourseOfferingListStatusParam.DRAFT, labelKey: 'courseOfferings.status.DRAFT' },
    {
      value: CourseOfferingListStatusParam.PUBLISHED,
      labelKey: 'courseOfferings.status.PUBLISHED',
    },
    {
      value: CourseOfferingListStatusParam.COMPLETED,
      labelKey: 'courseOfferings.status.COMPLETED',
    },
    {
      value: CourseOfferingListStatusParam.CANCELLED,
      labelKey: 'courseOfferings.status.CANCELLED',
    },
  ],
} as const;

const offeringTrainingCourseTypeFilter = {
  kind: 'multiSelect',
  key: 'trainingCourseTypeId',
  labelKey: 'courseOfferings.list.filters.trainingCourseType',
  options: [],
  runtimeOptions: true,
  searchable: false,
} as const;

const offeringSortOptions = [
  { value: 'startDate,asc', labelKey: 'courseOfferings.list.sort.startAsc' },
  { value: 'startDate,desc', labelKey: 'courseOfferings.list.sort.startDesc' },
  { value: 'trainingCourseType,asc', labelKey: 'courseOfferings.list.sort.trainingCourseTypeAsc' },
  {
    value: 'trainingCourseType,desc',
    labelKey: 'courseOfferings.list.sort.trainingCourseTypeDesc',
  },
] as const;

/** Course-offering lists use the same menubar filter row as appointments and other org lists. */
const courseOfferingSheetLayout = 'menubar' as const;

/** Org workspace `/organizations/…/course-offerings`. */
export const orgCourseOfferingListDefinition = defineList({
  id: 'org-course-offering',
  filterOptionsSource: 'org/course-offerings',
  search: {},
  sort: { options: offeringSortOptions },
  sheetLayout: courseOfferingSheetLayout,
  filters: [offeringStatusFilter, offeringTrainingCourseTypeFilter],
});

/** Toggle: when selected, also list offerings for courses the user already holds. */
/** Templates of an organization (`…/course-offerings/templates`), shown in the templates modal. */
export const orgCourseOfferingTemplateListDefinition = defineList({
  id: 'org-course-offering-template',
  search: {},
});

const includeHeldFilter = {
  kind: 'multiSelect',
  key: 'includeHeld',
  labelKey: 'courseOfferings.self.filters.includeHeld',
  options: [{ value: 'true', labelKey: 'courseOfferings.self.filters.includeHeld' }],
} as const;

/** Toggle: when selected, also list offerings whose prerequisites the user does not meet. */
const includeUnfulfilledFilter = {
  kind: 'multiSelect',
  key: 'includeUnfulfilled',
  labelKey: 'courseOfferings.self.filters.includeUnfulfilled',
  options: [{ value: 'true', labelKey: 'courseOfferings.self.filters.includeUnfulfilled' }],
} as const;

/** Registration window and seats of an offering; sent as the `registration` param. */
export const SelfCourseOfferingRegistrationParam = {
  OPEN: 'OPEN',
  CLOSED: 'CLOSED',
  FULL: 'FULL',
} as const;

/** The caller's own registration; sent as the `participation` param. */
export const SelfCourseOfferingParticipationParam = {
  REQUESTED: 'REQUESTED',
  PARTICIPANT: 'PARTICIPANT',
  WAITLIST: 'WAITLIST',
} as const;

/** Registration filter: open window by default; each filter is a union, the two combine with AND. */
const selfRegistrationFilter = {
  kind: 'multiSelect',
  key: 'registration',
  labelKey: 'courseOfferings.self.filters.registration',
  defaultValue: [SelfCourseOfferingRegistrationParam.OPEN],
  countsInMenubarWhenSelected: true,
  options: [
    {
      value: SelfCourseOfferingRegistrationParam.OPEN,
      labelKey: 'courseOfferings.self.filters.statusOpen',
    },
    {
      value: SelfCourseOfferingRegistrationParam.CLOSED,
      labelKey: 'myArea.courseRegistrations.offerings.registrationClosed',
    },
    {
      value: SelfCourseOfferingRegistrationParam.FULL,
      labelKey: 'myArea.courseRegistrations.offerings.full',
    },
  ],
} as const;

/** Own participation filter: no restriction by default. */
const selfParticipationFilter = {
  kind: 'multiSelect',
  key: 'participation',
  labelKey: 'courseOfferings.self.filters.participation',
  countsInMenubarWhenSelected: true,
  options: [
    {
      value: SelfCourseOfferingParticipationParam.REQUESTED,
      labelKey: 'courseOfferings.self.filters.statusRequested',
    },
    {
      value: SelfCourseOfferingParticipationParam.PARTICIPANT,
      labelKey: 'courseOfferings.registrations.status.PARTICIPANT',
    },
    {
      value: SelfCourseOfferingParticipationParam.WAITLIST,
      labelKey: 'courseOfferings.registrations.status.WAITLIST',
    },
  ],
} as const;

/** Self eligible offerings (`/api/v1/self/course-offerings`). */
export const selfCourseOfferingListDefinition = defineList({
  id: 'self-course-offering',
  search: {},
  sort: { options: offeringSortOptions },
  sheetLayout: courseOfferingSheetLayout,
  filters: [
    selfRegistrationFilter,
    selfParticipationFilter,
    includeHeldFilter,
    includeUnfulfilledFilter,
  ],
});

const selfRegistrationSortOptions = [
  { value: 'startDate,desc', labelKey: 'courseOfferings.registrations.sort.startDesc' },
  { value: 'startDate,asc', labelKey: 'courseOfferings.registrations.sort.startAsc' },
  {
    value: 'registeredAt,desc',
    labelKey: 'courseOfferings.registrations.sort.registeredDesc',
  },
  {
    value: 'registeredAt,asc',
    labelKey: 'courseOfferings.registrations.sort.registeredAsc',
  },
] as const;

/** Self registrations (`/api/v1/self/course-registrations`). */
export const selfCourseRegistrationListDefinition = defineList({
  id: 'self-course-registration',
  search: {},
  sort: { options: selfRegistrationSortOptions },
  sheetLayout: courseOfferingSheetLayout,
});

const registrationStatusFilter = {
  kind: 'multiSelect',
  key: 'status',
  labelKey: 'courseOfferings.registrations.filters.status',
  options: [
    { value: 'PENDING', labelKey: 'courseOfferings.registrations.status.PENDING' },
    { value: 'PARTICIPANT', labelKey: 'courseOfferings.registrations.status.PARTICIPANT' },
    { value: 'WAITLIST', labelKey: 'courseOfferings.registrations.status.WAITLIST' },
    { value: 'REJECTED', labelKey: 'courseOfferings.registrations.status.REJECTED' },
    { value: 'CANCELLED', labelKey: 'courseOfferings.registrations.status.CANCELLED' },
  ],
} as const;

/** Open, waitlisted and approved registrations: the ones that still matter for the organization. */
const orgActiveRegistrationStatuses = ['PENDING', 'PARTICIPANT', 'WAITLIST'];

/** Registrations of one offering (org detail). */
export const orgCourseOfferingRegistrationListDefinition = defineList({
  id: 'org-course-offering-registration',
  search: {},
  sort: {
    options: [
      { value: 'registeredAt,asc', labelKey: 'courseOfferings.registrations.sort.registeredAsc' },
      {
        value: 'registeredAt,desc',
        labelKey: 'courseOfferings.registrations.sort.registeredDesc',
      },
      { value: 'name,asc', labelKey: 'courseOfferings.registrations.sort.nameAsc' },
      { value: 'name,desc', labelKey: 'courseOfferings.registrations.sort.nameDesc' },
    ],
  },
  sheetLayout: courseOfferingSheetLayout,
  filters: [
    {
      ...registrationStatusFilter,
      defaultValue: orgActiveRegistrationStatuses,
      countsInMenubarWhenSelected: true,
    },
  ],
});

const userRegistrationSortOptions = [
  ...selfRegistrationSortOptions,
  { value: 'courseType,asc', labelKey: 'courseOfferings.list.sort.trainingCourseTypeAsc' },
  { value: 'courseType,desc', labelKey: 'courseOfferings.list.sort.trainingCourseTypeDesc' },
] as const;

/** Upcoming only: offerings whose last session has not ended yet. Checked by default. */
const upcomingRegistrationFilter = {
  kind: 'multiSelect',
  key: 'upcoming',
  labelKey: 'courseOfferings.registrations.filters.upcoming',
  defaultValue: ['true'],
  options: [{ value: 'true', labelKey: 'courseOfferings.registrations.filters.upcoming' }],
} as const;

/** Member course registrations across offerings. */
export const userCourseRegistrationListDefinition = defineList({
  id: 'user-course-registration',
  search: {},
  sort: { options: userRegistrationSortOptions },
  sheetLayout: courseOfferingSheetLayout,
  filters: [registrationStatusFilter, upcomingRegistrationFilter],
});

const orgRegistrationSortOptions = [
  { value: 'startDate,asc', labelKey: 'courseOfferings.list.sort.startAsc' },
  { value: 'startDate,desc', labelKey: 'courseOfferings.list.sort.startDesc' },
  { value: 'name,asc', labelKey: 'courseOfferings.registrations.sort.nameAsc' },
  { value: 'name,desc', labelKey: 'courseOfferings.registrations.sort.nameDesc' },
] as const;

/** Registrations of all offerings of the organization (members area). */
export const orgCourseRegistrationListDefinition = defineList({
  id: 'org-course-registration',
  search: {},
  sort: { options: orgRegistrationSortOptions },
  sheetLayout: courseOfferingSheetLayout,
  filters: [
    {
      ...registrationStatusFilter,
      defaultValue: orgActiveRegistrationStatuses,
      countsInMenubarWhenSelected: true,
    },
    { ...upcomingRegistrationFilter, countsInMenubarWhenSelected: true },
  ],
});

/** @deprecated Use {@link orgCourseOfferingRegistrationListDefinition}. */
export const offeringRegistrationListDefinition = orgCourseOfferingRegistrationListDefinition;

export function selfCourseOfferingListQuery(
  input:
    | { listQuery: ListQuery; filters: ListQueryState['filters'] }
    | Pick<ListQueryState, 'debouncedSearch' | 'filters' | 'sort'>,
): ListQuery {
  const query =
    'listQuery' in input
      ? { ...input.listQuery }
      : toListQuery(selfCourseOfferingListDefinition, input);
  const includeHeld = input.filters.includeHeld;
  if (Array.isArray(includeHeld) && includeHeld.includes('true')) {
    query.includeHeld = true;
  }
  const includeUnfulfilled = input.filters.includeUnfulfilled;
  if (Array.isArray(includeUnfulfilled) && includeUnfulfilled.includes('true')) {
    query.includeUnfulfilled = true;
  }
  return query;
}
