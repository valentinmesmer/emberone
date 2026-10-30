/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Check, ChevronDown, Loader2, Search, X } from 'lucide-react';
import { useEffect, useId, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { HoverHint } from '@/components/ui/hover-hint';
import { IconButton } from '@/components/ui/icon-button';
import { EmptyState } from '@/components/ui/message-cards';
import { UserAvatar } from '@/components/ui/user-avatar';
import { useFieldHoverTitle } from '@/hooks/useDisabledHoverTitle';
import { useTranslation } from '@/hooks/useTranslation';
import { organizationMemberListDefinition } from '@/lib/list/organizationMemberList';
import { useOrganizationMembersForPicker, useUsersForOrganization } from '@/lib/store';
import { cn } from '@/lib/utils/cn';
import { formatOrganizationMemberLabel, resolveUserDisplayName } from '@/lib/utils/userDisplayName';
import type { UserDto } from '@/types/user';

const SEARCH_DEBOUNCE_MS = organizationMemberListDefinition.search?.debounceMs ?? 300;

export type OrganizationMemberSelectFieldProps = {
  organizationId?: number;
  users?: UserDto[];
  value?: number;
  onChange: (userId: number | undefined) => void;
  label?: string;
  required?: boolean;
  disabled?: boolean;
  name?: string;
  error?: string;
  allowEmpty?: boolean;
  filterUser?: (user: UserDto) => boolean;
};

function filterMembersByQuery(users: UserDto[], query: string): UserDto[] {
  const needle = query.trim().toLowerCase();
  if (!needle) {
    return users;
  }
  return users.filter((user) => {
    const label = formatOrganizationMemberLabel(user).toLowerCase();
    const username = user.username?.toLowerCase() ?? '';
    return label.includes(needle) || username.includes(needle);
  });
}

/**
 * Person picker: a select-style trigger opens a full-screen sheet on phones (centered panel on
 * larger screens) with a sticky search row on top and a scrollable member list below.
 */
export function OrganizationMemberSelectField({
  organizationId,
  users: usersProp,
  value,
  onChange,
  label,
  required,
  disabled = false,
  name,
  error,
  allowEmpty = false,
  filterUser,
}: OrganizationMemberSelectFieldProps) {
  const t = useTranslation();
  const fieldId = useId();
  const triggerRef = useRef<HTMLButtonElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);
  /** Label chosen in-sheet before parent `value` / lookup list caught up. */
  const pickedLabelRef = useRef<string | null>(null);

  const [isOpen, setIsOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');

  const usesRemoteList = usersProp == null && organizationId != null;

  useEffect(() => {
    if (!isOpen) {
      setDebouncedSearch('');
      return;
    }
    const handle = window.setTimeout(() => setDebouncedSearch(searchQuery), SEARCH_DEBOUNCE_MS);
    return () => window.clearTimeout(handle);
  }, [isOpen, searchQuery]);

  const { data: remoteUsers = [], isLoading: remoteLoading } = useOrganizationMembersForPicker(
    usesRemoteList && isOpen ? organizationId : null,
    debouncedSearch,
  );

  const { data: lookupUsers = [], isLoading: lookupLoading } = useUsersForOrganization(
    usesRemoteList ? organizationId : null,
  );

  const catalogMembers = useMemo(() => {
    const base = usersProp ?? (usesRemoteList ? lookupUsers : []);
    return filterUser ? base.filter(filterUser) : base;
  }, [usersProp, usesRemoteList, lookupUsers, filterUser]);

  const catalogLoading = usersProp == null && usesRemoteList && lookupLoading;
  const noSelectableMembers = !catalogLoading && catalogMembers.length === 0;
  const interactionDisabled = disabled || noSelectableMembers;
  const noOptionsLabel = t('common.selectNoOptions');
  const hoverTitle = useFieldHoverTitle(
    interactionDisabled,
    noSelectableMembers && !disabled ? noOptionsLabel : undefined,
  );

  const users = useMemo(() => {
    const base = usersProp ?? remoteUsers;
    const filtered = filterUser ? base.filter(filterUser) : base;
    if (usersProp != null) {
      return filterMembersByQuery(filtered, searchQuery);
    }
    return filtered;
  }, [usersProp, remoteUsers, filterUser, searchQuery]);

  const isLoading = usesRemoteList && remoteLoading;

  const selectedLabel = useMemo(() => {
    if (value == null || value === 0) {
      pickedLabelRef.current = null;
      return null;
    }
    const lookupPool = usersProp ?? lookupUsers;
    const fromPool = lookupPool.find((user) => user.id === value);
    if (fromPool) {
      pickedLabelRef.current = null;
      return formatOrganizationMemberLabel(fromPool);
    }
    if (pickedLabelRef.current) {
      return pickedLabelRef.current;
    }
    return resolveUserDisplayName(lookupPool, value, t('common.formerMember'));
  }, [lookupUsers, t, usersProp, value]);

  const placeholderLabel = noSelectableMembers
    ? noOptionsLabel
    : allowEmpty
      ? t('userManagement.selectUser')
      : t('common.selectOption');
  const triggerLabel = selectedLabel ?? placeholderLabel;

  const close = () => {
    setIsOpen(false);
    setSearchQuery('');
    triggerRef.current?.focus();
  };

  useEffect(() => {
    if (!isOpen) return;
    // Lock page scroll behind the sheet; the list scrolls on its own.
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    searchInputRef.current?.focus({ preventScroll: true });
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [isOpen]);

  const handleSelect = (user: UserDto) => {
    pickedLabelRef.current = formatOrganizationMemberLabel(user);
    onChange(user.id);
    close();
  };

  const handleClear = () => {
    pickedLabelRef.current = null;
    onChange(undefined);
    close();
  };

  const sheetTitle = label ?? t('userManagement.userLabel');

  const sheet =
    isOpen && typeof document !== 'undefined'
      ? createPortal(
          <div
            role="dialog"
            aria-modal="true"
            aria-label={sheetTitle}
            className="fixed inset-0 z-[120] flex items-stretch justify-center bg-black/50 backdrop-blur-sm sm:items-center sm:p-4"
            onClick={(event) => {
              if (event.target === event.currentTarget) close();
            }}
            onKeyDown={(event) => {
              if (event.key === 'Escape') {
                // Keep a parent modal open.
                event.stopPropagation();
                close();
              }
            }}
          >
            <div
              className={cn(
                'flex w-full flex-col bg-[var(--card-bg)] shadow-2xl',
                'max-sm:h-[100dvh] sm:max-h-[min(36rem,85vh)] sm:max-w-md sm:rounded-2xl',
              )}
            >
              <div className="flex shrink-0 items-center gap-2 border-b border-[var(--border-color)] px-3 py-3 sm:px-4">
                <div className="relative min-w-0 flex-1">
                  <Search
                    className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-[var(--text-secondary)]"
                    aria-hidden
                  />
                  <input
                    ref={searchInputRef}
                    type="search"
                    role="combobox"
                    aria-expanded
                    aria-controls={`${fieldId}-listbox`}
                    aria-label={sheetTitle}
                    autoComplete="off"
                    enterKeyHint="search"
                    value={searchQuery}
                    placeholder={t('list.search.placeholder')}
                    onChange={(event) => setSearchQuery(event.target.value)}
                    className={cn(
                      'w-full rounded-xl border border-[var(--border-color)] bg-[var(--card-bg)] py-3 pl-10 pr-10',
                      'text-base text-[var(--text-primary)] placeholder:text-[var(--text-secondary)]',
                      'focus:border-[var(--fire-red)] focus:outline-none focus:ring-2 focus:ring-[var(--focus-ring)]',
                      '[&::-webkit-search-cancel-button]:hidden',
                    )}
                  />
                  {searchQuery ? (
                    <button
                      type="button"
                      aria-label={t('list.search.clear')}
                      onClick={() => {
                        setSearchQuery('');
                        searchInputRef.current?.focus();
                      }}
                      className="absolute right-2 top-1/2 -translate-y-1/2 rounded-full p-1.5 text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
                    >
                      <X className="h-4 w-4" aria-hidden />
                    </button>
                  ) : null}
                </div>
                <IconButton
                  icon={<X />}
                  onClick={close}
                  aria-label={t('common.close')}
                  variant="ghost"
                  size="md"
                  className="shrink-0"
                />
              </div>
              <div
                id={`${fieldId}-listbox`}
                role="listbox"
                aria-label={sheetTitle}
                className="min-h-0 flex-1 overflow-y-auto overscroll-contain p-2"
                style={{ WebkitOverflowScrolling: 'touch' }}
              >
                {allowEmpty && value ? (
                  <button
                    type="button"
                    onClick={handleClear}
                    className="flex min-h-12 w-full items-center gap-3 rounded-xl px-3 py-2 text-left text-sm text-[var(--text-secondary)] transition-colors hover:bg-[var(--fire-red)]/10"
                  >
                    <X className="h-4 w-4 shrink-0" aria-hidden />
                    {t('list.filters.clearSelection')}
                  </button>
                ) : null}
                {isLoading ? (
                  <p className="flex items-center justify-center gap-2 px-2 py-10 text-sm text-[var(--text-secondary)]">
                    <Loader2 className="h-4 w-4 animate-spin" aria-hidden />
                    {t('common.loading')}
                  </p>
                ) : users.length === 0 ? (
                  <EmptyState centered className="px-2 py-10 text-sm">
                    {t('events.attendance.noSearchHits')}
                  </EmptyState>
                ) : (
                  users.map((user) => {
                    const memberLabel = formatOrganizationMemberLabel(user);
                    const isSelected = value === user.id;
                    return (
                      <button
                        key={user.id}
                        type="button"
                        role="option"
                        aria-selected={isSelected}
                        onClick={() => handleSelect(user)}
                        className={cn(
                          'flex min-h-14 w-full items-center gap-3 rounded-xl px-3 py-2 text-left transition-colors',
                          'hover:bg-[var(--fire-red)]/10 active:bg-[var(--fire-red)]/20',
                          isSelected && 'bg-[var(--fire-red)]/15 font-semibold',
                        )}
                      >
                        <UserAvatar
                          firstName={user.firstName}
                          lastName={user.lastName}
                          size="md"
                          className="shrink-0"
                        />
                        <span className="min-w-0 flex-1 truncate">{memberLabel}</span>
                        {isSelected ? (
                          <Check className="h-5 w-5 shrink-0 text-[var(--fire-red)]" aria-hidden />
                        ) : null}
                      </button>
                    );
                  })
                )}
              </div>
            </div>
          </div>,
          document.body,
        )
      : null;

  return (
    <div className="w-full space-y-2">
      {label ? (
        <label htmlFor={fieldId} className="block text-sm font-semibold text-[var(--text-primary)]">
          {label}
          {required ? <span className="text-[var(--fire-red)] ml-1">*</span> : null}
        </label>
      ) : null}
      <HoverHint
        hint={hoverTitle}
        capturePointer={interactionDisabled}
        className="relative block w-full"
        shape="rounded-xl"
      >
        <div className="relative">
          <button
            ref={triggerRef}
            id={fieldId}
            name={name}
            type="button"
            aria-haspopup="dialog"
            aria-expanded={isOpen}
            disabled={interactionDisabled}
            onClick={() => setIsOpen(true)}
            className={cn(
              'w-full cursor-pointer rounded-xl border border-[var(--border-color)] bg-[var(--card-bg)] px-4 py-3 pr-12 text-left',
              'text-[var(--text-primary)] transition-colors duration-200',
              'focus:border-[var(--fire-red)] focus:outline-none focus:ring-2 focus:ring-[var(--focus-ring)]',
              error && 'border-red-500 focus:ring-red-500/25',
              interactionDisabled && 'cursor-not-allowed opacity-50',
            )}
          >
            <span
              className={cn('block truncate', !selectedLabel && 'text-[var(--text-secondary)]')}
            >
              {triggerLabel}
            </span>
          </button>
          <ChevronDown
            className="pointer-events-none absolute right-4 top-1/2 h-5 w-5 -translate-y-1/2 text-[var(--text-secondary)]"
            aria-hidden
          />
        </div>
      </HoverHint>
      {sheet}
      {error ? <p className="text-sm font-medium text-red-600 dark:text-red-400">{error}</p> : null}
    </div>
  );
}
