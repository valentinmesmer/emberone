/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type {
  OrganizationGroupTypeName,
  PermissionGroupTypeName,
  StandardContentItemDto,
  StandardDomainKey,
} from '@/lib/api/standards';
import { workflowTypeIdFromEnumKey } from '@/lib/utils/workflowLabels';
import { PermissionTypeEnum } from '@/types/permission';

export const ALL_PERMISSION_TYPES = Object.keys(PermissionTypeEnum).filter(
  (k) => Number.isNaN(Number(k)) && k !== 'ORGANIZATION_READ',
);

const ORG_GROUP_TYPES: OrganizationGroupTypeName[] = ['LEADERSHIP', 'ALL_MEMBERS', 'CUSTOM'];

function standardOrgGroupItemType(item: StandardContentItemDto): OrganizationGroupTypeName | null {
  const raw = item.attributes?.type ?? item.subtitle;
  if (raw === 'LEADERSHIP' || raw === 'ALL_MEMBERS' || raw === 'CUSTOM') {
    return raw;
  }
  return null;
}

export function isSystemStandardOrgGroupItem(item: StandardContentItemDto | undefined): boolean {
  if (!item) {
    return false;
  }
  const type = standardOrgGroupItemType(item);
  return type != null && type !== 'CUSTOM';
}

/** Function grants: leadership and custom org groups only (not all-members). */
export function standardOrgGroupSupportsFunctionGrants(
  item: StandardContentItemDto | undefined,
): boolean {
  if (!item) {
    return false;
  }
  const type = standardOrgGroupItemType(item);
  return type === 'LEADERSHIP' || type === 'CUSTOM';
}

export function availableOrgGroupTypesForCreate(
  items: StandardContentItemDto[],
): OrganizationGroupTypeName[] {
  const existingSystemTypes = new Set(
    items
      .filter((i) => i.domainKey === 'organization-groups')
      .map(standardOrgGroupItemType)
      .filter((t): t is OrganizationGroupTypeName => t === 'LEADERSHIP' || t === 'ALL_MEMBERS'),
  );
  return ORG_GROUP_TYPES.filter((t) => t === 'CUSTOM' || !existingSystemTypes.has(t));
}

export function standardPermissionGroupItemType(
  item: StandardContentItemDto,
): PermissionGroupTypeName | null {
  const raw = item.attributes?.type ?? item.subtitle;
  if (raw === 'ADMIN' || raw === 'STANDARD' || raw === 'CUSTOM') {
    return raw;
  }
  return null;
}

export function isSystemStandardPermissionGroupItem(
  item: StandardContentItemDto | undefined,
): boolean {
  if (!item) {
    return false;
  }
  const type = standardPermissionGroupItemType(item);
  return type != null && type !== 'CUSTOM';
}

/** Administration group is fixed (all permissions); not editable in the standard UI. */
export function isAdminStandardPermissionGroupItem(item: StandardContentItemDto): boolean {
  return standardPermissionGroupItemType(item) === 'ADMIN';
}

type TranslateFn = (key: string) => string;

function isEligibleWorkflowApproverOrgGroup(item: StandardContentItemDto): boolean {
  const type = standardOrgGroupItemType(item);
  return type === 'LEADERSHIP' || type === 'CUSTOM';
}

export function eligibleWorkflowApproverOrgGroups(
  items: StandardContentItemDto[],
): StandardContentItemDto[] {
  return items.filter(
    (i) => i.domainKey === 'organization-groups' && isEligibleWorkflowApproverOrgGroup(i),
  );
}

export function buildStandardTitleByKey(items: StandardContentItemDto[]) {
  const map = new Map<string, string>();
  items
    .filter((i) => i.domainKey === 'organization-groups')
    .forEach((i) => {
      map.set(`org:${i.configKey}`, i.title);
    });
  items
    .filter((i) => i.domainKey === 'permission-groups')
    .forEach((i) => {
      map.set(`perm:${i.configKey}`, i.title);
    });
  return map;
}

export function standardGroupTypeLabel(t: TranslateFn, key?: string | null) {
  return key ? t(`admin.standards.groupTypes.${key}`) : '';
}

export function standardPermissionGroupTypeLabel(t: TranslateFn, key?: string | null) {
  return key ? t(`admin.standards.permissionGroupTypes.${key}`) : '';
}

export function standardWorkflowTypeLabel(t: TranslateFn, key: string) {
  return t(`admin.standards.workflowTypes.${key}`);
}

export function standardWorkflowTypeId(item: StandardContentItemDto): number {
  return workflowTypeIdFromEnumKey(item.title) ?? 0;
}

export function standardPermissionLabel(t: TranslateFn, name: string) {
  return t(`permissions.${name}`);
}

export function standardOrgKeyLabel(t: TranslateFn, key: string, titleByKey: Map<string, string>) {
  return titleByKey.get(`org:${key}`) ?? standardGroupTypeLabel(t, key) ?? key;
}

export function findStandardItem(
  items: StandardContentItemDto[],
  domainKey: StandardDomainKey,
  configKey: string,
) {
  return items.find((i) => i.domainKey === domainKey && i.configKey === configKey);
}

export function getMappedPermissionGroupKeys(
  items: StandardContentItemDto[],
  orgGroupConfigKey: string,
): string[] {
  return items
    .filter((i) => i.domainKey === 'group-mappings')
    .filter((i) => (i.attributes?.orgGroupConfigKey ?? i.title) === orgGroupConfigKey)
    .map((i) => i.attributes?.permissionGroupConfigKey ?? i.subtitle ?? '')
    .filter((key) => key.length > 0);
}

export function getMappedPermissionGroupTitles(
  items: StandardContentItemDto[],
  orgGroupConfigKey: string,
): string[] {
  const titleByKey = buildStandardTitleByKey(items);
  return getMappedPermissionGroupKeys(items, orgGroupConfigKey).map(
    (key) => titleByKey.get(`perm:${key}`) ?? key,
  );
}

export function findNewOrgGroupConfigKey(
  beforeItems: StandardContentItemDto[],
  afterItems: StandardContentItemDto[],
  name: string,
): string | null {
  const beforeKeys = new Set(
    beforeItems.filter((i) => i.domainKey === 'organization-groups').map((i) => i.configKey),
  );
  const created = afterItems.find(
    (i) =>
      i.domainKey === 'organization-groups' && i.title === name && !beforeKeys.has(i.configKey),
  );
  return created?.configKey ?? null;
}
