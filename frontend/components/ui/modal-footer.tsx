/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Info } from 'lucide-react';
import type React from 'react';
import { useTranslation } from '@/hooks/useTranslation';
import { cn } from '@/lib/utils/cn';
import { Button } from './button';

export interface ModalFooterProps {
  onCancel: () => void;
  onSave?: () => void;
  /** Labels of missing or invalid fields; a non-empty list disables save and is named on hover. */
  blockers?: string[];
  isLoading?: boolean;
  cancelText?: string;
  saveText?: string;
  additionalActions?: React.ReactNode;
  showSave?: boolean;
  className?: string;
  saveButtonClassName?: string;
}

export function ModalFooter({
  onCancel,
  onSave,
  blockers = [],
  isLoading = false,
  cancelText,
  saveText,
  additionalActions,
  showSave = true,
  className,
  saveButtonClassName,
}: ModalFooterProps) {
  const t = useTranslation();
  const blocked = blockers.length > 0;

  return (
    <div className={cn('flex flex-1 flex-col gap-3', className)}>
      {blocked && !isLoading && (
        <p className="flex items-start gap-2 text-sm text-[var(--text-primary)]">
          <Info
            className="mt-0.5 h-4 w-4 shrink-0 text-[var(--status-warning-accent)]"
            aria-hidden
          />
          {t('common.saveBlockedHint', { fields: blockers.join(', ') })}
        </p>
      )}
      <div className="flex w-full flex-col-reverse gap-2 sm:w-auto sm:flex-row sm:items-center sm:justify-end sm:self-end">
        {additionalActions}
        <Button variant="secondary" onClick={onCancel} disabled={isLoading} fullWidth="responsive">
          {cancelText || t('common.cancel')}
        </Button>
        {showSave && onSave && (
          <Button
            variant="primary"
            onClick={onSave}
            disabled={blocked || isLoading}
            isLoading={isLoading}
            fullWidth="responsive"
            className={saveButtonClassName}
          >
            {saveText || t('common.save')}
          </Button>
        )}
      </div>
    </div>
  );
}
