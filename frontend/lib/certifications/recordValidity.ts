/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { CertificationStatus, UserCertificationRecordDto } from '@/lib/api/certifications';

function localDateIso(): string {
  return new Date().toLocaleDateString('en-CA');
}

function addDaysToIsoDate(isoDate: string, days: number): string {
  const date = new Date(`${isoDate}T12:00:00`);
  date.setDate(date.getDate() + days);
  return date.toLocaleDateString('en-CA');
}

/** Mirrors backend validity check: expiry null or on/after the reference date. */
export function isActiveCertificationRecord(
  record: Pick<UserCertificationRecordDto, 'expiryDate'>,
  onDate: string = localDateIso(),
): boolean {
  return !record.expiryDate || record.expiryDate >= onDate;
}

/** Derives display status for a single record (default reminder window matches backend). */
export function computeRecordCertificationStatus(
  record: Pick<UserCertificationRecordDto, 'expiryDate'>,
  reminderDaysBeforeExpiry = 30,
  onDate: string = localDateIso(),
): CertificationStatus {
  if (!record.expiryDate) {
    return 'VALID';
  }
  if (record.expiryDate < onDate) {
    return 'EXPIRED';
  }
  if (record.expiryDate <= addDaysToIsoDate(onDate, reminderDaysBeforeExpiry)) {
    return 'EXPIRING_SOON';
  }
  return 'VALID';
}
