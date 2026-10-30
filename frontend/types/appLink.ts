/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/** Semantic link target - persisted as type + id; route resolved server-side. */
export type AppLinkType =
  | 'WORKFLOW'
  | 'CERTIFICATION_RECORD'
  | 'TRAINING_COURSE_RECORD'
  | 'DRIVING_LICENSE_RECORD'
  | 'CERTIFICATION_EVENT_GRANT'
  | 'ONBOARDING_INVITATION'
  | 'COURSE_REGISTRATION';

export interface AppLinkFields {
  linkType?: AppLinkType | null;
  linkId?: number | null;
  /** Relative in-app route, resolved by the API at read time. */
  navigationPath?: string | null;
}
