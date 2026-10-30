/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ChevronDown, ChevronRight, ListFilter } from 'lucide-react';
import { useCallback, useEffect, useId, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { handleRadioGroupKeyDown } from '@/components/ui/radio-group-keys';
import { Select } from '@/components/ui/select';
import { useTranslation } from '@/hooks/useTranslation';
import type {
  ListDatePreset,
  ListDateRangeFilter,
  ListDateRangeValue,
  ListDefinition,
  ListFilterField,
  ListFilterValues,
  ListMultiSelectFilter,
  ListSelectFilter,
} from '@/lib/list';
import {
  computePresetRange,
  createInitialFilterValues,
  detectDatePreset,
  formatIsoDateDe,
  isDateRangeActive,
  isFilterEnabled,
  isFilterValueSet,
  isSheetFilter,
  LIST_DATE_PRESETS,
} from '@/lib/list';
import { menubarPopoverCoords } from '@/lib/ui/menubarPopoverCoords';
import { cn } from '@/lib/utils/cn';

type SheetFilter = ListDateRangeFilter | ListMultiSelectFilter | ListSelectFilter;

export type ListRuntimeOptions = Record<string, readonly { value: string; label: string }[]>;

/** Filters that live in the sheet: date range, multi-select and selects with `presentation: 'sheet'`. */
export function sheetFiltersOf(filters: readonly ListFilterField[] | undefined): SheetFilter[] {
  return (filters ?? []).filter((filter): filter is SheetFilter => isSheetFilter(filter));
}

/**
 * Sheet filters that are available right now: the parent of a `dependsOn` filter has a value, and
 * a runtime-option select has options (or a value, or its options are still loading, i.e. the key
 * is absent from `runtimeOptions`).
 */
export function visibleSheetFiltersOf(
  definition: ListDefinition,
  values: ListFilterValues,
  runtimeOptions?: ListRuntimeOptions,
): SheetFilter[] {
  const baseline = createInitialFilterValues(definition);
  return sheetFiltersOf(definition.filters).filter((filter) => {
    if (!isFilterEnabled(definition, filter, values)) {
      return false;
    }
    if ((filter.kind === 'select' || filter.kind === 'multiSelect') && filter.runtimeOptions) {
      const options = runtimeOptions?.[filter.key];
      return (
        options === undefined ||
        options.length > 0 ||
        isFilterValueSet(filter, values[filter.key], baseline[filter.key])
      );
    }
    return true;
  });
}

function asDateRange(raw: ListFilterValues[string]): ListDateRangeValue {
  return raw && typeof raw === 'object' && !Array.isArray(raw) ? (raw as ListDateRangeValue) : {};
}

function asStringArray(raw: ListFilterValues[string]): readonly string[] {
  return Array.isArray(raw) ? raw : [];
}

function isFilterActive(
  filter: SheetFilter,
  values: ListFilterValues,
  baseline: ListFilterValues,
): boolean {
  if (filter.kind === 'multiSelect' && filter.countsInMenubarWhenSelected) {
    return asStringArray(values[filter.key]).length > 0;
  }
  return isFilterValueSet(filter, values[filter.key], baseline[filter.key]);
}

/** Number of sheet filters that differ from their default. */
export function countActiveSheetFilters(
  filters: readonly ListFilterField[],
  values: ListFilterValues,
): number {
  const baseline = createInitialFilterValues({ id: '_', filters });
  return sheetFiltersOf(filters).filter((filter) => isFilterActive(filter, values, baseline))
    .length;
}

const DATE_PRESET_KEY: Record<ListDatePreset, string> = {
  thisMonth: 'list.dateRange.presets.thisMonth',
  lastMonth: 'list.dateRange.presets.lastMonth',
  thisYear: 'list.dateRange.presets.thisYear',
};

function useDateRangeLabel() {
  const t = useTranslation();
  return (range: ListDateRangeValue): string => {
    const preset = detectDatePreset(range);
    if (preset) {
      return t(DATE_PRESET_KEY[preset]);
    }
    const { from, to } = range;
    if (from && to) {
      const sameYear = from.slice(0, 4) === to.slice(0, 4);
      const fromLabel = formatIsoDateDe(from);
      return `${sameYear ? fromLabel.slice(0, 6) : fromLabel}-${formatIsoDateDe(to)}`;
    }
    if (from) {
      return t('list.dateRange.chipFrom', { date: formatIsoDateDe(from) });
    }
    return t('list.dateRange.chipTo', { date: formatIsoDateDe(to ?? '') });
  };
}

/** Show inline search once a filter has more than this many options (typical admin tenant/org lists). */
const MULTI_SELECT_SEARCH_THRESHOLD = 8;

/** Use hub rows (one category per screen) from this many sheet filters upward. */
const FILTER_HUB_MIN_FILTERS = 2;

function filterSheetSummary(
  filter: SheetFilter,
  values: ListFilterValues,
  baseline: ListFilterValues,
  runtimeOptions: ListRuntimeOptions | undefined,
  t: (key: string, params?: { count: number }) => string,
  dateRangeLabel: (range: ListDateRangeValue) => string,
): string {
  const allLabel = t('list.filters.all');
  if (!isFilterActive(filter, values, baseline)) {
    return allLabel;
  }
  if (filter.kind === 'dateRange') {
    return dateRangeLabel(asDateRange(values[filter.key]));
  }
  if (filter.kind === 'select') {
    const current = values[filter.key];
    if (typeof current !== 'string' || current === '') {
      return allLabel;
    }
    const option = filter.runtimeOptions
      ? runtimeOptions?.[filter.key]?.find((entry) => entry.value === current)
      : filter.options
          .filter((entry) => entry.value === current)
          .map((entry) => ({ value: entry.value, label: t(entry.labelKey) }))[0];
    return option?.label ?? allLabel;
  }
  const selected = asStringArray(values[filter.key]);
  const optionList = filter.runtimeOptions
    ? (runtimeOptions?.[filter.key] ?? [])
    : filter.options.map((option) => ({ value: option.value, label: t(option.labelKey) }));
  const labels = selected
    .map((value) => optionList.find((entry) => entry.value === value)?.label)
    .filter((label): label is string => Boolean(label));
  if (labels.length === 0) {
    return allLabel;
  }
  if (labels.length <= 2) {
    return labels.join(', ');
  }
  return t('list.filters.selectedCount', { count: labels.length });
}

function FilterSheetHub({
  filters,
  values,
  baseline,
  runtimeOptions,
  onSelect,
}: {
  filters: readonly SheetFilter[];
  values: ListFilterValues;
  baseline: ListFilterValues;
  runtimeOptions?: ListRuntimeOptions;
  onSelect: (key: string) => void;
}) {
  const t = useTranslation();
  const dateRangeLabel = useDateRangeLabel();

  return (
    <div className="space-y-3">
      <p className="text-sm text-[var(--text-secondary)]">{t('list.filters.hubHint')}</p>
      <ul className="divide-y divide-[var(--border-color)] overflow-hidden rounded-2xl bg-[var(--surface-muted)]/50 ring-1 ring-inset ring-[var(--border-color)]">
        {filters.map((filter) => {
          const active = isFilterActive(filter, values, baseline);
          const summary = filterSheetSummary(
            filter,
            values,
            baseline,
            runtimeOptions,
            t,
            dateRangeLabel,
          );
          return (
            <li key={filter.key}>
              <button
                type="button"
                onClick={() => onSelect(filter.key)}
                className={cn(
                  'flex min-h-14 w-full items-center gap-3 px-4 py-3 text-left transition-colors',
                  'hover:bg-[var(--hover-bg)] active:bg-[var(--hover-bg)]',
                  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-[var(--focus-ring)]',
                )}
              >
                <span className="min-w-0 flex-1 font-medium text-[var(--text-primary)]">
                  {t(filter.labelKey)}
                </span>
                <span
                  className={cn(
                    'max-w-[45%] truncate text-right text-sm',
                    active ? 'text-[var(--text-primary)]' : 'text-[var(--text-secondary)]',
                  )}
                >
                  {summary}
                </span>
                <ChevronRight
                  className="h-4 w-4 shrink-0 text-[var(--text-secondary)]"
                  aria-hidden
                />
              </button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

function MultiSelectFilterSection({
  filter,
  value,
  runtimeOptions,
  onChange,
  showLegend = true,
  density = 'default',
}: {
  filter: ListMultiSelectFilter;
  value: readonly string[];
  runtimeOptions?: ListRuntimeOptions[string];
  onChange: (next: readonly string[]) => void;
  /** False in hub detail view (title is already in the header). */
  showLegend?: boolean;
  /** Tighter list for filter menubar popovers. */
  density?: 'default' | 'compact';
}) {
  const t = useTranslation();
  const labelId = useId();
  const [search, setSearch] = useState('');
  const selected = new Set(value);
  const options = filter.runtimeOptions
    ? (runtimeOptions ?? [])
    : filter.options.map((option) => ({ value: option.value, label: t(option.labelKey) }));
  const loading = filter.runtimeOptions && runtimeOptions === undefined;
  const normalizedSearch = search.trim().toLowerCase();
  const visibleOptions = useMemo(() => {
    if (!normalizedSearch) {
      return options;
    }
    return options.filter((option) => option.label.toLowerCase().includes(normalizedSearch));
  }, [normalizedSearch, options]);
  const showSearch = filter.searchable ?? options.length > MULTI_SELECT_SEARCH_THRESHOLD;
  const compact = density === 'compact';

  const toggle = (optionValue: string, next: boolean) => {
    const copy = new Set(selected);
    if (next) {
      copy.add(optionValue);
    } else {
      copy.delete(optionValue);
    }
    onChange([...copy]);
  };

  return (
    <fieldset className={cn(compact ? 'space-y-1' : 'space-y-2.5')} disabled={loading}>
      {showLegend ? (
        <div className="flex items-center justify-between gap-2">
          <legend
            id={labelId}
            className="text-xs font-semibold uppercase tracking-wide text-[var(--text-secondary)]"
          >
            {t(filter.labelKey)}
            {selected.size > 0 ? (
              <span className="ml-2 normal-case tracking-normal text-[var(--text-primary)]">
                ({t('list.filters.selectedCount', { count: selected.size })})
              </span>
            ) : null}
          </legend>
          {selected.size > 0 ? (
            <button
              type="button"
              onClick={() => onChange([])}
              className="shrink-0 text-xs font-medium text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
            >
              {t('list.filters.clearSelection')}
            </button>
          ) : null}
        </div>
      ) : !compact && selected.size > 0 ? (
        <div className="mb-0.5 flex justify-end">
          <button
            type="button"
            onClick={() => onChange([])}
            className="text-xs font-medium text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
          >
            {t('list.filters.clearSelection')}
          </button>
        </div>
      ) : null}
      {loading ? (
        <p
          className={cn('text-[var(--text-secondary)]', compact ? 'px-1 py-1 text-xs' : 'text-sm')}
        >
          {t('common.loading')}
        </p>
      ) : options.length === 0 ? (
        <p
          className={cn('text-[var(--text-secondary)]', compact ? 'px-1 py-1 text-xs' : 'text-sm')}
        >
          {t('common.selectNoOptions')}
        </p>
      ) : (
        <>
          {showSearch ? (
            <Input
              type="search"
              enterKeyHint="search"
              autoComplete="off"
              value={search}
              placeholder={t('list.filters.searchInList')}
              onChange={(event) => setSearch(event.target.value)}
              className={compact ? 'h-8 text-sm' : 'h-10'}
            />
          ) : null}
          <ul
            className={cn(
              'overflow-y-auto',
              compact
                ? 'max-h-44 divide-y divide-[var(--border-color)]'
                : cn(
                    'rounded-2xl bg-[var(--surface-muted)]/70 ring-1 ring-inset ring-[var(--border-color)]',
                    showLegend ? 'max-h-48' : 'max-h-[min(58dvh,22rem)]',
                  ),
            )}
          >
            {visibleOptions.length === 0 ? (
              <li
                className={cn(
                  'text-center text-[var(--text-secondary)]',
                  compact ? 'px-2 py-3 text-xs' : 'px-4 py-5 text-sm',
                )}
              >
                {t('list.noResults.title')}
              </li>
            ) : (
              visibleOptions.map((option) => (
                <li key={option.value}>
                  <Checkbox
                    checked={selected.has(option.value)}
                    label={option.label}
                    size={compact ? 'sm' : 'md'}
                    className={
                      compact
                        ? cn(
                            'min-h-9 w-full px-2 py-1.5 active:bg-[var(--hover-bg)]',
                            '[&_span]:font-normal [&_span]:whitespace-nowrap max-sm:[&_span]:whitespace-normal',
                          )
                        : 'min-h-12 w-full px-3.5 py-2.5 active:bg-[var(--hover-bg)] [&_span]:font-normal'
                    }
                    onCheckedChange={(next) => toggle(option.value, next)}
                  />
                </li>
              ))
            )}
          </ul>
          {compact && selected.size > 0 ? (
            <button
              type="button"
              onClick={() => onChange([])}
              className={cn(
                'mt-1 w-full border-t border-[var(--border-color)] pt-1.5 text-center text-xs text-[var(--text-secondary)]',
                'hover:text-[var(--text-primary)] active:bg-[var(--hover-bg)]',
              )}
            >
              {t('list.filters.clearSelection')}
            </button>
          ) : null}
        </>
      )}
    </fieldset>
  );
}

/** At most this many static options render as choice chips; more become a dropdown. */
const SELECT_CHIP_MAX_OPTIONS = 6;

function SelectFilterSection({
  filter,
  value,
  runtimeOptions,
  onChange,
}: {
  filter: ListSelectFilter;
  value: string;
  runtimeOptions?: ListRuntimeOptions[string];
  onChange: (next: string) => void;
}) {
  const t = useTranslation();
  const labelId = useId();
  const label = t(filter.labelKey);
  const allLabel = t(
    (typeof filter.runtimeOptions === 'object' ? filter.runtimeOptions.allLabelKey : undefined) ??
      filter.allLabelKey ??
      'list.filters.all',
  );
  const options = [
    { value: '', label: allLabel },
    ...(filter.runtimeOptions
      ? (runtimeOptions ?? [])
      : filter.options.map((option) => ({ value: option.value, label: t(option.labelKey) }))),
  ];

  const useDropdown =
    filter.sheetUi === 'dropdown' ||
    filter.runtimeOptions ||
    filter.options.length > SELECT_CHIP_MAX_OPTIONS;

  if (useDropdown) {
    return (
      <Select
        label={label}
        value={value}
        options={options}
        disabled={runtimeOptions === undefined && filter.runtimeOptions != null}
        onChange={(event) => onChange(event.target.value)}
      />
    );
  }

  const hasSelection = options.some((option) => option.value === value);
  return (
    <div className="space-y-2">
      <p id={labelId} className="text-sm font-semibold text-[var(--text-primary)]">
        {label}
      </p>
      <div
        role="radiogroup"
        aria-labelledby={labelId}
        className="flex flex-wrap gap-2"
        onKeyDown={handleRadioGroupKeyDown}
      >
        {options.map((option, index) => {
          const selected = option.value === value;
          return (
            <Button
              key={option.value}
              type="button"
              role="radio"
              aria-checked={selected}
              tabIndex={hasSelection ? (selected ? 0 : -1) : index === 0 ? 0 : -1}
              size="sm"
              variant={selected ? 'secondary' : 'outline'}
              onClick={() => onChange(option.value)}
            >
              {option.label}
            </Button>
          );
        })}
      </div>
    </div>
  );
}

function DateRangeFilterSection({
  filter,
  value,
  onChange,
}: {
  filter: ListDateRangeFilter;
  value: ListDateRangeValue;
  onChange: (next: ListDateRangeValue) => void;
}) {
  const t = useTranslation();
  const preset = detectDatePreset(value);
  const [forceCustom, setForceCustom] = useState(false);
  const customActive = forceCustom || (isDateRangeActive(value) && preset == null);

  return (
    <fieldset className="space-y-3">
      <legend className="text-sm font-semibold text-[var(--text-primary)]">
        {t(filter.labelKey)}
      </legend>
      <div className="flex flex-wrap gap-2">
        {LIST_DATE_PRESETS.map((id) => {
          const selected = !forceCustom && preset === id;
          return (
            <Button
              key={id}
              type="button"
              size="sm"
              variant={selected ? 'secondary' : 'outline'}
              aria-pressed={selected}
              onClick={() => {
                setForceCustom(false);
                onChange(selected ? {} : computePresetRange(id));
              }}
            >
              {t(DATE_PRESET_KEY[id])}
            </Button>
          );
        })}
        <Button
          type="button"
          size="sm"
          variant={customActive ? 'secondary' : 'outline'}
          aria-pressed={customActive}
          onClick={() => setForceCustom(true)}
        >
          {t('list.dateRange.presets.custom')}
        </Button>
      </div>
      {customActive ? (
        <div className="grid grid-cols-2 gap-3">
          <Input
            type="date"
            label={t('list.dateRange.from')}
            value={value.from ?? ''}
            max={value.to}
            onChange={(event) => {
              const from = event.target.value || undefined;
              onChange({ from, to: from && value.to && from > value.to ? from : value.to });
            }}
          />
          <Input
            type="date"
            label={t('list.dateRange.to')}
            value={value.to ?? ''}
            min={value.from}
            onChange={(event) => {
              const to = event.target.value || undefined;
              onChange({ from: to && value.from && to < value.from ? to : value.from, to });
            }}
          />
        </div>
      ) : null}
    </fieldset>
  );
}

type ListSheetFiltersProps = {
  filters: readonly SheetFilter[];
  values: ListFilterValues;
  onFilterChange: (key: string, value: ListFilterValues[string]) => void;
  onDateRangeChange: (key: string, value: ListDateRangeValue) => void;
  runtimeOptions?: ListRuntimeOptions;
};

function renderSheetFilterSection(
  filter: SheetFilter,
  {
    values,
    onFilterChange,
    onDateRangeChange,
    runtimeOptions,
    showLegend,
    density = 'default',
  }: ListSheetFiltersProps & { showLegend: boolean; density?: 'default' | 'compact' },
) {
  if (filter.kind === 'multiSelect') {
    return (
      <MultiSelectFilterSection
        key={filter.key}
        filter={filter}
        showLegend={showLegend}
        density={density}
        value={asStringArray(values[filter.key])}
        runtimeOptions={runtimeOptions?.[filter.key]}
        onChange={(next) => onFilterChange(filter.key, next)}
      />
    );
  }
  if (filter.kind === 'select') {
    const raw = values[filter.key];
    return (
      <SelectFilterSection
        key={filter.key}
        filter={filter}
        value={typeof raw === 'string' ? raw : ''}
        runtimeOptions={runtimeOptions?.[filter.key]}
        onChange={(next) => onFilterChange(filter.key, next)}
      />
    );
  }
  return (
    <DateRangeFilterSection
      key={filter.key}
      filter={filter}
      value={asDateRange(values[filter.key])}
      onChange={(next) => onDateRangeChange(filter.key, next)}
    />
  );
}

function sheetFilterActiveCount(
  filter: SheetFilter,
  values: ListFilterValues,
  baseline: ListFilterValues,
): number {
  if (!isFilterActive(filter, values, baseline)) {
    return 0;
  }
  if (filter.kind === 'multiSelect') {
    return asStringArray(values[filter.key]).length;
  }
  return 1;
}

type MenubarToggleTarget =
  | { mode: 'multiSelect'; filter: ListMultiSelectFilter; option: { value: string; label: string } }
  | { mode: 'select'; filter: ListSelectFilter; option: { value: string; label: string } };

/**
 * Filter that statically declares exactly one option (a yes/no switch such as "include X"): the
 * menubar uses a direct toggle instead of a popover. Runtime options never qualify, a list that
 * happens to hold one entry stays a dropdown.
 */
function menubarToggleTarget(
  filter: SheetFilter,
  t: (key: string) => string,
): MenubarToggleTarget | null {
  if (filter.kind === 'dateRange' || filter.runtimeOptions || filter.options.length !== 1) {
    return null;
  }
  const [only] = filter.options;
  const option = { value: only.value, label: t(only.labelKey) };
  return filter.kind === 'multiSelect'
    ? { mode: 'multiSelect', filter, option }
    : { mode: 'select', filter, option };
}

function menubarFilterButtonClass(active: boolean, fillWidth?: boolean) {
  return cn(
    'inline-flex h-9 items-center gap-1.5 whitespace-nowrap rounded-xl border px-2.5 text-sm font-medium transition-colors',
    fillWidth && 'w-full justify-center',
    'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--focus-ring-offset)]',
    active
      ? 'border-[var(--fire-red)]/40 bg-[var(--fire-red)]/10 text-[var(--fire-red)] dark:text-[var(--fire-red-light)]'
      : 'border-[var(--border-color)] bg-[var(--card-bg)] text-[var(--text-secondary)] hover:bg-[var(--hover-bg)] hover:text-[var(--text-primary)]',
  );
}

function SheetFilterMenubarToggleItem({
  target,
  values,
  fillWidth,
  onFilterChange,
}: {
  target: MenubarToggleTarget;
  values: ListFilterValues;
  fillWidth?: boolean;
  onFilterChange: (key: string, value: ListFilterValues[string]) => void;
}) {
  const t = useTranslation();
  const filter = target.filter;
  const label = t(filter.labelKey);
  const active =
    target.mode === 'multiSelect'
      ? asStringArray(values[filter.key]).includes(target.option.value)
      : values[filter.key] === target.option.value;

  const toggle = () => {
    if (target.mode === 'multiSelect') {
      onFilterChange(filter.key, active ? [] : [target.option.value]);
      return;
    }
    onFilterChange(filter.key, active ? '' : target.option.value);
  };

  return (
    <div className={cn('relative', fillWidth ? 'min-w-0 flex-1' : 'shrink-0')}>
      <button
        type="button"
        aria-pressed={active}
        aria-label={label}
        onClick={toggle}
        className={menubarFilterButtonClass(active, fillWidth)}
      >
        <span className="truncate">{label}</span>
      </button>
    </div>
  );
}

function SheetFilterMenubarItem({
  filter,
  isOpen,
  onOpenChange,
  sectionProps,
  fillWidth,
}: {
  filter: SheetFilter;
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
  sectionProps: ListSheetFiltersProps;
  fillWidth?: boolean;
}) {
  const t = useTranslation();
  const rootRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const popoverRef = useRef<HTMLDivElement>(null);
  const [popoverCoords, setPopoverCoords] = useState<{ top: number; left: number } | null>(null);
  const baseline = useMemo(
    () => createInitialFilterValues({ id: '_', filters: [filter] }),
    [filter],
  );
  const activeCount = sheetFilterActiveCount(filter, sectionProps.values, baseline);
  const label = t(filter.labelKey);

  const updatePopoverCoords = useCallback(() => {
    const button = buttonRef.current;
    if (!button) {
      return;
    }
    const rect = button.getBoundingClientRect();
    const popoverWidth = popoverRef.current?.getBoundingClientRect().width;
    setPopoverCoords(menubarPopoverCoords(rect, window.innerWidth, popoverWidth));
  }, []);

  useLayoutEffect(() => {
    if (!isOpen) {
      setPopoverCoords(null);
      return;
    }
    updatePopoverCoords();
    const frame = requestAnimationFrame(updatePopoverCoords);
    window.addEventListener('resize', updatePopoverCoords);
    window.addEventListener('scroll', updatePopoverCoords, true);
    return () => {
      cancelAnimationFrame(frame);
      window.removeEventListener('resize', updatePopoverCoords);
      window.removeEventListener('scroll', updatePopoverCoords, true);
    };
  }, [isOpen, updatePopoverCoords]);

  useEffect(() => {
    if (!isOpen) {
      return;
    }
    const handlePointerDown = (event: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        onOpenChange(false);
      }
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onOpenChange(false);
      }
    };
    document.addEventListener('mousedown', handlePointerDown);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handlePointerDown);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onOpenChange]);

  return (
    <div ref={rootRef} className={cn('relative', fillWidth ? 'min-w-0 flex-1' : 'shrink-0')}>
      <button
        ref={buttonRef}
        type="button"
        aria-expanded={isOpen}
        aria-haspopup="dialog"
        aria-label={
          activeCount > 0
            ? `${label}, ${t('list.filters.activeCount', { count: activeCount })}`
            : label
        }
        onClick={() => onOpenChange(!isOpen)}
        className={menubarFilterButtonClass(activeCount > 0, fillWidth)}
      >
        <span className="truncate">{label}</span>
        {activeCount > 0 ? (
          <Badge tone="accent" pill className="min-w-5 justify-center px-1.5" aria-hidden>
            {activeCount}
          </Badge>
        ) : null}
        <ChevronDown
          className={cn('h-4 w-4 shrink-0 opacity-70 transition-transform', isOpen && 'rotate-180')}
          aria-hidden
        />
      </button>
      {isOpen && popoverCoords ? (
        <div
          ref={popoverRef}
          role="dialog"
          aria-label={label}
          style={{ top: popoverCoords.top, left: popoverCoords.left }}
          className={cn(
            'fixed z-[100] w-max min-w-[11rem] max-w-[min(26rem,calc(100vw-1.5rem))] rounded-lg border border-[var(--border-color)]',
            'bg-[var(--card-bg)] p-1.5 shadow-md',
          )}
        >
          {renderSheetFilterSection(filter, {
            ...sectionProps,
            showLegend: false,
            density: 'compact',
          })}
        </div>
      ) : null}
    </div>
  );
}

/** Compact filter buttons with popovers (`ListDefinition.sheetLayout` = `menubar`). Live updates. */
export function ListSheetFilterMenubar({
  id,
  filters,
  values,
  onFilterChange,
  onDateRangeChange,
  runtimeOptions,
  fillWidth,
  className,
}: ListSheetFiltersProps & { id?: string; fillWidth?: boolean; className?: string }) {
  const t = useTranslation();
  const [openKey, setOpenKey] = useState<string | null>(null);

  if (filters.length === 0) {
    return null;
  }

  const sectionProps = { filters, values, onFilterChange, onDateRangeChange, runtimeOptions };

  return (
    <div
      id={id}
      role="toolbar"
      aria-label={t('list.filters.title')}
      className={cn(
        'flex w-full min-w-0 flex-nowrap gap-2 overflow-x-auto overscroll-x-contain pb-0.5 [scrollbar-width:thin]',
        className,
      )}
    >
      {filters.map((filter) => {
        const toggleTarget = menubarToggleTarget(filter, t);
        if (toggleTarget) {
          return (
            <SheetFilterMenubarToggleItem
              key={filter.key}
              target={toggleTarget}
              values={values}
              fillWidth={fillWidth}
              onFilterChange={onFilterChange}
            />
          );
        }
        return (
          <SheetFilterMenubarItem
            key={filter.key}
            filter={filter}
            isOpen={openKey === filter.key}
            onOpenChange={(open) => setOpenKey(open ? filter.key : null)}
            sectionProps={sectionProps}
            fillWidth={fillWidth}
          />
        );
      })}
    </div>
  );
}

type ListFilterPanelProps = {
  /** Sheet filters (`dateRange` / `multiSelect`); other kinds are ignored. */
  filters: readonly ListFilterField[];
  values: ListFilterValues;
  onFilterChange: (key: string, value: ListFilterValues[string]) => void;
  onDateRangeChange: (key: string, value: ListDateRangeValue) => void;
  /** Clears the sheet filters only (not search, not segments). */
  onReset: () => void;
  /** Options of `runtimeOptions` selects by filter key (without the "all" option). */
  runtimeOptions?: ListRuntimeOptions;
  /** Icon-only trigger with count badge (search sits next to it); otherwise a labelled button. */
  iconOnly?: boolean;
  className?: string;
};

/** Trigger + {@link Modal} for sheet filters (hub navigation when there are several). Applies live. */
export function ListFilterPanel({
  filters,
  values,
  onFilterChange,
  onDateRangeChange,
  onReset,
  runtimeOptions,
  iconOnly = false,
  className,
}: ListFilterPanelProps) {
  const t = useTranslation();
  const [open, setOpen] = useState(false);
  const [detailKey, setDetailKey] = useState<string | null>(null);
  const sheetFilters = useMemo(() => sheetFiltersOf(filters), [filters]);
  const activeCount = useMemo(() => countActiveSheetFilters(filters, values), [filters, values]);
  const baseline = useMemo(() => createInitialFilterValues({ id: '_', filters }), [filters]);
  const useHubNavigation = sheetFilters.length >= FILTER_HUB_MIN_FILTERS;
  const detailFilter = detailKey
    ? sheetFilters.find((filter) => filter.key === detailKey)
    : undefined;

  if (sheetFilters.length === 0) {
    return null;
  }

  const closeModal = () => {
    setDetailKey(null);
    setOpen(false);
  };

  const sectionProps = {
    filters: sheetFilters,
    values,
    onFilterChange,
    onDateRangeChange,
    runtimeOptions,
  };

  const openLabel =
    activeCount > 0
      ? `${t('list.filters.open')}, ${t('list.filters.activeCount', { count: activeCount })}`
      : t('list.filters.open');
  const badge =
    activeCount > 0 ? (
      <Badge
        tone="accent"
        pill
        className={cn(
          'pointer-events-none min-w-5 justify-center px-1.5',
          iconOnly && 'absolute -right-1.5 -top-1.5',
        )}
        aria-hidden
      >
        {activeCount}
      </Badge>
    ) : null;

  const modalTitle =
    useHubNavigation && detailFilter ? t(detailFilter.labelKey) : t('list.filters.title');

  return (
    <>
      <button
        type="button"
        aria-label={openLabel}
        onClick={() => setOpen(true)}
        className={cn(
          'relative inline-flex h-10 shrink-0 items-center justify-center gap-2 rounded-xl text-sm font-medium transition-colors duration-200',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--focus-ring-offset)]',
          iconOnly ? 'w-10 p-0' : 'px-3.5',
          activeCount > 0
            ? 'border border-[var(--fire-red)]/40 bg-[var(--fire-red)]/10 text-[var(--fire-red)] dark:text-[var(--fire-red-light)]'
            : 'border border-[var(--border-color)] bg-[var(--card-bg)] text-[var(--text-secondary)] hover:bg-[var(--hover-bg)] hover:text-[var(--text-primary)]',
          className,
        )}
      >
        <ListFilter className="h-4 w-4" aria-hidden strokeWidth={2} />
        {iconOnly ? null : <span className="whitespace-nowrap">{t('list.filters.open')}</span>}
        {badge}
      </button>

      <Modal
        isOpen={open}
        size="sm"
        title={modalTitle}
        onClose={closeModal}
        onBack={useHubNavigation && detailFilter ? () => setDetailKey(null) : undefined}
        bodyClassName="py-4"
        footer={
          useHubNavigation && detailFilter ? undefined : (
            <ModalFooter
              onCancel={() => {
                if (activeCount > 0) {
                  onReset();
                }
              }}
              cancelText={t('list.filters.reset')}
              onSave={closeModal}
              saveText={t('list.filters.done')}
            />
          )
        }
      >
        {useHubNavigation && !detailFilter ? (
          <FilterSheetHub
            filters={sheetFilters}
            values={values}
            baseline={baseline}
            runtimeOptions={runtimeOptions}
            onSelect={setDetailKey}
          />
        ) : useHubNavigation && detailFilter ? (
          renderSheetFilterSection(detailFilter, { ...sectionProps, showLegend: false })
        ) : (
          <div className="space-y-6">
            {sheetFilters.map((filter) =>
              renderSheetFilterSection(filter, { ...sectionProps, showLegend: true }),
            )}
          </div>
        )}
      </Modal>
    </>
  );
}
