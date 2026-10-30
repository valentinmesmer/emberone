/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { EventType } from '@/types/event';

export type CertEvidenceType = 'DOCUMENT' | 'SERVICE_EVENT' | 'OPERATION_EVENT';

/** How a concrete certification record was obtained (display origin). */
export type RecordEvidenceOrigin = CertEvidenceType | 'ONBOARDING' | 'NONE';

/**
 * Self-service / manual allowed unless restricted to event-only evidence.
 * Empty set = confirmation without attachment.
 */
export function allowsSubmittableEvidence(
  evidenceTypes?: CertEvidenceType[] | string[] | null,
): boolean {
  return !isEventOnlyEvidence(evidenceTypes);
}

export function isEventOnlyEvidence(evidenceTypes?: CertEvidenceType[] | string[] | null): boolean {
  if (!evidenceTypes || evidenceTypes.length === 0) {
    return false;
  }
  return evidenceTypes.every((type) => type === 'SERVICE_EVENT' || type === 'OPERATION_EVENT');
}

export function allowsEventGrantEvidence(
  evidenceTypes?: CertEvidenceType[] | string[] | null,
  eventType?: EventType,
): boolean {
  if (!evidenceTypes || evidenceTypes.length === 0 || !eventType) {
    return false;
  }
  if (eventType === 'SERVICE') {
    return evidenceTypes.includes('SERVICE_EVENT');
  }
  return evidenceTypes.includes('OPERATION_EVENT');
}

/** Required when DOCUMENT is among the configured evidence types. */
export function requiresSubmissionAttachment(
  evidenceTypes?: CertEvidenceType[] | string[] | null,
): boolean {
  if (!evidenceTypes || evidenceTypes.length === 0) {
    return false;
  }
  return evidenceTypes.includes('DOCUMENT');
}

export function allowsManualRecordCreation(
  evidenceTypes?: CertEvidenceType[] | string[] | null,
): boolean {
  return allowsSubmittableEvidence(evidenceTypes);
}

/**
 * Resolves display origin for a saved record from how it was created.
 * Event grants map to service/operation; onboarding via app link; otherwise documents → DOCUMENT, else NONE
 * (self-confirmation; optional documents may still be attached).
 */
export function resolveRecordEvidenceOrigin(record: {
  appLinkType?: string | null;
  submissionMethod?: 'MANUAL' | 'WORKFLOW' | 'EVENT' | 'ONBOARDING' | null;
  eventType?: 'SERVICE' | 'OPERATION' | null;
  userDocumentIds?: number[] | null;
}): RecordEvidenceOrigin {
  if (record.submissionMethod === 'EVENT') {
    if (record.eventType === 'OPERATION') {
      return 'OPERATION_EVENT';
    }
    return 'SERVICE_EVENT';
  }
  if (record.appLinkType === 'ONBOARDING_INVITATION') {
    return 'ONBOARDING';
  }
  if ((record.userDocumentIds?.length ?? 0) > 0) {
    return 'DOCUMENT';
  }
  return 'NONE';
}
