/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useState } from 'react';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Textarea } from '@/components/ui/textarea';
import { useTranslation } from '@/hooks/useTranslation';
import { statusDangerSolidCtaClass } from '@/lib/utils/statusActionClasses';
import type {
  EndUserOrganizationMembershipRequest,
  UserOrganizationMembershipDto,
} from '@/types/user';

export interface EndOrganizationMembershipDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onEnd: (data: EndUserOrganizationMembershipRequest) => Promise<void>;
  membership: UserOrganizationMembershipDto | null;
  organizationName: string;
  isLoading?: boolean;
}

export function EndOrganizationMembershipDialog({
  isOpen,
  onClose,
  onEnd,
  membership,
  organizationName,
  isLoading = false,
}: EndOrganizationMembershipDialogProps) {
  const t = useTranslation();
  const [reason, setReason] = useState('');
  const [prevIsOpen, setPrevIsOpen] = useState(isOpen);

  if (isOpen !== prevIsOpen) {
    setPrevIsOpen(isOpen);
    if (isOpen) {
      setReason('');
    }
  }

  const trimmedReason = reason.trim();
  const canSubmit = trimmedReason.length > 0 && !isLoading;

  const handleEnd = async () => {
    if (!membership || !canSubmit) return;
    try {
      await onEnd({ reason: trimmedReason });
      onClose();
    } catch {
      // Caller shows toast
    }
  };

  const isMainMembership = membership?.main === true;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={t('userManagement.organizationMembership.confirmEndTitle')}
      size="sm"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={handleEnd}
          isValid={canSubmit}
          isLoading={isLoading}
          cancelText={t('common.cancel')}
          saveText={t('userManagement.organizationMembershipEndWorkflow.submit')}
          saveButtonClassName={statusDangerSolidCtaClass}
        />
      }
    >
      <div className="space-y-4 py-2">
        <p className="text-[var(--text-primary)]">
          {t('userManagement.organizationMembership.confirmEndMessage', { organizationName })}
        </p>
        {isMainMembership ? (
          <p className="rounded-md border border-amber-600/40 bg-amber-50 px-3 py-2 text-sm text-amber-900 dark:bg-amber-950/30 dark:text-amber-200">
            {t('userManagement.organizationMembership.mainOrganizationWarning')}
          </p>
        ) : (
          <p className="text-sm text-[var(--text-secondary)]">
            {t('userManagement.organizationMembership.confirmEndHint')}
          </p>
        )}
        <Textarea
          id="org-membership-end-reason"
          label={t('userManagement.organizationMembershipEndWorkflow.reasonLabel')}
          value={reason}
          onChange={(event) => setReason(event.target.value)}
          rows={4}
          maxLength={2000}
          required
        />
      </div>
    </Modal>
  );
}
