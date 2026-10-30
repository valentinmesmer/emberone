/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { ListQuery } from '@/lib/list/types';

/** Normalizes list `year` filter state (select or multiSelect) for API query params. */
export function resolveYearListQueryParam(year: ListQuery[string] | undefined): number[] {
  if (year === undefined || year === null) {
    return [];
  }
  if (typeof year === 'number') {
    return Number.isFinite(year) ? [year] : [];
  }
  if (typeof year === 'string') {
    const trimmed = year.trim();
    if (!trimmed) {
      return [];
    }
    const parsed = Number(trimmed);
    return Number.isFinite(parsed) ? [parsed] : [];
  }
  if (Array.isArray(year)) {
    const parsed = year
      .map((value) => Number(String(value).trim()))
      .filter((value) => Number.isFinite(value));
    return [...new Set(parsed)];
  }
  return [];
}
