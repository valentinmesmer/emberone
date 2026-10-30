/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { BadgeTone, ListRowTone } from '@/components/ui/tones';
import type { CertificationStatus } from '@/lib/api/certifications';

export const CERTIFICATION_STATUS_BADGE_TONE: Record<CertificationStatus, BadgeTone> = {
  VALID: 'success',
  IN_REVIEW: 'warning',
  EXPIRING_SOON: 'warning',
  EXPIRED: 'danger',
  MISSING: 'danger',
  ARCHIVED: 'neutral',
};

export const CERTIFICATION_STATUS_ROW_TONE: Record<CertificationStatus, ListRowTone> = {
  VALID: 'success',
  IN_REVIEW: 'warning',
  EXPIRING_SOON: 'warning',
  EXPIRED: 'danger',
  MISSING: 'danger',
  ARCHIVED: 'neutral',
};
