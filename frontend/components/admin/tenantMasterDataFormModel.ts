/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type {
  CreateTenantRequest,
  TenantAdminDetailDto,
  TenantAdminPatchRequest,
} from '@/lib/api/admin';
import { DEFAULT_FEDERAL_STATE, type FederalState } from '@/lib/federal-state';
import { DEFAULT_LOCALE } from '@/lib/i18n/locale';
import type { SupportedLocale } from '@/types/permission';

export type TenantMasterDataFormState = {
  locale: SupportedLocale;
  federalState: FederalState;
  name: string;
  abbreviation: string;
};

export function emptyTenantMasterDataForm(
  federalState: FederalState = DEFAULT_FEDERAL_STATE,
): TenantMasterDataFormState {
  return {
    locale: DEFAULT_LOCALE,
    federalState,
    name: '',
    abbreviation: '',
  };
}

export function tenantDetailToMasterDataForm(d: TenantAdminDetailDto): TenantMasterDataFormState {
  return {
    locale: d.locale ?? DEFAULT_LOCALE,
    federalState: d.federalState ?? DEFAULT_FEDERAL_STATE,
    name: d.name ?? '',
    abbreviation: d.abbreviation ?? '',
  };
}

export function masterDataFormToCreateRequest(f: TenantMasterDataFormState): CreateTenantRequest {
  return {
    name: f.name.trim(),
    abbreviation: f.abbreviation.trim(),
    locale: f.locale,
    federalState: f.federalState,
  };
}

export function masterDataFormToPatchRequest(
  f: TenantMasterDataFormState,
): TenantAdminPatchRequest {
  return {
    name: f.name.trim(),
  };
}

export function isTenantFormComplete(form: TenantMasterDataFormState): boolean {
  return Boolean(form.name.trim() && form.abbreviation.trim());
}
