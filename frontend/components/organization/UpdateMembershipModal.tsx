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
import { Select } from '@/components/ui/select';
import { useToast } from '@/components/ui/toast';
import { useFormModal } from '@/hooks/useFormModal';
import { useTranslation } from '@/hooks/useTranslation';
import {
  getMembershipTypeOptions,
  validateDateRange,
  validateRequiredFields,
  validateStartNotInFuture,
} from '@/lib/utils';
import {
  formatToLocalDateTime,
  getOrganizationWallClockNow,
  isEndNotAfterStart,
  isStartInTheFuture,
} from '@/lib/utils/date';
import { GroupMembershipType } from '@/types/enums/groupMembershipType';
import type {
  UpdateUserOrganizationGroupMembershipRequest,
  UserOrganizationGroupMembershipDto,
} from '@/types/user';

export interface UpdateMembershipModalProps {
  isOpen: boolean;
  onClose: () => void;
  onUpdate: (data: UpdateUserOrganizationGroupMembershipRequest) => Promise<void>;
  membership: UserOrganizationGroupMembershipDto | null;
  isLoading?: boolean;
}

export function UpdateMembershipModal({
  isOpen,
  onClose,
  onUpdate,
  membership,
  isLoading = false,
}: UpdateMembershipModalProps) {
  const t = useTranslation();
  const { showError } = useToast();

  const getInitialData = useCallback((): UpdateUserOrganizationGroupMembershipRequest => {
    return {
      startDate: membership?.startDate ? formatToLocalDateTime(membership.startDate) : '',
      endDate: membership?.endDate ? formatToLocalDateTime(membership.endDate) : undefined,
      membershipType: membership?.membershipType ?? GroupMembershipType.MEMBER,
    };
  }, [membership]);

  const { formData, setFormData, reset } = useFormModal(getInitialData, isOpen, true, [
    membership?.id,
    membership?.startDate,
    membership?.endDate,
    membership?.membershipType,
  ]);

  const handleUpdate = async () => {
    if (!membership) return;

    if (
      !validateRequiredFields(
        {
          membershipType: formData.membershipType || '',
          startDate: formData.startDate || '',
        },
        t,
        showError,
        'common.validation.fillRequired',
      )
    ) {
      return;
    }

    if (!validateDateRange(formData.startDate, formData.endDate, t, showError)) {
      return;
    }
    if (!validateStartNotInFuture(formData.startDate, t, showError)) {
      return;
    }

    try {
      await onUpdate({
        startDate: formData.startDate,
        endDate: formData.endDate || undefined,
        membershipType: formData.membershipType,
      });
      onClose();
    } catch (error) {
      console.error('Error updating membership:', error);
    }
  };

  const handleClose = () => {
    reset();
    onClose();
  };

  const membershipEndDateError =
    formData.startDate &&
    formData.endDate &&
    isEndNotAfterStart(formData.startDate, formData.endDate)
      ? t('common.validation.endAfterStart')
      : undefined;

  const startError = isStartInTheFuture(formData.startDate)
    ? t('common.validation.startNotInFuture')
    : undefined;

  const isValid = () =>
    formData.membershipType !== undefined &&
    formData.startDate.trim() !== '' &&
    !isStartInTheFuture(formData.startDate) &&
    (!formData.endDate || !isEndNotAfterStart(formData.startDate, formData.endDate));

  const typeChanged = formData.membershipType !== membership?.membershipType;
  const membershipTypeOptions = getMembershipTypeOptions(t);

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      title={t('userManagement.groupMembership.updateMembership')}
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
        <Select
          label={t('userManagement.groupMembership.membershipType')}
          value={formData.membershipType}
          onChange={(e) =>
            setFormData((prev) => ({
              ...prev,
              membershipType: e.target.value as GroupMembershipType,
            }))
          }
          options={membershipTypeOptions}
          required
          disabled={isLoading}
        />

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 [&>*]:min-w-0">
          <DateTimePicker
            label={t('userManagement.groupMembership.startDate')}
            value={formData.startDate}
            onChange={(e) => setFormData((prev) => ({ ...prev, startDate: e.target.value }))}
            required
            hideTime
            allDayTime="start"
            max={getOrganizationWallClockNow()}
            error={startError}
            disabled={isLoading || typeChanged}
          />

          <DateTimePicker
            label={t('userManagement.groupMembership.endDate')}
            value={formData.endDate || ''}
            onChange={(e) =>
              setFormData((prev) => ({
                ...prev,
                endDate: e.target.value || undefined,
              }))
            }
            error={membershipEndDateError}
            hideTime
            allDayTime="end"
            disabled={isLoading}
            allowEmpty
          />
        </div>

        {typeChanged ? (
          <p className="text-xs text-[var(--text-secondary)]">
            {t('userManagement.groupMembership.typeChangeHint')}
          </p>
        ) : null}
      </div>
    </Modal>
  );
}
