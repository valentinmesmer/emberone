/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { currentCalendarYear } from './calendarYear';

/** Menubar multi-select for calendar year (default: current year). */
export function calendarYearMenubarFilter(labelKey: string) {
  return {
    kind: 'multiSelect',
    key: 'year',
    labelKey,
    defaultValue: [currentCalendarYear()],
    countsInMenubarWhenSelected: true,
    options: [],
    runtimeOptions: true,
  } as const;
}
