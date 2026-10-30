/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

export const USER_ID = 'userId';
export const TENANT_ID = 'tenantId';

export const APP_ROLE_VALUES = {
  GLOBAL_ADMIN: 'GLOBAL_ADMIN',
  TENANT_ADMIN: 'TENANT_ADMIN',
  USER: 'USER',
} as const;

const APP_ROLE_RANK: Record<string, number> = {
  [APP_ROLE_VALUES.GLOBAL_ADMIN]: 4,
  [APP_ROLE_VALUES.TENANT_ADMIN]: 3,
  [APP_ROLE_VALUES.USER]: 1,
};

const APP_ROLES_ORDERED_HIGH_TO_LOW: readonly string[] = [
  APP_ROLE_VALUES.GLOBAL_ADMIN,
  APP_ROLE_VALUES.TENANT_ADMIN,
  APP_ROLE_VALUES.USER,
];

export function getAssignableAppRoles(appRole: string | null | undefined): string[] {
  const maxRank = appRoleToRank(appRole);
  if (maxRank <= 0) {
    return [];
  }
  return APP_ROLES_ORDERED_HIGH_TO_LOW.filter((r) => (APP_ROLE_RANK[r] ?? 0) <= maxRank);
}

function appRoleToRank(appRole: string | null | undefined): number {
  if (appRole == null || String(appRole).trim() === '') {
    return 0;
  }
  return APP_ROLE_RANK[String(appRole).trim().toUpperCase()] ?? 0;
}

/** Global administrators only (create tenants, bootstrap root org, etc.). */
export function isGlobalAdmin(appRole: string | null | undefined): boolean {
  return appRole === APP_ROLE_VALUES.GLOBAL_ADMIN;
}

/** Global and tenant admins: administration area (tenant list, users). */
export function canAccessAdministration(appRole: string | null | undefined): boolean {
  return isGlobalAdmin(appRole) || appRole === APP_ROLE_VALUES.TENANT_ADMIN;
}
