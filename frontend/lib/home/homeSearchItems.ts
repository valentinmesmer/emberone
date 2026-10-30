/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { compareText } from '@/lib/i18n/locale';
import type { WorkspaceOrganization } from '@/lib/management/workspaceOrganizations';
import { getAccessibleMyAreaSections, MY_AREA_BASE_PATH } from '@/lib/my-area/myAreaSections';
import { MY_AREA_SETTINGS_SECTIONS } from '@/lib/my-area/settingsSections';
import {
  ADMIN_HUB_PATH,
  getAdminStandardsPath,
  getAdminTenantStandardsPath,
  getAdminWorkspaceModules,
} from '@/lib/navigation/adminWorkspace';
import { buildFlatOrgNavItems } from '@/lib/navigation/organizationNavItems';
import {
  canAccessUserManagement,
  getOrganizationUsersPath,
  getVisibleUserManagementSections,
  type UserManagementSectionId,
} from '@/lib/navigation/userManagementSections';
import { getOrganizationWorkspaceEntryPath } from '@/lib/utils';
import type { PermissionTypeEnum } from '@/types/permission';

export type HomeSearchItemCategory = 'myArea' | 'organization' | 'orgSection' | 'admin';

export type HomeSearchItem = {
  id: string;
  label: string;
  subtitle?: string;
  href: string;
  category: HomeSearchItemCategory;
  keywords?: string[];
  /** Lower = higher in suggestion list. */
  suggestionPriority?: number;
};

export type BuildHomeSearchItemsParams = {
  t: (key: string, params?: Record<string, string | number>) => string;
  hasEmberOneUserId: boolean;
  organizations: WorkspaceOrganization[];
  hasPermission: (orgId: number, permission: PermissionTypeEnum) => boolean;
  canAccessAdministration: boolean;
  appRole: string | null | undefined;
  authTenantId?: number | null;
};

/** Org hub tiles that already cover a user-management section label. */
const ORG_NAV_USER_MANAGEMENT_SECTION_IDS = new Set<UserManagementSectionId>([
  'workflows',
  'certifications',
  'qualifications',
  'absences',
]);

export const HOME_SEARCH_CATEGORY_ORDER: readonly HomeSearchItemCategory[] = [
  'organization',
  'orgSection',
  'myArea',
  'admin',
] as const;

/** Lower = ranked higher when match scores are equal. */
const HOME_SEARCH_CATEGORY_TIE_BREAK: Record<HomeSearchItemCategory, number> = {
  organization: 0,
  orgSection: 1,
  admin: 2,
  myArea: 3,
};

export const HOME_SEARCH_RESULT_LIMIT = 20;

export function getHomeSearchCategoryLabelKey(category: HomeSearchItemCategory): string {
  return `home.search.category.${category}`;
}

/**
 * Builds the home quick-search index from navigation config and already-known context
 * (permissions, org memberships, auth role). No extra API calls - dynamic content
 * search will be added via a dedicated search endpoint later.
 */
export function buildHomeSearchItems({
  t,
  hasEmberOneUserId,
  organizations,
  hasPermission,
  canAccessAdministration,
  appRole,
  authTenantId,
}: BuildHomeSearchItemsParams): HomeSearchItem[] {
  const items: HomeSearchItem[] = [];

  if (hasEmberOneUserId) {
    items.push({
      id: 'my-area-hub',
      label: t('home.navigation.personalData.title'),
      subtitle: t('home.search.category.myArea'),
      href: MY_AREA_BASE_PATH,
      category: 'myArea',
      suggestionPriority: 2,
    });

    for (const section of getAccessibleMyAreaSections(organizations.length > 0)) {
      items.push({
        id: `my-area-${section.id}`,
        label: t(section.labelKey),
        subtitle: t('home.search.category.myArea'),
        href: section.path,
        category: 'myArea',
      });
    }

    const settingsSubtitle = `${t('home.search.category.myArea')} · ${t('myArea.menu.settings')}`;
    for (const section of MY_AREA_SETTINGS_SECTIONS) {
      if (!section.path) continue;
      items.push({
        id: `my-area-settings-${section.id}`,
        label: t(section.labelKey),
        subtitle: settingsSubtitle,
        href: section.path,
        category: 'myArea',
        keywords: [t(section.descriptionKey)],
      });
    }
  }

  for (const org of organizations) {
    const navItems = buildFlatOrgNavItems(
      { id: org.id, name: org.name },
      '',
      t,
      hasPermission,
      'hub',
    );

    if (navItems.length === 0 && !canAccessUserManagement(org.id, hasPermission)) {
      continue;
    }

    items.push({
      id: `org-${org.id}`,
      label: org.name,
      subtitle: org.main
        ? `${t('home.search.category.organization')} · ${t('home.navigation.organizations.mainBadge')}`
        : t('home.search.category.organization'),
      href: getOrganizationWorkspaceEntryPath(org.id),
      category: 'organization',
      keywords: org.code ? [org.code] : [],
      suggestionPriority: org.main ? 1 : undefined,
    });

    for (const navItem of navItems) {
      items.push({
        id: navItem.id,
        label: navItem.label,
        subtitle: `${org.name} · ${navItem.hint}`,
        href: navItem.href,
        category: 'orgSection',
        keywords: [navItem.hint],
      });
    }

    if (canAccessUserManagement(org.id, hasPermission)) {
      const manageUsersLabel = t('organization.hub.navigation.manageUsers');
      const manageUsersSubtitle = `${org.name} · ${manageUsersLabel}`;

      for (const section of getVisibleUserManagementSections(org.id, hasPermission)) {
        if (ORG_NAV_USER_MANAGEMENT_SECTION_IDS.has(section.id)) {
          continue;
        }
        items.push({
          id: `org-${org.id}-user-mgmt-${section.id}`,
          label: t(section.labelKey),
          subtitle: manageUsersSubtitle,
          href: getOrganizationUsersPath(org.id),
          category: 'orgSection',
          keywords: [manageUsersLabel, t(section.labelKey)],
        });
      }
    }
  }

  if (canAccessAdministration) {
    const adminModules = getAdminWorkspaceModules({ appRole, tenantId: authTenantId });
    if (adminModules.length > 0) {
      items.push({
        id: 'admin-hub',
        label: t('home.navigation.administration.title'),
        subtitle: t('home.search.category.admin'),
        href: ADMIN_HUB_PATH,
        category: 'admin',
        suggestionPriority: 5,
      });

      for (const adminModule of adminModules) {
        items.push({
          id: `admin-${adminModule.id}`,
          label: t(adminModule.labelKey),
          subtitle: t('home.search.category.admin'),
          href: adminModule.href,
          category: 'admin',
        });
      }

      const normalizedRole = appRole?.trim().toUpperCase();
      if (normalizedRole === 'TENANT_ADMIN' && authTenantId != null && authTenantId > 0) {
        items.push({
          id: `admin-tenant-standards-${authTenantId}`,
          label: t('admin.tenantStandards.manageLink'),
          subtitle: `${t('home.search.category.admin')} · ${t('admin.tenantStandards.title')}`,
          href: getAdminTenantStandardsPath(authTenantId),
          category: 'admin',
          keywords: [t('admin.tenantStandards.title'), t('admin.tenantStandards.subtitle')],
        });
        items.push({
          id: `admin-org-standards-${authTenantId}`,
          label: t('admin.standards.manageOrgStandardsLink'),
          subtitle: `${t('home.search.category.admin')} · ${t('admin.standards.title')}`,
          href: getAdminStandardsPath(authTenantId),
          category: 'admin',
          keywords: [t('admin.standards.title'), t('admin.standards.subtitle')],
        });
      }
    }
  }

  return items;
}

