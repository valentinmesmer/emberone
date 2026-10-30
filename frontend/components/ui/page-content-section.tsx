/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { ReactNode } from 'react';
import { cn } from '@/lib/utils/cn';

interface PageContentSectionProps {
  children: ReactNode;
  className?: string;
}

/**
 * Standard vertical stack below `PageHeader` (e.g. inside `MobileNavigationLayout`).
 * Mobile-first spacing aligned with profile / department pages.
 */
export function PageContentSection({ children, className }: PageContentSectionProps) {
  return <div className={cn('mt-1 space-y-2 sm:mt-0 sm:space-y-3', className)}>{children}</div>;
}
