/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useTranslation } from '@/hooks/useTranslation';
import { statusDangerSolidCtaClass } from '@/lib/utils/statusActionClasses';

export interface ConfirmDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title?: string;
  message: string;
  /** Optional secondary line (muted), e.g. side effects of the action. */
  detail?: string;
  confirmText?: string;
  cancelText?: string;
  variant?: 'danger' | 'warning' | 'info';
  isLoading?: boolean;
}

export function ConfirmDialog({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  detail,
  confirmText,
  cancelText,
  variant = 'danger',
  isLoading = false,
}: ConfirmDialogProps) {
  const t = useTranslation();

  const handleConfirm = () => {
    onConfirm();
  };

  /** `danger` tints the confirm button red-danger; `warning` and `info` use the standard primary CTA (brand red). */
  const saveButtonClassName = variant === 'danger' ? statusDangerSolidCtaClass : undefined;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={title || t('common.confirm')}
      size="sm"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={handleConfirm}
          isLoading={isLoading}
          cancelText={cancelText || t('common.cancel')}
          saveText={confirmText || t('common.confirm')}
          saveButtonClassName={saveButtonClassName}
        />
      }
    >
      <div className="space-y-2 py-4">
        <p className="text-[var(--text-primary)]">{message}</p>
        {detail ? <p className="text-sm text-[var(--text-secondary)]">{detail}</p> : null}
      </div>
    </Modal>
  );
}
