/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ChevronLeft, ChevronRight } from 'lucide-react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { type ReactNode, useEffect, useState } from 'react';
import { useTranslation } from '@/hooks/useTranslation';
import { readListSiblings } from '@/lib/navigation/listSiblings';
import { cn } from '@/lib/utils/cn';

const arrowClassName = cn(
  'inline-flex h-9 w-9 items-center justify-center rounded-lg border border-[var(--border-color)]',
  'bg-[var(--card-bg)] text-[var(--text-primary)] transition-colors hover:bg-[var(--hover-bg)]',
  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)]',
);

/**
 * Previous / next arrows on a detail page that was opened from a list row.
 * Renders nothing when the page was not reached through a list.
 */
export function ListSiblingNav() {
  const t = useTranslation();
  const pathname = usePathname();
  const [paths, setPaths] = useState<string[]>([]);

  // biome-ignore lint/correctness/useExhaustiveDependencies: re-read the stored list after each navigation
  useEffect(() => {
    setPaths(readListSiblings());
  }, [pathname]);

  const index = paths.indexOf(pathname);
  if (index < 0 || paths.length < 2) return null;

  const prev = index > 0 ? paths[index - 1] : null;
  const next = index < paths.length - 1 ? paths[index + 1] : null;

  const arrow = (href: string | null, label: string, icon: ReactNode) =>
    href ? (
      <Link href={href} replace aria-label={label} title={label} className={arrowClassName}>
        {icon}
      </Link>
    ) : (
      <span aria-hidden className={cn(arrowClassName, 'pointer-events-none opacity-40')}>
        {icon}
      </span>
    );

  return (
    <nav aria-label={t('common.listNavigation')} className="ml-auto flex items-center gap-1.5">
      <span className="mr-1 text-xs text-[var(--text-secondary)]">
        {index + 1} / {paths.length}
      </span>
      {arrow(prev, t('common.previous'), <ChevronLeft className="h-5 w-5" />)}
      {arrow(next, t('common.next'), <ChevronRight className="h-5 w-5" />)}
    </nav>
  );
}
