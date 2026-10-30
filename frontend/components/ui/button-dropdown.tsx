/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ChevronDown, Loader2 } from 'lucide-react';
import type React from 'react';
import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { HoverHint } from '@/components/ui/hover-hint';
import { useActionHoverTitle } from '@/hooks/useDisabledHoverTitle';
import { useTranslation } from '@/hooks/useTranslation';
import { cn } from '@/lib/utils/cn';

export interface DropdownOption {
  value: string;
  label: string;
}

export interface DropdownButtonProps
  extends Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, 'onChange'> {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost';
  isLoading?: boolean;
  /** Ignored when `chevronOnly` is true. */
  children?: React.ReactNode;
  icon?: React.ReactNode;
  options?: DropdownOption[];
  selectedValue?: string;
  onOptionSelect?: (value: string) => void;
  onMainClick?: () => void;
  /** Default true (dashboard cards). Set false for inline / header usage. */
  fullWidth?: boolean;
  /** Tighter padding and typography (e.g. org hub title row). */
  size?: 'default' | 'compact';
  /** Only a chevron toggle (no main label). Requires `options`. */
  chevronOnly?: boolean;
  /**
   * When set, the open menu uses `position: fixed` and matches this element’s width and horizontal position
   * (e.g. org hub card). Use a selector unique on the page, e.g. `[data-org-hub-header]`.
   */
  menuAnchorSelector?: string;
  /** When true and the main area is not a separate action (`onMainClick` unset), tapping the label toggles the menu. */
  menuFromMainClick?: boolean;
  /** When `size` is compact, do not cap the main label width (e.g. long organization names in the hub title). */
  allowWideCompactLabel?: boolean;
  /** Override classes for the scrollable options list (e.g. taller menu for many organizations). */
  optionListClassName?: string;
}

