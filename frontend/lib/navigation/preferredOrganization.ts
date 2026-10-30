/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

export function findOrganizationById<T extends { id: number }>(
  organizations: readonly T[],
  id: number | null | undefined,
): T | undefined {
  if (id == null) {
    return undefined;
  }
  return organizations.find((org) => org.id === id);
}

/**
 * Resolves which organization to show in nav chrome when the user is not on an org route:
 * active route org → last-used preference → main/fallback.
 */
export function resolvePreferredOrganization<T extends { id: number }>(
  organizations: readonly T[],
  options: {
    activeId?: number | null;
    preferredId?: number | null;
    fallback: T;
  },
): T {
  return (
    findOrganizationById(organizations, options.activeId) ??
    findOrganizationById(organizations, options.preferredId) ??
    options.fallback
  );
}
