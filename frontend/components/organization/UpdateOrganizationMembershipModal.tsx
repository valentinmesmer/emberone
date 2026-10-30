/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useCallback } from 'react';
import { DateTimePicker } from '@/components/ui/date-time-picker';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useToast } from '@/components/ui/toast';
import { useFormModal } from '@/hooks/useFormModal';
import { useTranslation } from '@/hooks/useTranslation';
import { validateRequiredFields, validateStartNotInFuture } from '@/lib/utils';
import {
  formatToLocalDateTime,
  getOrganizationWallClockNow,
  isStartInTheFuture,
} from '@/lib/utils/date';
import type {
  UpdateUserOrganizationMembershipRequest,
  UserOrganizationMembershipDto,
} from '@/types/user';

export interface UpdateOrganizationMembershipModalProps {
  isOpen: boolean;
  onClose: () => void;
  onUpdate: (data: UpdateUserOrganizationMembershipRequest) => Promise<void>;
  membership: UserOrganizationMembershipDto | null;
  isLoading?: boolean;
}

export function UpdateOrganizationMembershipModal({
  isOpen,
  onClose,
  onUpdate,
  membership,
  isLoading = false,
}: UpdateOrganizationMembershipModalProps) {
  const t = useTranslation();
  const { showError } = useToast();

  const getInitialData = useCallback((): UpdateUserOrganizationMembershipRequest => {
    return {
      startDate: membership?.startDate ? formatToLocalDateTime(membership.startDate) : '',
    };
  }, [membership]);

  const { formData, setFormData, reset } = useFormModal(getInitialData, isOpen, true, [
    membership?.id,
    membership?.startDate,
  ]);

  const handleUpdate = async () => {
    if (!membership) return;

    if (
      !validateRequiredFields(
        { startDate: formData.startDate || '' },
        t,
        showError,
        'common.validation.fillRequired',
      )
    ) {
      return;
    }
    if (!validateStartNotInFuture(formData.startDate, t, showError)) {
      return;
    }

    try {
      await onUpdate({
        startDate: formData.startDate,
      });
      onClose();
    } catch (error) {
      console.error('Error updating organization membership:', error);
    }
  };

  const handleClose = () => {
    reset();
    onClose();
  };

  const startError = isStartInTheFuture(formData.startDate)
    ? t('common.validation.startNotInFuture')
    : undefined;

  const isValid = () => formData.startDate.trim() !== '' && !isStartInTheFuture(formData.startDate);

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      title={t('userManagement.organizationMembership.updateMembership')}
      size="lg"
      footer={
        <ModalFooter
          onCancel={handleClose}
          onSave={handleUpdate}
          isValid={isValid()}
          isLoading={isLoading}
        />
      }
    >
      <div className="space-y-6">
        <DateTimePicker
          label={t('userManagement.organizationMembership.startDate')}
          value={formData.startDate}
          onChange={(e) => setFormData((prev) => ({ ...prev, startDate: e.target.value }))}
          required
          hideTime
          allDayTime="start"
          max={getOrganizationWallClockNow()}
          error={startError}
          disabled={isLoading}
        />
      </div>
    </Modal>
  );
}
