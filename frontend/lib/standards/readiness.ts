/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { OrganizationTypeDto } from '@/lib/api/organizationTypes';
import type { StandardAssignmentDto, StandardDto } from '@/lib/api/standards';

/** Matches backend {@code StandardReadinessService}. */
export function isStandardsReadyForOrgCreation(
  organizationTypes: OrganizationTypeDto[],
  standards: StandardDto[],
  assignments: StandardAssignmentDto[],
): boolean {
  const customStandardIds = new Set(standards.filter((s) => !s.system).map((s) => s.id));

  return organizationTypes
    .filter((type) => type.active)
    .every((type) => {
      const assignment = assignments.find((a) => a.organizationTypeId === type.id);
      return assignment != null && customStandardIds.has(assignment.standardId);
    });
}

export function hasCustomStandard(standards: StandardDto[]): boolean {
  return standards.some((s) => !s.system);
}
