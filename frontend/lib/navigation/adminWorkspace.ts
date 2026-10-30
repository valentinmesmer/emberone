/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/** Administration hub (overview). */
export const ADMIN_HUB_PATH = '/admin';

/** Tenant list (not tenant detail). */
export const ADMIN_TENANTS_LIST_PATH = '/admin/tenants';

export const ADMIN_USERS_PATH = '/admin/users';

export const ADMIN_MANAGEMENT_PATH = '/admin/management';

export function getAdminTenantDetailPath(tenantId: number): string {
  return `/admin/tenants/${tenantId}`;
}

export function getAdminStandardsPath(tenantId: number): string {
  return `/admin/tenants/${tenantId}/standards`;
}

export function getAdminTenantStandardsPath(tenantId: number): string {
  return `/admin/tenants/${tenantId}/tenant-standards`;
}

export function getAdminTenantRetentionPath(tenantId: number): string {
  return `/admin/tenants/${tenantId}/retention`;
}

export function getAdminTenantRetentionCategoryPath(tenantId: number, category: string): string {
  return `${getAdminTenantRetentionPath(tenantId)}/${encodeURIComponent(category)}`;
}

export function getAdminStandardDetailPath(tenantId: number, standardId: number): string {
  return `${getAdminStandardsPath(tenantId)}/${standardId}`;
}

export function getAdminStandardOrgGroupPath(
  tenantId: number,
  standardId: number,
  configKey: string,
): string {
  return `${getAdminStandardDetailPath(tenantId, standardId)}/organization-groups/${encodeURIComponent(configKey)}`;
}

export function getAdminStandardPermissionGroupPath(
  tenantId: number,
  standardId: number,
  configKey: string,
): string {
  return `${getAdminStandardDetailPath(tenantId, standardId)}/permission-groups/${encodeURIComponent(configKey)}`;
}

export function getAdminStandardWorkflowPath(
  tenantId: number,
  standardId: number,
  configKey: string,
): string {
  return `${getAdminStandardDetailPath(tenantId, standardId)}/workflows/${encodeURIComponent(configKey)}`;
}

export function getAdminStandardOnboardingStepPath(
  tenantId: number,
  standardId: number,
  stepType: string,
): string {
  return `${getAdminStandardDetailPath(tenantId, standardId)}/onboarding-steps/${encodeURIComponent(stepType)}`;
}

export type AdminNavSectionId = 'tenants' | 'users' | 'management';

export type AdminModuleIcon = 'tenants' | 'users' | 'management' | 'organization';

export interface AdminWorkspaceModule {
  id: AdminNavSectionId;
  labelKey: string;
  href: string;
  icon: AdminModuleIcon;
}

export interface AdminWorkspaceContext {
  appRole: string | null | undefined;
  tenantId?: number | null;
}

/** Single source of truth for the growing administration hub and its workspace menu. */
export function getAdminWorkspaceModules({
  appRole,
  tenantId,
}: AdminWorkspaceContext): AdminWorkspaceModule[] {
  const modules: AdminWorkspaceModule[] = [];
  const normalizedRole = appRole?.trim().toUpperCase();

  if (normalizedRole === 'GLOBAL_ADMIN') {
    modules.push({
      id: 'tenants',
      labelKey: 'admin.sections.globalTenants',
      href: ADMIN_TENANTS_LIST_PATH,
      icon: 'tenants',
    });
  } else if (normalizedRole === 'TENANT_ADMIN' && tenantId != null && tenantId > 0) {
    modules.push({
      id: 'tenants',
      labelKey: 'admin.sections.myTenant',
      href: getAdminTenantDetailPath(tenantId),
      icon: 'tenants',
    });
  }

  if (normalizedRole === 'GLOBAL_ADMIN' || normalizedRole === 'TENANT_ADMIN') {
    modules.push({
      id: 'users',
      labelKey: 'admin.sections.userManagement',
      href: ADMIN_USERS_PATH,
      icon: 'users',
    });
    modules.push({
      id: 'management',
      labelKey: 'admin.sections.managementActivations',
      href: ADMIN_MANAGEMENT_PATH,
      icon: 'management',
    });
  }

  return modules;
}

/**
 * Active admin workspace in the sidebar (after picking a section).
 * On the hub (`/admin`) the shell provides no workspace menu, so this is typically `undefined`.
 */
export function resolveAdminNavSection(
  pathname: string,
  searchParams: URLSearchParams,
): AdminNavSectionId | undefined {
  if (searchParams.get('tab') === 'users') {
    return 'users';
  }
  const normalized =
    pathname.endsWith('/') && pathname.length > 1 ? pathname.slice(0, -1) : pathname;
  if (normalized === ADMIN_HUB_PATH) {
    return undefined;
  }
  if (normalized.startsWith(`${ADMIN_USERS_PATH}`)) {
    return 'users';
  }
  if (normalized.startsWith(`${ADMIN_MANAGEMENT_PATH}`)) {
    return 'management';
  }
  if (normalized.startsWith(`${ADMIN_TENANTS_LIST_PATH}`)) {
    return 'tenants';
  }
  return undefined;
}
