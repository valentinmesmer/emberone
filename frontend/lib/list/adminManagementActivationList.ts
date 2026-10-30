/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { defineList } from './defineList';

const tenantFilter = {
  kind: 'multiSelect',
  key: 'tenantId',
  labelKey: 'admin.managementActivations.filter.tenant',
  options: [],
  runtimeOptions: true,
} as const;

const statusFilter = {
  kind: 'multiSelect',
  key: 'status',
  labelKey: 'admin.managementActivations.filter.status',
  options: (['ACTIVE', 'EXPIRED', 'ENDED'] as const).map((value) => ({
    value,
    labelKey: `admin.managementActivations.status.${value}`,
  })),
  defaultValue: ['ACTIVE'],
  countsInMenubarWhenSelected: true,
} as const;

const organizationFilter = {
  kind: 'multiSelect',
  key: 'organizationId',
  labelKey: 'admin.table.organization',
  options: [],
  runtimeOptions: true,
} as const;

/**
 * Admin activation log: search (person, organizations, reason), filters for status (default:
 * active), organization (managing or target) and (global admin) tenant. Options come from
 * `filterOptionsSource`; statuses use static i18n keys.
 */
function createAdminManagementActivationListDefinition(isGlobalAdmin: boolean) {
  return defineList({
    id: isGlobalAdmin ? 'admin-management-activation-global' : 'admin-management-activation-tenant',
    filterOptionsSource: 'admin/management-activations',
    search: {},
    sort: {
      options: [
        {
          value: 'activatedAt,desc',
          labelKey: 'admin.managementActivations.sort.activatedAtDesc',
        },
        { value: 'activatedAt,asc', labelKey: 'admin.managementActivations.sort.activatedAtAsc' },
        { value: 'validUntil,desc', labelKey: 'admin.managementActivations.sort.validUntilDesc' },
        { value: 'validUntil,asc', labelKey: 'admin.managementActivations.sort.validUntilAsc' },
        { value: 'person,asc', labelKey: 'admin.managementActivations.sort.personAsc' },
        { value: 'person,desc', labelKey: 'admin.managementActivations.sort.personDesc' },
        {
          value: 'managingOrganization,asc',
          labelKey: 'admin.managementActivations.sort.managingAsc',
        },
        {
          value: 'managingOrganization,desc',
          labelKey: 'admin.managementActivations.sort.managingDesc',
        },
        {
          value: 'targetOrganization,asc',
          labelKey: 'admin.managementActivations.sort.targetAsc',
        },
        {
          value: 'targetOrganization,desc',
          labelKey: 'admin.managementActivations.sort.targetDesc',
        },
      ],
    },
    sheetLayout: 'menubar',
    filters: isGlobalAdmin
      ? ([tenantFilter, statusFilter, organizationFilter] as const)
      : ([statusFilter, organizationFilter] as const),
  });
}

const GLOBAL_ADMIN_DEFINITION = createAdminManagementActivationListDefinition(true);
const TENANT_ADMIN_DEFINITION = createAdminManagementActivationListDefinition(false);

export function getAdminManagementActivationListDefinition(isGlobalAdmin: boolean) {
  return isGlobalAdmin ? GLOBAL_ADMIN_DEFINITION : TENANT_ADMIN_DEFINITION;
}
