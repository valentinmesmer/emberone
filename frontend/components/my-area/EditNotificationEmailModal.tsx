/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo, useState } from 'react';
import { Input } from '@/components/ui/input';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { isValidEmail } from '@/lib/validation/email';
import type { UserContactInformationDto } from '@/types/user';

interface EditNotificationEmailModalProps {
  isOpen: boolean;
  onClose: () => void;
  contactInformation: UserContactInformationDto;
  onSave: (notificationEmail: string) => Promise<void>;
}

function EditNotificationEmailModalForm({
  onClose,
  contactInformation,
  onSave,
}: Omit<EditNotificationEmailModalProps, 'isOpen'>) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const [isLoading, setIsLoading] = useState(false);
  const [email, setEmail] = useState(contactInformation.privateEmail || '');

  const emailError = useMemo(() => {
    if (!email.trim()) {
      return t('common.validation.required');
    }
    if (!isValidEmail(email)) {
      return t('common.validation.invalidEmail');
    }
    return undefined;
  }, [email, t]);

  const formValid = emailError === undefined;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formValid) {
      return;
    }
    setIsLoading(true);
    try {
      await onSave(email.trim());
      showSuccess(t('myArea.success.notificationEmailChangeRequested'));
      onClose();
    } catch (error) {
      console.error('Error saving notification email:', error);
      showError(error instanceof Error ? error.message : t('myArea.error.loadFailed'));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={t('myArea.edit.notificationEmail')}
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => {
            const form = document.getElementById('notification-email-form') as HTMLFormElement;
            form?.requestSubmit();
          }}
          isValid={formValid}
          isLoading={isLoading}
        />
      }
    >
      <form id="notification-email-form" onSubmit={handleSubmit}>
        <div className="space-y-4">
          <p className="text-sm text-[var(--text-secondary)]">
            {t('myArea.notificationEmail.changeHint')}
          </p>
          <Input
            label={t('myArea.fields.notificationEmail')}
            type="text"
            inputMode="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder={t('myArea.fields.notificationEmailPlaceholder')}
            error={emailError}
          />
        </div>
      </form>
    </Modal>
  );
}

export function EditNotificationEmailModal({
  isOpen,
  onClose,
  contactInformation,
  onSave,
}: EditNotificationEmailModalProps) {
  if (!isOpen) {
    return null;
  }

  return (
    <EditNotificationEmailModalForm
      key={`${contactInformation.privateEmail}-${contactInformation.pendingEmail ?? ''}`}
      onClose={onClose}
      contactInformation={contactInformation}
      onSave={onSave}
    />
  );
}
