/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import Link from 'next/link';
import type { ReactNode } from 'react';
import { Card } from '@/components/ui/card';
import { EmptyState as InlineEmptyState } from '@/components/ui/message-cards';
import { cn } from '@/lib/utils/cn';

/** Preview size for home dashboard entity lists (todos / appointments). */
export const HOME_DASHBOARD_PREVIEW_LIMIT = 5;

/**
 * Same row height for title-only todos and title+subtitle appointments so the
 * two home cards align. ListRow centers single-line content in the taller row.
 */
export const HOME_DASHBOARD_ROW_CLASSNAME =
  'min-h-[2.75rem] sm:min-h-[3rem] [&>div>*]:!min-h-0 [&>div>*]:!py-1.5';

function PanelEmptyState({ label }: { label: string }) {
  return (
    <div className="flex flex-1 flex-col items-center justify-center rounded-lg border border-dashed border-[var(--border-color)]/70 px-3 py-4 text-center sm:py-5">
      <InlineEmptyState centered>{label}</InlineEmptyState>
    </div>
  );
}

function CountBadge({ count }: { count: number }) {
  return (
    <span
      className={cn(
        'inline-flex h-5 min-w-5 shrink-0 items-center justify-center rounded-full border border-[var(--border-color)]',
        'bg-[var(--surface-muted)] px-1.5 text-[11px] font-semibold tabular-nums leading-none text-[var(--text-secondary)] sm:h-6 sm:min-w-6 sm:px-2 sm:text-xs',
      )}
      title={`${count}`}
    >
      {count > 99 ? '99+' : count}
    </span>
  );
}

export interface HomeDashboardListPanelProps {
  title: string;
  /** Shown next to the title when the list has loaded (e.g. open / unread count). */
  count?: number | null;
  href?: string | null;
  isLoading: boolean;
  loadingLabel: string;
  emptyLabel: string;
  isEmpty: boolean;
  listLabel: string;
  children: ReactNode;
}

/** Shared shell for home dashboard entity lists (todos, appointments). */
export function HomeDashboardListPanel({
  title,
  count,
  href,
  isLoading,
  loadingLabel,
  emptyLabel,
  isEmpty,
  listLabel,
  children,
}: HomeDashboardListPanelProps) {
  const titleNode = (
    <h3 className="min-w-0 flex-1 truncate text-sm font-semibold leading-tight text-[var(--text-primary)] sm:text-base">
      {title}
    </h3>
  );

  const headerClassName = 'mb-1.5 flex min-h-6 items-center gap-2.5 sm:mb-2 sm:min-h-7 sm:gap-3';

  return (
    <Card className="flex h-full flex-col !p-2.5 sm:!p-3.5">
      {href ? (
        <Link
          href={href}
          className={cn(
            headerClassName,
            'outline-none focus-visible:rounded-md focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)]',
          )}
        >
          {titleNode}
          {count != null ? <CountBadge count={count} /> : null}
        </Link>
      ) : (
        <div className={headerClassName}>
          {titleNode}
          {count != null ? <CountBadge count={count} /> : null}
        </div>
      )}

      <section className="flex flex-1 flex-col" aria-label={listLabel}>
        {isLoading ? (
          <div className="flex flex-1 items-center justify-center rounded-lg border border-dashed border-[var(--border-color)]/70 px-3 py-4 text-center text-sm text-[var(--text-secondary)]">
            {loadingLabel}
          </div>
        ) : isEmpty ? (
          <PanelEmptyState label={emptyLabel} />
        ) : (
          <div className="-mx-0.5 flex min-h-0 flex-1 flex-col overflow-hidden rounded-lg bg-[var(--surface-muted)]/35 sm:-mx-1">
            {children}
          </div>
        )}
      </section>
    </Card>
  );
}
