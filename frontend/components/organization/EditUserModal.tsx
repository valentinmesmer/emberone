/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useState } from 'react';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useToast } from '@/components/ui/toast';
import { UserBasicInfoForm } from '@/components/user/UserBasicInfoForm';
import { useTranslation } from '@/hooks/useTranslation';
import { isValidDateOfBirth } from '@/lib/validation/dateOfBirth';
import { Salutation } from '@/types/enums/salutation';
import type { UpdatePersonalDataRequest, UserDto } from '@/types/user';

export interface EditUserModalProps {
  isOpen: boolean;
  onClose: () => void;
  onUpdate: (data: UpdatePersonalDataRequest) => Promise<void>;
  user: UserDto | null;
  isLoading?: boolean;
}

function EditUserModalForm({
  onClose,
  onUpdate,
  user,
  isLoading,
}: Omit<EditUserModalProps, 'isOpen'> & { user: UserDto }) {
  const t = useTranslation();
  const { showError } = useToast();
  const [formData, setFormData] = useState<UpdatePersonalDataRequest>({
    firstName: user.firstName || '',
    lastName: user.lastName || '',
    dateOfBirth: user.dateOfBirth || '',
    salutation: user.salutation || Salutation.MR,
  });

  const handleUpdate = async () => {
    // Validate form
    if (!formData.firstName.trim() || !formData.lastName.trim() || !formData.dateOfBirth) {
      showError(t('common.validation.fillRequired'));
      return;
    }

    if (!isValidDateOfBirth(formData.dateOfBirth)) {
      showError(t('common.validation.invalidDateOfBirth'));
      return;
    }

    try {
      await onUpdate(formData);
      onClose();
    } catch (error) {
      console.error('Error updating user:', error);
      // Error is already handled by parent component
    }
  };

  const handleClose = () => {
    onClose();
  };

  const isValid = () => {
    return (
      formData.firstName.trim() !== '' &&
      formData.lastName.trim() !== '' &&
      isValidDateOfBirth(formData.dateOfBirth)
    );
  };

  return (
    <Modal
      isOpen
      onClose={handleClose}
      title={t('userManagement.editUser')}
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
        <UserBasicInfoForm
          firstName={formData.firstName}
          lastName={formData.lastName}
          salutation={formData.salutation}
          dateOfBirth={formData.dateOfBirth}
          onFirstNameChange={(value) => setFormData((prev) => ({ ...prev, firstName: value }))}
          onLastNameChange={(value) => setFormData((prev) => ({ ...prev, lastName: value }))}
          onSalutationChange={(value) => setFormData((prev) => ({ ...prev, salutation: value }))}
          onDateOfBirthChange={(value) => setFormData((prev) => ({ ...prev, dateOfBirth: value }))}
          disabled={isLoading}
        />
      </div>
    </Modal>
  );
}

export function EditUserModal({
  isOpen,
  onClose,
  onUpdate,
  user,
  isLoading = false,
}: EditUserModalProps) {
  if (!isOpen || !user) {
    return null;
  }

  return (
    <EditUserModalForm
      key={user.id}
      onClose={onClose}
      onUpdate={onUpdate}
      user={user}
      isLoading={isLoading}
    />
  );
}
