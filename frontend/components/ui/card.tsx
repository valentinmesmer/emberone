/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type React from 'react';
import { cn } from '@/lib/utils/cn';

export interface CardProps {
  title?: string;
  children: React.ReactNode;
  className?: string;
  icon?: React.ReactNode;
  variant?: 'default' | 'success';
  action?: React.ReactNode;
  /** Tighter padding and header - matches department / list views (mobile-first). */
  density?: 'default' | 'compact';
}

export function Card({
  title,
  children,
  className = '',
  icon,
  variant = 'default',
  action,
  density = 'default',
}: CardProps) {
  const variantStyles = {
    default: 'bg-[var(--card-bg)] border-[var(--border-color)]',
    success: 'border-[var(--toast-success-border)] bg-[var(--toast-success-bg)]',
  };

  const hasHeader = title || icon || action;
  const compact = density === 'compact';

  return (
    <div
      className={cn(
        'relative rounded-lg border',
        compact ? 'p-3 sm:p-4' : 'p-5 sm:p-6',
        variantStyles[variant],
        className,
      )}
    >
      {hasHeader && (
        <div
          className={cn(
            'flex items-center justify-between gap-2 sm:gap-3',
            compact
              ? 'mb-3 pb-2 border-b border-[var(--border-color)]'
              : 'mb-5 pb-4 border-b border-[var(--border-color)]',
          )}
        >
          <div className="flex min-w-0 flex-1 items-center gap-2 sm:gap-3">
            {icon && (
              <span className="flex h-4 w-4 shrink-0 items-center justify-center text-[var(--text-secondary)]">
                {icon}
              </span>
            )}
            {title && (
              <h2
                className={cn(
                  'truncate text-[var(--text-primary)] tracking-tight',
                  compact ? 'text-sm font-semibold' : 'text-lg font-bold',
                )}
              >
                {title}
              </h2>
            )}
          </div>
          {action && <div className="relative z-10 flex-shrink-0">{action}</div>}
        </div>
      )}
      {children}
    </div>
  );
}
