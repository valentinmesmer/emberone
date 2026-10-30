/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useCallback } from 'react';
import { OrganizationMemberSelectField } from '@/components/organization/OrganizationMemberSelectField';
import { DateTimePicker } from '@/components/ui/date-time-picker';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Select } from '@/components/ui/select';
import { useToast } from '@/components/ui/toast';
import { useFormModal } from '@/hooks/useFormModal';
import { useTranslation } from '@/hooks/useTranslation';
import {
  getMembershipTypeOptions,
  isActiveMemberOfOrganizationGroup,
  validateDateRange,
  validateStartNotInFuture,
} from '@/lib/utils';
import {
  getOrganizationWallClockNow,
  isEndNotAfterStart,
  isStartInTheFuture,
  shiftEndWithStartDateChange,
} from '@/lib/utils/date';
import { GroupMembershipType } from '@/types/enums/groupMembershipType';
import type { CreateUserOrganizationGroupMembershipRequest, UserDto } from '@/types/user';

export interface AddMemberToGroupModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAssign: (data: CreateUserOrganizationGroupMembershipRequest) => Promise<void>;
  organizationGroupId: number;
  users: UserDto[];
  isLoading?: boolean;
}

export function AddMemberToGroupModal({
  isOpen,
  onClose,
  onAssign,
  organizationGroupId,
  users,
  isLoading = false,
}: AddMemberToGroupModalProps) {
  const t = useTranslation();
  const { showError } = useToast();

  const getInitialData = useCallback((): CreateUserOrganizationGroupMembershipRequest & {
    userId: number;
  } => {
    return {
      userId: 0,
      organizationGroupId,
      startDate: getOrganizationWallClockNow(),
      endDate: undefined,
      membershipType: GroupMembershipType.MEMBER,
    };
  }, [organizationGroupId]);

  const { formData, setFormData, reset } = useFormModal(getInitialData, isOpen, true, [
    organizationGroupId,
  ]);

  const handleAssign = async () => {
    if (formData.userId === 0 || !formData.startDate || !formData.membershipType) {
      showError(t('common.validation.fillRequired'));
      return;
    }
    if (!validateDateRange(formData.startDate, formData.endDate, t, showError)) {
      return;
    }
    if (!validateStartNotInFuture(formData.startDate, t, showError)) {
      return;
    }
    try {
      await onAssign({
        userId: formData.userId,
        organizationGroupId: formData.organizationGroupId,
        startDate: formData.startDate,
        endDate: formData.endDate,
        membershipType: formData.membershipType,
      });
      onClose();
    } catch (error) {
      console.error('Error adding member to group:', error);
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

  const isValid = () => {
    return (
      formData.userId > 0 &&
      formData.startDate.trim() !== '' &&
      !isStartInTheFuture(formData.startDate) &&
      formData.membershipType !== undefined &&
      (!formData.endDate || !isEndNotAfterStart(formData.startDate, formData.endDate))
    );
  };

  const membershipTypeOptions = getMembershipTypeOptions(t);

  const filterAssignableUser = (user: UserDto) =>
    !isActiveMemberOfOrganizationGroup(user.organizationGroupMemberships, organizationGroupId);

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      title={t('userManagement.addUser')}
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
        <OrganizationMemberSelectField
          label={t('userManagement.userLabel')}
          users={users}
          value={formData.userId === 0 ? undefined : formData.userId}
          onChange={(userId) =>
            setFormData((prev) => ({
              ...prev,
              userId: userId ?? 0,
            }))
          }
          filterUser={filterAssignableUser}
          allowEmpty
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
            onChange={(e) => {
              const nextStart = e.target.value;
              setFormData((prev) => ({
                ...prev,
                startDate: nextStart,
                endDate:
                  prev.endDate && nextStart
                    ? shiftEndWithStartDateChange(prev.startDate, nextStart, prev.endDate)
                    : prev.endDate,
              }));
            }}
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
