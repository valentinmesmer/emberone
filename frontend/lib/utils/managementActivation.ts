/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { ManagementActivationLogItemDto, ManagementActivationStatus } from '@/lib/api/admin';
import { PermissionTypeEnum } from '@/types/permission';

type TranslateFn = (key: string) => string;

export const ACTIVATION_STATUS_TONE: Record<
  ManagementActivationStatus,
  'success' | 'neutral' | 'warning'
> = {
  ACTIVE: 'success',
  EXPIRED: 'neutral',
  ENDED: 'warning',
};

/** Person name of a log entry; deleted users show as former member. */
export function getActivationPersonName(
  activation: ManagementActivationLogItemDto,
  formerMemberLabel: string,
): string {
  return activation.userId == null
    ? formerMemberLabel
    : `${activation.userFirstName ?? ''} ${activation.userLastName ?? ''}`.trim();
}

/** Label of a management scope entry, which the API sends as a numeric permission id or an enum name. */
export function managementPermissionLabel(t: TranslateFn, permission: string | number): string {
  const name =
    typeof permission === 'number' || /^\d+$/.test(permission)
      ? PermissionTypeEnum[Number(permission)]
      : permission;
  return name ? t(`permissions.${name}`) : String(permission);
}
