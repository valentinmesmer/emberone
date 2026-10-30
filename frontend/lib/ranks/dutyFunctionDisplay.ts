/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { ResolvedDutyFunctionDto } from '@/lib/api/ranks';
import { formatDateShort } from '@/lib/utils/date';

/** Subtitle for self/org function list rows: the period, prefixed by the organization name when multiple organizations are involved. */
export function formatDutyFunctionListSubtitle(
  fn: ResolvedDutyFunctionDto,
  multipleOrganizations: boolean,
  period: string,
): string {
  const organizationName = fn.organizationName.trim();
  return multipleOrganizations && organizationName ? `${organizationName} · ${period}` : period;
}

/** Period of an ended function, e.g. "01.01.2020 - 01.06.2025". */
export function formatDutyFunctionPeriod(fn: ResolvedDutyFunctionDto): string {
  return `${formatDateShort(fn.membershipStartDate)} - ${formatDateShort(fn.membershipEndDate)}`;
}

export function hasMultipleDutyFunctionOrganizations(
  functions: ResolvedDutyFunctionDto[] | undefined,
): boolean {
  if (!functions?.length) {
    return false;
  }
  const organizationIds = new Set(functions.map((fn) => fn.organizationId));
  return organizationIds.size > 1;
}
