/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type {
  FunctionGrantMembershipType,
  FunctionGrantRequest,
  StandardContentItemDto,
} from '@/lib/api/standards';
import { compareText } from '@/lib/i18n/locale';
export const FUNCTION_GRANT_MEMBERSHIP_TYPES: FunctionGrantMembershipType[] = [
  'LEADER',
  'DEPUTY_LEADER',
  'MEMBER',
];

export function dutyFunctionGrantItems(items: StandardContentItemDto[]): StandardContentItemDto[] {
  return items.filter((item) => item.domainKey === 'duty-functions');
}

export function grantItemToRequest(item: StandardContentItemDto): FunctionGrantRequest | null {
  const membershipType = item.attributes.membershipType?.trim();
  if (
    !membershipType ||
    !FUNCTION_GRANT_MEMBERSHIP_TYPES.includes(membershipType as FunctionGrantMembershipType)
  ) {
    return null;
  }
  const organizationGroupConfigKey = item.attributes.organizationGroupConfigKey?.trim();
  const functionConfigKey = item.attributes.functionConfigKey?.trim();
  if (!organizationGroupConfigKey || !functionConfigKey) {
    return null;
  }
  return {
    organizationGroupConfigKey,
    membershipType: membershipType as FunctionGrantMembershipType,
    functionConfigKey,
  };
}

export function grantItemsToRequests(items: StandardContentItemDto[]): FunctionGrantRequest[] {
  return dutyFunctionGrantItems(items)
    .map(grantItemToRequest)
    .filter((grant): grant is FunctionGrantRequest => grant != null);
}

function orgGroupItemByConfigKey(
  items: StandardContentItemDto[],
  organizationGroupConfigKey: string,
): StandardContentItemDto | undefined {
  return items.find(
    (item) =>
      item.domainKey === 'organization-groups' && item.configKey === organizationGroupConfigKey,
  );
}

function orgGroupSupportsFunctionGrants(item: StandardContentItemDto | undefined): boolean {
  const raw = item?.attributes?.type ?? item?.subtitle;
  return raw === 'LEADERSHIP' || raw === 'CUSTOM';
}

export function isEligibleFunctionGrantOrgGroup(
  items: StandardContentItemDto[],
  organizationGroupConfigKey: string,
): boolean {
  return orgGroupSupportsFunctionGrants(orgGroupItemByConfigKey(items, organizationGroupConfigKey));
}

export function filterFunctionGrantsToEligibleOrgGroups(
  items: StandardContentItemDto[],
  grants: FunctionGrantRequest[],
): FunctionGrantRequest[] {
  return grants.filter((grant) =>
    isEligibleFunctionGrantOrgGroup(items, grant.organizationGroupConfigKey),
  );
}

function compareGrants(a: FunctionGrantRequest, b: FunctionGrantRequest): number {
  const groupCmp = compareText(a.organizationGroupConfigKey, b.organizationGroupConfigKey);
  if (groupCmp !== 0) return groupCmp;
  return (
    FUNCTION_GRANT_MEMBERSHIP_TYPES.indexOf(a.membershipType) -
    FUNCTION_GRANT_MEMBERSHIP_TYPES.indexOf(b.membershipType)
  );
}

export function grantsByFunctionConfigKey(
  items: StandardContentItemDto[],
): Map<string, FunctionGrantRequest[]> {
  const map = new Map<string, FunctionGrantRequest[]>();
  for (const grant of grantItemsToRequests(items)) {
    const list = map.get(grant.functionConfigKey) ?? [];
    list.push(grant);
    map.set(grant.functionConfigKey, list);
  }
  for (const list of map.values()) {
    list.sort(compareGrants);
  }
  return map;
}

export function grantSlotKey(
  organizationGroupConfigKey: string,
  membershipType: FunctionGrantMembershipType,
): string {
  return `${organizationGroupConfigKey}|${membershipType}`;
}

export function grantMatchesOrgGroupSlot(
  grant: FunctionGrantRequest,
  organizationGroupConfigKey: string,
  membershipType: FunctionGrantMembershipType,
): boolean {
  return (
    grant.organizationGroupConfigKey === organizationGroupConfigKey &&
    grant.membershipType === membershipType
  );
}

export type OrgGroupFunctionGrantBySlot = Partial<Record<FunctionGrantMembershipType, string>>;

export function readOrgGroupFunctionGrantsFromItems(
  items: StandardContentItemDto[],
  organizationGroupConfigKey: string,
): OrgGroupFunctionGrantBySlot {
  const slots: OrgGroupFunctionGrantBySlot = {};
  for (const grant of grantItemsToRequests(items)) {
    if (grant.organizationGroupConfigKey !== organizationGroupConfigKey) continue;
    slots[grant.membershipType] = grant.functionConfigKey;
  }
  return slots;
}

export function mergeOrgGroupGrantsIntoAll(
  items: StandardContentItemDto[],
  organizationGroupConfigKey: string,
  slots: OrgGroupFunctionGrantBySlot,
): FunctionGrantRequest[] {
  let grants = grantItemsToRequests(items);
  for (const slot of FUNCTION_GRANT_MEMBERSHIP_TYPES) {
    const functionConfigKey = slots[slot]?.trim() ?? '';
    grants = replaceGrantAtSlot(
      grants,
      organizationGroupConfigKey,
      slot,
      functionConfigKey.length > 0 ? functionConfigKey : null,
    );
  }
  return grants;
}

export function orgGroupFunctionGrantsEqual(
  a: OrgGroupFunctionGrantBySlot,
  b: OrgGroupFunctionGrantBySlot,
): boolean {
  for (const slot of FUNCTION_GRANT_MEMBERSHIP_TYPES) {
    if ((a[slot] ?? '') !== (b[slot] ?? '')) return false;
  }
  return true;
}

export function replaceGrantAtSlot(
  grants: FunctionGrantRequest[],
  organizationGroupConfigKey: string,
  membershipType: FunctionGrantMembershipType,
  functionConfigKey: string | null,
): FunctionGrantRequest[] {
  const next = grants.filter(
    (grant) => !grantMatchesOrgGroupSlot(grant, organizationGroupConfigKey, membershipType),
  );
  if (functionConfigKey) {
    next.push({
      organizationGroupConfigKey,
      membershipType,
      functionConfigKey,
    });
  }
  return next;
}
