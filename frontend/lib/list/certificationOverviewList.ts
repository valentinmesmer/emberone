/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { defineList } from './defineList';

export const CertificationOverviewStatusParam = {
  MISSING: 'MISSING',
  EXPIRED: 'EXPIRED',
  EXPIRING_SOON: 'EXPIRING_SOON',
  VALID: 'VALID',
} as const;

export type CertificationOverviewStatusParam =
  (typeof CertificationOverviewStatusParam)[keyof typeof CertificationOverviewStatusParam];

const overviewStatusFilter = {
  kind: 'multiSelect',
  key: 'status',
  labelKey: 'certifications.overview.filterLabel',
  defaultValue: [],
  countsInMenubarWhenSelected: true,
  options: [
    {
      value: CertificationOverviewStatusParam.MISSING,
      labelKey: 'certifications.overview.status.missing',
    },
    {
      value: CertificationOverviewStatusParam.EXPIRED,
      labelKey: 'certifications.overview.status.expired',
    },
    {
      value: CertificationOverviewStatusParam.EXPIRING_SOON,
      labelKey: 'certifications.overview.status.expiringSoon',
    },
    {
      value: CertificationOverviewStatusParam.VALID,
      labelKey: 'certifications.overview.status.valid',
    },
  ],
} as const;

const overviewTypeFilter = {
  kind: 'multiSelect',
  key: 'type',
  labelKey: 'certifications.overview.typeLabel',
  options: [],
  runtimeOptions: true,
} as const;

/** Organization certification status overview - Status + Nachweistyp (leer = alle), Suche. */
export const certificationOverviewListDefinition = defineList({
  id: 'certification-overview',
  filterOptionsSource: 'org/certifications',
  search: {},
  sort: {
    options: [
      {
        value: 'name,asc',
        labelKey: 'certifications.overview.list.sort.nameAsc',
      },
      {
        value: 'name,desc',
        labelKey: 'certifications.overview.list.sort.nameDesc',
      },
      {
        value: 'expiryDate,asc',
        labelKey: 'certifications.overview.list.sort.expiryDateAsc',
      },
      {
        value: 'expiryDate,desc',
        labelKey: 'certifications.overview.list.sort.expiryDateDesc',
      },
    ],
  },
  sheetLayout: 'menubar',
  filters: [overviewStatusFilter, overviewTypeFilter],
});
