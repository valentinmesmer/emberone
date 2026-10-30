/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { Input } from '@/components/ui/input';
import { Select } from '@/components/ui/select';
import { useTranslation } from '@/hooks/useTranslation';
import { ORGANIZATION_CODE_MAX_LENGTH } from '@/lib/validation/constants';
import {
  isValidOrganizationCode,
  sanitizeOrganizationCodeInput,
} from '@/lib/validation/organization';

export interface OrganizationFormFieldsProps {
  organizationCode: string;
  name: string;
  onOrganizationCodeChange: (value: string) => void;
  onNameChange: (value: string) => void;
  disabled?: boolean;
  showType?: boolean;
  type?: number;
  onTypeChange?: (value: number) => void;
  /** Selectable organization type options (id/name pairs from the tenant's own catalog). */
  typeOptions?: { value: number; label: string }[];
}

/**
 * Reusable form fields for organization forms
 * Used in admin EditOrgModal and CreateChildOrgModal
 */
export function OrganizationFormFields({
  organizationCode,
  name,
  onOrganizationCodeChange,
  onNameChange,
  disabled = false,
  showType = false,
  type,
  onTypeChange,
  typeOptions = [],
}: OrganizationFormFieldsProps) {
  const t = useTranslation();

  const organizationCodeError = useMemo(() => {
    if (!organizationCode.trim() || isValidOrganizationCode(organizationCode)) {
      return undefined;
    }
    return t('common.validation.invalidOrganizationCode');
  }, [organizationCode, t]);

  const handleOrganizationCodeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onOrganizationCodeChange(sanitizeOrganizationCodeInput(e.target.value));
  };

  return (
    <div className="space-y-4">
      <Input
        label={t('setup.organization.code')}
        value={organizationCode}
        onChange={handleOrganizationCodeChange}
        placeholder={t('setup.organization.codePlaceholder')}
        maxLength={ORGANIZATION_CODE_MAX_LENGTH}
        disabled={disabled}
        error={organizationCodeError}
        hint={t('setup.organization.codeHint')}
        required
      />

      <Input
        label={t('setup.organization.name')}
        value={name}
        onChange={(e) => onNameChange(e.target.value)}
        placeholder={t('setup.organization.namePlaceholder')}
        disabled={disabled}
        required
      />

      {showType && type !== undefined && onTypeChange && (
        <Select
          label={t('setup.organization.type')}
          value={String(type)}
          onChange={(e) => onTypeChange(Number.parseInt(e.target.value, 10))}
          options={typeOptions.map((option) => ({
            value: String(option.value),
            label: option.label,
          }))}
          disabled={disabled}
          required
        />
      )}
    </div>
  );
}
