/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type { ReactNode } from 'react';
import { successWarningSplitSurfaceBackground } from '@/lib/ui/successWarningSplitStyle';
import { cn } from '@/lib/utils/cn';

export interface QualificationOrgProgressBadgeProps {
  children: ReactNode;
  /** Share of orgs that granted the decision (0-1). */
  ratio: number;
  pill?: boolean;
  className?: string;
}

/** Badge with a green/orange fill split at the org-decision progress ratio. */
export function QualificationOrgProgressBadge({
  children,
  ratio,
  pill = false,
  className,
}: QualificationOrgProgressBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 px-1.5 py-0.5 text-xs font-medium leading-tight text-[var(--text-primary)] ring-1 ring-[var(--border-color)]/50',
        pill ? 'rounded-full' : 'rounded-md',
        className,
      )}
      style={{ background: successWarningSplitSurfaceBackground(ratio) }}
    >
      {children}
    </span>
  );
}
