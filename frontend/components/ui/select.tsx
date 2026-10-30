/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ChevronDown } from 'lucide-react';
import type React from 'react';
import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { HoverHint } from '@/components/ui/hover-hint';
import { useFieldHoverTitle } from '@/hooks/useDisabledHoverTitle';
import { useTranslation } from '@/hooks/useTranslation';
import {
  getSelectDropdownOptions,
  isSelectInteractionDisabled,
  resolveSelectTriggerLabel,
  type SelectFieldOption,
} from '@/lib/ui/selectFieldState';
import { cn } from '@/lib/utils/cn';

interface SelectProps
  extends Omit<React.SelectHTMLAttributes<HTMLSelectElement>, 'onChange' | 'size'> {
  label?: string;
  error?: string;
  /** `sm` for compact inline use inside list rows and settings panels. */
  size?: 'md' | 'sm';
  /** `auto` sizes the trigger (and dropdown) to the widest option. */
  width?: 'full' | 'auto';
  options: SelectFieldOption[];
  onChange?: (e: React.ChangeEvent<HTMLSelectElement>) => void;
}

export function Select({
  label,
  error,
  size = 'md',
  width = 'full',
  options,
  className = '',
  value,
  onChange,
  disabled,
  title,
  'aria-label': ariaLabel,
  ...props
}: SelectProps) {
  const t = useTranslation();
  const selectId = props.id || props.name;
  const [isOpen, setIsOpen] = useState(false);
  const [dropdownPosition, setDropdownPosition] = useState<{
    top: number;
    left: number;
    width: number;
  } | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const dropdownOptions = useMemo(
    () => getSelectDropdownOptions(options, props.required),
    [options, props.required],
  );

  const { label: displayLabel, isPlaceholder: isPlaceholderLabel } = useMemo(
    () =>
      resolveSelectTriggerLabel({
        options,
        dropdownOptions,
        value,
        selectOptionLabel: t('common.selectOption'),
        noOptionsLabel: t('common.selectNoOptions'),
      }),
    [dropdownOptions, options, t, value],
  );

  const interactionDisabled = isSelectInteractionDisabled(disabled, dropdownOptions);
  const hoverTitle = useFieldHoverTitle(
    interactionDisabled,
    interactionDisabled && !disabled ? t('common.selectNoOptions') : title,
  );

  const isCompact = size === 'sm';
  const isAutoWidth = width === 'auto';
  const estimateOptionHeight = isCompact ? 36 : 44;

  // Always portal the menu (fixed + body) so options are not clipped by ancestor
  // overflow-hidden (e.g. ListRow / ListContainer).
  const updateDropdownPosition = useCallback(() => {
    if (!buttonRef.current) return;

    const rect = buttonRef.current.getBoundingClientRect();
    const spacing = 4;
    const viewportHeight = window.innerHeight;
    const viewportWidth = window.innerWidth;
    const dropdownHeight = Math.min(240, dropdownOptions.length * estimateOptionHeight);

    let top = rect.bottom + spacing;
    if (top + dropdownHeight > viewportHeight && rect.top > dropdownHeight) {
      top = rect.top - dropdownHeight - spacing;
    }
    if (top + dropdownHeight > viewportHeight) {
      top = Math.max(spacing, viewportHeight - dropdownHeight - spacing);
    }

    // Auto trigger is already sized to the widest option; match that width.
    const dropdownWidth =
      width === 'auto' ? Math.min(viewportWidth - spacing * 2, rect.width) : rect.width;

    let left = rect.left;
    if (left < spacing) {
      left = spacing;
    }
    if (left + dropdownWidth > viewportWidth - spacing) {
      left = Math.max(spacing, viewportWidth - dropdownWidth - spacing);
    }

    setDropdownPosition((prev) => {
      if (prev && prev.top === top && prev.left === left && prev.width === dropdownWidth) {
        return prev;
      }
      return { top, left, width: dropdownWidth };
    });
  }, [dropdownOptions, estimateOptionHeight, width]);

  useLayoutEffect(() => {
    if (!isOpen) return;

    updateDropdownPosition();
    window.addEventListener('resize', updateDropdownPosition);
    window.addEventListener('scroll', updateDropdownPosition, true);

    return () => {
      window.removeEventListener('resize', updateDropdownPosition);
      window.removeEventListener('scroll', updateDropdownPosition, true);
    };
  }, [isOpen, updateDropdownPosition]);

  useEffect(() => {
    const handleClickOutside = (event: PointerEvent) => {
      const target = event.target as Node;
      const insideTrigger = containerRef.current?.contains(target);
      const insidePortaledMenu = dropdownRef.current?.contains(target);
      if (!insideTrigger && !insidePortaledMenu) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      // Delay so the opening pointerdown/click does not immediately close the menu.
      // pointerdown covers touch + mouse; mousedown alone misses some mobile paths.
      const timeoutId = setTimeout(() => {
        document.addEventListener('pointerdown', handleClickOutside);
      }, 0);

      return () => {
        clearTimeout(timeoutId);
        document.removeEventListener('pointerdown', handleClickOutside);
      };
    }
  }, [isOpen]);

  // Scroll is handled by updateDropdownPosition above. Do not close on scroll:
  // mobile modals scroll the trigger into view on open, which would immediately
  // dismiss the menu and make add/edit membership unusable on phones.

  const handleSelect = (optionValue: string) => {
    const option = dropdownOptions.find((opt) => opt.value === optionValue);
    if (option?.disabled) return;

    if (onChange) {
      const syntheticEvent = {
        target: { value: optionValue, name: props.name || '' },
      } as React.ChangeEvent<HTMLSelectElement>;
      onChange(syntheticEvent);
    }
    setIsOpen(false);
  };

  const renderDropdownOptions = () =>
    dropdownOptions.map((option) => {
      const isSelected = value === option.value && !option.disabled;
      return (
        <button
          key={option.value}
          type="button"
          disabled={option.disabled}
          onClick={() => handleSelect(option.value)}
          className={cn(
            'w-full text-left transition-colors whitespace-nowrap',
            isCompact ? 'px-3 py-2 text-sm' : 'px-4 py-3',
            option.disabled
              ? 'cursor-default text-[var(--text-secondary)]'
              : 'hover:bg-[var(--fire-red)]/10 cursor-pointer',
            isCompact
              ? 'first:rounded-t-lg last:rounded-b-lg'
              : 'first:rounded-t-xl last:rounded-b-xl',
            isSelected && 'bg-[var(--fire-red)]/20 font-semibold text-[var(--text-primary)]',
            !isAutoWidth && 'truncate',
          )}
          title={option.label}
        >
          {option.label}
        </button>
      );
    });

  const dropdownMenuClassName = cn(
    // Above modal overlay (z-[100]) so options stay tappable inside full-screen mobile modals.
    'z-[110] bg-[var(--card-bg)] border border-[var(--border-color)] shadow-lg max-h-60 overflow-y-auto',
    isCompact ? 'rounded-lg' : 'rounded-xl',
  );

  const portaledDropdown =
    isOpen && dropdownPosition && typeof document !== 'undefined' ? (
      <div
        ref={dropdownRef}
        data-select-menu
        className={cn('fixed', dropdownMenuClassName)}
        style={{
          top: `${dropdownPosition.top}px`,
          left: `${dropdownPosition.left}px`,
          width: `${dropdownPosition.width}px`,
        }}
      >
        {renderDropdownOptions()}
      </div>
    ) : null;

  const triggerPaddingClass = isCompact
    ? 'rounded-lg px-3 py-2 pr-9 text-sm'
    : 'rounded-xl px-4 py-3 pr-12';

  const widthSizerLabels = useMemo(() => {
    if (!isAutoWidth) return [];
    const labels = dropdownOptions.map((option) => option.label);
    if (displayLabel && !labels.includes(displayLabel)) {
      labels.push(displayLabel);
    }
    return labels.length > 0 ? labels : [displayLabel];
  }, [displayLabel, dropdownOptions, isAutoWidth]);

  return (
    <div
      className={cn(
        isAutoWidth ? 'inline-block max-w-full' : 'w-full',
        label ? 'space-y-2' : undefined,
      )}
    >
      {label && (
        <label
          htmlFor={selectId}
          className="block text-sm font-semibold text-[var(--text-primary)]"
        >
          {label}
          {props.required && <span className="text-[var(--fire-red)] ml-1">*</span>}
        </label>
      )}
      <div ref={containerRef} className={cn('relative', isAutoWidth && 'inline-grid max-w-full')}>
        {widthSizerLabels.map((sizerLabel) => (
          <span
            key={`sizer-${sizerLabel}`}
            aria-hidden
            className={cn(
              'invisible col-start-1 row-start-1 whitespace-nowrap pointer-events-none',
              triggerPaddingClass,
            )}
          >
            {sizerLabel}
          </span>
        ))}
        <HoverHint
          hint={hoverTitle}
          capturePointer={interactionDisabled}
          className={cn('block', isAutoWidth ? 'col-start-1 row-start-1 w-full min-w-0' : 'w-full')}
          shape={isCompact ? 'rounded-lg' : 'rounded-xl'}
        >
          <button
            ref={buttonRef}
            type="button"
            id={selectId}
            onClick={() => {
              if (interactionDisabled) return;
              setIsOpen(!isOpen);
            }}
            disabled={interactionDisabled}
            title={undefined}
            aria-label={
              label
                ? undefined
                : (ariaLabel ??
                  (typeof props.name === 'string' && props.name.trim()
                    ? props.name
                    : t('common.a11y.selectField')))
            }
            className={cn(
              'border text-left bg-[var(--card-bg)] text-[var(--text-primary)] border-[var(--border-color)]',
              'focus:outline-none transition-colors duration-200 cursor-pointer w-full',
              isOpen
                ? 'border-[var(--fire-red)] ring-2 ring-[var(--focus-ring)]'
                : 'focus:border-[var(--fire-red)] focus:ring-2 focus:ring-[var(--focus-ring)]',
              triggerPaddingClass,
              error && 'border-red-500 focus:ring-red-500/25',
              interactionDisabled && 'opacity-50 cursor-not-allowed',
              className,
            )}
          >
            <span
              className={cn('block truncate', isPlaceholderLabel && 'text-[var(--text-secondary)]')}
            >
              {displayLabel}
            </span>
          </button>
        </HoverHint>
        <ChevronDown
          className={cn(
            'absolute top-1/2 -translate-y-1/2 text-[var(--text-secondary)] pointer-events-none transition-transform duration-200',
            isCompact ? 'right-3 h-4 w-4' : 'right-4 h-5 w-5',
            isOpen && 'rotate-180',
            isAutoWidth && 'z-[1]',
          )}
          aria-hidden="true"
        />
      </div>
      {typeof document !== 'undefined' &&
        portaledDropdown &&
        createPortal(portaledDropdown, document.body)}
      {error && <p className="text-sm text-red-600 dark:text-red-400 font-medium">{error}</p>}
    </div>
  );
}
