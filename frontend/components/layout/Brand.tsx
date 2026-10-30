/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useTranslation } from '@/hooks/useTranslation';
import { cn } from '@/lib/utils/cn';

type BrandVariant = 'sidebar' | 'compact';

interface BrandProps {
  onClick?: () => void;
  className?: string;
  variant?: BrandVariant;
  /** Hides the descriptive subtitle (e.g. compact mobile header). */
  showSubtitle?: boolean;
}

const brandTextSizes: Record<BrandVariant, string> = {
  sidebar: 'text-[1.375rem]',
  compact: 'text-lg sm:text-xl',
};

/**
 * App chrome branding: typographic wordmark (Brand Book v1.0).
 * Icon is never paired with the wordmark - app-icon / favicon only.
 */
export function Brand({
  onClick,
  className,
  variant = 'compact',
  showSubtitle = variant === 'sidebar',
}: BrandProps) {
  const t = useTranslation();
  const subtitle = t('brand.subtitle');

  const lockup = (
    <div className="min-w-0 text-left">
      <span
        className={cn(
          brandTextSizes[variant],
          'block font-heading font-extrabold leading-none tracking-[-0.025em] whitespace-nowrap text-[var(--text-primary)]',
        )}
      >
        Ember
        <span className="ml-[0.04em] text-[var(--fire-red)]">One</span>
      </span>
      {showSubtitle && subtitle ? (
        <span className="mt-1.5 block truncate text-[10px] font-medium leading-tight tracking-wide text-[var(--text-secondary)]">
          {subtitle}
        </span>
      ) : null}
    </div>
  );

  const rootClassName = cn(
    'inline-flex min-w-0 shrink-0 select-none',
    onClick &&
      'cursor-pointer rounded-lg transition-opacity hover:opacity-80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--focus-ring-offset)]',
    className,
  );

  if (onClick) {
    return (
      <button type="button" onClick={onClick} className={rootClassName} aria-label="EmberOne">
        {lockup}
      </button>
    );
  }

  return <div className={rootClassName}>{lockup}</div>;
}
