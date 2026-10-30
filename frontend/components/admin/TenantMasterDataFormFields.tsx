/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type { Dispatch, SetStateAction } from 'react';
import { Input } from '@/components/ui/input';
import { Select } from '@/components/ui/select';
import { useTranslation } from '@/hooks/useTranslation';
import type { FederalState } from '@/lib/federal-state';
import { cn } from '@/lib/utils/cn';
import type { TenantMasterDataFormState } from './tenantMasterDataFormModel';

export interface TenantMasterDataFormFieldsProps {
  form: TenantMasterDataFormState;
  setForm: Dispatch<SetStateAction<TenantMasterDataFormState>>;
  className?: string;
  /** Shown only when creating a tenant; locale is immutable after creation. */
  showLocaleField?: boolean;
  /** Shown only when creating a tenant; federal state is immutable after creation. */
  showFederalStateField?: boolean;
  configuredFederalStates?: FederalState[];
  /** Shown only when creating a tenant; abbreviation is immutable after creation. */
  showAbbreviationField?: boolean;
}

export function TenantMasterDataFormFields({
  form,
  setForm,
  className,
  showLocaleField = false,
  showFederalStateField = false,
  configuredFederalStates = ['BAYERN'],
  showAbbreviationField = false,
}: TenantMasterDataFormFieldsProps) {
  const t = useTranslation();

  return (
    <div className={cn('space-y-3 sm:space-y-4', className)}>
      <Input
        label={t('admin.tenantForm.name')}
        value={form.name}
        onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))}
        required
      />
      {showAbbreviationField ? (
        <div className="space-y-1">
          <Input
            label={t('admin.tenantForm.abbreviation')}
            value={form.abbreviation}
            onChange={(e) => setForm((p) => ({ ...p, abbreviation: e.target.value }))}
            required
          />
          <p className="text-xs text-[var(--text-secondary)]">
            {t('admin.tenantForm.abbreviationHint')}
          </p>
        </div>
      ) : null}
      {showFederalStateField ? (
        <div className="space-y-1">
          <Select
            label={t('admin.tenantForm.federalState')}
            value={form.federalState}
            onChange={(e) =>
              setForm((p) => ({ ...p, federalState: e.target.value as FederalState }))
            }
            options={configuredFederalStates.map((state) => ({
              value: state,
              label: t(`federalState.${state}`),
            }))}
          />
          <p className="text-xs text-[var(--text-secondary)]">
            {t('admin.tenantForm.federalStateHint')}
          </p>
        </div>
      ) : null}
      {showLocaleField ? (
        <div className="space-y-1">
          <Select
            label={t('admin.tenantForm.locale')}
            value={form.locale}
            onChange={(e) =>
              setForm((p) => ({
                ...p,
                locale: e.target.value as TenantMasterDataFormState['locale'],
              }))
            }
            options={[{ value: 'de', label: t('locale.deutsch') }]}
          />
          <p className="text-xs text-[var(--text-secondary)]">{t('admin.tenantForm.localeHint')}</p>
        </div>
      ) : null}
    </div>
  );
}
