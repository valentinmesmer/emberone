/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ListFilter, Search, X } from 'lucide-react';
import { type ReactNode, useId, useMemo, useRef, useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  countActiveSheetFilters,
  ListFilterPanel,
  ListSheetFilterMenubar,
  sheetFiltersOf,
  visibleSheetFiltersOf,
} from '@/components/ui/list-filter-panel';
import { ListSortMenu } from '@/components/ui/list-sort';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { handleRadioGroupKeyDown } from '@/components/ui/radio-group-keys';
import { useTranslation } from '@/hooks/useTranslation';
import type { ListSelectFilter, useListState } from '@/lib/list';
import {
  createInitialFilterValues,
  isFilterEnabled,
  isSegmentSelect,
  type ListDefinition,
  listHasSort,
} from '@/lib/list';
import { cn } from '@/lib/utils/cn';

export type ListControlsState = Omit<ReturnType<typeof useListState>, 'setSearch'> & {
  setSearch: (value: string) => void;
};

interface ListFilterTabsProps {
  label: string;
  children: ReactNode;
  className?: string;
}

/**
 * Segmented control (single choice, WAI-ARIA radio group): one tab stop, arrow keys move the
 * selection, full width, scrolls sideways when options overflow.
 */
export function ListFilterTabs({ label, children, className }: ListFilterTabsProps) {
  return (
    <div
      className={cn(
        'flex w-full gap-0.5 overflow-x-auto rounded-xl border border-[var(--border-color)] p-0.5',
        className,
      )}
      role="radiogroup"
      aria-label={label}
      onKeyDown={handleRadioGroupKeyDown}
    >
      {children}
    </div>
  );
}

export type ListFilterOption<T extends string> = {
  value: T;
  label: string;
};

interface ListFilterGroupProps<T extends string> {
  label: string;
  value: T;
  onChange: (value: T) => void;
  options: ListFilterOption<T>[];
}

/** One-of-N filter as segmented control. */
export function ListFilterGroup<T extends string>({
  label,
  value,
  onChange,
  options,
}: ListFilterGroupProps<T>) {
  const hasSelection = options.some((option) => option.value === value);
  return (
    <ListFilterTabs label={label}>
      {options.map((option, index) => (
        <ListFilterTab
          key={option.value}
          scrollable={options.length > 3}
          selected={value === option.value}
          tabbable={hasSelection ? value === option.value : index === 0}
          onClick={() => onChange(option.value)}
        >
          {option.label}
        </ListFilterTab>
      ))}
    </ListFilterTabs>
  );
}

interface ListFilterTabProps {
  selected: boolean;
  /** More than 3 segments: keep natural width and scroll instead of stretching. */
  scrollable?: boolean;
  /** Roving tab index: only one segment of a group is a tab stop. Defaults to the selected one. */
  tabbable?: boolean;
  onClick: () => void;
  children: ReactNode;
}

export function ListFilterTab({
  selected,
  scrollable,
  tabbable = selected,
  onClick,
  children,
}: ListFilterTabProps) {
  return (
    <Button
      type="button"
      role="radio"
      aria-checked={selected}
      tabIndex={tabbable ? 0 : -1}
      variant={selected ? 'secondary' : 'ghost'}
      size="sm"
      className={cn(
        'h-9 whitespace-nowrap rounded-lg px-3 text-sm',
        scrollable ? 'flex-none' : 'flex-1',
        selected && 'shadow-sm',
      )}
      onClick={onClick}
    >
      {children}
    </Button>
  );
}

interface ListSearchInputProps {
  value: string;
  onChange: (value: string) => void;
  className?: string;
}

/** Debounced search is handled by {@link useListState}; this binds the immediate input value. */
export function ListSearchInput({ value, onChange, className }: ListSearchInputProps) {
  const t = useTranslation();
  const inputId = useId();
  const inputRef = useRef<HTMLInputElement>(null);

  return (
    <div className={cn('relative min-w-0 flex-1', className)}>
      <label className="sr-only" htmlFor={inputId}>
        {t('list.search.label')}
      </label>
      <Search
        className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--text-secondary)]"
        aria-hidden
      />
      <input
        ref={inputRef}
        id={inputId}
        type="search"
        enterKeyHint="search"
        autoComplete="off"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        onKeyDown={(event) => {
          if (event.key === 'Enter') {
            event.preventDefault();
            event.currentTarget.blur();
          }
        }}
        placeholder={t('list.search.placeholder')}
        className={cn(
          'h-10 w-full rounded-xl border border-[var(--border-color)] bg-[var(--card-bg)]',
          'py-2 pl-9 pr-10 text-sm text-[var(--text-primary)] placeholder:text-[var(--text-secondary)]',
          '[&::-webkit-search-cancel-button]:appearance-none [&::-webkit-search-decoration]:appearance-none',
          'focus:border-[var(--fire-red)] focus:outline-none focus:ring-2 focus:ring-[var(--focus-ring)]',
        )}
      />
      {value !== '' ? (
        <button
          type="button"
          aria-label={t('list.search.clear')}
          onClick={() => {
            onChange('');
            inputRef.current?.focus();
          }}
          className={cn(
            'absolute right-0.5 top-1/2 flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-lg',
            'text-[var(--text-secondary)] hover:text-[var(--text-primary)]',
            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)]',
          )}
        >
          <X className="h-4 w-4" aria-hidden />
        </button>
      ) : null}
    </div>
  );
}

