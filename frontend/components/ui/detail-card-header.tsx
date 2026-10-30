/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type { ReactNode } from 'react';
import { IconButton } from '@/components/ui/icon-button';
import { cn } from '@/lib/utils/cn';

export interface DetailCardActionProps {
  /** Accessible name (and optional hover hint). */
  label: string;
  icon: ReactNode;
  onClick: () => void;
  disabled?: boolean;
  title?: string;
}

/** Icon-only control for detail card headers (edit, delete, archive, …). */
export function DetailCardAction({ label, icon, onClick, disabled, title }: DetailCardActionProps) {
  const hint = title ?? label;
  return (
    <IconButton
      variant="action"
      size="sm"
      icon={icon}
      onClick={onClick}
      disabled={disabled}
      title={hint}
      aria-label={label}
    />
  );
}

export interface DetailCardHeaderProps {
  title: ReactNode;
  /** Small uppercase line above the title (e.g. section kind). */
  eyebrow?: ReactNode;
  /** Icon or avatar column left of the title block. */
  leading?: ReactNode;
  badges?: ReactNode;
  subtitle?: ReactNode;
  actions?: ReactNode;
  className?: string;
}

/**
 * Title block for entity detail cards.
 * Icon actions sit top-right beside the title (events, appointments, org users, admin, …).
 */
export function DetailCardHeader({
  title,
  eyebrow,
  leading,
  badges,
  subtitle,
  actions,
  className,
}: DetailCardHeaderProps) {
  return (
    <div className={cn('mb-3 space-y-2', className)}>
      <div className="flex items-start justify-between gap-2">
        <div className="flex min-w-0 flex-1 items-start gap-2.5">
          {leading}
          <div className="min-w-0 flex-1">
            {eyebrow ? <div className="text-xs text-[var(--text-secondary)]">{eyebrow}</div> : null}
            <h1
              className={cn(
                'break-words text-base font-semibold text-[var(--text-primary)] sm:text-lg',
                eyebrow && 'mt-1',
              )}
            >
              {title}
            </h1>
            {badges ? <div className="mt-1.5 flex flex-wrap gap-1.5">{badges}</div> : null}
          </div>
        </div>
        {actions ? (
          <div className="flex shrink-0 flex-wrap items-center justify-end gap-1">{actions}</div>
        ) : null}
      </div>
      {subtitle ? (
        <div className="space-y-1 text-xs text-[var(--text-secondary)] sm:text-sm">{subtitle}</div>
      ) : null}
    </div>
  );
}
