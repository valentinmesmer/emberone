/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useState } from 'react';
import { AddressInput } from '@/components/ui/address-input';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { UserBasicInfoForm } from '@/components/user/UserBasicInfoForm';
import { useTranslation } from '@/hooks/useTranslation';
import { formBlockers } from '@/lib/utils';
import { isValidDateOfBirth } from '@/lib/validation/dateOfBirth';
import { isValidEmail } from '@/lib/validation/email';
import { Salutation } from '@/types/enums/salutation';
import type { CreateUserRequest } from '@/types/user';

export interface CreateUserModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreate: (data: CreateUserRequest) => Promise<void>;
  isLoading?: boolean;
}

const initialCreateUserForm: CreateUserRequest = {
  firstName: '',
  lastName: '',
  privateEmail: '',
  dateOfBirth: '',
  salutation: Salutation.MR,
};

function CreateUserModalForm({
  onClose,
  onCreate,
  isLoading,
}: Omit<CreateUserModalProps, 'isOpen'>) {
  const t = useTranslation();
  const [formData, setFormData] = useState<CreateUserRequest>(initialCreateUserForm);

  const handleCreate = async () => {
    if (blockers.length > 0) return;

    try {
      await onCreate(formData);
      onClose();
    } catch (error) {
      console.error('Error creating user:', error);
      // Error is already handled by parent component
    }
  };

  const handleClose = () => {
    onClose();
  };

  const blockers = formBlockers([
    [t('setup.user.salutation'), Boolean(formData.salutation)],
    [t('setup.user.firstName'), formData.firstName.trim() !== ''],
    [t('setup.user.lastName'), formData.lastName.trim() !== ''],
    [t('setup.user.email'), isValidEmail(formData.privateEmail.trim())],
    [t('setup.user.dateOfBirth'), isValidDateOfBirth(formData.dateOfBirth)],
    [
      t('setup.user.street'),
      Boolean(
        formData.street?.trim() &&
          formData.city?.trim() &&
          formData.postalCode?.trim() &&
          formData.country?.trim(),
      ),
    ],
  ]);

  const emailFieldError =
    formData.privateEmail.trim() && !isValidEmail(formData.privateEmail.trim())
      ? t('common.validation.invalidEmail')
      : undefined;

  return (
    <Modal
      isOpen
      onClose={handleClose}
      title={t('userManagement.addUser')}
      size="lg"
      footer={
        <ModalFooter
          onCancel={handleClose}
          onSave={handleCreate}
          blockers={blockers}
          isLoading={isLoading}
        />
      }
    >
      <div className="space-y-6">
        <p className="text-sm text-[var(--text-secondary)]">
          {t('userManagement.usernameFormatHint')}
        </p>
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
          showEmail={true}
          email={formData.privateEmail}
          onEmailChange={(value) => setFormData((prev) => ({ ...prev, privateEmail: value }))}
          emailError={emailFieldError}
        />

        <AddressInput
          label={t('setup.user.street')}
          street={formData.street}
          city={formData.city}
          postalCode={formData.postalCode}
          country={formData.country}
          onAddressChange={(address) => {
            setFormData((prev) => ({
              ...prev,
              street: address.street || '',
              city: address.city || '',
              postalCode: address.postalCode || '',
              country: address.country || '',
            }));
          }}
          placeholder={t('setup.user.streetPlaceholder')}
          required
          disabled={isLoading}
        />
      </div>
    </Modal>
  );
}

export function CreateUserModal({
  isOpen,
  onClose,
  onCreate,
  isLoading = false,
}: CreateUserModalProps) {
  if (!isOpen) {
    return null;
  }

  return (
    <CreateUserModalForm
      key="create-user"
      onClose={onClose}
      onCreate={onCreate}
      isLoading={isLoading}
    />
  );
}
