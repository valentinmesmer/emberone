/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { ListDefinition, ListFilterField, ListFilterValues } from './types';

function filterLoadsRuntimeOptionsFromApi(filter: ListFilterField): boolean {
  if (filter.kind !== 'select' && filter.kind !== 'multiSelect') {
    return false;
  }
  if (filter.runtimeOptions === true) {
    return true;
  }
  return filter.kind === 'select' && filter.runtimeOptions != null;
}

/** Filter keys that load options from a list-filter-options API (`runtimeOptions: true`). */
export function listRuntimeOptionKeys(definition: ListDefinition): string[] {
  const keys: string[] = [];
  for (const filter of definition.filters ?? []) {
    if (filterLoadsRuntimeOptionsFromApi(filter)) {
      keys.push(filter.key);
    }
  }
  return keys;
}

/** Drops selected values that are not in the loaded option set (stale URL / config). */
export function pruneFiltersToRuntimeOptions(
  definition: ListDefinition,
  filters: ListFilterValues,
  runtimeOptions: Record<string, readonly { value: string }[] | undefined>,
): ListFilterValues {
  let next = filters;
  for (const filter of definition.filters ?? []) {
    if (filter.kind !== 'multiSelect' && filter.kind !== 'select') {
      continue;
    }
    if (!filter.runtimeOptions) {
      continue;
    }
    const options = runtimeOptions[filter.key];
    if (!options || options.length === 0) {
      continue;
    }
    const allowed = new Set(options.map((option) => option.value));
    const raw = next[filter.key];
    if (filter.kind === 'multiSelect') {
      const current = Array.isArray(raw) ? raw : [];
      const pruned = current.filter((value) => allowed.has(value));
      if (pruned.length !== current.length) {
        next = { ...next, [filter.key]: pruned };
      }
      continue;
    }
    if (typeof raw === 'string' && raw !== '' && !allowed.has(raw)) {
      next = { ...next, [filter.key]: '' };
    }
  }
  return next;
}
