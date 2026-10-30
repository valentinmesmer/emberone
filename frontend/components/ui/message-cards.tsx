/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type { ReactNode } from 'react';
import { Card } from '@/components/ui/card';
import { useTranslation } from '@/hooks/useTranslation';
import { cn } from '@/lib/utils/cn';

export type StatusTone = 'success' | 'warning';

/** Shared status surfaces - same tokens as toasts, tuned for light and dark. */
export const statusCardToneClasses: Record<StatusTone, string> = {
  success: 'border-[var(--toast-success-border)] bg-[var(--toast-success-bg)]',
  warning: 'border-[var(--toast-warning-border)] bg-[var(--toast-warning-bg)]',
};

export const statusIconToneClasses: Record<StatusTone, string> = {
  success: 'text-[var(--status-success-accent)]',
  warning: 'text-[var(--toast-warning-text)]',
};

/**
 * Neutral in-card notice (e.g. not found). Prefer over ad-hoc Card + paragraph.
 * Pass `tone="warning"` to highlight a consequential message (e.g. an edit that affects live data).
 */
export function NoticeCard({
  children,
  className,
  tone = 'neutral',
}: {
  children: ReactNode;
  className?: string;
  tone?: 'neutral' | 'warning' | 'success';
}) {
  const tinted = tone === 'warning' || tone === 'success';
  return (
    <Card
      className={cn(
        'p-4 sm:p-5 md:p-6',
        tone === 'warning' && statusCardToneClasses.warning,
        tone === 'success' && statusCardToneClasses.success,
        className,
      )}
    >
      <div
        className={cn(
          'text-sm sm:text-base',
          tinted ? `text-[var(--text-primary)]` : 'text-[var(--text-primary)]',
        )}
      >
        {children}
      </div>
    </Card>
  );
}

/**
 * Lightweight inline “no data” message for use *inside* a section/card, where a
 * full EmptyStateCard would be too heavy. Replaces ad-hoc
 * `<p className="text-sm text-[var(--text-secondary)]">…</p>` empty messages.
 */
export function EmptyState({
  children,
  className,
  centered,
}: {
  children: ReactNode;
  className?: string;
  /** Adds vertical padding + centering (e.g. inside a bordered scroll box). */
  centered?: boolean;
}) {
  return (
    <p
      className={cn(
        'text-sm text-[var(--text-secondary)]',
        centered && 'py-4 text-center',
        className,
      )}
    >
      {children}
    </p>
  );
}

/**
 * Centered secondary copy for empty states and “no data” messages.
 */
export function EmptyStateCard({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <Card
      density="compact"
      className={cn('text-center text-sm leading-relaxed text-[var(--text-secondary)]', className)}
    >
      {children}
    </Card>
  );
}

type StateSurfaceVariant = 'embedded' | 'fullscreen';

function StateSurface({
  message,
  hint,
  className,
  variant = 'embedded',
}: {
  message: string;
  hint?: string;
  className?: string;
  variant?: StateSurfaceVariant;
}) {
  return (
    <div
      className={cn(
        variant === 'embedded'
          ? 'flex min-h-[220px] items-center justify-center rounded-xl border border-[var(--border-color)] bg-[var(--card-bg)] p-6 sm:p-8'
          : 'flex min-h-screen items-center justify-center bg-[var(--page-bg)] p-6',
        className,
      )}
    >
      <div className="mx-auto max-w-sm space-y-2 text-center sm:space-y-3">
        <p className="text-base font-semibold text-[var(--text-primary)] sm:text-lg">{message}</p>
        {hint && (
          <p className="text-sm leading-relaxed text-[var(--text-secondary)] sm:text-base">
            {hint}
          </p>
        )}
      </div>
    </div>
  );
}

/**
 * Centered page-level “not found” surface. Matches LoadingState embedded styling.
 * Navigation is handled by the app chrome back control - no inline back button.
 */
export function NotFoundState({
  message,
  hint,
  className,
  variant = 'embedded',
}: {
  message: string;
  /** When false, no subtitle. When string, custom subtitle. When omitted, uses common.notFoundHint */
  hint?: string | false;
  className?: string;
  variant?: StateSurfaceVariant;
}) {
  const t = useTranslation();
  const displayHint = hint === false ? undefined : (hint ?? t('common.notFoundHint'));

  return (
    <StateSurface variant={variant} className={className} message={message} hint={displayHint} />
  );
}

/**
 * Centered page-level load/error surface. Same layout as NotFoundState.
 */
export function ErrorState({
  message,
  hint,
  className,
  variant = 'embedded',
}: {
  message: string;
  /** When false, no subtitle. When string, custom subtitle. When omitted, uses common.errorHint */
  hint?: string | false;
  className?: string;
  variant?: StateSurfaceVariant;
}) {
  const t = useTranslation();
  const displayHint = hint === false ? undefined : (hint ?? t('common.errorHint'));

  return (
    <StateSurface variant={variant} className={className} message={message} hint={displayHint} />
  );
}

/**
 * Inline error surface for page-level load failures (keeps red typography consistent).
 */
export function ErrorStateCard({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <Card density="compact" className={cn('text-center', className)}>
      <div className="text-sm text-red-600 dark:text-red-400 sm:text-base">{children}</div>
    </Card>
  );
}
