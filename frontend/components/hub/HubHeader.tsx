/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type { ReactNode } from 'react';

interface HubHeaderProps {
  eyebrow?: string;
  title: string;
  subtitle?: string;
  /** Left-side media (avatar, icon chip). */
  media?: ReactNode;
  /** Right-side slot (e.g. a switch-organization control). */
  action?: ReactNode;
  /** Tighter header when many hub tiles need vertical space (e.g. long grids). */
  density?: 'default' | 'compact';
  /** When true, sets `data-org-hub-header` for org-switch menu width anchoring. */
  organizationMenuAnchor?: boolean;
}

/** Identity header for a section hub - mirrors the dashboard greeting card. */
export function HubHeader({
  eyebrow,
  title,
  subtitle,
  media,
  action,
  density = 'default',
  organizationMenuAnchor = false,
}: HubHeaderProps) {
  const compact = density === 'compact';

  const compactCardClass =
    'relative mb-2 flex w-full max-w-full items-center gap-2 rounded-lg border border-[var(--border-color)] bg-[var(--card-bg)] px-2.5 py-2 sm:mb-2.5 sm:gap-2.5 sm:px-3 sm:py-2';

  const eyebrowClass = compact
    ? 'min-w-0 text-[10px] font-semibold uppercase tracking-wide text-[var(--text-secondary)] sm:text-[10px] sm:font-medium'
    : 'text-[10px] font-medium uppercase tracking-wide text-[var(--text-secondary)] sm:text-xs';

  return (
    <div
      {...(organizationMenuAnchor ? { 'data-org-hub-header': true } : {})}
      className={
        compact
          ? compactCardClass
          : 'relative mb-3 flex w-full max-w-full items-center gap-3 rounded-lg border border-[var(--border-color)] bg-[var(--card-bg)] p-3.5 sm:gap-4 sm:p-4'
      }
    >
      {media ? <div className="shrink-0">{media}</div> : null}
      <div className="min-w-0 flex-1">
        {eyebrow ? <p className={eyebrowClass}>{eyebrow}</p> : null}
        <h1
          className={
            compact
              ? 'mt-0.5 flex min-w-0 items-center gap-1 text-base font-semibold leading-tight tracking-tight text-[var(--text-primary)] sm:text-lg'
              : 'mt-0.5 flex min-w-0 items-center gap-1 text-lg font-semibold leading-tight tracking-tight text-[var(--text-primary)] sm:text-xl'
          }
        >
          <span className="min-w-0 flex-1 truncate">{title}</span>
        </h1>
        {subtitle ? (
          <p
            className={
              compact
                ? 'mt-0.5 truncate text-xs text-[var(--text-secondary)] sm:text-xs'
                : 'mt-0.5 truncate text-xs text-[var(--text-secondary)] sm:text-sm'
            }
          >
            {subtitle}
          </p>
        ) : null}
      </div>
      {action ? <div className="shrink-0">{action}</div> : null}
    </div>
  );
}
