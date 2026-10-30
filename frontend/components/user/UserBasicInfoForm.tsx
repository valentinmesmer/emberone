/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { DatePicker } from '@/components/ui/date-picker';
import { Input } from '@/components/ui/input';
import { Select } from '@/components/ui/select';
import { useTranslation } from '@/hooks/useTranslation';
import { getDateOfBirthBounds, isValidDateOfBirth } from '@/lib/validation/dateOfBirth';
import { Salutation } from '@/types/enums/salutation';

interface UserBasicInfoFormProps {
  firstName: string;
  lastName: string;
  salutation: Salutation;
  dateOfBirth: string;
  onFirstNameChange: (value: string) => void;
  onLastNameChange: (value: string) => void;
  onSalutationChange: (value: Salutation) => void;
  onDateOfBirthChange: (value: string) => void;
  disabled?: boolean;
  showEmail?: boolean;
  email?: string;
  onEmailChange?: (value: string) => void;
  /** Shown under the email field when the value is non-empty but invalid */
  emailError?: string;
}

/**
 * Reusable form component for user basic information fields
 * Used in CreateUserModal and EditUserModal to avoid code duplication
 */
export function UserBasicInfoForm({
  firstName,
  lastName,
  salutation,
  dateOfBirth,
  onFirstNameChange,
  onLastNameChange,
  onSalutationChange,
  onDateOfBirthChange,
  disabled = false,
  showEmail = false,
  email = '',
  onEmailChange,
  emailError,
}: UserBasicInfoFormProps) {
  const t = useTranslation();
  const { min: minValue, max: maxValue } = getDateOfBirthBounds();
  const dateOfBirthError =
    dateOfBirth.trim() && !isValidDateOfBirth(dateOfBirth)
      ? t('common.validation.invalidDateOfBirth')
      : undefined;

  return (
    <>
      <Select
        label={t('setup.user.salutation')}
        name="salutation"
        value={salutation}
        onChange={(e) => onSalutationChange(e.target.value as Salutation)}
        required
        disabled={disabled}
        options={[
          { value: Salutation.MR, label: t('setup.user.salutationMr') },
          { value: Salutation.MRS, label: t('setup.user.salutationMrs') },
        ]}
      />

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <Input
          label={t('setup.user.firstName')}
          name="firstName"
          value={firstName}
          onChange={(e) => onFirstNameChange(e.target.value)}
          required
          placeholder={t('setup.user.firstNamePlaceholder')}
          disabled={disabled}
        />

        <Input
          label={t('setup.user.lastName')}
          name="lastName"
          value={lastName}
          onChange={(e) => onLastNameChange(e.target.value)}
          required
          placeholder={t('setup.user.lastNamePlaceholder')}
          disabled={disabled}
        />
      </div>

      {showEmail && onEmailChange && (
        <Input
          label={t('setup.user.email')}
          name="privateEmail"
          type="text"
          inputMode="email"
          autoComplete="email"
          value={email}
          onChange={(e) => onEmailChange(e.target.value)}
          required
          placeholder={t('setup.user.emailPlaceholder')}
          disabled={disabled}
          error={emailError}
        />
      )}

      <DatePicker
        label={t('setup.user.dateOfBirth')}
        name="dateOfBirth"
        value={dateOfBirth}
        onChange={(e) => onDateOfBirthChange(e.target.value)}
        required
        disabled={disabled}
        minValue={minValue}
        maxValue={maxValue}
        error={dateOfBirthError}
      />
    </>
  );
}
