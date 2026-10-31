/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { defineList } from './defineList';

/** Org qualifications overview - server-side search over groups and members (URL-synced). */
export const organizationQualificationOverviewListDefinition = defineList({
  id: 'organizationQualificationOverview',
  search: {},
});