export function DropdownButton({
  variant = 'primary',
  isLoading = false,
  children,
  className = '',
  disabled,
  icon,
  options = [],
  selectedValue,
  onOptionSelect,
  onMainClick,
  fullWidth = true,
  size = 'default',
  chevronOnly = false,
  menuAnchorSelector,
  menuFromMainClick = false,
  allowWideCompactLabel = false,
  optionListClassName,
  title,
  ...props
}: DropdownButtonProps) {
  const t = useTranslation();
  const hoverTitle = useActionHoverTitle({ disabled, isLoading, title });
  const chevronHoverTitle = useActionHoverTitle({
    disabled,
    isLoading: false,
  });
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const isCompact = size === 'compact';
  const [fixedMenuStyle, setFixedMenuStyle] = useState<React.CSSProperties | undefined>();

  useLayoutEffect(() => {
    if (!isOpen || !menuAnchorSelector) {
      return;
    }
    const update = () => {
      const anchor = document.querySelector(menuAnchorSelector);
      const anchorRect = anchor?.getBoundingClientRect();
      const fallbackRect = dropdownRef.current?.getBoundingClientRect();
      const r = anchorRect ?? fallbackRect;
      if (!r) {
        setFixedMenuStyle(undefined);
        return;
      }
      setFixedMenuStyle({
        position: 'fixed',
        top: r.bottom + 8,
        left: r.left,
        width: r.width,
        zIndex: 200,
      });
    };
    update();
    window.addEventListener('scroll', update, true);
    window.addEventListener('resize', update);
    return () => {
      window.removeEventListener('scroll', update, true);
      window.removeEventListener('resize', update);
      setFixedMenuStyle(undefined);
    };
  }, [isOpen, menuAnchorSelector]);

  const baseClasses =
    'inline-flex items-center justify-center font-semibold transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--focus-ring-offset)]';

  const variantClasses = {
    primary: 'bg-[var(--fire-red)] hover:bg-[var(--fire-red-dark)] text-white',
    secondary:
      'bg-[var(--surface-soft)] hover:bg-[var(--surface-muted)] text-[var(--text-primary)] border border-[var(--border-color)]',
    outline:
      'border border-[var(--fire-red)] text-[var(--fire-red)] hover:bg-[var(--fire-red)]/10 dark:border-[var(--fire-red-light)] dark:text-[var(--fire-red-light)]',
    ghost: 'hover:bg-[var(--hover-bg)] text-[var(--text-primary)]',
  };

  // Close dropdown when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }
  }, [isOpen]);

  const hasOptions = options.length > 0;

  const handleMainButtonClick = () => {
    if (onMainClick) {
      onMainClick();
      return;
    }
    if (menuFromMainClick && hasOptions && !isLoading) {
      setIsOpen((v) => !v);
    }
  };

  const handleDropdownToggle = (e: React.MouseEvent) => {
    e.stopPropagation();
    setIsOpen(!isOpen);
  };

  const handleOptionClick = (value: string) => {
    if (onOptionSelect) {
      onOptionSelect(value);
    }
    setIsOpen(false);
  };

  const menuItemDense = isCompact && !chevronOnly;

  if (chevronOnly) {
    if (!hasOptions) {
      return null;
    }

    return (
      <div className="relative inline-flex shrink-0" ref={dropdownRef}>
        <HoverHint
          hint={hoverTitle}
          capturePointer={!!(disabled || isLoading)}
          className="inline-flex"
          shape="rounded-lg"
        >
          <button
            type="button"
            className={cn(
              'inline-flex items-center justify-center transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--focus-ring-offset)]',
              variant === 'ghost'
                ? 'min-h-11 min-w-11 shrink-0 select-none rounded-lg bg-transparent p-0 font-medium text-[var(--text-secondary)] [-webkit-tap-highlight-color:transparent] hover:text-[var(--text-primary)] sm:min-h-9 sm:min-w-9'
                : cn(
                    baseClasses,
                    variantClasses[variant],
                    'min-h-8 min-w-8 rounded-lg px-1.5 py-1 text-[var(--text-secondary)] opacity-80 hover:opacity-100',
                  ),
            )}
            disabled={disabled || isLoading}
            onClick={(e) => {
              e.stopPropagation();
              if (!disabled && !isLoading) {
                setIsOpen((v) => !v);
              }
            }}
            aria-expanded={isOpen}
            aria-haspopup="menu"
            aria-busy={isLoading}
            {...props}
            title={undefined}
          >
            {isLoading ? (
              <>
                <Loader2 className="h-5 w-5 shrink-0 animate-spin" aria-hidden />
                <span className="sr-only">{t('common.processing')}</span>
              </>
            ) : (
              <ChevronDown
                className={cn('h-5 w-5 transition-transform duration-200', isOpen && 'rotate-180')}
              />
            )}
          </button>
        </HoverHint>

        {isOpen && (!menuAnchorSelector || fixedMenuStyle) ? (
          <div
            style={fixedMenuStyle}
            className={cn(
              'bg-[var(--card-bg)] shadow-[var(--interactive-shadow-strong)] border border-[var(--border-color)] overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200 rounded-xl',
              fixedMenuStyle
                ? ''
                : 'absolute right-0 z-[100] mt-2 min-w-[12rem] max-w-[min(92vw,24rem)]',
            )}
          >
            <div className={cn('max-h-60 overflow-y-auto', optionListClassName)}>
              {options.map((option) => (
                <button
                  key={option.value}
                  type="button"
                  className={cn(
                    'w-full px-4 py-3 text-left text-sm transition-colors',
                    'hover:bg-[var(--hover-bg)]',
                    'focus:bg-[var(--hover-bg)] focus:outline-none',
                    selectedValue === option.value && 'bg-[var(--fire-red)]/12 font-semibold',
                  )}
                  onClick={() => handleOptionClick(option.value)}
                >
                  {option.label}
                </button>
              ))}
            </div>
          </div>
        ) : null}
      </div>
    );
  }

  const shellRounded = 'rounded-xl';
  const mainRounded = hasOptions ? 'rounded-l-xl rounded-r-none' : shellRounded;
  const chevronRounded = 'rounded-r-xl';
  const hintShape = 'rounded-xl';

  return (
    <div className={cn('relative', fullWidth ? 'w-full' : 'w-max max-w-full')} ref={dropdownRef}>
      <div className={cn('flex overflow-hidden', shellRounded, variantClasses[variant], className)}>
        <HoverHint
          hint={hoverTitle}
          capturePointer={!!(disabled || isLoading)}
          className="flex min-w-0 flex-1"
          shape={hintShape}
        >
          <button
            className={cn(
              baseClasses,
              'w-full min-w-0 shrink-0',
              isCompact ? 'h-8 gap-1.5 px-2.5 text-xs' : 'gap-2 px-6 py-3',
              mainRounded,
            )}
            disabled={disabled || isLoading}
            onClick={handleMainButtonClick}
            aria-busy={isLoading}
            {...props}
            title={undefined}
            aria-expanded={menuFromMainClick && hasOptions ? isOpen : undefined}
            aria-haspopup={menuFromMainClick && hasOptions ? 'menu' : undefined}
          >
            <span className="relative inline-flex w-full min-w-0 items-center justify-center">
              <span
                className={cn(
                  'inline-flex min-w-0 items-center justify-center gap-2',
                  isLoading && 'invisible',
                  isCompact && !allowWideCompactLabel && 'max-w-[10rem] sm:max-w-[14rem]',
                )}
                aria-hidden={isLoading || undefined}
              >
                {icon && (
                  <span className="flex-shrink-0 flex items-center justify-center">{icon}</span>
                )}
                <span
                  className={cn(
                    'min-w-0',
                    isCompact
                      ? 'truncate text-left text-xs font-semibold leading-tight'
                      : 'text-center',
                  )}
                >
                  {children}
                </span>
              </span>
              {isLoading ? (
                <span className="absolute inset-0 flex items-center justify-center gap-2">
                  <Loader2
                    className={cn('shrink-0 animate-spin', isCompact ? 'h-4 w-4' : 'h-5 w-5')}
                    aria-hidden
                  />
                  <span className="sr-only">{t('common.processing')}</span>
                </span>
              ) : null}
            </span>
          </button>
        </HoverHint>

        {hasOptions && !isLoading && (
          <HoverHint
            hint={chevronHoverTitle}
            capturePointer={!!disabled}
            className="inline-flex shrink-0"
            shape={hintShape}
          >
            <button
              className={cn(
                baseClasses,
                'border-l border-current/20',
                isCompact ? 'h-8 px-2' : 'px-3',
                chevronRounded,
              )}
              disabled={disabled}
              onClick={handleDropdownToggle}
              aria-label={t('common.selectOption')}
              type="button"
              title={undefined}
            >
              <ChevronDown
                className={cn(
                  'transition-transform duration-200',
                  isCompact ? 'h-4 w-4' : 'h-5 w-5',
                  isOpen && 'rotate-180',
                )}
              />
            </button>
          </HoverHint>
        )}
      </div>

      {/* Dropdown Menu */}
      {hasOptions && isOpen && (!menuAnchorSelector || fixedMenuStyle) && (
        <div
          style={menuAnchorSelector ? fixedMenuStyle : undefined}
          className={cn(
            'bg-[var(--card-bg)] shadow-[var(--interactive-shadow-strong)] border border-[var(--border-color)] overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200',
            fixedMenuStyle
              ? 'rounded-xl'
              : cn(
                  'absolute z-[100]',
                  fullWidth
                    ? 'mt-2 w-full rounded-xl'
                    : 'left-0 mt-1 min-w-[12rem] max-w-[min(92vw,20rem)] rounded-xl',
                  isCompact && !fullWidth && 'rounded-lg',
                ),
          )}
        >
          <div className={cn('max-h-60 overflow-y-auto', optionListClassName)}>
            {options.map((option) => (
              <button
                key={option.value}
                className={cn(
                  'w-full text-left transition-colors',
                  menuItemDense ? 'px-3 py-2 text-xs' : 'px-4 py-3 text-sm',
                  'hover:bg-[var(--hover-bg)]',
                  'focus:bg-[var(--hover-bg)] focus:outline-none',
                  selectedValue === option.value && 'bg-[var(--fire-red)]/12 font-semibold',
                )}
                onClick={() => handleOptionClick(option.value)}
                type="button"
              >
                {option.label}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
