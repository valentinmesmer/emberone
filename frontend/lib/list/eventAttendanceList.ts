/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { createInitialListQueryState, defineList, toListQuery } from './defineList';
import type { ListQuery } from './types';

export const eventAttendanceListDefinition = defineList({
  id: 'event-attendance',
  search: {},
});

export const eventAttendanceListDefaultQuery: ListQuery = toListQuery(
  eventAttendanceListDefinition,
  createInitialListQueryState(eventAttendanceListDefinition),
);
