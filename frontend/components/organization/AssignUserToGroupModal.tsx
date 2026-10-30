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
  getGroupDisplayName,
  getMembershipTypeOptions,
  validateDateRange,
  validateStartNotInFuture,
} from '@/lib/utils';
import {
  getOrganizationWallClockNow,
  isEndNotAfterStart,
  isStartInTheFuture,
} from '@/lib/utils/date';
import { GroupMembershipType } from '@/types/enums/groupMembershipType';
import type { OrganizationGroupDto } from '@/types/organizationGroup';
import type { CreateUserOrganizationGroupMembershipRequest } from '@/types/user';

export interface AssignUserToGroupModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAssign: (data: CreateUserOrganizationGroupMembershipRequest) => Promise<void>;
  userId: number;
  organizationGroups: OrganizationGroupDto[];
  isLoading?: boolean;
}

export function AssignUserToGroupModal({
  isOpen,
  onClose,
  onAssign,
  userId,
  organizationGroups,
  isLoading = false,
}: AssignUserToGroupModalProps) {
  const t = useTranslation();
  const { showError } = useToast();

  const getInitialData = useCallback((): CreateUserOrganizationGroupMembershipRequest => {
    return {
      userId,
      organizationGroupId: 0,
      startDate: getOrganizationWallClockNow(),
      endDate: undefined,
      membershipType: GroupMembershipType.MEMBER,
    };
  }, [userId]);

  const { formData, setFormData, reset } = useFormModal(getInitialData, isOpen, true, [userId]);

  const handleAssign = async () => {
    // Validate required fields
    if (formData.organizationGroupId === 0 || !formData.startDate || !formData.membershipType) {
      showError(t('common.validation.fillRequired'));
      return;
    }

    // Validate end date is after start date if provided
    if (!validateDateRange(formData.startDate, formData.endDate, t, showError)) {
      return;
    }
    if (!validateStartNotInFuture(formData.startDate, t, showError)) {
      return;
    }

    try {
      await onAssign(formData);
      onClose();
    } catch (error) {
      console.error('Error assigning user to group:', error);
      // Error is already handled by parent component
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
    formData.organizationGroupId > 0 &&
    formData.startDate.trim() !== '' &&
    !isStartInTheFuture(formData.startDate) &&
    formData.membershipType !== undefined &&
    (!formData.endDate || !isEndNotAfterStart(formData.startDate, formData.endDate));

  const membershipTypeOptions = getMembershipTypeOptions(t);

  const groupOptions = organizationGroups.map((group) => ({
    value: group.id.toString(),
    label: getGroupDisplayName(group),
  }));

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      title={t('userManagement.groupMembership.assignToGroup')}
      size="lg"
      footer={
        <ModalFooter
          onCancel={handleClose}
          onSave={handleAssign}
          isValid={isValid()}
          isLoading={isLoading}
        />
      }
    >
      <div className="space-y-6">
        <Select
          label={t('userManagement.groupMembership.organizationGroup')}
          value={formData.organizationGroupId.toString()}
          onChange={(e) =>
            setFormData((prev) => ({
              ...prev,
              organizationGroupId: parseInt(e.target.value, 10),
            }))
          }
          options={[
            { value: '0', label: t('userManagement.groupMembership.selectGroup') },
            ...groupOptions,
          ]}
          required
          disabled={isLoading}
        />

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
            disabled={isLoading}
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
      </div>
    </Modal>
  );
}
