/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo, useState } from 'react';
import { AddressInput } from '@/components/ui/address-input';
import { Input } from '@/components/ui/input';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { PhoneInput } from '@/components/ui/phone-input';
import { Select } from '@/components/ui/select';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import {
  EMERGENCY_CONTACT_RELATIONSHIP_OPTIONS,
  getEmergencyContactRelationshipLabelKey,
} from '@/lib/my-area/emergencyContactOptions';
import { isValidEmail } from '@/lib/validation/email';
import { isValidRequiredPhone } from '@/lib/validation/phone';
import type {
  CreateEmergencyContactRequest,
  EmergencyContactDto,
  UpdateEmergencyContactRequest,
} from '@/types/emergencyContact';
import type { EmergencyContactRelationship } from '@/types/enums/emergencyContactRelationship';

interface EmergencyContactFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  contact?: EmergencyContactDto;
  onSave: (data: CreateEmergencyContactRequest | UpdateEmergencyContactRequest) => Promise<void>;
}

type EmergencyContactFormData = {
  relationship: EmergencyContactRelationship | '';
  firstName: string;
  lastName: string;
  street: string;
  city: string;
  postalCode: string;
  country: string;
  phone: string;
  email: string;
};

function isEmergencyContactFormValid(formData: EmergencyContactFormData): boolean {
  return (
    formData.relationship !== '' &&
    formData.firstName.trim() !== '' &&
    formData.lastName.trim() !== '' &&
    formData.street.trim() !== '' &&
    formData.city.trim() !== '' &&
    formData.postalCode.trim() !== '' &&
    formData.country.trim() !== '' &&
    isValidRequiredPhone(formData.phone) &&
    isValidEmail(formData.email)
  );
}

function EmergencyContactFormModalContent({
  onClose,
  contact,
  onSave,
}: Omit<EmergencyContactFormModalProps, 'isOpen'>) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const [isLoading, setIsLoading] = useState(false);

  const [formData, setFormData] = useState<EmergencyContactFormData>({
    relationship: contact?.relationship ?? '',
    firstName: contact?.firstName ?? '',
    lastName: contact?.lastName ?? '',
    street: contact?.street ?? '',
    city: contact?.city ?? '',
    postalCode: contact?.postalCode ?? '',
    country: contact?.country ?? '',
    phone: contact?.phone ?? '',
    email: contact?.email ?? '',
  });

  const relationshipOptions = EMERGENCY_CONTACT_RELATIONSHIP_OPTIONS.map((value) => ({
    value,
    label: t(getEmergencyContactRelationshipLabelKey(value)),
  }));

  const isFormValid = useMemo(() => isEmergencyContactFormValid(formData), [formData]);

  const emailFieldError = useMemo(() => {
    const trimmed = formData.email.trim();
    if (!trimmed || isValidEmail(trimmed)) {
      return undefined;
    }
    return t('common.validation.invalidEmail');
  }, [formData.email, t]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!isFormValid || formData.relationship === '') {
      return;
    }

    setIsLoading(true);
    try {
      const data = {
        relationship: formData.relationship,
        firstName: formData.firstName.trim(),
        lastName: formData.lastName.trim(),
        street: formData.street.trim(),
        city: formData.city.trim(),
        postalCode: formData.postalCode.trim(),
        country: formData.country.trim(),
        phone: formData.phone.trim(),
        email: formData.email.trim(),
      };
      await onSave(data);
      showSuccess(
        contact
          ? t('myArea.emergencyContact.success.updated')
          : t('myArea.emergencyContact.success.created'),
      );
      onClose();
    } catch (error) {
      console.error('Error saving emergency contact:', error);
      showError(error instanceof Error ? error.message : t('myArea.error.loadFailed'));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={
        contact ? t('myArea.emergencyContact.editTitle') : t('myArea.emergencyContact.addTitle')
      }
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => {
            const form = document.getElementById('emergency-contact-form') as HTMLFormElement;
            form?.requestSubmit();
          }}
          isValid={isFormValid}
          isLoading={isLoading}
        />
      }
    >
      <form id="emergency-contact-form" onSubmit={handleSubmit}>
        <div className="space-y-4">
          <Select
            label={t('myArea.emergencyContact.fields.relationship')}
            name="relationship"
            value={formData.relationship}
            onChange={(e) =>
              setFormData({
                ...formData,
                relationship: e.target.value as EmergencyContactRelationship,
              })
            }
            options={relationshipOptions}
            required
          />
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label={t('myArea.emergencyContact.fields.firstName')}
              value={formData.firstName}
              onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
              required
            />
            <Input
              label={t('myArea.emergencyContact.fields.lastName')}
              value={formData.lastName}
              onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
              required
            />
          </div>
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
          />
          <PhoneInput
            label={t('myArea.emergencyContact.fields.phone')}
            value={formData.phone}
            onChange={(value) => setFormData({ ...formData, phone: value })}
            validate
            required
          />
          <Input
            label={t('myArea.emergencyContact.fields.email')}
            type="text"
            inputMode="email"
            autoComplete="email"
            value={formData.email}
            onChange={(e) => setFormData({ ...formData, email: e.target.value })}
            placeholder={t('myArea.fields.notificationEmailPlaceholder')}
            required
            error={emailFieldError}
          />
        </div>
      </form>
    </Modal>
  );
}

export function EmergencyContactFormModal({
  isOpen,
  onClose,
  contact,
  onSave,
}: EmergencyContactFormModalProps) {
  if (!isOpen) {
    return null;
  }

  return (
    <EmergencyContactFormModalContent
      key={contact?.id ?? 'new'}
      onClose={onClose}
      contact={contact}
      onSave={onSave}
    />
  );
}
