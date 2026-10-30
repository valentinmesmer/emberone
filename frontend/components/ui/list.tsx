/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ChevronRight } from 'lucide-react';
import Link from 'next/link';
import { Children, type ReactNode } from 'react';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import type { ListRowTone } from '@/components/ui/tones';
import { LIST_ROW_HREF_ATTRIBUTE, rememberListSiblings } from '@/lib/navigation/listSiblings';
import { cn } from '@/lib/utils/cn';

/* ============================================================================
 * Shared list design system - one look for every list in the app.
 * ----------------------------------------------------------------------------
 * Use <List> / <ListContainer> as the shell and <ListItem> / <ListRow> for each
 * entry. Rows render as real <li> elements - do not wrap them in another <li>.
 *
 * Status / label chips: always pass via `badges` (never inside `title`,
 * `subtitle`, or `trailing`). They render outside the text block, right-aligned
 * before the chevron. Multiple chips stack on mobile (`flex-col`) and sit in a
 * wrapping row from `sm` up - title keeps the remaining width. Pass one or more
 * <Badge>s; wrapping with <ListRowBadges> is optional.
 *
 * `trailing` is for non-chip content (counts, role labels, icons). Order:
 * text → badges → trailing → chevron.
 *
 * Row state color (`tone`): left accent bar only - no full-row background tint
 * (avoids clashing with status badges). Hover stays neutral.
 *
 * For icon+text detail lines inside a row use <ListRowMeta> / <ListRowMetaItem>.
 * For schedule entries (when · [type]) pass <ScheduleMetaRow> as `meta`.
 *
 * Shells:
 *   card  - elevated page-level lists (default)
 *   inset - bordered box for modals, scroll areas, nested panels
 *   flush - no outer chrome; embed inside an existing Card/section
 *
 * Actions sit on the same row as the content (right-aligned) - no second line,
 * no side-rail border. On mobile they hide when the row navigates (`href` /
 * `onClick`) unless `keepActionsOnMobile` is set; use the detail page there.
 * Non-navigating rows keep actions so inline controls stay reachable. Use
 * `below` for a primary full-width CTA (e.g. stamp-out).
 *
 * Drag handles must use `dragHandle` (outside `href` / `onClick`) so a drop
 * does not navigate and touch reorder is not swallowed by the row link.
 * ==========================================================================*/

export type { ListRowTone };

type ListVariant = 'card' | 'inset' | 'flush';

/** Full-width hairlines (default) or indented dividers between rows. */
export type ListRowSeparator = 'divide' | 'inset';

const listSeparatorClasses: Record<ListRowSeparator, string> = {
  // gap-px + row surfaces: stable hairlines when rows mount (e.g. infinite scroll) without
  // divide-y border repaint flashes at the insertion seam.
  divide: 'flex flex-col gap-px bg-[var(--border-color)] [&>li]:bg-[var(--card-bg)]',
  inset:
    '[&>li+li]:relative [&>li+li]:before:pointer-events-none [&>li+li]:before:absolute [&>li+li]:before:inset-x-3 [&>li+li]:before:top-0 [&>li+li]:before:h-px [&>li+li]:before:bg-[var(--border-color)]/80',
};

export interface ListContainerProps {
  children: ReactNode;
  /** Accessible name for the underlying <ul>. */
  label?: string;
  /**
   * `card` (default): elevated surface for page-level lists.
   * `inset`: bordered box without the card shadow - for modals, scroll areas and trees.
   * `flush`: no outer shell - rows only, for embedding inside another surface.
   */
  variant?: ListVariant;
  /**
   * Row dividers. `divide` (default): edge-to-edge hairlines.
   * `inset`: shorter lines inset from the left/right - opt-in per list.
   */
  rowSeparator?: ListRowSeparator;
  /** Caps the height and enables scrolling (e.g. `max-h-64`), mainly for `inset` lists. */
  maxHeightClassName?: string;
  className?: string;
}

/** The list shell: a single surface whose rows are separated by hairlines. */
export function ListContainer({
  children,
  label,
  variant = 'card',
  rowSeparator = 'divide',
  maxHeightClassName,
  className,
}: ListContainerProps) {
  const separatorClass = listSeparatorClasses[rowSeparator];

  if (variant === 'inset') {
    return (
      <ul
        aria-label={label}
        className={cn(
          separatorClass,
          'rounded-lg border border-[var(--border-color)]',
          // Scroll when capped, otherwise clip so rows don't bleed past the rounded corners.
          maxHeightClassName ? `${maxHeightClassName} overflow-y-auto` : 'overflow-hidden',
          className,
        )}
      >
        {children}
      </ul>
    );
  }

  if (variant === 'flush') {
    return (
      <ul aria-label={label} className={cn(separatorClass, 'overflow-hidden', className)}>
        {children}
      </ul>
    );
  }

  return (
    <Card className={cn('overflow-hidden !p-0', className)}>
      <ul aria-label={label} className={separatorClass}>
        {children}
      </ul>
    </Card>
  );
}

/** Preferred alias - same as `ListContainer`. */
export const List = ListContainer;

