/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import type { UserQualificationOrgDecisionDto } from '@/lib/api/qualifications';
import { getTodayIsoDate } from '@/lib/utils/date';

export function isOrgDecisionIntervalActive(
  interval: UserQualificationOrgDecisionDto,
  today: string = getTodayIsoDate(),
): boolean {
  if (interval.status !== 'GRANTED') return false;
  if (today < interval.startDate) return false;
  if (interval.endDate && today > interval.endDate) return false;
  return true;
}

export function findActiveGrantInterval(
  intervals: UserQualificationOrgDecisionDto[] | undefined,
): UserQualificationOrgDecisionDto | undefined {
  return intervals?.find((interval) => isOrgDecisionIntervalActive(interval));
}
