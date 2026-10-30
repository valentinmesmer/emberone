/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { usePathname, useRouter, useSearchParams } from 'next/navigation';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { createInitialFilterValues, createInitialListQueryState, toListQuery } from './defineList';
import { clearDependentFilters, isSegmentSelect } from './filterState';
import type { ListDateRangeValue, ListDefinition, ListFilterValues, ListQuery } from './types';
import { listUrlParamValues, mergeListUrlParams, readListUrlState } from './urlSync';

export type ListScopedFilterDefaults = ListFilterValues;

function baselineFilters(
  definition: ListDefinition,
  scopedDefaults?: ListScopedFilterDefaults,
): ListFilterValues {
  return scopedDefaults
    ? { ...createInitialFilterValues(definition), ...scopedDefaults }
    : createInitialFilterValues(definition);
}

type UrlBinding = {
  /** Current query string of the route (without `?`). */
  paramsString: string;
  replace: (next: URLSearchParams) => void;
};

function useUrlBinding(): UrlBinding {
  const searchParams = useSearchParams();
  const pathname = usePathname();
  const router = useRouter();
  const replace = useCallback(
    (next: URLSearchParams) => {
      const qs = next.toString();
      router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
    },
    [pathname, router],
  );
  return { paramsString: searchParams.toString(), replace };
}

/** Shared implementation; `binding` is fixed per public hook, so hook order never changes. */
function useListStateCore(
  definition: ListDefinition,
  binding: UrlBinding | null,
  scopedDefaults?: ListScopedFilterDefaults,
) {
  const bindingRef = useRef(binding);
  bindingRef.current = binding;
  const scopedDefaultsRef = useRef(scopedDefaults);
  scopedDefaultsRef.current = scopedDefaults;
  /** Query strings this list wrote itself and has not seen echoed back yet. */
  const pendingWrites = useRef<string[]>([]);
  const lastSeenParams = useRef(binding?.paramsString);

  const [initial] = useState(() => {
    const base = createInitialListQueryState(definition);
    const filtersBaseline = baselineFilters(definition, scopedDefaults);
    if (!binding) {
      return { ...base, filters: filtersBaseline };
    }
    const fromUrl = readListUrlState(
      definition,
      new URLSearchParams(binding.paramsString),
      filtersBaseline,
    );
    return { ...base, ...fromUrl };
  });

  const [search, setSearch] = useState(initial.search);
  const [debouncedSearch, setDebouncedSearch] = useState(initial.search);
  const [filters, setFilters] = useState<ListFilterValues>(initial.filters);
  const [sort, setSort] = useState<string | undefined>(initial.sort);

  const debounceMs = definition.search?.debounceMs ?? 300;

  useEffect(() => {
    const handle = window.setTimeout(() => {
      setDebouncedSearch(search);
    }, debounceMs);
    return () => window.clearTimeout(handle);
  }, [search, debounceMs]);

  // State -> URL.
  const synced = binding != null;
  useEffect(() => {
    const current = bindingRef.current;
    if (!synced || !current) {
      return;
    }
    const params = new URLSearchParams(current.paramsString);
    const next = mergeListUrlParams(
      params,
      listUrlParamValues(definition, { debouncedSearch, filters, sort }),
    );
    if (next) {
      pendingWrites.current.push(next.toString());
      current.replace(next);
    }
  }, [synced, definition, debouncedSearch, filters, sort]);

  // URL -> state, for changes this list did not write (back/forward, same route with other params).
  const paramsString = binding?.paramsString;
  useEffect(() => {
    if (paramsString === undefined || paramsString === lastSeenParams.current) {
      return;
    }
    lastSeenParams.current = paramsString;
    const echoIndex = pendingWrites.current.indexOf(paramsString);
    if (echoIndex >= 0) {
      pendingWrites.current.splice(0, echoIndex + 1);
      return;
    }
    pendingWrites.current = [];
    const fromUrl = readListUrlState(
      definition,
      new URLSearchParams(paramsString),
      baselineFilters(definition, scopedDefaultsRef.current),
    );
    setSearch(fromUrl.search);
    setDebouncedSearch(fromUrl.search);
    setFilters(fromUrl.filters);
    setSort(fromUrl.sort);
  }, [paramsString, definition]);

  // Changing a filter resets the filters that depend on it (`dependsOn`).
  const setFilter = useCallback(
    (key: string, value: ListFilterValues[string]) => {
      setFilters((current) => clearDependentFilters(definition, { ...current, [key]: value }, key));
    },
    [definition],
  );

  const setDateRange = useCallback(
    (key: string, value: ListDateRangeValue) => {
      setFilters((current) => clearDependentFilters(definition, { ...current, [key]: value }, key));
    },
    [definition],
  );

  const reset = useCallback(() => {
    setSearch('');
    setDebouncedSearch('');
    setFilters(baselineFilters(definition, scopedDefaultsRef.current));
    setSort(undefined);
  }, [definition]);

  const listQuery: ListQuery = useMemo(
    () => toListQuery(definition, { debouncedSearch, filters, sort }),
    [definition, debouncedSearch, filters, sort],
  );

  const hasActiveFilters = useMemo(() => {
    const baseFilters = baselineFilters(definition, scopedDefaults);
    if (search.trim() !== '') {
      return true;
    }
    for (const filter of definition.filters ?? []) {
      // Plain segments are always visible; only runtime-option segments and sheet filters count.
      if (isSegmentSelect(filter) && !filter.runtimeOptions) {
        continue;
      }
      if (JSON.stringify(filters[filter.key]) !== JSON.stringify(baseFilters[filter.key])) {
        return true;
      }
    }
    return false;
  }, [definition, scopedDefaults, search, filters]);

  return {
    search,
    setSearch,
    debouncedSearch,
    filters,
    sort,
    setSort,
    setFilter,
    setDateRange,
    reset,
    listQuery,
    hasActiveFilters,
  };
}

/** Local list state (search, filters, sort). Use for section lists and modals. */
export function useListState(
  definition: ListDefinition,
  scopedDefaults?: ListScopedFilterDefaults,
) {
  return useListStateCore(definition, null, scopedDefaults);
}

/**
 * List state mirrored in the URL (non-default values only, `router.replace`) and re-read when
 * the route's search params change externally. At most one synced list per page; the page needs
 * a `<Suspense>` boundary above it.
 */
export function useUrlSyncedListState(
  definition: ListDefinition,
  scopedDefaults?: ListScopedFilterDefaults,
) {
  return useListStateCore(definition, useUrlBinding(), scopedDefaults);
}
