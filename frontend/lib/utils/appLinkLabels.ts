/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { AppLinkFields, AppLinkType } from '@/types/appLink';

type TranslateFn = (key: string) => string;

/** i18n keys for action labels (e.g. "Zum Antrag"). Add new link types here. */
const APP_LINK_ACTION_KEYS: Record<AppLinkType, string> = {
  WORKFLOW: 'appLink.actions.WORKFLOW',
  CERTIFICATION_RECORD: 'appLink.actions.CERTIFICATION_RECORD',
  TRAINING_COURSE_RECORD: 'appLink.actions.TRAINING_COURSE_RECORD',
  DRIVING_LICENSE_RECORD: 'appLink.actions.DRIVING_LICENSE_RECORD',
  CERTIFICATION_EVENT_GRANT: 'appLink.actions.CERTIFICATION_EVENT_GRANT',
  ONBOARDING_INVITATION: 'appLink.actions.ONBOARDING_INVITATION',
  COURSE_REGISTRATION: 'appLink.actions.COURSE_REGISTRATION',
};

/** i18n keys for short type names (e.g. "Antrag"). */
const APP_LINK_TYPE_KEYS: Record<AppLinkType, string> = {
  WORKFLOW: 'appLink.types.WORKFLOW',
  CERTIFICATION_RECORD: 'appLink.types.CERTIFICATION_RECORD',
  TRAINING_COURSE_RECORD: 'appLink.types.TRAINING_COURSE_RECORD',
  DRIVING_LICENSE_RECORD: 'appLink.types.DRIVING_LICENSE_RECORD',
  CERTIFICATION_EVENT_GRANT: 'appLink.types.CERTIFICATION_EVENT_GRANT',
  ONBOARDING_INVITATION: 'appLink.types.ONBOARDING_INVITATION',
  COURSE_REGISTRATION: 'appLink.types.COURSE_REGISTRATION',
};

const DEFAULT_ACTION_KEY = 'appLink.actions.default';

export function isAppLinkType(value: string | null | undefined): value is AppLinkType {
  return value != null && value in APP_LINK_ACTION_KEYS;
}

export function appLinkActionTranslationKey(linkType?: AppLinkType | null): string {
  if (linkType && isAppLinkType(linkType)) {
    return APP_LINK_ACTION_KEYS[linkType];
  }
  return DEFAULT_ACTION_KEY;
}

export function appLinkTypeTranslationKey(linkType: AppLinkType): string {
  return APP_LINK_TYPE_KEYS[linkType];
}

/** Button / aria label for navigating to a linked resource. */
export function appLinkActionLabel(link: Pick<AppLinkFields, 'linkType'>, t: TranslateFn): string {
  return t(appLinkActionTranslationKey(link.linkType));
}

/** Short human-readable name for a link type. */
export function appLinkTypeLabel(linkType: AppLinkType, t: TranslateFn): string {
  return t(appLinkTypeTranslationKey(linkType));
}