interface MenubarFiltersToggleProps {
  expanded: boolean;
  activeCount: number;
  controlsId: string;
  onToggle: () => void;
}

/** Shows or hides the menubar filter row (`sheetLayout: 'menubar'`). */
function MenubarFiltersToggle({
  expanded,
  activeCount,
  controlsId,
  onToggle,
}: MenubarFiltersToggleProps) {
  const t = useTranslation();
  const ariaLabel = expanded
    ? t('list.filters.hideMenubar')
    : activeCount > 0
      ? `${t('list.filters.showMenubar')}, ${t('list.filters.activeCount', { count: activeCount })}`
      : t('list.filters.showMenubar');

  return (
    <button
      type="button"
      aria-label={ariaLabel}
      aria-expanded={expanded}
      aria-controls={controlsId}
      onClick={onToggle}
      className={cn(
        'relative inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-xl p-0 text-sm font-medium transition-colors duration-200',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--focus-ring-offset)]',
        activeCount > 0 || expanded
          ? 'border border-[var(--fire-red)]/40 bg-[var(--fire-red)]/10 text-[var(--fire-red)] dark:text-[var(--fire-red-light)]'
          : 'border border-[var(--border-color)] bg-[var(--card-bg)] text-[var(--text-secondary)] hover:bg-[var(--hover-bg)] hover:text-[var(--text-primary)]',
      )}
    >
      <ListFilter className="h-4 w-4" aria-hidden strokeWidth={2} />
      {activeCount > 0 ? (
        <Badge
          tone="accent"
          pill
          className="pointer-events-none absolute -right-1.5 -top-1.5 min-w-5 justify-center px-1.5"
          aria-hidden
        >
          {activeCount}
        </Badge>
      ) : null}
    </button>
  );
}

function resolveSelectValue(filter: ListSelectFilter, raw: ListControlsState['filters'][string]) {
  if (typeof raw === 'string' && raw !== '') {
    return raw;
  }
  return filter.defaultValue ?? filter.options[0]?.value ?? '';
}

export interface ListControlsProps {
  definition: ListDefinition;
  state: ListControlsState;
  /** Per select filter key and option value: appended to the segment label as "Label (n)". */
  selectCounts?: Record<string, Partial<Record<string, number>>>;
  /**
   * Options for select filters declared with `runtimeOptions`, by filter key (the "all" option is
   * added automatically). A key that is absent means "still loading": segments stay hidden, sheet
   * selects render disabled. An empty array means "none": the filter is hidden.
   */
  runtimeOptions?: Record<string, readonly ListFilterOption<string>[]>;
  className?: string;
}

/**
 * Renders a {@link ListDefinition}: select segments on top, then one row with the full-width
 * search, a sort icon ({@link ListSortMenu}, only with `definition.sort` options)
 * and a filter icon on the right (modal sheet or menubar row toggle), then optional menubar sheet
 * filters when expanded.
 */
