/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

// Organizations the current user works in: memberships and open management activations.
// Pure functions, so the origin of an organization is decided in one place.

import { APP_LOCALE_TAG } from '@/lib/i18n/locale';
import type { ManagementActivationDto } from '@/types/management';
import type { PermissionTypeEnum } from '@/types/permission';
import type { UserOrganizationMembershipDto } from '@/types/user';

/** Where the rights and the master data of an organization come from. */
export type OrganizationSource = 'membership' | 'management';

export interface WorkspaceOrganization {
  id: number;
  name: string;
  /** Organization code; null for managed organizations, which the caller has no membership in. */
  code: string | null;
  main: boolean;
  source: OrganizationSource;
  /**
   * Open activation covering the organization. Always set for `management`; a member of the
   * organization carries it too while one is open, since it can grant additional rights.
   */
  activation: ManagementActivationDto | null;
}

function isOpen(activation: ManagementActivationDto, nowMs: number): boolean {
  return Date.parse(activation.validUntil) > nowMs;
}

/** Open activation per target. A membership of the same organization always wins. */
function openActivationsByTarget(
  activations: readonly ManagementActivationDto[],
  nowMs: number,
): Map<number, ManagementActivationDto> {
  const byTarget = new Map<number, ManagementActivationDto>();
  for (const activation of activations) {
    if (!isOpen(activation, nowMs)) continue;
    const current = byTarget.get(activation.targetOrganizationId);
    if (!current || Date.parse(activation.validUntil) > Date.parse(current.validUntil)) {
      byTarget.set(activation.targetOrganizationId, activation);
    }
  }
  return byTarget;
}

/** Local clock time until which a managed organization stays open, e.g. `17:30`. */
export function formatManagementUntil(validUntil: string): string {
  return new Date(validUntil).toLocaleTimeString(APP_LOCALE_TAG, {
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function buildWorkspaceOrganizations(
  memberships: readonly UserOrganizationMembershipDto[],
  activations: readonly ManagementActivationDto[],
  nowMs: number,
): WorkspaceOrganization[] {
  const openActivations = openActivationsByTarget(activations, nowMs);
  const membershipEntries: WorkspaceOrganization[] = [];
  const memberOf = new Set<number>();
  for (const membership of memberships) {
    if (!membership.organization) continue;
    memberOf.add(membership.organization.id);
    membershipEntries.push({
      id: membership.organization.id,
      name: membership.organization.name,
      code: membership.organization.organizationCode,
      main: membership.main,
      source: 'membership',
      activation: openActivations.get(membership.organization.id) ?? null,
    });
  }

  const managedEntries: WorkspaceOrganization[] = [];
  for (const [targetId, activation] of openActivations) {
    if (memberOf.has(targetId)) continue;
    managedEntries.push({
      id: targetId,
      name: activation.targetOrganizationName ?? '',
      code: null,
      main: false,
      source: 'management',
      activation,
    });
  }

  return [...membershipEntries, ...managedEntries];
}

/**
 * Source of an organization for org data calls. Membership wins; management only when the
 * organization is not a membership and an open activation covers it. Unknown ids fall back to
 * membership, which keeps the previous behaviour (the backend answers with not found).
 */
export function resolveOrganizationSource(
  organizationId: number,
  memberships: readonly UserOrganizationMembershipDto[],
  activations: readonly ManagementActivationDto[],
  nowMs: number,
): OrganizationSource {
  if (memberships.some((membership) => membership.organization?.id === organizationId)) {
    return 'membership';
  }
  return openActivationsByTarget(activations, nowMs).has(organizationId)
    ? 'management'
    : 'membership';
}

/** Permissions granted through open activations, for client gating only. The backend decides. */
export function managedPermissionsByOrganization(
  activations: readonly ManagementActivationDto[],
  nowMs: number,
): Map<number, PermissionTypeEnum[]> {
  const byTarget = new Map<number, PermissionTypeEnum[]>();
  for (const [targetId, activation] of openActivationsByTarget(activations, nowMs)) {
    byTarget.set(targetId, activation.permissionTypes);
  }
  return byTarget;
}