function normalizeForSearch(value: string): string {
  return value.trim().toLowerCase().normalize('NFD').replace(/\p{M}/gu, '');
}

function scoreHomeSearchItemTokens(item: HomeSearchItem, normalizedToken: string): number {
  const label = normalizeForSearch(item.label);
  const subtitle = normalizeForSearch(item.subtitle ?? '');
  const keywords = (item.keywords ?? []).map(normalizeForSearch);

  if (label === normalizedToken) return 100;
  if (label.startsWith(normalizedToken)) return 80;
  if (keywords.some((keyword) => keyword === normalizedToken)) return 75;
  if (keywords.some((keyword) => keyword.startsWith(normalizedToken))) return 70;
  if (label.includes(normalizedToken)) return 60;
  if (keywords.some((keyword) => keyword.includes(normalizedToken))) return 50;
  if (subtitle.includes(normalizedToken)) return 40;
  return 0;
}

function scoreHomeSearchItem(item: HomeSearchItem, normalizedQuery: string): number {
  const tokens = normalizedQuery.split(/\s+/).filter(Boolean);
  if (tokens.length <= 1) {
    return scoreHomeSearchItemTokens(item, normalizedQuery);
  }

  const tokenScores = tokens.map((token) => scoreHomeSearchItemTokens(item, token));
  if (tokenScores.some((score) => score === 0)) {
    return 0;
  }

  return Math.round(tokenScores.reduce((sum, score) => sum + score, 0) / tokenScores.length);
}

function compareHomeSearchItems(
  a: HomeSearchItem,
  b: HomeSearchItem,
  normalizedQuery: string,
): number {
  const scoreDiff =
    scoreHomeSearchItem(b, normalizedQuery) - scoreHomeSearchItem(a, normalizedQuery);
  if (scoreDiff !== 0) return scoreDiff;

  const categoryDiff =
    HOME_SEARCH_CATEGORY_TIE_BREAK[a.category] - HOME_SEARCH_CATEGORY_TIE_BREAK[b.category];
  if (categoryDiff !== 0) return categoryDiff;

  return compareText(a.label, b.label);
}

export function filterHomeSearchItems(
  items: HomeSearchItem[],
  query: string,
  limit = HOME_SEARCH_RESULT_LIMIT,
): HomeSearchItem[] {
  const normalizedQuery = normalizeForSearch(query);
  if (!normalizedQuery) return [];

  return items
    .filter((item) => scoreHomeSearchItem(item, normalizedQuery) > 0)
    .sort((a, b) => compareHomeSearchItems(a, b, normalizedQuery))
    .slice(0, limit);
}

export function getHomeSearchSuggestions(items: HomeSearchItem[], limit = 5): HomeSearchItem[] {
  return [...items]
    .filter((item) => item.suggestionPriority != null)
    .sort((a, b) => (a.suggestionPriority ?? 99) - (b.suggestionPriority ?? 99))
    .slice(0, limit);
}

export type HomeSearchResultGroup = {
  category: HomeSearchItemCategory;
  items: HomeSearchItem[];
};

export function groupHomeSearchResults(items: HomeSearchItem[]): HomeSearchResultGroup[] {
  const groups: HomeSearchResultGroup[] = [];

  for (const category of HOME_SEARCH_CATEGORY_ORDER) {
    const categoryItems = items.filter((item) => item.category === category);
    if (categoryItems.length > 0) {
      groups.push({ category, items: categoryItems });
    }
  }

  return groups;
}
