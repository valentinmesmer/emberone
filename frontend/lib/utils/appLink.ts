/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import {
  getMyAreaCertificationDetailPath,
  getMyAreaCourseRegistrationsPath,
  getMyAreaDrivingLicensesPath,
  getMyAreaOnboardingDetailPath,
  getMyAreaTrainingCourseRecordDetailPath,
  getMyAreaWorkflowDetailPath,
} from '@/lib/my-area/myAreaSections';
import {
  getOrganizationUserCertificationDetailPath,
  getOrganizationUserSectionPath,
  getOrganizationUserTrainingCourseRecordDetailPath,
  getOrganizationUserWorkflowDetailPath,
} from '@/lib/navigation/userManagementSections';
import type { AppLinkFields } from '@/types/appLink';
import type { UserDocumentDto } from '@/types/userDocument';

export {
  appLinkActionLabel,
  appLinkActionTranslationKey,
  appLinkTypeLabel,
  appLinkTypeTranslationKey,
  isAppLinkType,
} from '@/lib/utils/appLinkLabels';

export type AppNavigationContext =
  | { scope: 'self' }
  | { scope: 'org'; organizationId: number; userId: number };

/**
 * Maps API AppLink targets to in-app routes. Backend {@code navigationPath} is the self/my-area
 * canon; org user-management UI rewrites to organization-scoped routes.
 *
 * Permission gating belongs in {@code sourceLink.ts} ({@code resolveGatedAppNavigationHref}).
 */
export function resolveAppNavigationHref(
  link: Pick<AppLinkFields, 'linkType' | 'linkId' | 'navigationPath'>,
  context: AppNavigationContext,
): string | null {
  const fallback = link.navigationPath?.trim() || null;
  const linkType = link.linkType;
  const linkId = link.linkId;

  if (!linkType || linkId == null) {
    return fallback;
  }

  if (context.scope === 'self') {
    switch (linkType) {
      case 'WORKFLOW':
        return fallback ?? getMyAreaWorkflowDetailPath(linkId);
      case 'CERTIFICATION_RECORD':
        return getMyAreaCertificationDetailPath(linkId);
      case 'TRAINING_COURSE_RECORD':
        return getMyAreaTrainingCourseRecordDetailPath(linkId);
      case 'DRIVING_LICENSE_RECORD':
        return getMyAreaDrivingLicensesPath();
      case 'ONBOARDING_INVITATION':
        return fallback ?? getMyAreaOnboardingDetailPath(linkId);
      case 'COURSE_REGISTRATION':
        return fallback ?? getMyAreaCourseRegistrationsPath();
      case 'CERTIFICATION_EVENT_GRANT':
        return fallback;
      default:
        return fallback;
    }
  }

  const { organizationId, userId } = context;
  switch (linkType) {
    case 'WORKFLOW':
      return getOrganizationUserWorkflowDetailPath(organizationId, userId, linkId);
    case 'CERTIFICATION_RECORD':
      return getOrganizationUserCertificationDetailPath(organizationId, userId, linkId);
    case 'TRAINING_COURSE_RECORD':
      return getOrganizationUserTrainingCourseRecordDetailPath(organizationId, userId, linkId);
    case 'DRIVING_LICENSE_RECORD':
      return getOrganizationUserSectionPath(organizationId, userId, 'driving-licenses');
    case 'ONBOARDING_INVITATION':
      return getOrganizationUserSectionPath(organizationId, userId, 'onboarding');
    case 'COURSE_REGISTRATION':
      // Provider org is not on the link; avoid routing via the member's org context.
      return fallback;
    case 'CERTIFICATION_EVENT_GRANT':
      return fallback;
    default:
      return fallback;
  }
}

export function hasAppNavigationLink(
  fields: Pick<AppLinkFields, 'navigationPath'>,
): fields is AppLinkFields & { navigationPath: string } {
  return !!fields.navigationPath?.trim();
}

export function isOnboardingAppLink(appLinkType?: string | null): boolean {
  return appLinkType === 'ONBOARDING_INVITATION';
}

export function hasDocumentLinks(doc: Pick<UserDocumentDto, 'links'>): boolean {
  return doc.links.length > 0;
}
