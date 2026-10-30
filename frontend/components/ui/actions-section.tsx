/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type { LucideIcon } from 'lucide-react';
import { Children, cloneElement, isValidElement, type ReactElement, type ReactNode } from 'react';
import { ListContainer, ListItem, ListRow, type ListRowProps } from '@/components/ui/list';
import { useTranslation } from '@/hooks/useTranslation';
import { cn } from '@/lib/utils/cn';

export interface ActionsSectionProps {
  children: ReactNode;
  /** Section heading; defaults to `admin.userDetail.actionsSection`. */
  title?: string;
  /** Accessible label for the list; defaults to `title`. */
  label?: string;
  className?: string;
}

function isActionListRow(child: ReactElement): child is ReactElement<ListRowProps> {
  return child.type === ListRow || child.type === ListItem;
}

/** Uniform leading icon for an action row: neutral tile, no tone. Pass via `ListRow.leading`. */
export function ActionRowIcon({ icon: Icon }: { icon: LucideIcon }) {
  return (
    <span
      className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-[var(--hover-bg)] text-[var(--text-secondary)]"
      aria-hidden
    >
      <Icon className="h-4 w-4" />
    </span>
  );
}

/** Action rows keep an optional leading icon (`ActionRowIcon`); no navigation chevron, no tone accent. */
function normalizeActionRow(child: ReactNode): ReactNode {
  if (!isValidElement(child) || !isActionListRow(child)) {
    return child;
  }
  return cloneElement(child, { chevron: false, tone: 'default' });
}

/**
 * Page-bottom block for rare, structural, or destructive operations.
 * Each child should be a `ListRow`.
 * Rows are normalized to a uniform look: optional `ActionRowIcon` + title/subtitle - no chevron, no tone.
 */
export function ActionsSection({ children, title, label, className }: ActionsSectionProps) {
  const t = useTranslation();
  const sectionTitle = title ?? t('admin.userDetail.actionsSection');
  const containerLabel = label ?? sectionTitle;

  // Children.toArray drops false/null/undefined, so conditionally omitted actions do not count.
  if (Children.toArray(children).length === 0) {
    return null;
  }

  return (
    <section className={cn('space-y-1.5', className)}>
      <h2 className="text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
        {sectionTitle}
      </h2>
      <ListContainer label={containerLabel} rowSeparator="inset">
        {Children.map(children, normalizeActionRow)}
      </ListContainer>
    </section>
  );
}
