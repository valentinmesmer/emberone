/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type React from 'react';
import { HoverHint } from '@/components/ui/hover-hint';
import { useActionHoverTitle } from '@/hooks/useDisabledHoverTitle';
import { cn } from '@/lib/utils/cn';

export interface IconButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  icon: React.ReactNode;
  variant?: 'ghost' | 'outline' | 'subtle' | 'action';
  size?: 'sm' | 'md' | 'lg';
  'aria-label': string; // Required for accessibility
}

const sizeClasses = {
  sm: 'p-1.5',
  md: 'p-2',
  lg: 'p-3',
};

const iconSizeClasses = {
  sm: 'h-4 w-4',
  md: 'h-5 w-5',
  lg: 'h-6 w-6',
};

const variantClasses = {
  ghost: 'hover:bg-[var(--hover-bg)] text-[var(--text-secondary)]',
  outline:
    'border border-[var(--border-color)] hover:bg-[var(--hover-bg)] text-[var(--text-secondary)]',
  subtle: 'hover:bg-[var(--hover-bg)] text-[var(--text-secondary)]',
  action:
    'min-h-9 min-w-9 text-[var(--text-secondary)] hover:bg-[var(--fire-red)]/10 hover:text-[var(--fire-red)] disabled:hover:bg-transparent disabled:hover:text-[var(--text-secondary)]',
};

export function IconButton({
  icon,
  variant = 'ghost',
  size = 'md',
  className,
  disabled,
  title,
  'aria-label': ariaLabel,
  ...props
}: IconButtonProps) {
  const hoverTitle = useActionHoverTitle({ disabled, isLoading: false, title });
  return (
    <HoverHint hint={hoverTitle} capturePointer={!!disabled} shape="rounded-lg">
      <button
        className={cn(
          'inline-flex items-center justify-center rounded-lg transition-colors',
          'disabled:opacity-50 disabled:cursor-not-allowed',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--focus-ring-offset)]',
          sizeClasses[size],
          variantClasses[variant],
          className,
        )}
        disabled={disabled}
        {...props}
        aria-label={ariaLabel}
        title={undefined}
      >
        <span className={cn('flex items-center justify-center', iconSizeClasses[size])}>
          {icon}
        </span>
      </button>
    </HoverHint>
  );
}
