/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { isDateRangeActive } from './dateRange';
import { createInitialFilterValues } from './defineList';
import type {
  ListDateRangeValue,
  ListDefinition,
  ListFilterField,
  ListFilterValues,
  ListSelectFilter,
} from './types';

/** `true` for filters rendered in the filter sheet (everything except `segment` selects). */
export function isSheetFilter(filter: ListFilterField): boolean {
  return filter.kind !== 'select' || filter.presentation === 'sheet';
}

export function isSegmentSelect(filter: ListFilterField): filter is ListSelectFilter {
  return filter.kind === 'select' && filter.presentation !== 'sheet';
}

/** Whether `value` differs from the unset state of `filter` (`baseline` = its default value). */
export function isFilterValueSet(
  filter: ListFilterField,
  value: ListFilterValues[string],
  baseline: ListFilterValues[string],
): boolean {
  switch (filter.kind) {
    case 'select':
      return typeof value === 'string' && value !== '' && value !== baseline;
    case 'multiSelect':
      return (
        JSON.stringify(Array.isArray(value) ? value : []) !==
        JSON.stringify(Array.isArray(baseline) ? baseline : [])
      );
    case 'dateRange':
      return isDateRangeActive(
        value && typeof value === 'object' && !Array.isArray(value)
          ? (value as ListDateRangeValue)
          : {},
      );
  }
}

/** Whether the filter `key` currently narrows the list. */
function hasValue(definition: ListDefinition, values: ListFilterValues, key: string): boolean {
  const filter = definition.filters?.find((entry) => entry.key === key);
  if (!filter) {
    return false;
  }
  const baseline = createInitialFilterValues(definition)[key];
  return isFilterValueSet(filter, values[key], baseline);
}

/** A filter without `dependsOn`, or whose parent has a value, is enabled. */
export function isFilterEnabled(
  definition: ListDefinition,
  filter: ListFilterField,
  values: ListFilterValues,
): boolean {
  return filter.dependsOn == null || hasValue(definition, values, filter.dependsOn);
}

/**
 * Resets every filter that (transitively) depends on `key` to its default. Returns `values`
 * itself when nothing had to change.
 */
export function clearDependentFilters(
  definition: ListDefinition,
  values: ListFilterValues,
  key: string,
): ListFilterValues {
  const initial = createInitialFilterValues(definition);
  let next = values;
  const queue = [key];
  const seen = new Set<string>();
  while (queue.length > 0) {
    const parent = queue.shift() as string;
    if (seen.has(parent)) {
      continue;
    }
    seen.add(parent);
    for (const filter of definition.filters ?? []) {
      if (filter.dependsOn !== parent) {
        continue;
      }
      if (JSON.stringify(next[filter.key]) !== JSON.stringify(initial[filter.key])) {
        next = { ...next, [filter.key]: initial[filter.key] };
      }
      queue.push(filter.key);
    }
  }
  return next;
}

/** Drops values of filters whose parent is unset (e.g. from a hand-edited or stale URL). */
export function sanitizeDependentFilters(
  definition: ListDefinition,
  values: ListFilterValues,
): ListFilterValues {
  let next = values;
  for (const filter of definition.filters ?? []) {
    if (filter.dependsOn != null && !isFilterEnabled(definition, filter, next)) {
      next = clearDependentFilters(definition, next, filter.dependsOn);
    }
  }
  return next;
}
