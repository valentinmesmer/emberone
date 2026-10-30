/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type { ReactNode } from 'react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils/cn';

export interface SectionHeaderProps {
  title: string;
  /** Secondary line under the title (e.g. a count or short description). */
  subtitle?: ReactNode;
  /** Right-aligned slot (e.g. an add button). */
  action?: ReactNode;
  className?: string;
}

export type SectionActionsLayout = 'fill' | 'inline';

export interface SectionActionsProps {
  children: ReactNode;
  className?: string;
  /**
   * - `fill`: stacked full-width on small screens; from `sm`, equal-width columns across the row
   *   (1 button = 100%, 2 = 50/50, etc.). Default for card footers and content action rows.
   * - `inline`: stacked on small screens; from `sm`, content-sized buttons in a row (header actions).
   */
  layout?: SectionActionsLayout;
}

/**
 * Groups action buttons with a consistent mobile-first layout.
 * Use `layout="inline"` inside `SectionHeader` / `PageHeader` action slots.
 */
export function SectionActions({ children, className, layout = 'fill' }: SectionActionsProps) {
  return (
    <div
      className={cn(
        layout === 'fill'
          ? cn(
              'grid w-full grid-cols-1 gap-2 sm:grid-cols-[repeat(auto-fit,minmax(0,1fr))]',
              '[&>*]:min-w-0',
              '[&_button]:w-full sm:[&_button]:!w-full',
            )
          : 'flex w-full flex-col gap-2 sm:w-auto sm:flex-row sm:flex-wrap sm:items-stretch sm:justify-start',
        className,
      )}
    >
      {children}
    </div>
  );
}

/**
 * Mid-level section header: title (+ optional count line) + action.
 * One look for in-page section headings - shared by manage sections, attendance,
 * detail cards, etc. (For page titles use `PageHeader`; for hubs use `HubSection`.)
 * Decorative section icons are not shown - use the sidebar or hub tiles for wayfinding.
 *
 * **Card actions (mobile-first):**
 * - Use `SectionHeader` inside cards - never `PageHeader`.
 * - Create: `action={<SectionActions layout="inline"><Button … icon={Plus} /></SectionActions>}`.
 * - Edit this surface: `action={<SectionEditAction … />}` or `DetailCardHeader` for entity detail cards.
 * - Row maintenance: `ListRow.actions` with `IconButton`; navigating rows hide actions on mobile.
 * - Rare / destructive / admin lifecycle: `ActionsSection` at page bottom with `ListRow` children (optional `ActionRowIcon`; no chevrons).
 * - Pass labeled CTAs via `action={<SectionActions layout="inline">…</SectionActions>}`.
 * - Buttons: `size="xs"`, `fullWidth="responsive"` (same CTA pattern as `PageHeader` / `ModalFooter`).
 * - Multiple actions: secondary first, primary last (thumb reach on mobile).
 */
export interface SectionEditActionProps {
  label: string;
  onClick: () => void;
  disabled?: boolean;
  title?: string;
}

/** Pencil control for the `SectionHeader` / card `action` slot when editing the current surface. */
export function SectionEditAction({ label, onClick, disabled, title }: SectionEditActionProps) {
  const hint = title ?? label;
  return (
    <SectionActions layout="inline" className="sm:justify-end">
      <Button
        type="button"
        variant="secondary"
        size="xs"
        fullWidth="responsive"
        title={hint}
        onClick={onClick}
        disabled={disabled}
      >
        {label}
      </Button>
    </SectionActions>
  );
}

export function SectionHeader({ title, subtitle, action, className }: SectionHeaderProps) {
  return (
    <div
      className={cn(
        'flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between',
        className,
      )}
    >
      <div className="flex min-w-0 flex-1 items-start gap-2 sm:gap-2.5">
        <div className="min-w-0 flex-1">
          <h2 className="text-sm font-semibold text-[var(--text-primary)] sm:text-base">{title}</h2>
          {subtitle != null ? (
            <p className="text-xs text-[var(--text-secondary)]">{subtitle}</p>
          ) : null}
        </div>
      </div>
      {action ? (
        <div className="flex w-full shrink-0 flex-col items-stretch gap-2 sm:ml-auto sm:w-auto sm:items-end sm:justify-center">
          {action}
        </div>
      ) : null}
    </div>
  );
}
