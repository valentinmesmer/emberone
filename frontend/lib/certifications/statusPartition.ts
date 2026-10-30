/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { CertificationStatusItemDto } from '@/lib/api/certifications';

/** Items without an active assignment are historical and read-only. */
export function isHistoricalCertificationItem(item: CertificationStatusItemDto): boolean {
  return item.assigned === false || item.archived || item.status === 'ARCHIVED';
}

export function partitionCertificationStatusItems(items: CertificationStatusItemDto[]) {
  const active: CertificationStatusItemDto[] = [];
  const history: CertificationStatusItemDto[] = [];

  for (const item of items) {
    if (isHistoricalCertificationItem(item)) {
      history.push(item);
    } else {
      active.push(item);
    }
  }

  return { active, history };
}
