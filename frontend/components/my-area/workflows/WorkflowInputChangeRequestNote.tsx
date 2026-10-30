/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { MessageSquareText } from 'lucide-react';
import { useTranslation } from '@/hooks/useTranslation';
import { cn } from '@/lib/utils/cn';

/**
 * Reviewer feedback for an open input-change request - used on the workflow
 * detail banner and inside edit modals so the note reads as actionable guidance.
 */
export function WorkflowInputChangeRequestNote({
  message,
  className,
  tone = 'warning',
}: {
  message: string;
  className?: string;
  /** `warning` for the awaiting-input banner; `neutral` for modals. */
  tone?: 'warning' | 'neutral';
}) {
  const t = useTranslation();
  const trimmed = message.trim();
  if (!trimmed) return null;

  return (
    <div className={cn(className)}>
      <p
        className={cn(
          'mb-1 flex items-center gap-1.5 text-xs font-medium',
          tone === 'warning'
            ? 'text-[var(--status-warning-accent)]'
            : 'text-[var(--text-secondary)]',
        )}
      >
        <MessageSquareText className="h-3.5 w-3.5 shrink-0" aria-hidden />
        {t('myArea.workflowDetail.awaitingInputChangeNoteLabel')}
      </p>
      <p className="text-sm leading-relaxed text-[var(--text-primary)] whitespace-pre-wrap">
        {trimmed}
      </p>
    </div>
  );
}
