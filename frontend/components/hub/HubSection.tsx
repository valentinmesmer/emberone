/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Sparkles } from 'lucide-react';
import type { ReactNode } from 'react';
import { cn } from '@/lib/utils/cn';

interface HubSectionProps {
  /** Omit when group subheadings already structure the content. */
  title?: string;
  children: ReactNode;
  /** Optional right-aligned slot (e.g. a "show all" link). */
  action?: ReactNode;
  className?: string;
  /** Tighter spacing under the hub header (mobile-first). */
  density?: 'default' | 'compact';
}

/** Section wrapper for hubs - optional title mirrors dashboard section headers. */
export function HubSection({
  title,
  children,
  action,
  className,
  density = 'default',
}: HubSectionProps) {
  const compact = density === 'compact';
  const showHeading = Boolean(title) || Boolean(action);

  return (
    <section
      className={cn(
        compact
          ? 'mt-1.5 space-y-1.5 first:mt-0 sm:mt-2 sm:space-y-2'
          : 'mt-3 space-y-1.5 first:mt-0 sm:space-y-2',
        className,
      )}
    >
      {showHeading ? (
        <div className="flex items-center justify-between gap-1.5 sm:gap-2">
          {title ? (
            <div className="flex min-w-0 items-center gap-1 text-[var(--text-primary)] sm:gap-1.5">
              <Sparkles
                className={cn(
                  'shrink-0 text-[var(--fire-red)] opacity-90',
                  compact ? 'h-3 w-3 sm:h-3.5 sm:w-3.5' : 'h-3.5 w-3.5 sm:h-4 sm:w-4',
                )}
              />
              <h2
                className={cn(
                  'font-semibold tracking-tight',
                  compact ? 'text-xs sm:text-sm' : 'text-sm sm:text-base',
                )}
              >
                {title}
              </h2>
            </div>
          ) : (
            <div />
          )}
          {action}
        </div>
      ) : null}
      {children}
    </section>
  );
}
