/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { userDocumentApi } from '@/lib/api/userDocument';
import type { OnboardingInvitationAccess } from '@/types/onboardingInvitation';

export type OnboardingPromotedDocumentScope =
  | { kind: 'self' }
  | { kind: 'organization'; organizationId: number; subjectUserId: number };

export function resolveOnboardingPromotedDocumentScope(
  access: OnboardingInvitationAccess,
  targetOrganizationId: number,
  subjectUserId: number,
): OnboardingPromotedDocumentScope {
  if (access.kind === 'self') {
    return { kind: 'self' };
  }
  const organizationId =
    access.kind === 'admin'
      ? access.organizationId
      : (access.organizationId ?? targetOrganizationId);
  return { kind: 'organization', organizationId, subjectUserId };
}

export async function viewOnboardingPromotedUserDocument(
  scope: OnboardingPromotedDocumentScope,
  documentId: number,
  fileName: string,
  targetWindow: Window,
): Promise<void> {
  if (scope.kind === 'self') {
    await userDocumentApi.view(documentId, targetWindow, fileName);
    return;
  }
  await userDocumentApi.viewForOrganizationUser(
    scope.organizationId,
    scope.subjectUserId,
    documentId,
    targetWindow,
    fileName,
  );
}
