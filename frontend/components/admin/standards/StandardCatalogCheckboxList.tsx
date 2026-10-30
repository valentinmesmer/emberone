/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { type ReactNode, useMemo, useState } from 'react';
import { CheckboxControl } from '@/components/ui/checkbox';
import { ListContainer } from '@/components/ui/list';
import { ListSearchInput } from '@/components/ui/list-filter';
import { useTranslation } from '@/hooks/useTranslation';
import { matchesListSearch } from '@/lib/list/overviewListSearch';
import { cn } from '@/lib/utils/cn';

export interface StandardCatalogCheckboxListItem {
  id: string;
  label: string;
  subtitle?: string | null;
  disabled?: boolean;
  title?: string;
}

export interface StandardCatalogCheckboxListProps {
  items: StandardCatalogCheckboxListItem[];
  selected: Set<string>;
  onToggle: (id: string) => void;
  disabled?: boolean;
  /** Accessible name for the underlying list. */
  listLabel?: string;
  /** Prefix for stable checkbox `id`s (`${idPrefix}-${item.id}`). */
  idPrefix: string;
  maxHeightClassName?: string;
  className?: string;
  /** Shows the shared list search field and filters rows by label/subtitle. */
  searchable?: boolean;
  renderTrailing?: (item: StandardCatalogCheckboxListItem, isSelected: boolean) => ReactNode;
}

export function StandardCatalogCheckboxList({
  items,
  selected,
  onToggle,
  disabled = false,
  listLabel,
  idPrefix,
  maxHeightClassName,
  className,
  searchable = false,
  renderTrailing,
}: StandardCatalogCheckboxListProps) {
  const t = useTranslation();
  const [search, setSearch] = useState('');
  const visibleItems = useMemo(() => {
    if (!searchable || !search.trim()) {
      return items;
    }
    return items.filter((item) =>
      matchesListSearch(search, item.label, item.subtitle ?? undefined),
    );
  }, [items, searchable, search]);

  return (
    <div className={cn('space-y-2', className)}>
      {searchable ? <ListSearchInput value={search} onChange={setSearch} /> : null}
      <ListContainer
        variant="inset"
        label={listLabel}
        maxHeightClassName={maxHeightClassName}
        className={searchable ? undefined : className}
      >
        {visibleItems.length === 0 ? (
          <li className="px-3 py-4 text-center text-sm text-[var(--text-secondary)]">
            {t('list.noResults.title')}
          </li>
        ) : null}
        {visibleItems.map((item) => {
          const isSelected = selected.has(item.id);
          const controlId = `${idPrefix}-${item.id}`;
          const trailing = renderTrailing?.(item, isSelected);
          const rowDisabled = disabled || item.disabled;

          return (
            <li
              key={item.id}
              className={cn(
                trailing
                  ? 'flex flex-col gap-2 px-3 py-2.5 sm:flex-row sm:items-center sm:justify-between sm:gap-4'
                  : 'px-3 py-2.5',
                'hover:bg-[var(--hover-bg)]/40',
                rowDisabled && 'opacity-50',
              )}
            >
              <label
                htmlFor={controlId}
                title={item.title}
                className={cn(
                  'flex min-w-0 cursor-pointer items-center gap-3',
                  rowDisabled && 'cursor-not-allowed',
                )}
              >
                <CheckboxControl
                  id={controlId}
                  checked={isSelected}
                  onCheckedChange={() => onToggle(item.id)}
                  label={item.label}
                  disabled={rowDisabled}
                  size="sm"
                />
                <span className="min-w-0 flex-1">
                  <span
                    className={cn(
                      'block text-sm font-medium',
                      isSelected ? 'text-[var(--text-primary)]' : 'text-[var(--text-secondary)]',
                    )}
                  >
                    {item.label}
                  </span>
                  {item.subtitle ? (
                    <span className="mt-0.5 block text-xs text-[var(--text-secondary)]">
                      {item.subtitle}
                    </span>
                  ) : null}
                </span>
              </label>
              {trailing}
            </li>
          );
        })}
      </ListContainer>
    </div>
  );
}
