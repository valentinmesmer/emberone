/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { defineList, toListQuery } from './defineList';
import type { ListQuery } from './types';

export const OrganizationMemberListAudienceParam = {
  MEMBERS: 'MEMBERS',
  INVITED: 'INVITED',
} as const;

export type OrganizationMemberListAudienceParam =
  (typeof OrganizationMemberListAudienceParam)[keyof typeof OrganizationMemberListAudienceParam];

const audienceFilter = {
  kind: 'multiSelect',
  key: 'audience',
  labelKey: 'userManagement.listFilter.audience.label',
  countsInMenubarWhenSelected: true,
  options: [
    {
      value: OrganizationMemberListAudienceParam.MEMBERS,
      labelKey: 'userManagement.listFilter.active',
    },
    {
      value: OrganizationMemberListAudienceParam.INVITED,
      labelKey: 'userManagement.listFilter.invited',
    },
  ],
} as const;

/**
 * Org `/organizations/.../users` - search + menubar (Aktive / Eingeladene; leer = alle).
 * `audience` steuert nur, welche List-GETs geladen werden (nicht an die APIs senden).
 */
export const organizationMemberListDefinition = defineList({
  id: 'organizationMember',
  search: { debounceMs: 300 },
  sort: {
    options: [
      { value: 'lastName,asc', labelKey: 'userManagement.list.sort.lastNameAsc' },
      { value: 'lastName,desc', labelKey: 'userManagement.list.sort.lastNameDesc' },
      { value: 'firstName,asc', labelKey: 'userManagement.list.sort.firstNameAsc' },
      { value: 'firstName,desc', labelKey: 'userManagement.list.sort.firstNameDesc' },
    ],
  },
  sheetLayout: 'menubar',
  filters: [audienceFilter],
});

export type OrganizationMemberListVisibility = {
  showMembers: boolean;
  showInvitations: boolean;
  showSectionHeadings: boolean;
};

function normalizeAudienceSelection(query: ListQuery): readonly string[] {
  const raw = query.audience;
  if (Array.isArray(raw)) {
    return raw.filter((value): value is string => typeof value === 'string');
  }
  if (raw === OrganizationMemberListAudienceParam.MEMBERS) {
    return [OrganizationMemberListAudienceParam.MEMBERS];
  }
  if (raw === OrganizationMemberListAudienceParam.INVITED) {
    return [OrganizationMemberListAudienceParam.INVITED];
  }
  if (raw === 'ALL') {
    return [];
  }
  return [];
}

export function resolveOrganizationMemberListVisibility(
  query: ListQuery,
): OrganizationMemberListVisibility {
  const selected = normalizeAudienceSelection(query);
  if (selected.length === 0) {
    return { showMembers: true, showInvitations: true, showSectionHeadings: true };
  }
  const showMembers = selected.includes(OrganizationMemberListAudienceParam.MEMBERS);
  const showInvitations = selected.includes(OrganizationMemberListAudienceParam.INVITED);
  return {
    showMembers,
    showInvitations,
    showSectionHeadings: showMembers && showInvitations,
  };
}

/** Drops UI-only `audience` before calling member or invitation list APIs. */
export function toOrganizationMemberApiListQuery(query: ListQuery): ListQuery {
  const { audience: _audience, ...apiQuery } = query;
  return apiQuery;
}

export function toOrganizationMemberListQuery(input: Parameters<typeof toListQuery>[1]): ListQuery {
  return toOrganizationMemberApiListQuery(toListQuery(organizationMemberListDefinition, input));
}
