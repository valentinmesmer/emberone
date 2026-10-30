/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Building2, ChevronRight, Search, Shield, User } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { type ReactNode, useCallback, useEffect, useId, useMemo, useRef, useState } from 'react';
import { Modal } from '@/components/ui/modal';
import { useTranslation } from '@/hooks/useTranslation';
import {
  filterHomeSearchItems,
  getHomeSearchCategoryLabelKey,
  getHomeSearchSuggestions,
  groupHomeSearchResults,
  type HomeSearchItem,
  type HomeSearchItemCategory,
} from '@/lib/home/homeSearchItems';
import { cn } from '@/lib/utils/cn';
import { plainIconSurfaceClassName } from '@/lib/utils/plainIconSurface';

interface HomeQuickSearchProps {
  items: HomeSearchItem[];
  isLoading?: boolean;
}

const CATEGORY_ICONS: Record<HomeSearchItemCategory, ReactNode> = {
  myArea: <User className="h-4 w-4" />,
  organization: <Building2 className="h-4 w-4" />,
  orgSection: <Building2 className="h-4 w-4" />,
  admin: <Shield className="h-4 w-4" />,
};

const searchInputClassName = cn(
  'w-full rounded-lg border border-[var(--border-color)] bg-[var(--card-bg)] py-3 pl-10 pr-4 text-base text-[var(--text-primary)] sm:text-sm',
  'placeholder:text-[var(--text-secondary)]',
  'focus:border-[var(--fire-red)] focus:outline-none focus:ring-2 focus:ring-[var(--focus-ring)]',
  'disabled:cursor-not-allowed disabled:opacity-60',
);

function SearchResultButton({
  item,
  index,
  listboxId,
  isActive,
  onHover,
  onSelect,
}: {
  item: HomeSearchItem;
  index: number;
  listboxId: string;
  isActive: boolean;
  onHover: () => void;
  onSelect: (item: HomeSearchItem) => void;
}) {
  return (
    <button
      id={`${listboxId}-option-${index}`}
      type="button"
      role="option"
      aria-selected={isActive}
      onMouseEnter={onHover}
      onClick={() => onSelect(item)}
      className={cn(
        'flex w-full items-center gap-3 px-3 py-2.5 text-left transition-colors',
        isActive ? 'bg-[var(--accent-surface)]' : 'hover:bg-[var(--hover-bg)]',
      )}
    >
      <span
        className={cn(
          plainIconSurfaceClassName,
          'h-8 w-8 shrink-0',
          isActive && 'text-[var(--fire-red)]',
        )}
        aria-hidden
      >
        {CATEGORY_ICONS[item.category]}
      </span>
      <span className="min-w-0 flex-1">
        <span className="block truncate text-sm font-medium text-[var(--text-primary)]">
          {item.label}
        </span>
        {item.subtitle ? (
          <span className="mt-0.5 block truncate text-xs text-[var(--text-secondary)]">
            {item.subtitle}
          </span>
        ) : null}
      </span>
      <ChevronRight
        className={cn(
          'h-4 w-4 shrink-0 text-[var(--text-secondary)]/70',
          isActive && 'text-[var(--fire-red)]',
        )}
        aria-hidden
      />
    </button>
  );
}

