/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { ReactNode } from 'react';
import { cn } from '@/lib/utils/cn';

interface PageHeaderProps {
  title: string;
  description?: string;
  action?: ReactNode;
  size?: 'compact' | 'default';
  /** When centered, title and description span the full width and align to the middle (e.g. setup). */
  layout?: 'default' | 'centered';
  className?: string;
}

/**
 * Standard page header component with title and optional description
 * Used across all pages for consistent header styling
 * Mobile-first: action CTAs stack full-width under the title; from `sm` they sit end-aligned inline.
 * Prefer `Button` with `fullWidth="responsive"` (and `SectionActions` for multiple actions).
 */
export function PageHeader({
  title,
  description,
  action,
  size = 'default',
  layout = 'default',
  className = '',
}: PageHeaderProps) {
  const compact = size === 'compact';
  const centered = layout === 'centered';

  return (
    <div
      className={cn(
        'mb-2 sm:mb-3',
        centered
          ? 'flex w-full flex-col items-center text-center'
          : 'flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between',
        className,
      )}
    >
      <div className={cn('min-w-0', centered && 'w-full max-w-2xl')}>
        <h1
          className={
            compact
              ? cn(
                  'flex items-center gap-1.5 text-base font-semibold text-[var(--text-primary)]',
                  centered && 'justify-center',
                )
              : cn(
                  'text-lg font-semibold text-[var(--text-primary)] sm:text-xl md:text-2xl lg:text-3xl',
                  centered && 'block',
                )
          }
        >
          <span className={cn('min-w-0', !centered && 'truncate')}>{title}</span>
        </h1>
        {description && (
          <p
            className={
              compact
                ? 'truncate text-xs text-[var(--text-secondary)]'
                : cn(
                    'mt-2 text-sm text-[var(--text-secondary)] sm:text-base',
                    centered && 'mx-auto max-w-xl',
                  )
            }
          >
            {description}
          </p>
        )}
      </div>
      {action ? (
        <div className="flex w-full shrink-0 flex-col items-start gap-2 sm:ml-auto sm:w-auto sm:items-end">
          {action}
        </div>
      ) : null}
    </div>
  );
}
