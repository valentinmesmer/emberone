/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type React from 'react';
import { useTranslation } from '@/hooks/useTranslation';
import { cn } from '@/lib/utils/cn';
import { Button } from './button';

export interface ModalFooterProps {
  onCancel: () => void;
  onSave?: () => void;
  isValid?: boolean;
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
  isValid = true,
  isLoading = false,
  cancelText,
  saveText,
  additionalActions,
  showSave = true,
  className,
  saveButtonClassName,
}: ModalFooterProps) {
  const t = useTranslation();

  return (
    <div
      className={cn(
        'flex w-full flex-col-reverse gap-2 sm:w-auto sm:flex-row sm:items-center sm:justify-end',
        className,
      )}
    >
      {additionalActions}
      <Button variant="secondary" onClick={onCancel} disabled={isLoading} fullWidth="responsive">
        {cancelText || t('common.cancel')}
      </Button>
      {showSave && onSave && (
        <Button
          variant="primary"
          onClick={onSave}
          disabled={!isValid || isLoading}
          isLoading={isLoading}
          fullWidth="responsive"
          title={!isValid && !isLoading ? t('common.hoverDisabledSave') : undefined}
          className={saveButtonClassName}
        >
          {saveText || t('common.save')}
        </Button>
      )}
    </div>
  );
}
