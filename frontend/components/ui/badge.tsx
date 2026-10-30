/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type { ReactNode } from 'react';
import type { BadgeTone } from '@/components/ui/tones';
import { cn } from '@/lib/utils/cn';

export type { BadgeTone };

/** Tones reuse the app tokens so badges match toasts / list accents. */
const toneClasses: Record<BadgeTone, string> = {
  neutral: 'bg-[var(--surface-muted)] text-[var(--text-secondary)]',
  accent: 'bg-[var(--fire-red)]/10 text-[var(--fire-red)]',
  success: 'bg-[var(--status-success-surface)] text-[var(--status-success-accent)]',
  danger: 'bg-[var(--status-danger-surface)] text-[var(--status-danger-accent)]',
  warning: 'bg-[var(--status-warning-surface)] text-[var(--status-warning-accent)]',
};

export interface BadgeProps {
  children?: ReactNode;
  tone?: BadgeTone;
  /** Fully rounded pill instead of the default `rounded-md`. */
  pill?: boolean;
  className?: string;
}

/**
 * Small status/label chip. One look for every badge in the app - replaces the
 * ad-hoc `<span className="rounded ... px-1.5 ... text-xs">` spans scattered
 * across feature components.
 */
export function Badge({ children, tone = 'neutral', pill, className }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex w-fit max-w-full shrink-0 items-center gap-1 whitespace-nowrap px-1.5 py-0.5 text-xs font-medium leading-tight',
        pill ? 'rounded-full' : 'rounded-md',
        toneClasses[tone],
        className,
      )}
    >
      {children}
    </span>
  );
}
