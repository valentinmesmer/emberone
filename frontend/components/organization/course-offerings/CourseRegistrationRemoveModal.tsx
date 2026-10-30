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
import { useRemoveCourseRegistration } from '@/lib/store';
import type { CourseRegistrationDto } from '@/types/courseOffering';

interface Props {
  organizationId: number;
  offeringId: number;
  registration: CourseRegistrationDto;
  onClose: () => void;
}

/** Asks for the mandatory reason and removes the registration from the offering. */
export function CourseRegistrationRemoveModal({
  organizationId,
  offeringId,
  registration,
  onClose,
}: Props) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const remove = useRemoveCourseRegistration(organizationId, offeringId);
  const [reason, setReason] = useState('');

  const save = async () => {
    try {
      await remove.mutateAsync({ registrationId: registration.id, reason: reason.trim() });
      showSuccess(t('courseOfferings.registrations.removeSuccess'));
      onClose();
    } catch {
      showError(t('courseOfferings.registrations.removeFailed'));
    }
  };

  return (
    <Modal isOpen onClose={onClose} title={t('courseOfferings.registrations.removeTitle')}>
      <p className="mb-4 text-sm text-[var(--text-secondary)]">{registration.userDisplayName}</p>
      <Textarea
        label={t('courseOfferings.registrations.removeReasonLabel')}
        value={reason}
        onChange={(event) => setReason(event.target.value)}
        rows={3}
        required
      />
      <ModalFooter
        onCancel={onClose}
        saveText={t('courseOfferings.registrations.removeConfirm')}
        isValid={reason.trim().length > 0}
        isLoading={remove.isPending}
        onSave={() => void save()}
      />
    </Modal>
  );
}
