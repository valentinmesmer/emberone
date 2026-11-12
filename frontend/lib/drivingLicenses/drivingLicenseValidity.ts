/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

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

export function findClass<
  C extends { tenantDrivingLicenseDefinitionId: number; expirationDate: string | null },
>(license: { classes: C[] } | null | undefined, definitionId: number): C | undefined {
  return (license?.classes ?? []).find(
    (licenseClass) => licenseClass.tenantDrivingLicenseDefinitionId === definitionId,
  );
}
