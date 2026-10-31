/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { defineList } from './defineList';

export const organizationRankMemberListDefinition = defineList({
  id: 'organizationRankMember',
  search: {},
  sort: {
    options: [
      { value: 'lastName,asc', labelKey: 'ranks.overview.list.sort.lastNameAsc' },
      { value: 'lastName,desc', labelKey: 'ranks.overview.list.sort.lastNameDesc' },
      { value: 'firstName,asc', labelKey: 'ranks.overview.list.sort.firstNameAsc' },
      { value: 'firstName,desc', labelKey: 'ranks.overview.list.sort.firstNameDesc' },
    ],
  },
});
