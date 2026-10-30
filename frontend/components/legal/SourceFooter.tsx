/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import Link from 'next/link';
import { useTranslation } from '@/hooks/useTranslation';
import { cn } from '@/lib/utils/cn';

type SourceFooterProps = {
  className?: string;
  /** Called when the link is activated (e.g. close the mobile nav drawer). */
  onNavigate?: () => void;
};

/** Discreet AGPL §13 pointer to the public Corresponding Source page. */
export function SourceFooter({ className, onNavigate }: SourceFooterProps) {
  const t = useTranslation();

  return (
    <footer className={cn('w-full', className)}>
      <Link
        href="/source"
        onClick={onNavigate}
        className={cn(
          'inline-block max-w-full text-[11px] leading-snug text-[var(--text-secondary)]/70',
          'underline-offset-2 transition-colors duration-150',
          'hover:text-[var(--text-secondary)] hover:underline',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--focus-ring-offset)]',
        )}
      >
        {t('legal.source.footer')}
      </Link>
    </footer>
  );
}