/** Left accent bar only (no full-row tint). The 3px reserve is constant so toned rows never shift content. */
const toneClasses: Record<ListRowTone, string> = {
  default: 'border-l-transparent hover:bg-[var(--hover-bg)]/60',
  success: 'border-l-[var(--status-success-accent)] hover:bg-[var(--hover-bg)]/60',
  danger: 'border-l-[var(--status-danger-accent)] hover:bg-[var(--hover-bg)]/60',
  neutral: 'border-l-[var(--text-secondary)] hover:bg-[var(--hover-bg)]/60',
  warning: 'border-l-[var(--status-warning-accent)] hover:bg-[var(--hover-bg)]/60',
  active: 'border-l-[var(--fire-red)] hover:bg-[var(--hover-bg)]/60',
  info: 'border-l-[var(--toast-info-border)] hover:bg-[var(--hover-bg)]/60',
};

type ListRowRootProps = React.HTMLAttributes<HTMLLIElement> & {
  [dataAttr: `data-${string}`]: string | number | undefined;
};

export interface ListRowProps {
  title: ReactNode;
  /** Secondary line directly under the title. */
  subtitle?: ReactNode;
  /**
   * Status / label chips outside the text block (right-aligned, before chevron).
   * Prefer this over putting <Badge> inside `title` or `trailing`.
   * Multiple chips stack on mobile and wrap in a row from `sm` up.
   */
  badges?: ReactNode;
  /** Icon+text detail lines; build with <ListRowMeta>. */
  meta?: ReactNode;
  /**
   * Control to the left of the row, outside `href` / `onClick`.
   * Use this for reorder handles - never put them in `leading`.
   */
  dragHandle?: ReactNode;
  /** Leading column: accent icon, avatar, … (inside the row link). */
  leading?: ReactNode;
  /**
   * Non-chip content between badges and the chevron (counts, role labels, icons).
   * Status / label chips belong in `badges`.
   */
  trailing?: ReactNode;
  /**
   * Right-aligned controls on the same row. Hidden on mobile when the row
   * navigates (`href` / `onClick`); always visible on desktop.
   * Set `keepActionsOnMobile` when inline edit must stay reachable on phones.
   */
  actions?: ReactNode;
  /** Keep `actions` visible below `sm` even when the row navigates. */
  keepActionsOnMobile?: boolean;
  /** Full-width content below the main row (e.g. primary mobile CTA). */
  below?: ReactNode;
  /** Makes the whole main area a link. */
  href?: string;
  /**
   * Detail route for rows that navigate via `onClick`; enables prev/next on that detail page.
   * Not needed with `href`.
   */
  detailHref?: string;
  /** Makes the whole main area a button. Ignored when `href` is set. */
  onClick?: () => void;
  /** Accessible name for the interactive control (`href` / `onClick`). */
  ariaLabel?: string;
  /**
   * Trailing chevron affordance. Defaults to true when `href`/`onClick` is set.
   * Inside `ActionsSection`, chevrons are stripped for a uniform look.
   */
  chevron?: boolean;
  tone?: ListRowTone;
  /** Colored left accent and row tint. Default `true`; set `false` for neutral rows inside grouped lists. */
  accent?: boolean;
  /** Highlight the row as selected (e.g. picker rows in a modal). */
  selected?: boolean;
  /** Tighter padding for dense inbox-style lists (notifications, todos). */
  compact?: boolean;
  className?: string;
  /** Extra props for the root <li>: drag handlers, `data-*` attributes, dynamic classes, … */
  rootProps?: ListRowRootProps;
}

