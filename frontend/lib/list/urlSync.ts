/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { isIsoDate } from './dateRange';
import { createInitialFilterValues } from './defineList';
import { sanitizeDependentFilters } from './filterState';
import type {
  ListDateRangeValue,
  ListDefinition,
  ListFilterField,
  ListFilterValues,
  ListQueryState,
} from './types';

export const LIST_URL_SEARCH_PARAM = 'q';
export const LIST_URL_SORT_PARAM = 'sort';

type UrlReadable = { get(name: string): string | null };

/** Query-param names this list owns in the URL. */
export function listUrlParamNames(definition: ListDefinition): string[] {
  const names: string[] = [];
  if (definition.search) {
    names.push(LIST_URL_SEARCH_PARAM);
  }
  if (definition.sort) {
    names.push(LIST_URL_SORT_PARAM);
  }
  for (const filter of definition.filters ?? []) {
    if (filter.kind === 'dateRange') {
      names.push(filter.fromParam, filter.toParam);
    } else {
      names.push(filter.key);
    }
  }
  return names;
}

function readFilter(
  filter: ListFilterField,
  params: UrlReadable,
  fallback: ListFilterValues[string],
): ListFilterValues[string] {
  switch (filter.kind) {
    case 'select': {
      const raw = params.get(filter.key);
      if (filter.runtimeOptions) {
        return raw ? raw : fallback;
      }
      return raw != null && filter.options.some((option) => option.value === raw) ? raw : fallback;
    }
    case 'multiSelect': {
      const raw = params.get(filter.key);
      if (raw === null) {
        return fallback;
      }
      if (raw === '') {
        return [];
      }
      const values = [...new Set(raw.split(','))].filter((value) => value !== '');
      if (filter.runtimeOptions) {
        return values.length > 0 ? values : fallback;
      }
      const allowed = new Set(filter.options.map((option) => option.value));
      const allowedValues = values.filter((value) => allowed.has(value));
      return allowedValues.length > 0 ? allowedValues : fallback;
    }
    case 'dateRange': {
      const from = params.get(filter.fromParam) ?? undefined;
      const to = params.get(filter.toParam) ?? undefined;
      return { from: isIsoDate(from) ? from : undefined, to: isIsoDate(to) ? to : undefined };
    }
  }
}

/** Initial list state from URL params; invalid or unknown values fall back to defaults. */
export function readListUrlState(
  definition: ListDefinition,
  params: UrlReadable,
  baselineFilters?: ListFilterValues,
): Pick<ListQueryState, 'search' | 'filters' | 'sort'> {
  const defaults = baselineFilters ?? createInitialFilterValues(definition);
  const filters: ListFilterValues = { ...defaults };
  for (const filter of definition.filters ?? []) {
    filters[filter.key] = readFilter(filter, params, defaults[filter.key]);
  }
  const search = definition.search ? (params.get(LIST_URL_SEARCH_PARAM) ?? '').trim() : '';
  const rawSort = definition.sort ? params.get(LIST_URL_SORT_PARAM) : null;
  const sort = definition.sort?.options.some((option) => option.value === rawSort)
    ? (rawSort ?? undefined)
    : undefined;
  return { search, filters: sanitizeDependentFilters(definition, filters), sort };
}

/** Only non-default values, as the params this list owns (`undefined` = remove from URL). */
export function listUrlParamValues(
  definition: ListDefinition,
  state: Pick<ListQueryState, 'debouncedSearch' | 'filters' | 'sort'>,
): Record<string, string | undefined> {
  const defaults = createInitialFilterValues(definition);
  const out: Record<string, string | undefined> = {};
  for (const name of listUrlParamNames(definition)) {
    out[name] = undefined;
  }
  if (definition.search) {
    out[LIST_URL_SEARCH_PARAM] = state.debouncedSearch.trim() || undefined;
  }
  if (definition.sort) {
    out[LIST_URL_SORT_PARAM] = state.sort || undefined;
  }
  for (const filter of definition.filters ?? []) {
    const current = state.filters[filter.key];
    switch (filter.kind) {
      case 'select':
        if (typeof current === 'string' && current !== '' && current !== defaults[filter.key]) {
          out[filter.key] = current;
        }
        break;
      case 'multiSelect': {
        if (!Array.isArray(current)) {
          break;
        }
        const baseline = defaults[filter.key];
        const differs = JSON.stringify(current) !== JSON.stringify(baseline);
        const persistDefault = filter.countsInMenubarWhenSelected && !differs && current.length > 0;
        if (current.length === 0 && differs) {
          out[filter.key] = '';
          break;
        }
        if (current.length > 0 && (differs || persistDefault)) {
          out[filter.key] = current.join(',');
        }
        break;
      }
      case 'dateRange': {
        const range = current as ListDateRangeValue | undefined;
        out[filter.fromParam] = range?.from || undefined;
        out[filter.toParam] = range?.to || undefined;
        break;
      }
    }
  }
  return out;
}

/**
 * Applies owned params to a copy of `current`, keeping unrelated params.
 * Returns `null` when nothing changed (no router call needed).
 */
export function mergeListUrlParams(
  current: URLSearchParams,
  values: Record<string, string | undefined>,
): URLSearchParams | null {
  const next = new URLSearchParams(current.toString());
  let changed = false;
  for (const [name, value] of Object.entries(values)) {
    if ((current.get(name) ?? undefined) === value) {
      continue;
    }
    changed = true;
    if (value === undefined) {
      next.delete(name);
    } else {
      next.set(name, value);
    }
  }
  return changed ? next : null;
}
