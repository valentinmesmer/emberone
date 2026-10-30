/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { defineList } from './defineList';

const APP_ROLES_GLOBAL = ['USER', 'TENANT_ADMIN', 'GLOBAL_ADMIN'] as const;
const APP_ROLES_TENANT_ADMIN = ['USER', 'TENANT_ADMIN'] as const;

function roleFilter(isGlobalAdmin: boolean) {
  const roles = isGlobalAdmin ? APP_ROLES_GLOBAL : APP_ROLES_TENANT_ADMIN;
  return {
    kind: 'multiSelect',
    key: 'appRole',
    labelKey: 'admin.table.role',
    options: roles.map((value) => ({
      value,
      labelKey: `admin.roles.${value}`,
    })),
  } as const;
}

const organizationFilter = {
  kind: 'multiSelect',
  key: 'organizationId',
  labelKey: 'admin.table.organization',
  options: [],
  runtimeOptions: true,
} as const;

const tenantFilter = {
  kind: 'multiSelect',
  key: 'tenantId',
  labelKey: 'admin.table.tenant',
  options: [],
  runtimeOptions: true,
} as const;

/**
 * Admin user list: search, menubar filters (tenant for global admin, organization, role).
 * Tenant / organization options from `filterOptionsSource`; roles use static i18n keys.
 */
function createAdminUserListDefinition(isGlobalAdmin: boolean) {
  return defineList({
    id: isGlobalAdmin ? 'admin-user-global' : 'admin-user-tenant',
    filterOptionsSource: 'admin/users',
    search: {},
    sort: {
      options: [
        { value: 'lastName,asc', labelKey: 'admin.users.list.sort.lastNameAsc' },
        { value: 'lastName,desc', labelKey: 'admin.users.list.sort.lastNameDesc' },
        { value: 'firstName,asc', labelKey: 'admin.users.list.sort.firstNameAsc' },
        { value: 'firstName,desc', labelKey: 'admin.users.list.sort.firstNameDesc' },
      ],
    },
    sheetLayout: 'menubar',
    filters: isGlobalAdmin
      ? ([tenantFilter, organizationFilter, roleFilter(true)] as const)
      : ([organizationFilter, roleFilter(false)] as const),
  });
}

const GLOBAL_ADMIN_DEFINITION = createAdminUserListDefinition(true);
const TENANT_ADMIN_DEFINITION = createAdminUserListDefinition(false);

export function getAdminUserListDefinition(isGlobalAdmin: boolean) {
  return isGlobalAdmin ? GLOBAL_ADMIN_DEFINITION : TENANT_ADMIN_DEFINITION;
}