/** A single list entry. Renders as <li> - put it directly inside <List>/<ListContainer>. */
export function ListRow({
  title,
  subtitle,
  badges,
  meta,
  dragHandle,
  leading,
  trailing,
  actions,
  keepActionsOnMobile = false,
  below,
  href,
  detailHref,
  onClick,
  ariaLabel,
  chevron,
  tone = 'default',
  accent = true,
  selected,
  compact = false,
  className,
  rootProps,
}: ListRowProps) {
  const interactive = !!href || !!onClick;
  const showChevron = chevron ?? interactive;
  // Navigating rows: mobile goes to detail - keep the list uncluttered.
  const hideActionsOnMobile = interactive && !keepActionsOnMobile;
  const { className: rootClassName, ...restRootProps } = rootProps ?? {};

  const mainClassName = cn(
    'flex min-w-0 flex-1 items-center gap-2.5',
    // Touch-friendly default; compact keeps inbox lists denser without shrinking below ~44px.
    compact ? 'min-h-11 py-2' : 'min-h-12 py-2.5',
    dragHandle ? 'pl-1.5 pr-3' : 'px-3',
    interactive && 'active:bg-[var(--hover-bg)]/50',
  );

  const mainInner = (
    <>
      {leading}
      <div className="min-w-0 flex-1">
        <div className="line-clamp-2 text-sm font-semibold leading-snug text-[var(--text-primary)]">
          {title}
        </div>
        {subtitle != null ? (
          <div className={cn('text-xs text-[var(--text-secondary)]', !compact && 'mt-0.5')}>
            {subtitle}
          </div>
        ) : null}
        {meta}
      </div>
      {badges != null ? <ListRowBadges>{badges}</ListRowBadges> : null}
      {trailing != null ? <div className="shrink-0">{trailing}</div> : null}
      {showChevron ? (
        <ChevronRight className="h-4 w-4 shrink-0 text-[var(--text-secondary)]" aria-hidden />
      ) : null}
    </>
  );

  const main = href ? (
    <Link
      href={href}
      aria-label={ariaLabel}
      className={mainClassName}
      {...{ [LIST_ROW_HREF_ATTRIBUTE]: href }}
      onClick={(event) => rememberListSiblings(event.currentTarget)}
    >
      {mainInner}
    </Link>
  ) : onClick ? (
    <button
      type="button"
      onClick={(event) => {
        if (detailHref) rememberListSiblings(event.currentTarget);
        onClick();
      }}
      aria-label={ariaLabel}
      className={cn(mainClassName, 'text-left')}
      {...(detailHref ? { [LIST_ROW_HREF_ATTRIBUTE]: detailHref } : {})}
    >
      {mainInner}
    </button>
  ) : (
    <div className={mainClassName}>{mainInner}</div>
  );

  return (
    <li
      {...restRootProps}
      className={cn(
        // shrink-0: height-capped flex lists (max-h + overflow-y-auto) must scroll,
        // not compress rows - overflow-hidden would otherwise allow min-size 0.
        'flex shrink-0 flex-col overflow-hidden border-l-[3px] transition-colors',
        accent ? toneClasses[tone] : 'border-l-transparent hover:bg-[var(--hover-bg)]/60',
        selected && 'bg-[var(--fire-red)]/10',
        className,
        rootClassName,
      )}
    >
      <div className="flex min-h-0 flex-1 items-stretch">
        {dragHandle ? (
          <div className="flex shrink-0 items-center self-stretch pl-1.5">{dragHandle}</div>
        ) : null}
        {main}
        {actions ? (
          <div
            className={cn(
              'flex shrink-0 items-center justify-end gap-0.5 py-1 pr-2',
              hideActionsOnMobile && 'hidden sm:flex',
            )}
          >
            {actions}
          </div>
        ) : null}
      </div>
      {below ? (
        <div className="border-t border-[var(--border-color)] px-3 py-2">{below}</div>
      ) : null}
    </li>
  );
}

/** Preferred alias - same as `ListRow`. */
export const ListItem = ListRow;

/**
 * Status/label chip cluster for a row’s `badges` slot.
 * Mobile: single non-wrapping line, horizontally scrollable, capped width so the title keeps space.
 * `sm+`: horizontal wrap, right-aligned.
 */
export function ListRowBadges({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  const items = Children.toArray(children).filter(Boolean);
  if (items.length === 0) return null;
  return (
    <div
      className={cn(
        'flex max-w-[min(11rem,46%)] shrink-0 flex-row flex-nowrap items-center gap-1 overflow-x-auto overscroll-x-contain',
        'sm:max-w-[min(16rem,50%)] sm:flex-wrap sm:justify-end sm:gap-1.5 sm:overflow-visible',
        className,
      )}
    >
      {items}
    </div>
  );
}

/** Container for the icon+text detail lines inside a <ListRow> (`meta` slot). */
export function ListRowMeta({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <ul className={cn('mt-1.5 space-y-1 text-xs text-[var(--text-primary)]', className)}>
      {children}
    </ul>
  );
}

/** A single icon+text detail line. Pass an already-sized icon (e.g. `h-3 w-3 text-[var(--fire-red)]`). */
export function ListRowMetaItem({
  icon,
  children,
  align = 'start',
}: {
  icon?: ReactNode;
  children: ReactNode;
  align?: 'start' | 'center';
}) {
  return (
    <li className={cn('flex min-w-0 gap-1.5', align === 'center' ? 'items-center' : 'items-start')}>
      {icon}
      <span className={cn('min-w-0', align === 'start' && 'break-words')}>{children}</span>
    </li>
  );
}

export interface ScheduleMetaRowProps {
  /** Full range from {@link formatAppointmentDateTimeRange} (same-day or multi-day). */
  when: string;
  /** Optional middle badge (e.g. appointment type). Omit/`null`/`''` for single-type lists. */
  typeChip?: string | null;
}

/** Standard schedule meta line: when · [type chip] - for appointment/event/session rows. */
export function ScheduleMetaRow({ when, typeChip }: ScheduleMetaRowProps) {
  const showChip = typeChip != null && String(typeChip).trim() !== '';
  return (
    <div className="mt-0.5 flex flex-wrap items-center gap-x-1.5 gap-y-0.5 text-xs tabular-nums text-[var(--text-secondary)]">
      <span>{when}</span>
      {showChip ? (
        <>
          <span aria-hidden>·</span>
          <Badge tone="neutral" className="py-px">
            {typeChip}
          </Badge>
        </>
      ) : null}
    </div>
  );
}
