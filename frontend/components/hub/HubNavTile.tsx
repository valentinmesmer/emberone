/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ArrowRight } from 'lucide-react';
import Link from 'next/link';
import type { ReactNode } from 'react';
import { cn } from '@/lib/utils/cn';
import { plainIconSurfaceClassName } from '@/lib/utils/plainIconSurface';

type HubNavTileProps = {
  icon: ReactNode;
  title: string;
  hint?: string;
  disabled?: boolean;
  /** Dense row for long lists (e.g. many org areas); mobile-first. */
  variant?: 'default' | 'compact';
  /**
   * Tighter card chrome for hub grids - uniform min-height, smaller padding (mobile-first).
   */
  cardDensity?: 'default' | 'dense';
  /** Positive integer shown as a count badge (e.g. unread notifications, open todos). */
  badge?: number;
  className?: string;
} & ({ href: string } | { onClick: () => void });

/** Large tap-friendly navigation tile linking from a hub into a sub-area. */
export function HubNavTile(props: HubNavTileProps) {
  const {
    icon,
    title,
    hint,
    disabled,
    variant = 'default',
    cardDensity = 'default',
    badge,
    className,
  } = props;
  const href = 'href' in props ? props.href : undefined;
  const onClick = 'onClick' in props ? props.onClick : undefined;
  const isCompact = variant === 'compact';
  const isDenseCard = !isCompact && cardDensity === 'dense';

  /** Hub grid tiles: no subtitle - tighter, equal-height chips. */
  const showHint = Boolean(hint) && !isDenseCard;

  const countBadge =
    badge != null && badge > 0 ? (
      <span
        className={cn(
          'inline-flex shrink-0 items-center justify-center self-center rounded-full px-1.5 font-semibold tabular-nums leading-none',
          'bg-[var(--fire-red)]/12 text-[var(--fire-red)] dark:bg-[var(--fire-red)]/20 dark:text-[var(--fire-red-light)]',
          isCompact ? 'min-h-[1.125rem] text-[10px]' : 'min-h-[1.25rem] text-[10px] sm:text-[11px]',
        )}
        title={`${badge}`}
      >
        {badge > 99 ? '99+' : badge}
      </span>
    ) : null;

  const inner = (
    <div
      className={cn(
        'group flex w-full transition-all',
        isDenseCard ? 'items-center' : 'items-start',
        isCompact
          ? 'min-h-11 gap-2.5 bg-transparent px-3 py-2 sm:min-h-12 sm:gap-3 sm:px-3.5 sm:py-2.5'
          : isDenseCard
            ? 'box-border flex min-h-[3.25rem] w-full flex-1 items-center gap-2.5 overflow-hidden rounded-lg border border-[var(--border-color)] bg-[var(--card-bg)] px-2.5 py-2 sm:h-[4rem] sm:min-h-0 sm:gap-2 sm:px-2.5 sm:py-2'
            : 'flex h-full min-h-[5.25rem] gap-3 rounded-lg border border-[var(--border-color)] bg-[var(--card-bg)] p-3.5 sm:min-h-0 sm:p-4',
        !disabled &&
          !isCompact &&
          (isDenseCard
            ? 'active:bg-[var(--hover-bg)] sm:hover:bg-[var(--hover-bg)]'
            : 'hover:bg-[var(--hover-bg)] active:bg-[var(--hover-bg)]'),
        !disabled && isCompact && 'active:bg-[var(--hover-bg)] sm:hover:bg-[var(--hover-bg)]',
        disabled && 'opacity-60',
      )}
    >
      <div
        className={cn(
          plainIconSurfaceClassName,
          'shrink-0',
          isCompact
            ? 'mt-0.5 [&>svg]:h-4 [&>svg]:w-4'
            : isDenseCard
              ? '[&>svg]:h-4 [&>svg]:w-4 sm:[&>svg]:h-[18px] sm:[&>svg]:w-[18px]'
              : 'mt-0.5 [&>svg]:h-5 [&>svg]:w-5',
        )}
      >
        {icon}
      </div>
      <div className="min-w-0 flex-1 pr-0.5">
        <p
          className={cn(
            'break-words font-semibold leading-snug text-[var(--text-primary)] [overflow-wrap:anywhere]',
            isCompact
              ? 'line-clamp-2 text-sm sm:text-[15px]'
              : isDenseCard
                ? 'line-clamp-2 text-sm font-semibold leading-snug sm:text-[13px]'
                : 'line-clamp-2 text-[15px] sm:text-base',
          )}
        >
          {title}
        </p>
        {showHint && !isCompact ? (
          <p
            className={cn(
              'line-clamp-2 break-words leading-snug text-[var(--text-secondary)]',
              'mt-1 text-[13px] sm:text-sm',
            )}
          >
            {hint}
          </p>
        ) : null}
        {hint && isCompact ? (
          <p className="mt-0.5 line-clamp-2 break-words text-[11px] leading-snug text-[var(--text-secondary)] sm:text-xs">
            {hint}
          </p>
        ) : null}
      </div>
      {countBadge}
      {!disabled ? (
        <ArrowRight
          className={cn(
            'shrink-0 self-center text-[var(--text-secondary)]',
            isCompact && 'mt-1 h-3.5 w-3.5 sm:h-4 sm:w-4',
            isDenseCard && 'h-4 w-4 opacity-70 sm:h-3.5 sm:w-3.5 sm:opacity-100',
            !isCompact && !isDenseCard && 'mt-1 h-4 w-4',
          )}
        />
      ) : null}
    </div>
  );

  if (disabled) {
    return <div aria-disabled="true">{inner}</div>;
  }

  const focusRingClass =
    'block focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--focus-ring-offset)]';

  const outerClassName = cn(
    focusRingClass,
    isCompact && 'rounded-none',
    !isCompact && isDenseCard && 'flex h-full min-h-0 rounded-xl sm:rounded-lg',
    !isCompact && !isDenseCard && 'h-full rounded-xl',
    className,
  );

  if (onClick) {
    return (
      <button type="button" onClick={onClick} className={cn(outerClassName, 'text-left')}>
        {inner}
      </button>
    );
  }

  const isAbsoluteHttp = href != null && /^https?:\/\//i.test(href);

  if (isAbsoluteHttp) {
    return (
      <a href={href} className={outerClassName} rel="noopener noreferrer">
        {inner}
      </a>
    );
  }

  if (!href) {
    return null;
  }

  return (
    <Link href={href} className={outerClassName}>
      {inner}
    </Link>
  );
}
