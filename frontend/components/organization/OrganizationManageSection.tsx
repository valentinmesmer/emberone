/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import type { ReactNode } from 'react';
import { SectionHeader } from '@/components/ui/section-header';
import { cn } from '@/lib/utils/cn';

interface OrganizationManageSectionProps {
  title: string;
  subtitle?: ReactNode;
  action?: ReactNode;
  /** Keep `action` beside the title on small screens (icon-only actions). */
  actionInline?: boolean;
  children: ReactNode;
  /** Inside a Card on the org detail page - no extra section padding. */
  embedded?: boolean;
}

/**
 * Plain section header + body for organization manage - no outer Card
 * (lists use row Cards / EmptyStateCard like appointments).
 */
export function OrganizationManageSection({
  title,
  subtitle,
  action,
  actionInline,
  children,
  embedded = false,
}: OrganizationManageSectionProps) {
  return (
    <section className={cn('space-y-2', !embedded && 'py-5 sm:py-6')}>
      <SectionHeader
        title={title}
        subtitle={subtitle}
        action={action}
        actionInline={actionInline}
        className={embedded ? 'mb-3' : undefined}
      />
      {children}
    </section>
  );
}
