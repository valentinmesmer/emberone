/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useCallback } from 'react';
import { DateTimePicker } from '@/components/ui/date-time-picker';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useFormModal } from '@/hooks/useFormModal';
import { useTranslation } from '@/hooks/useTranslation';
import { formBlockers } from '@/lib/utils';
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

    if (blockers.length > 0) return;

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

  const blockers = formBlockers([
    [
      t('userManagement.organizationMembership.startDate'),
      formData.startDate.trim() !== '' && !isStartInTheFuture(formData.startDate),
    ],
  ]);

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
          blockers={blockers}
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
