/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { EmergencyContactRelationship } from '@/types/enums/emergencyContactRelationship';
import type {
  OnboardingCertificationDraft,
  OnboardingDrivingLicenseDraft,
  OnboardingEmergencyContactDraft,
  OnboardingEntryDocumentDraft,
  OnboardingTrainingCourseDraft,
} from '@/types/onboardingInvitation';

function asRecord(value: unknown): Record<string, unknown> | null {
  return value && typeof value === 'object' && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : null;
}

function asList(value: unknown): unknown[] {
  return Array.isArray(value) ? value : [];
}

function asString(value: unknown): string {
  return typeof value === 'string' ? value : value == null ? '' : String(value);
}

function asOptionalString(value: unknown): string | null {
  const text = asString(value).trim();
  return text ? text : null;
}

function asNumber(value: unknown): number | null {
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  if (typeof value === 'string' && value.trim()) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
}

function asBoolean(value: unknown): boolean {
  return value === true || value === 'true';
}

export function readEmergencyContacts(
  payload: Record<string, unknown>,
): OnboardingEmergencyContactDraft[] {
  return asList(payload.contacts).flatMap((item) => {
    const record = asRecord(item);
    if (!record) return [];
    return [
      {
        relationship: asString(record.relationship) as EmergencyContactRelationship,
        firstName: asString(record.firstName),
        lastName: asString(record.lastName),
        street: asString(record.street),
        city: asString(record.city),
        postalCode: asString(record.postalCode),
        country: asString(record.country),
        phone: asString(record.phone),
        email: asString(record.email),
      },
    ];
  });
}

function asStringList(value: unknown): string[] {
  return asList(value)
    .map((item) => asString(item).trim())
    .filter((item) => item.length > 0);
}

function readAttachmentFileNames(record: Record<string, unknown>): {
  fileName: string | null;
  fileNames: string[];
  uploaded: boolean;
} {
  const fileNames = asStringList(record.fileNames);
  const legacyFileName = asOptionalString(record.fileName);
  const resolvedFileNames =
    fileNames.length > 0 ? fileNames : legacyFileName ? [legacyFileName] : [];
  const uploaded = asBoolean(record.uploaded) || resolvedFileNames.length > 0;
  return {
    fileName: resolvedFileNames[0] ?? null,
    fileNames: resolvedFileNames,
    uploaded,
  };
}

export function readEntryDocuments(
  payload: Record<string, unknown>,
): OnboardingEntryDocumentDraft[] {
  return asList(payload.documents).flatMap((item) => {
    const record = asRecord(item);
    if (!record) return [];
    return [
      {
        configKey: asString(record.configKey),
        confirmed: asBoolean(record.confirmed),
        declined: asBoolean(record.declined),
        fileName: asOptionalString(record.fileName),
        uploaded: asBoolean(record.uploaded),
        templateOpened: asBoolean(record.templateOpened),
      },
    ];
  });
}

export function readTrainingCourses(
  payload: Record<string, unknown>,
): OnboardingTrainingCourseDraft[] {
  return asList(payload.records).flatMap((item) => {
    const record = asRecord(item);
    if (!record) return [];
    const typeIds = asList(record.tenantTrainingCourseTypeIds)
      .map((id) => asNumber(id))
      .filter((id): id is number => id != null);
    return [
      {
        recordKey: asString(record.recordKey) || crypto.randomUUID(),
        tenantTrainingCourseTypeIds: typeIds,
        name: asOptionalString(record.name),
        startDate: asString(record.startDate),
        endDate: asString(record.endDate),
        location: asString(record.location),
        hours: asNumber(record.hours),
        notes: asOptionalString(record.notes),
        ...readAttachmentFileNames(record),
      },
    ];
  });
}

export function readCertifications(
  payload: Record<string, unknown>,
): OnboardingCertificationDraft[] {
  return asList(payload.records).flatMap((item) => {
    const record = asRecord(item);
    if (!record) return [];
    const definitionId = asNumber(record.tenantCertDefinitionId);
    if (definitionId == null) return [];
    return [
      {
        recordKey: asString(record.recordKey) || crypto.randomUUID(),
        tenantCertDefinitionId: definitionId,
        completedDate: asString(record.completedDate),
        notes: asOptionalString(record.notes),
        ...readAttachmentFileNames(record),
      },
    ];
  });
}

export function readNoneDeclared(payload: Record<string, unknown>): boolean {
  return asBoolean(payload.noneDeclared);
}

export function readOnboardingRankId(payload: Record<string, unknown>): number | null {
  return asNumber(payload.tenantRankId);
}

export function readDrivingLicense(
  payload: Record<string, unknown>,
): OnboardingDrivingLicenseDraft | null | undefined {
  if (!('license' in payload)) return undefined;
  const record = asRecord(payload.license);
  if (!record) return null;
  return {
    issueDate: asString(record.issueDate),
    documentExpirationDate: asOptionalString(record.documentExpirationDate),
    licenseNumber: asString(record.licenseNumber),
    issuingAuthority: asString(record.issuingAuthority),
    notes: asOptionalString(record.notes),
    ...readAttachmentFileNames(record),
    classes: asList(record.classes).flatMap((item) => {
      const classRecord = asRecord(item);
      if (!classRecord) return [];
      const definitionId = asNumber(classRecord.tenantDrivingLicenseDefinitionId);
      if (definitionId == null) return [];
      return [
        {
          tenantDrivingLicenseDefinitionId: definitionId,
          grantDate: asOptionalString(classRecord.grantDate),
          expirationDate: asOptionalString(classRecord.expirationDate),
        },
      ];
    }),
  };
}