export function ListControls({
  definition,
  state,
  selectCounts,
  runtimeOptions,
  className,
}: ListControlsProps) {
  const t = useTranslation();
  const menubarRegionId = useId();
  const [menubarOpen, setMenubarOpen] = useState(false);
  const segmentFilters = (definition.filters ?? []).filter(
    (filter): filter is ListSelectFilter =>
      isSegmentSelect(filter) && isFilterEnabled(definition, filter, state.filters),
  );
  const sheetFilters = visibleSheetFiltersOf(definition, state.filters, runtimeOptions);
  const hasSearch = definition.search != null;
  const hasSort = listHasSort(definition);
  const menubarSheet = definition.sheetLayout === 'menubar' && sheetFilters.length > 0;
  const menubarAlwaysVisible = menubarSheet && !hasSearch;
  const menubarActiveCount = useMemo(
    () => countActiveSheetFilters(definition.filters ?? [], state.filters),
    [definition.filters, state.filters],
  );

  const resetSheetFilters = () => {
    const initial = createInitialFilterValues(definition);
    for (const filter of sheetFiltersOf(definition.filters)) {
      if (filter.kind === 'dateRange') {
        state.setDateRange(filter.key, {});
      } else {
        state.setFilter(filter.key, initial[filter.key]);
      }
    }
  };

  const segments = segmentFilters.map((filter) => {
    if (filter.runtimeOptions) {
      const extra = runtimeOptions?.[filter.key] ?? [];
      if (extra.length === 0) {
        return null;
      }
      const raw = state.filters[filter.key];
      return (
        <ListFilterGroup
          key={filter.key}
          label={t(filter.labelKey)}
          value={typeof raw === 'string' ? raw : ''}
          onChange={(next) => state.setFilter(filter.key, next)}
          options={[
            {
              value: '',
              label: t(
                typeof filter.runtimeOptions === 'object'
                  ? filter.runtimeOptions.allLabelKey
                  : 'list.filters.all',
              ),
            },
            ...extra,
          ]}
        />
      );
    }
    return (
      <ListFilterGroup
        key={filter.key}
        label={t(filter.labelKey)}
        value={resolveSelectValue(filter, state.filters[filter.key])}
        onChange={(next) => state.setFilter(filter.key, next)}
        options={filter.options.map((option) => {
          const count = selectCounts?.[filter.key]?.[option.value];
          const label = t(option.labelKey);
          return { value: option.value, label: count == null ? label : `${label} (${count})` };
        })}
      />
    );
  });

  const sortNode = hasSort ? (
    <ListSortMenu definition={definition} value={state.sort} onChange={state.setSort} />
  ) : null;
  const hasVisibleSegments = segments.some(Boolean);
  const sortBesideMenubar = Boolean(sortNode && menubarSheet && !hasSearch);
  const sortBesideSegments = Boolean(
    sortNode && !hasSearch && hasVisibleSegments && !sortBesideMenubar,
  );
  const sortInModalRow = Boolean(sortNode && !sortBesideMenubar && !sortBesideSegments);

  const modalFilterRow =
    hasSearch || (!menubarSheet && (sheetFilters.length > 0 || sortInModalRow)) ? (
      <div className="flex w-full items-center gap-2">
        {hasSearch ? <ListSearchInput value={state.search} onChange={state.setSearch} /> : null}
        {sortInModalRow ? sortNode : null}
        {menubarSheet && hasSearch ? (
          <MenubarFiltersToggle
            expanded={menubarOpen}
            activeCount={menubarActiveCount}
            controlsId={menubarRegionId}
            onToggle={() => setMenubarOpen((open) => !open)}
          />
        ) : !menubarSheet ? (
          <ListFilterPanel
            filters={sheetFilters}
            values={state.filters}
            iconOnly={hasSearch}
            runtimeOptions={runtimeOptions}
            onFilterChange={state.setFilter}
            onDateRangeChange={state.setDateRange}
            onReset={resetSheetFilters}
          />
        ) : null}
      </div>
    ) : null;

  const menubar =
    menubarAlwaysVisible || (menubarSheet && menubarOpen) ? (
      <div className="flex w-full min-w-0 items-center gap-2">
        <ListSheetFilterMenubar
          id={menubarRegionId}
          filters={sheetFilters}
          values={state.filters}
          runtimeOptions={runtimeOptions}
          fillWidth={false}
          className={sortBesideMenubar ? 'min-w-0 flex-1' : undefined}
          onFilterChange={state.setFilter}
          onDateRangeChange={state.setDateRange}
        />
        {sortBesideMenubar ? sortNode : null}
      </div>
    ) : null;

  const segmentRow = hasVisibleSegments ? (
    <div className="flex w-full min-w-0 items-center gap-2">
      <div className="flex min-w-0 flex-1 flex-col gap-2">{segments}</div>
      {sortBesideSegments ? sortNode : null}
    </div>
  ) : null;

  if (!segmentRow && !modalFilterRow && !menubar) {
    return null;
  }

  return (
    <div className={cn('mb-3 flex flex-col gap-2', className)}>
      {segmentRow}
      {modalFilterRow}
      {menubar}
    </div>
  );
}

interface ListNoResultsProps {
  onReset: () => void;
  className?: string;
}

/** Empty state when filters/search yield no rows; offers a full reset via {@link useListState.reset}. */
export function ListNoResults({ onReset, className }: ListNoResultsProps) {
  const t = useTranslation();

  return (
    <EmptyStateCard className={cn('space-y-3 py-6', className)}>
      <p className="font-medium text-[var(--text-primary)]">{t('list.noResults.title')}</p>
      <Button type="button" variant="outline" size="sm" onClick={onReset}>
        {t('list.noResults.reset')}
      </Button>
    </EmptyStateCard>
  );
}

export interface ListLoadMoreProps {
  hasNextPage: boolean;
  isFetchingNextPage: boolean;
  onLoadMore: () => void;
  className?: string;
}

/** Appends the next page when the list has more rows. */
export function ListLoadMore({
  hasNextPage,
  isFetchingNextPage,
  onLoadMore,
  className,
}: ListLoadMoreProps) {
  const t = useTranslation();

  if (!hasNextPage) {
    return null;
  }

  return (
    <div className={cn('flex justify-center py-3', className)}>
      <Button
        type="button"
        variant="outline"
        size="sm"
        onClick={onLoadMore}
        disabled={isFetchingNextPage}
      >
        {isFetchingNextPage ? t('common.loading') : t('list.loadMore')}
      </Button>
    </div>
  );
}
