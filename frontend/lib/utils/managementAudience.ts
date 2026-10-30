/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type {
  ManagementAudienceEntryDto,
  ManagementAudienceEntryRequest,
  ManagementAudienceSettingsDto,
} from '@/lib/api/admin';

type TranslateFn = (key: string) => string;

export function toManagementAudienceRequest(
  entry: ManagementAudienceEntryDto,
): ManagementAudienceEntryRequest {
  return {
    kind: entry.kind,
    refId: entry.refId,
    permissionTypes: entry.permissionTypes,
    maxDurationMinutes: entry.maxDurationMinutes,
  };
}

/** Organization or organization type an entry points at; null for the whole tenant. */
export function managementAudienceTargetName(
  t: TranslateFn,
  entry: ManagementAudienceEntryDto,
  settings: ManagementAudienceSettingsDto,
): string | null {
  if (entry.kind === 'TENANT') return null;
  const options = entry.kind === 'ORG_TYPE' ? settings.organizationTypes : settings.organizations;
  return (
    options.find((option) => option.id === entry.refId)?.name ??
    t('admin.managementAudience.unknownTarget')
  );
}
