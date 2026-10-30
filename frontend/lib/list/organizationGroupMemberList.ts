/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { defineList } from './defineList';

export const organizationGroupMemberListDefinition = defineList({
  id: 'organizationGroupMember',
  search: {},
  sort: {
    options: [
      { value: 'lastName,asc', labelKey: 'organization.groups.members.list.sort.lastNameAsc' },
      { value: 'lastName,desc', labelKey: 'organization.groups.members.list.sort.lastNameDesc' },
      { value: 'firstName,asc', labelKey: 'organization.groups.members.list.sort.firstNameAsc' },
      { value: 'firstName,desc', labelKey: 'organization.groups.members.list.sort.firstNameDesc' },
    ],
  },
});
