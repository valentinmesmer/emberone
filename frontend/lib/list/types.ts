/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { ListFilterOptionsSourceId } from '@/lib/api/listFilterOptions';

export type ListFilterOptionValue = string;

export type ListFilterOption<T extends ListFilterOptionValue = ListFilterOptionValue> = {
  value: T;
  /** i18n key (resolved in list UI). */
  labelKey: string;
};

export type ListSortOption = {
  /** Wire value `<property>,<asc|desc>`, sent as the `sort` query param. */
  value: string;
  /** i18n key (resolved in list UI). */
  labelKey: string;
};

export type ListSortConfig = {
  /** Flat options after the implicit first option "Standard" (nothing sent, backend order). */
  options: readonly ListSortOption[];
};

export type ListSearchConfig = {
  /** Query param name; default `search`. */
  param?: string;
  debounceMs?: number;
};

/** Fields shared by every filter kind. */
export type ListFilterBase = {
  /**
   * Key of another filter this one depends on. The filter is hidden in the sheet until the parent
   * has a value, is never sent to the API while the parent is unset, and is cleared whenever the
   * parent changes (state and URL).
   */
  dependsOn?: string;
};

export type ListSelectFilter<
  Key extends string = string,
  Value extends ListFilterOptionValue = ListFilterOptionValue,
> = ListFilterBase & {
  kind: 'select';
  key: Key;
  labelKey: string;
  /**
   * Where the select is rendered. `segment` (default): full-width segmented control above the
   * search row. `sheet`: a section in the filter sheet (choice chips for up to 6 static options,
   * a dropdown otherwise), shown as chip below the search row when set. Sheet selects have an
   * implicit first option "all" (value `''`, not sent, no chip) and should not set `defaultValue`.
   */
  presentation?: 'segment' | 'sheet';
  /** Sheet UI for static options; default chips for up to six options, otherwise dropdown. */
  sheetUi?: 'chips' | 'dropdown';
  /** Label key of the "all" option of a `sheet` select; default `list.filters.all`. */
  allLabelKey?: string;
  /** Static options. Empty when {@link runtimeOptions} is set (options then come from the UI). */
  options: readonly ListFilterOption<Value>[];
  defaultValue?: Value;
  /**
   * Options from `GET …/list-filter-options` when {@link ListDefinition.filterOptionsSource} is
   * set, or via `ListControls` `runtimeOptions`. Sheet selects use `''` as “all”.
   */
  runtimeOptions?: true | { allLabelKey: string };
};

export type ListMultiSelectFilter<
  Key extends string = string,
  Value extends ListFilterOptionValue = ListFilterOptionValue,
> = ListFilterBase & {
  kind: 'multiSelect';
  key: Key;
  labelKey: string;
  /** Static options. Empty when {@link runtimeOptions} is set (options then come from the UI). */
  options: readonly ListFilterOption<Value>[];
  defaultValue?: readonly Value[];
  /**
   * Menubar filter badge / highlight counts this filter whenever at least one value is selected,
   * including the {@link defaultValue} (e.g. default “open only” still shows as one active filter).
   */
  countsInMenubarWhenSelected?: boolean;
  /** Inline “In Liste suchen”. Default: shown once there are more than 8 options. `false` hides it. */
  searchable?: boolean;
  /**
   * Options are only known at runtime (e.g. tenants, organizations). Supply them through
   * `ListControls` `runtimeOptions`.
   */
  runtimeOptions?: true;
};

export type ListDateRangeValue = {
  from?: string;
  to?: string;
};

export type ListDateRangeFilter<Key extends string = string> = ListFilterBase & {
  kind: 'dateRange';
  key: Key;
  labelKey: string;
  fromParam: string;
  toParam: string;
};

export type ListFilterField = ListSelectFilter | ListMultiSelectFilter | ListDateRangeFilter;

export type ListDefinition = {
  id: string;
  search?: ListSearchConfig;
  filters?: readonly ListFilterField[];
  /**
   * Per-list filter options API (`GET …/list-filter-options` next to the list GET). Must match a
   * backend {@code ListFilterOptionsSource} on the same controller.
   */
  filterOptionsSource?: ListFilterOptionsSourceId;
  /** Sort menu. Without it the list has no sort UI and never sends `sort`. */
  sort?: ListSortConfig;
  /**
   * Where sheet filters render. `modal` (default): filter button + dialog (+ chips when set).
   * `menubar`: compact category buttons with popovers under the search row (+ chips when set).
   */
  sheetLayout?: 'modal' | 'menubar';
};

export type ListFilterValues = Record<
  string,
  string | readonly string[] | ListDateRangeValue | undefined
>;

export type ListQueryState = {
  search: string;
  debouncedSearch: string;
  filters: ListFilterValues;
  /** Selected {@link ListSortOption.value}; `undefined` = Standard (nothing sent). */
  sort?: string;
};

/** API-facing list query (pagination + filters); serialized via {@link buildQuery}. */
export type ListQuery = {
  page?: number;
  size?: number;
  search?: string;
  sort?: string;
  [param: string]: string | number | boolean | readonly string[] | undefined;
};
