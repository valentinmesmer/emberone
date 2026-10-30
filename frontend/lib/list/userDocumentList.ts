/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { defineList } from './defineList';

export const UserDocumentViewedParam = {
  UNREAD: 'UNREAD',
  READ: 'READ',
} as const;

export type UserDocumentViewedParam =
  (typeof UserDocumentViewedParam)[keyof typeof UserDocumentViewedParam];

/** Same filter kinds as {@link adminUserList} / {@link adminTenantList} (multiSelect + menubar). */
const contentTypeFilter = {
  kind: 'multiSelect',
  key: 'contentType',
  labelKey: 'userDocument.list.contentType.label',
  options: [],
  runtimeOptions: true,
} as const;

const tagFilter = {
  kind: 'multiSelect',
  key: 'tag',
  labelKey: 'myArea.documents.filterByTag',
  options: [],
  runtimeOptions: true,
} as const;

const viewedFilter = {
  kind: 'multiSelect',
  key: 'viewed',
  labelKey: 'userDocument.list.viewed.label',
  options: [
    { value: UserDocumentViewedParam.UNREAD, labelKey: 'userDocument.list.viewed.unread' },
    { value: UserDocumentViewedParam.READ, labelKey: 'userDocument.list.viewed.read' },
  ],
} as const;

const sharedFilters = [contentTypeFilter, tagFilter, viewedFilter] as const;

/** Self `/my-area/documents` - same menubar + filter-options pattern as admin users/tenants. */
export const userDocumentSelfListDefinition = defineList({
  id: 'user-document-self',
  filterOptionsSource: 'self/documents',
  search: {},
  sort: {
    options: [
      { value: 'createdAt,asc', labelKey: 'userDocument.list.sort.createdAtAsc' },
      { value: 'createdAt,desc', labelKey: 'userDocument.list.sort.createdAtDesc' },
      { value: 'title,asc', labelKey: 'userDocument.list.sort.titleAsc' },
      { value: 'title,desc', labelKey: 'userDocument.list.sort.titleDesc' },
    ],
  },
  sheetLayout: 'menubar',
  filters: sharedFilters,
});

/** Org user-management `…/users/{userId}/documents` - filter options on the list controller. */
export const userDocumentOrgListDefinition = defineList({
  id: 'user-document-org',
  filterOptionsSource: 'org/user-documents',
  search: {},
  sort: {
    options: [
      { value: 'createdAt,asc', labelKey: 'userDocument.list.sort.createdAtAsc' },
      { value: 'createdAt,desc', labelKey: 'userDocument.list.sort.createdAtDesc' },
      { value: 'title,asc', labelKey: 'userDocument.list.sort.titleAsc' },
      { value: 'title,desc', labelKey: 'userDocument.list.sort.titleDesc' },
    ],
  },
  sheetLayout: 'menubar',
  filters: sharedFilters,
});
