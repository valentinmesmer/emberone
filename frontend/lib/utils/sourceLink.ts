/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import {
  type AppNavigationContext,
  isOnboardingAppLink,
  resolveAppNavigationHref,
} from '@/lib/utils/appLink';
import { isAppLinkType } from '@/lib/utils/appLinkLabels';
import { getReadPermissionForEventType } from '@/lib/utils/event';
import type { AppLinkFields, AppLinkType } from '@/types/appLink';
import { EventType } from '@/types/event';
import { PermissionTypeEnum } from '@/types/permission';
import type { UserDocumentLinkDto } from '@/types/userDocument';

export type HasPermission = (organizationId: number, permission: PermissionTypeEnum) => boolean;

type AppLinkEntityFields = {
  appLinkType?: string | null;
  appLinkId?: number | null;
  navigationPath?: string | null;
};

export type EventSourceFields = {
  navigationPath?: string | null;
  eventType?: 'SERVICE' | 'OPERATION' | null;
  eventOrganizationId?: number | null;
};

const DOCUMENT_ENTITY_LINK_TYPES: AppLinkType[] = [
  'CERTIFICATION_RECORD',
  'TRAINING_COURSE_RECORD',
];

/**
 * Org-scope read permission for a semantic AppLink target. {@code null} means no org gate beyond
 * path resolution (self my-area, or event grant via {@link resolveEventSourceHref}).
 */
export function readPermissionForAppLinkType(
  linkType: AppLinkType | string | null | undefined,
): PermissionTypeEnum | null {
  switch (linkType) {
    case 'WORKFLOW':
      return PermissionTypeEnum.WORKFLOW_READ;
    case 'ONBOARDING_INVITATION':
      return PermissionTypeEnum.USER_READ;
    case 'CERTIFICATION_RECORD':
      return PermissionTypeEnum.CERTIFICATION_READ;
    case 'TRAINING_COURSE_RECORD':
      return PermissionTypeEnum.TRAINING_COURSE_READ;
    case 'COURSE_REGISTRATION':
      return PermissionTypeEnum.TRAINING_COURSE_READ;
    case 'DRIVING_LICENSE_RECORD':
      return PermissionTypeEnum.DRIVING_LICENSE_READ;
    case 'CERTIFICATION_EVENT_GRANT':
      return null;
    default:
      return null;
  }
}

/**
 * Resolves an AppLink href and hides it when the viewer lacks the org read permission for that
 * target. Self scope is always allowed. Event grants require event read via
 * {@link resolveEventSourceHref}.
 */
export function resolveGatedAppNavigationHref(
  link: Pick<AppLinkFields, 'linkType' | 'linkId' | 'navigationPath'>,
  context: AppNavigationContext,
  hasPermission: HasPermission,
  eventSource?: EventSourceFields,
): string | null {
  if (link.linkType === 'CERTIFICATION_EVENT_GRANT') {
    return resolveEventSourceHref(
      {
        navigationPath: link.navigationPath ?? eventSource?.navigationPath,
        eventType: eventSource?.eventType,
        eventOrganizationId: eventSource?.eventOrganizationId,
      },
      hasPermission,
    );
  }

  const href = resolveAppNavigationHref(link, context);
  if (!href) {
    return null;
  }
  if (context.scope === 'self') {
    return href;
  }
  const permission = readPermissionForAppLinkType(link.linkType);
  if (permission == null) {
    return href;
  }
  return hasPermission(context.organizationId, permission) ? href : null;
}

/**
 * Event-origin source link (Dienstleistung/Einsatz). Hidden without the matching event read
 * permission on {@code eventOrganizationId}.
 */
export function resolveEventSourceHref(
  record: EventSourceFields,
  hasPermission: HasPermission,
): string | null {
  const href = record.navigationPath?.trim() || null;
  const orgId = record.eventOrganizationId;
  const eventType = record.eventType;
  if (!href || orgId == null || eventType == null) {
    return null;
  }
  const permission = getReadPermissionForEventType(
    eventType === 'SERVICE' ? EventType.SERVICE : EventType.OPERATION,
  );
  return hasPermission(orgId, permission) ? href : null;
}

/** Workflow source link - self always; org requires {@code WORKFLOW_READ}. */
export function resolveWorkflowSourceHref(
  fields: AppLinkEntityFields,
  context: AppNavigationContext,
  hasPermission: HasPermission,
): string | null {
  if (fields.appLinkType !== 'WORKFLOW' || fields.appLinkId == null) {
    return null;
  }
  return resolveGatedAppNavigationHref(
    {
      linkType: 'WORKFLOW',
      linkId: fields.appLinkId,
      navigationPath: fields.navigationPath,
    },
    context,
    hasPermission,
  );
}

/** Onboarding source link - self always; org requires {@code USER_READ}. */
export function resolveOnboardingSourceHref(
  fields: AppLinkEntityFields,
  context: AppNavigationContext,
  hasPermission: HasPermission,
): string | null {
  if (!isOnboardingAppLink(fields.appLinkType)) {
    return null;
  }
  const linkType = fields.appLinkType;
  if (!linkType || !isAppLinkType(linkType)) {
    return null;
  }
  return resolveGatedAppNavigationHref(
    {
      linkType,
      linkId: fields.appLinkId ?? null,
      navigationPath: fields.navigationPath,
    },
    context,
    hasPermission,
  );
}

/** First document AppLink the viewer may open (entity links preferred over workflow). */
export function getFirstGatedNavigableDocumentLink(
  links: readonly UserDocumentLinkDto[],
  context: AppNavigationContext,
  hasPermission: HasPermission,
): { link: UserDocumentLinkDto; href: string } | null {
  const gated: { link: UserDocumentLinkDto; href: string }[] = [];
  for (const link of links) {
    const href = resolveGatedAppNavigationHref(link, context, hasPermission);
    if (href) {
      gated.push({ link, href });
    }
  }
  const entity = gated.find(
    (entry) =>
      entry.link.linkType != null && DOCUMENT_ENTITY_LINK_TYPES.includes(entry.link.linkType),
  );
  return entity ?? gated[0] ?? null;
}
