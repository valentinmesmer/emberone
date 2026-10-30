/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { defineList } from './defineList';

const federalStateFilter = {
  kind: 'multiSelect',
  key: 'federalState',
  labelKey: 'admin.tenantForm.federalState',
  options: [],
  runtimeOptions: true,
} as const;

/** Admin tenant list: search by name / abbreviation, filter by configured federal state. */
export const adminTenantListDefinition = defineList({
  id: 'admin-tenant',
  filterOptionsSource: 'admin/tenants',
  search: {},
  sort: {
    options: [
      { value: 'name,asc', labelKey: 'admin.tenants.list.sort.nameAsc' },
      { value: 'name,desc', labelKey: 'admin.tenants.list.sort.nameDesc' },
    ],
  },
  sheetLayout: 'menubar',
  filters: [federalStateFilter] as const,
});
