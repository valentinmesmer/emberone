/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type {
  CertificationDefinitionDto,
  CertificationStatusItemDto,
  OrgCertificationCatalogDto,
  UserCertificationAssignmentDto,
} from '@/lib/api/certifications';

export function getAssignedDefinitionIds(statusItems: CertificationStatusItemDto[]): Set<number> {
  return new Set(
    statusItems.filter((item) => item.assigned).map((item) => item.tenantCertDefinitionId),
  );
}

export function filterAssignableDefinitions(
  definitions: CertificationDefinitionDto[],
  statusItems: CertificationStatusItemDto[],
): CertificationDefinitionDto[] {
  const assigned = getAssignedDefinitionIds(statusItems);
  return definitions.filter((definition) => definition.active && !assigned.has(definition.id));
}

export function hasAssignableCertifications(
  catalog: OrgCertificationCatalogDto | undefined,
  _assignments: UserCertificationAssignmentDto[],
  statusItems: CertificationStatusItemDto[],
): boolean | undefined {
  if (!catalog) return undefined;
  return filterAssignableDefinitions(catalog.definitions, statusItems).length > 0;
}
