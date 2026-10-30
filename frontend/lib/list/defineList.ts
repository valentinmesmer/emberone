/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { dateRangeToWallClock } from './dateRange';
import { isFilterEnabled } from './filterState';
import type {
  ListDateRangeValue,
  ListDefinition,
  ListFilterField,
  ListFilterValues,
  ListQuery,
  ListQueryState,
} from './types';

export function defineList<const T extends ListDefinition>(definition: T): T {
  return definition;
}

/** Whether {@link ListControls} shows a sort menu for this list (independent of search/filters). */
export function listHasSort(definition: ListDefinition): boolean {
  return (definition.sort?.options.length ?? 0) > 0;
}

export function createInitialFilterValues(definition: ListDefinition): ListFilterValues {
  const values: ListFilterValues = {};
  for (const filter of definition.filters ?? []) {
    values[filter.key] = initialValueForFilter(filter);
  }
  return values;
}

function initialValueForFilter(filter: ListFilterField): ListFilterValues[string] {
  switch (filter.kind) {
    case 'select':
      return filter.defaultValue;
    case 'multiSelect':
      return filter.defaultValue ? [...filter.defaultValue] : [];
    case 'dateRange':
      return {};
    default:
      return undefined;
  }
}

export function createInitialListQueryState(definition: ListDefinition): ListQueryState {
  return {
    search: '',
    debouncedSearch: '',
    filters: createInitialFilterValues(definition),
    sort: undefined,
  };
}

type ListQueryInput = Pick<ListQueryState, 'debouncedSearch' | 'filters' | 'sort'> & {
  page?: number;
  size?: number;
};

/** Maps list UI state to flat query params (before {@link buildQuery}). */
export function toListQuery(definition: ListDefinition, input: ListQueryInput): ListQuery {
  const query: ListQuery = {};

  if (input.page != null) {
    query.page = input.page;
  }
  if (input.size != null) {
    query.size = input.size;
  }

  const searchParam = definition.search?.param ?? 'search';
  const trimmed = input.debouncedSearch.trim();
  if (trimmed) {
    query[searchParam] = trimmed;
  }

  if (input.sort && definition.sort?.options.some((option) => option.value === input.sort)) {
    query.sort = input.sort;
  }

  for (const filter of definition.filters ?? []) {
    if (!isFilterEnabled(definition, filter, input.filters)) {
      continue;
    }
    const raw = input.filters[filter.key];
    switch (filter.kind) {
      case 'select': {
        if (typeof raw === 'string' && raw !== '') {
          query[filter.key] = raw;
        }
        break;
      }
      case 'multiSelect': {
        if (Array.isArray(raw) && raw.length > 0) {
          query[filter.key] = raw;
        }
        break;
      }
      case 'dateRange': {
        const range = raw as ListDateRangeValue | undefined;
        const wallClock = dateRangeToWallClock(range ?? {});
        if (wallClock.from) {
          query[filter.fromParam] = wallClock.from;
        }
        if (wallClock.to) {
          query[filter.toParam] = wallClock.to;
        }
        break;
      }
      default:
        break;
    }
  }

  return query;
}
