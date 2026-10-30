/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { formatDateLong } from '@/lib/utils';
import type { OnboardingDocumentCatalogItemDto } from '@/types/onboardingInvitation';

type TranslateFn = (key: string, params?: Record<string, string | number>) => string;

export function buildEntryDocumentConfirmationText(
  t: TranslateFn,
  document: OnboardingDocumentCatalogItemDto,
): string {
  if (document.templateVersion == null || !document.templateUploadedAt) {
    return t('myArea.onboarding.wizard.entryDocuments.confirmationProofFallback', {
      name: document.name,
    });
  }
  return t('myArea.onboarding.wizard.entryDocuments.confirmationProof', {
    name: document.name,
    version: document.templateVersion,
    uploadedAt: formatDateLong(document.templateUploadedAt),
  });
}
