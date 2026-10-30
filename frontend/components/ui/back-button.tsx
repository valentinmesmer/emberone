/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ArrowLeft } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useTranslation } from '@/hooks/useTranslation';
import { useBackHrefOverride } from '@/lib/navigation/backHrefOverride';
import { useParentPath } from '@/lib/navigation/parentPath';
import { cn } from '@/lib/utils/cn';

export interface BackButtonProps {
  /**
   * Explicit parent route. When omitted, resolves from the current URL hierarchy.
   */
  href?: string;

  /**
   * Custom click handler. When provided, href resolution is skipped.
   */
  onClick?: () => void;

  className?: string;
  label?: string;
  variant?: 'subtle' | 'default';
}

function canUseBrowserBack(): boolean {
  return typeof window !== 'undefined' && window.history.length > 1;
}

/**
 * Page-level back control. Prefers browser history when available, otherwise
 * navigates to the parent route in the app hierarchy.
 */
export function BackButton({
  href: hrefProp,
  onClick,
  className,
  label,
  variant = 'subtle',
}: BackButtonProps) {
  const router = useRouter();
  const t = useTranslation();
  const resolvedParentPath = useParentPath();
  const hrefOverride = useBackHrefOverride();
  const parentPath = hrefProp ?? hrefOverride ?? resolvedParentPath;

  if (!onClick && !parentPath) {
    return null;
  }

  const handleClick = () => {
    if (onClick) {
      onClick();
      return;
    }
    if (!hrefProp && canUseBrowserBack()) {
      router.back();
      return;
    }
    if (parentPath) {
      router.push(parentPath);
    }
  };

  const displayLabel = label || t('common.back');

  if (variant === 'default') {
    return (
      <button
        type="button"
        onClick={handleClick}
        className={cn(
          'inline-flex items-center gap-2.5 px-4 py-2.5 rounded-lg',
          'text-base font-medium text-[var(--text-primary)]',
          'bg-[var(--card-bg)] border border-[var(--border-color)]',
          'hover:bg-[var(--hover-bg)]',
          'transition-colors duration-200',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--focus-ring-offset)]',
          className,
        )}
        aria-label={displayLabel}
      >
        <ArrowLeft className="h-5 w-5" aria-hidden="true" />
        <span>{displayLabel}</span>
      </button>
    );
  }

  return (
    <button
      type="button"
      onClick={handleClick}
      className={cn(
        'inline-flex items-center gap-2 py-1.5 pl-0 pr-0',
        'text-sm font-medium text-[var(--text-secondary)] sm:gap-2.5 sm:py-2 sm:text-base',
        'hover:text-[var(--fire-red)]',
        'transition-colors duration-200',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--focus-ring-offset)]',
        className,
      )}
      aria-label={displayLabel}
    >
      <ArrowLeft className="h-4 w-4 sm:h-5 sm:w-5" aria-hidden="true" />
      <span>{displayLabel}</span>
    </button>
  );
}
