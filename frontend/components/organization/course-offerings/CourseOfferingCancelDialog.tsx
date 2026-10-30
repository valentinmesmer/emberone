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
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useCancelCourseOffering } from '@/lib/store';
import { validateRequiredFields } from '@/lib/utils';

interface CourseOfferingCancelDialogProps {
  organizationId: number;
  offeringId: number;
  isOpen: boolean;
  onClose: () => void;
}

export function CourseOfferingCancelDialog({
  organizationId,
  offeringId,
  isOpen,
  onClose,
}: CourseOfferingCancelDialogProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const cancel = useCancelCourseOffering(organizationId, offeringId);
  const [reason, setReason] = useState('');

  if (!isOpen) {
    return null;
  }

  const valid = reason.trim().length > 0;

  return (
    <Modal isOpen onClose={onClose} title={t('courseOfferings.cancel.dialogTitle')}>
      <p className="mb-4 text-sm text-[var(--text-secondary)]">
        {t('courseOfferings.cancel.dialogDescription')}
      </p>
      <Textarea
        label={t('courseOfferings.cancel.reasonLabel')}
        value={reason}
        onChange={(event) => setReason(event.target.value)}
        rows={4}
        required
      />
      <ModalFooter
        onCancel={onClose}
        saveText={t('courseOfferings.cancel.confirm')}
        isValid={valid}
        isLoading={cancel.isPending}
        onSave={() => {
          void (async () => {
            if (
              !validateRequiredFields({ reason }, t, showError, 'common.validation.fillRequired')
            ) {
              return;
            }
            try {
              await cancel.mutateAsync(reason.trim());
              showSuccess(t('courseOfferings.cancel.success'));
              setReason('');
              onClose();
            } catch {
              showError(t('courseOfferings.cancel.failed'));
            }
          })();
        }}
      />
    </Modal>
  );
}
