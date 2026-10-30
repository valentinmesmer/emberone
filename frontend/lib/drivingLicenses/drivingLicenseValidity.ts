/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { DrivingLicenseClassDto, DrivingLicenseDto } from '@/lib/api/drivingLicenses';
import { getTodayIsoDate } from '@/lib/utils/date';

/** Driving license document is valid when documentExpirationDate is unset or on/after today. */
export function isDrivingLicenseDocumentValidOn(
  documentExpirationDate: string | null | undefined,
  referenceDate = getTodayIsoDate(),
): boolean {
  const trimmed = documentExpirationDate?.trim();
  if (!trimmed) {
    return true;
  }
  return trimmed >= referenceDate;
}

/** Driving license class is valid when expirationDate is unset or on/after today. */
export function isDrivingLicenseClassValidOn(
  expirationDate: string | null | undefined,
  referenceDate = getTodayIsoDate(),
): boolean {
  const trimmed = expirationDate?.trim();
  if (!trimmed) {
    return true;
  }
  return trimmed >= referenceDate;
}

export function findClass(
  license: DrivingLicenseDto | null | undefined,
  definitionId: number,
): DrivingLicenseClassDto | undefined {
  return (license?.classes ?? []).find(
    (licenseClass) => licenseClass.tenantDrivingLicenseDefinitionId === definitionId,
  );
}
