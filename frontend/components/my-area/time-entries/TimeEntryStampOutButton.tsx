/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Loader2, LogOut } from 'lucide-react';
import { useTranslation } from '@/hooks/useTranslation';
import { cn } from '@/lib/utils/cn';

interface TimeEntryStampOutButtonProps {
  onClick: () => void;
  disabled?: boolean;
  isLoading?: boolean;
  className?: string;
}

export function TimeEntryStampOutButton({
  onClick,
  disabled = false,
  isLoading = false,
  className,
}: TimeEntryStampOutButtonProps) {
  const t = useTranslation();

  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled || isLoading}
      aria-busy={isLoading}
      className={cn(
        'flex w-full items-center gap-3 rounded-xl border border-[var(--border-color)] bg-[var(--hover-bg)]/30 px-3 py-2.5 text-left transition-colors',
        'hover:border-[var(--fire-red)]/60 hover:bg-[var(--hover-bg)]',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--focus-ring-offset)]',
        'disabled:cursor-not-allowed disabled:opacity-50',
        className,
      )}
    >
      {isLoading ? (
        <Loader2 className="h-5 w-5 shrink-0 animate-spin text-[var(--fire-red)]" aria-hidden />
      ) : (
        <LogOut className="h-5 w-5 shrink-0 text-[var(--fire-red)]" aria-hidden />
      )}
      <span className="min-w-0 flex-1">
        <span className="block text-sm font-semibold text-[var(--text-primary)]">
          {t('myArea.timeEntries.stampOut')}
        </span>
        <span className="mt-0.5 block text-xs leading-snug text-[var(--text-secondary)]">
          {t('myArea.timeEntries.stampOutHint')}
        </span>
      </span>
    </button>
  );
}