export function HomeQuickSearch({ items, isLoading = false }: HomeQuickSearchProps) {
  const t = useTranslation();
  const router = useRouter();
  const listboxId = useId();
  const inputRef = useRef<HTMLInputElement>(null);
  const triggerInputRef = useRef<HTMLInputElement>(null);

  const [query, setQuery] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [navigatedIndex, setNavigatedIndex] = useState(-1);

  const trimmedQuery = query.trim();
  const isSearching = trimmedQuery.length > 0;

  const filteredResults = useMemo(
    () => (isSearching ? filterHomeSearchItems(items, query) : []),
    [items, query, isSearching],
  );

  const suggestions = useMemo(() => getHomeSearchSuggestions(items), [items]);

  const displayedItems = isSearching ? filteredResults : suggestions;

  const showResults = isModalOpen && !isLoading && items.length > 0;
  const showSuggestions = showResults && !isSearching && suggestions.length > 0;
  const showSearchResults = showResults && isSearching;

  const activeIndex = useMemo(() => {
    if (!showResults || displayedItems.length === 0) return -1;
    if (navigatedIndex >= 0 && navigatedIndex < displayedItems.length) return navigatedIndex;
    return 0;
  }, [showResults, displayedItems.length, navigatedIndex]);

  const suggestionEntries = useMemo(
    () => suggestions.map((item, index) => ({ item, index })),
    [suggestions],
  );

  const resultGroups = useMemo(() => groupHomeSearchResults(filteredResults), [filteredResults]);

  const groupedEntries = useMemo(() => {
    let index = 0;
    return resultGroups.map((group) => ({
      category: group.category,
      entries: group.items.map((item) => ({
        item,
        index: index++,
      })),
    }));
  }, [resultGroups]);

  const closeModal = useCallback(() => {
    setQuery('');
    setIsModalOpen(false);
    setNavigatedIndex(-1);
  }, []);

  const openModal = useCallback(() => {
    if (isLoading || items.length === 0) return;
    setNavigatedIndex(-1);
    setIsModalOpen(true);
  }, [isLoading, items.length]);

  const navigateTo = useCallback(
    (item: HomeSearchItem) => {
      closeModal();
      router.push(item.href);
    },
    [router, closeModal],
  );

  useEffect(() => {
    if (isModalOpen || isLoading || items.length === 0) return;

    const handleGlobalKeyDown = (event: KeyboardEvent) => {
      if (event.metaKey || event.ctrlKey || event.altKey) return;
      if (event.key.length !== 1) return;

      const target = event.target as HTMLElement;
      if (
        target.tagName === 'INPUT' ||
        target.tagName === 'TEXTAREA' ||
        target.tagName === 'SELECT' ||
        target.isContentEditable
      ) {
        return;
      }

      event.preventDefault();
      setQuery((prev) => prev + event.key);
      setNavigatedIndex(-1);
      setIsModalOpen(true);
    };

    window.addEventListener('keydown', handleGlobalKeyDown);
    return () => window.removeEventListener('keydown', handleGlobalKeyDown);
  }, [isModalOpen, isLoading, items.length]);

  useEffect(() => {
    if (!isModalOpen) return;
    const frameId = requestAnimationFrame(() => {
      inputRef.current?.focus();
    });
    return () => cancelAnimationFrame(frameId);
  }, [isModalOpen]);

  useEffect(() => {
    if (activeIndex < 0) return;
    document
      .getElementById(`${listboxId}-option-${activeIndex}`)
      ?.scrollIntoView({ block: 'nearest' });
  }, [activeIndex, listboxId]);

  const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (!showResults || displayedItems.length === 0) return;

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setNavigatedIndex((prev) => {
        const current = prev >= 0 && prev < displayedItems.length ? prev : 0;
        return (current + 1) % displayedItems.length;
      });
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      setNavigatedIndex((prev) => {
        const current = prev >= 0 && prev < displayedItems.length ? prev : 0;
        return current <= 0 ? displayedItems.length - 1 : current - 1;
      });
    } else if (event.key === 'Enter' && activeIndex >= 0) {
      event.preventDefault();
      const selected = displayedItems[activeIndex];
      if (selected) navigateTo(selected);
    }
  };

  const placeholder = isLoading
    ? t('common.loading')
    : items.length === 0
      ? t('home.search.unavailable')
      : t('home.search.placeholder');

  return (
    <>
      <div className="relative mb-2 sm:mb-2.5">
        <label htmlFor={`${listboxId}-trigger`} className="sr-only">
          {t('home.search.ariaLabel')}
        </label>
        <Search
          className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--text-secondary)]"
          aria-hidden
        />
        <input
          ref={triggerInputRef}
          id={`${listboxId}-trigger`}
          type="search"
          value={isModalOpen ? '' : query}
          disabled={isLoading || items.length === 0}
          onFocus={openModal}
          onChange={(event) => {
            setQuery(event.target.value);
            setNavigatedIndex(-1);
            if (!isModalOpen) {
              setIsModalOpen(true);
            }
          }}
          placeholder={placeholder}
          autoComplete="off"
          aria-haspopup="dialog"
          className={searchInputClassName}
        />
      </div>

      <Modal
        isOpen={isModalOpen}
        onClose={closeModal}
        title={t('home.search.ariaLabel')}
        size="lg"
        className="flex flex-col sm:max-h-[min(90vh,40rem)]"
        bodyClassName="flex min-h-0 flex-1 flex-col overflow-hidden"
      >
        <div className="flex min-h-0 flex-1 flex-col gap-3">
          <p className="shrink-0 text-sm text-[var(--text-secondary)]">{t('home.search.hint')}</p>

          <label htmlFor={`${listboxId}-input`} className="sr-only">
            {t('home.search.ariaLabel')}
          </label>
          <div className="relative shrink-0">
            <Search
              className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--text-secondary)]"
              aria-hidden
            />
            <input
              ref={inputRef}
              id={`${listboxId}-input`}
              type="search"
              value={query}
              onChange={(event) => {
                setQuery(event.target.value);
                setNavigatedIndex(-1);
              }}
              onKeyDown={handleKeyDown}
              placeholder={t('home.search.placeholder')}
              autoComplete="off"
              role="combobox"
              aria-expanded={showResults}
              aria-controls={showResults ? listboxId : undefined}
              aria-activedescendant={
                showResults && activeIndex >= 0 ? `${listboxId}-option-${activeIndex}` : undefined
              }
              className={searchInputClassName}
            />
          </div>

          {showResults ? (
            <div className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-[var(--border-color)] bg-[var(--card-bg)]">
              {showSearchResults && filteredResults.length === 0 ? (
                <p className="px-3 py-4 text-sm text-[var(--text-secondary)]">
                  {t('home.search.noResults')}
                </p>
              ) : null}

              {showSuggestions ? (
                <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
                  <p className="shrink-0 border-b border-[var(--border-color)] px-3 py-2 text-[10px] font-semibold uppercase tracking-wide text-[var(--text-secondary)]">
                    {t('home.search.suggestionsTitle')}
                  </p>
                  <ul
                    id={listboxId}
                    aria-label={t('home.search.suggestionsAria')}
                    className="min-h-0 flex-1 overflow-y-auto overscroll-contain pb-1 scrollbar-thin"
                  >
                    {suggestionEntries.map(({ item, index }) => (
                      <li key={item.id} role="presentation">
                        <SearchResultButton
                          item={item}
                          index={index}
                          listboxId={listboxId}
                          isActive={index === activeIndex}
                          onHover={() => setNavigatedIndex(index)}
                          onSelect={navigateTo}
                        />
                      </li>
                    ))}
                  </ul>
                </div>
              ) : null}

              {showSearchResults && resultGroups.length > 0 ? (
                <ul
                  id={listboxId}
                  aria-label={t('home.search.resultsAria')}
                  className="min-h-0 flex-1 overflow-y-auto overscroll-contain pb-1 scrollbar-thin"
                >
                  {groupedEntries.map((group) => (
                    <li key={group.category} role="presentation">
                      <p className="sticky top-0 z-[1] border-b border-t border-[var(--border-color)] bg-[var(--card-bg)] px-3 py-2 text-[10px] font-semibold uppercase tracking-wide text-[var(--text-secondary)] first:border-t-0">
                        {t(getHomeSearchCategoryLabelKey(group.category))}
                      </p>
                      <ul aria-label={t(getHomeSearchCategoryLabelKey(group.category))}>
                        {group.entries.map(({ item, index }) => (
                          <li key={item.id} role="presentation">
                            <SearchResultButton
                              item={item}
                              index={index}
                              listboxId={listboxId}
                              isActive={index === activeIndex}
                              onHover={() => setNavigatedIndex(index)}
                              onSelect={navigateTo}
                            />
                          </li>
                        ))}
                      </ul>
                    </li>
                  ))}
                </ul>
              ) : null}
            </div>
          ) : null}
        </div>
      </Modal>
    </>
  );
}
