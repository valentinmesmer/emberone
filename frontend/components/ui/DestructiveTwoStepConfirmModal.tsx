/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useEffect, useState } from 'react';
import { Input } from '@/components/ui/input';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useTranslation } from '@/hooks/useTranslation';
import { statusDangerSolidCtaClass } from '@/lib/utils/statusActionClasses';

export interface DestructiveTwoStepConfirmModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  message: string;
  detail?: string;
  confirmationLabel: string;
  expectedConfirmation: string;
  confirmMessage: string;
  confirmDetail?: string;
  confirmButtonText: string;
  isLoading?: boolean;
  onConfirm: () => void | Promise<void>;
}

export function DestructiveTwoStepConfirmModal({
  isOpen,
  onClose,
  title,
  message,
  detail,
  confirmationLabel,
  expectedConfirmation,
  confirmMessage,
  confirmDetail,
  confirmButtonText,
  isLoading = false,
  onConfirm,
}: DestructiveTwoStepConfirmModalProps) {
  const t = useTranslation();
  const [step, setStep] = useState<1 | 2>(1);
  const [confirmation, setConfirmation] = useState('');

  const confirmationMatches =
    confirmation.trim().length > 0 && confirmation.trim() === expectedConfirmation;

  useEffect(() => {
    if (!isOpen) {
      setStep(1);
      setConfirmation('');
    }
  }, [isOpen]);

  const handleClose = () => {
    if (isLoading) return;
    setStep(1);
    setConfirmation('');
    onClose();
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      title={title}
      size="sm"
      footer={
        step === 1 ? (
          <ModalFooter
            onCancel={handleClose}
            onSave={() => {
              if (confirmationMatches) setStep(2);
            }}
            isLoading={isLoading}
            isValid={confirmationMatches}
            cancelText={t('common.cancel')}
            saveText={t('common.continue')}
          />
        ) : (
          <ModalFooter
            onCancel={() => setStep(1)}
            onSave={() => void onConfirm()}
            isLoading={isLoading}
            isValid={!isLoading}
            cancelText={t('common.back')}
            saveText={confirmButtonText}
            saveButtonClassName={statusDangerSolidCtaClass}
          />
        )
      }
    >
      <div className="space-y-3 py-2">
        {step === 1 ? (
          <>
            <p className="text-[var(--text-primary)]">{message}</p>
            {detail ? <p className="text-sm text-[var(--text-secondary)]">{detail}</p> : null}
            <Input
              label={confirmationLabel}
              value={confirmation}
              onChange={(event) => setConfirmation(event.target.value)}
              autoComplete="off"
              disabled={isLoading}
            />
          </>
        ) : (
          <>
            <p className="text-[var(--text-primary)]">{confirmMessage}</p>
            {confirmDetail ? (
              <p className="text-sm text-[var(--text-secondary)]">{confirmDetail}</p>
            ) : null}
          </>
        )}
      </div>
    </Modal>
  );
}
