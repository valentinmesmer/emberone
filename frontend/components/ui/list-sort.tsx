/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ArrowUpDown, Check } from 'lucide-react';
import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { Button } from '@/components/ui/button';
import { handleRadioGroupKeyDown } from '@/components/ui/radio-group-keys';
import { useTranslation } from '@/hooks/useTranslation';
import type { ListDefinition } from '@/lib/list';
import { menubarPopoverCoords } from '@/lib/ui/menubarPopoverCoords';
import { cn } from '@/lib/utils/cn';

export interface ListSortMenuProps {
  definition: ListDefinition;
  /** Selected sort option value; `undefined` = Standard. */
  value: string | undefined;
  onChange: (value: string | undefined) => void;
  className?: string;
}

/**
 * Icon button with a popover radio group: "Standard" (nothing sent, backend order) followed by
 * {@link ListDefinition.sort} options. Renders nothing for lists without sort.
 */
export function ListSortMenu({ definition, value, onChange, className }: ListSortMenuProps) {
  const t = useTranslation();
  const [isOpen, setIsOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const popoverRef = useRef<HTMLDivElement>(null);
  const [coords, setCoords] = useState<{ top: number; left: number } | null>(null);
  const options = definition.sort?.options ?? [];
  const selected = options.find((option) => option.value === value);
  const active = selected != null;
  const label = t('list.sort.label');

  const updateCoords = useCallback(() => {
    const button = buttonRef.current;
    if (!button) {
      return;
    }
    const popoverWidth = popoverRef.current?.getBoundingClientRect().width;
    setCoords(
      menubarPopoverCoords(button.getBoundingClientRect(), window.innerWidth, popoverWidth),
    );
  }, []);

  useLayoutEffect(() => {
    if (!isOpen) {
      setCoords(null);
      return;
    }
    updateCoords();
    const frame = requestAnimationFrame(updateCoords);
    window.addEventListener('resize', updateCoords);
    window.addEventListener('scroll', updateCoords, true);
    return () => {
      cancelAnimationFrame(frame);
      window.removeEventListener('resize', updateCoords);
      window.removeEventListener('scroll', updateCoords, true);
    };
  }, [isOpen, updateCoords]);

  useEffect(() => {
    if (!isOpen) {
      return;
    }
    const handlePointerDown = (event: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsOpen(false);
        buttonRef.current?.focus();
      }
    };
    document.addEventListener('mousedown', handlePointerDown);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handlePointerDown);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen]);

  // Move focus into the radio group (the selected radio is its tab stop) once it is placed.
  const placed = coords != null;
  useEffect(() => {
    if (isOpen && placed) {
      popoverRef.current?.querySelector<HTMLElement>('[role="radio"][tabindex="0"]')?.focus();
    }
  }, [isOpen, placed]);

  if (options.length === 0) {
    return null;
  }

  const choices = [
    { value: undefined, label: t('list.sort.default') },
    ...options.map((option) => ({ value: option.value, label: t(option.labelKey) })),
  ];

  return (
    <div ref={rootRef} className={cn('relative shrink-0', className)}>
      <button
        ref={buttonRef}
        type="button"
        aria-expanded={isOpen}
        aria-haspopup="dialog"
        aria-label={selected ? `${label}, ${t(selected.labelKey)}` : label}
        onClick={() => setIsOpen((open) => !open)}
        className={cn(
          'relative inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-xl p-0 text-sm font-medium transition-colors duration-200',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--focus-ring-offset)]',
          active || isOpen
            ? 'border border-[var(--fire-red)]/40 bg-[var(--fire-red)]/10 text-[var(--fire-red)] dark:text-[var(--fire-red-light)]'
            : 'border border-[var(--border-color)] bg-[var(--card-bg)] text-[var(--text-secondary)] hover:bg-[var(--hover-bg)] hover:text-[var(--text-primary)]',
        )}
      >
        <ArrowUpDown className="h-4 w-4" aria-hidden strokeWidth={2} />
        {active ? (
          <span
            className="pointer-events-none absolute -right-1 -top-1 h-2.5 w-2.5 rounded-full border-2 border-[var(--card-bg)] bg-[var(--fire-red)]"
            aria-hidden
          />
        ) : null}
      </button>
      {isOpen && coords ? (
        <div
          ref={popoverRef}
          role="dialog"
          aria-label={label}
          style={{ top: coords.top, left: coords.left }}
          className={cn(
            'fixed z-[100] w-max min-w-[11rem] max-w-[min(26rem,calc(100vw-1.5rem))] rounded-lg border border-[var(--border-color)]',
            'bg-[var(--card-bg)] p-1.5 shadow-md',
          )}
        >
          <div
            role="radiogroup"
            aria-label={label}
            className="flex flex-col gap-0.5"
            onKeyDown={handleRadioGroupKeyDown}
          >
            {choices.map((choice) => {
              const isSelected = choice.value === (selected?.value ?? undefined);
              return (
                <Button
                  key={choice.value ?? ''}
                  type="button"
                  role="radio"
                  aria-checked={isSelected}
                  tabIndex={isSelected ? 0 : -1}
                  variant={isSelected ? 'secondary' : 'ghost'}
                  size="sm"
                  onClick={() => {
                    onChange(choice.value);
                    setIsOpen(false);
                    buttonRef.current?.focus();
                  }}
                  className="h-9 w-full justify-between gap-3 whitespace-nowrap rounded-lg px-3 text-left text-sm"
                >
                  <span>{choice.label}</span>
                  {isSelected ? <Check className="h-4 w-4 shrink-0" aria-hidden /> : null}
                </Button>
              );
            })}
          </div>
        </div>
      ) : null}
    </div>
  );
}
