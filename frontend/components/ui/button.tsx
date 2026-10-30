/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Loader2 } from 'lucide-react';
import type React from 'react';
import { HoverHint } from '@/components/ui/hover-hint';
import { useActionHoverTitle } from '@/hooks/useDisabledHoverTitle';
import { useTranslation } from '@/hooks/useTranslation';
import { cn } from '@/lib/utils/cn';

export type ButtonFullWidth = boolean | 'responsive';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost';
  size?: 'xs' | 'sm' | 'md' | 'lg';
  isLoading?: boolean;
  /**
   * Width behavior:
   * - `true`: always full-bleed
   * - `'responsive'`: full width on mobile, content-sized from `sm` (standard header/footer/action CTAs)
   * - `false`/omit: inline (icon rows, pickers, etc.)
   */
  fullWidth?: ButtonFullWidth;
  children: React.ReactNode;
  icon?: React.ReactNode;
  /**
   * Optional extra copy in the hover bubble when enabled; when disabled/loading, shown instead of the default.
   */
  title?: string;
}

export function Button({
  variant = 'primary',
  size = 'md',
  isLoading = false,
  fullWidth = false,
  children,
  className = '',
  disabled,
  icon,
  title,
  'aria-label': ariaLabel,
  ...props
}: ButtonProps) {
  const t = useTranslation();
  const hoverTitle = useActionHoverTitle({ disabled, isLoading, title });
  const alwaysFullWidth = fullWidth === true;
  const responsiveFullWidth = fullWidth === 'responsive';
  const fillsWidth = alwaysFullWidth || responsiveFullWidth;
  const baseClasses =
    'inline-flex items-center justify-center gap-2 rounded-xl font-semibold transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--focus-ring-offset)]';

  const sizeClasses = {
    xs: 'h-8 px-2.5 text-xs',
    sm: 'h-9 px-3.5 text-sm',
    md: 'h-11 px-6 text-sm sm:text-base',
    lg: 'h-12 px-7 text-base',
  };

  const variantClasses = {
    primary: 'bg-[var(--fire-red)] hover:bg-[var(--fire-red-dark)] text-white',
    secondary:
      'bg-[var(--surface-soft)] hover:bg-[var(--surface-muted)] text-[var(--text-primary)] border border-[var(--border-color)]',
    outline:
      'border border-[var(--fire-red)] text-[var(--fire-red)] hover:bg-[var(--fire-red)]/10 dark:border-[var(--fire-red-light)] dark:text-[var(--fire-red-light)]',
    ghost: 'hover:bg-[var(--hover-bg)] text-[var(--text-primary)]',
  };

  const capture = Boolean(disabled || isLoading);

  return (
    <HoverHint
      hint={hoverTitle}
      capturePointer={capture}
      shape="rounded-xl"
      className={cn(
        alwaysFullWidth && 'block w-full min-w-0',
        responsiveFullWidth && 'block w-full min-w-0 sm:w-auto',
        !fillsWidth && 'shrink-0',
      )}
    >
      <button
        className={cn(
          baseClasses,
          sizeClasses[size],
          variantClasses[variant],
          alwaysFullWidth && 'w-full',
          responsiveFullWidth && 'w-full sm:w-auto',
          /* Avoid flex parents shrinking the label area when switching to loading UI */
          'shrink-0',
          className,
        )}
        disabled={disabled || isLoading}
        aria-busy={isLoading}
        {...props}
        aria-label={ariaLabel}
        title={undefined}
      >
        <span
          className={cn(
            'relative inline-flex items-center justify-center gap-2',
            fillsWidth && 'w-full',
          )}
        >
          <span
            className={cn(
              'inline-flex items-center justify-center gap-2 whitespace-nowrap',
              fillsWidth && 'w-full justify-center',
              isLoading && 'invisible',
            )}
            aria-hidden={isLoading || undefined}
          >
            {icon && <span className="flex-shrink-0 flex items-center justify-center">{icon}</span>}
            {children}
          </span>
          {isLoading ? (
            <span className="absolute inset-0 flex items-center justify-center gap-2">
              <Loader2
                className={cn(size === 'xs' ? 'h-3.5 w-3.5' : 'h-4 w-4', 'animate-spin shrink-0')}
                aria-hidden
              />
              <span className="sr-only">{t('common.processing')}</span>
            </span>
          ) : null}
        </span>
      </button>
    </HoverHint>
  );
}
