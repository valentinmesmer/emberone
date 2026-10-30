/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { OrganizationTreeDto } from '@/types/organization';

/** Finds the user's main organization anywhere in the membership tree (not only root nodes). */
export function findMainOrganizationInTree(
  organizations: OrganizationTreeDto[] | undefined | null,
): OrganizationTreeDto | undefined {
  if (!organizations?.length) {
    return undefined;
  }

  for (const org of organizations) {
    if (org.main) {
      return org;
    }
    const mainInChildren = findMainOrganizationInTree(org.children);
    if (mainInChildren) {
      return mainInChildren;
    }
  }

  return undefined;
}

export function countOrganizationsInTree(organizations: OrganizationTreeDto[]): number {
  return organizations.reduce(
    (n, o) => n + 1 + (o.children?.length ? countOrganizationsInTree(o.children) : 0),
    0,
  );
}

export function findOrganizationInTree(
  organizations: OrganizationTreeDto[] | undefined | null,
  organizationId: number,
): OrganizationTreeDto | undefined {
  if (!organizations?.length) {
    return undefined;
  }

  for (const org of organizations) {
    if (org.id === organizationId) {
      return org;
    }
    const inChildren = findOrganizationInTree(org.children, organizationId);
    if (inChildren) {
      return inChildren;
    }
  }

  return undefined;
}
