/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import {
  getAdminTenantDetailPath,
  getAdminTenantStandardsPath,
} from '@/lib/navigation/adminWorkspace';

export function getAdminManagementActivationsPath(): string {
  return '/admin/management';
}

export function getAdminManagementActivationDetailPath(activationId: number): string {
  return `${getAdminManagementActivationsPath()}/${activationId}`;
}

export function getAdminManagementAudienceDetailPath(
  tenantId: number,
  organizationId: number,
  entryId: number,
): string {
  return `${getAdminOrganizationDetailPath(tenantId, organizationId)}/management/${entryId}`;
}

export function getAdminUsersPath(): string {
  return '/admin/users';
}

export function getAdminUserDetailPath(userId: number): string {
  return `${getAdminUsersPath()}/${userId}`;
}

export function getAdminTenantWorkflowDetailPath(tenantId: number, workflowTypeId: number): string {
  return `${getAdminTenantDetailPath(tenantId)}/workflows/${workflowTypeId}`;
}

export { getAdminTenantStandardsPath };

function getAdminOrganizationTypesPath(tenantId: number): string {
  return `${getAdminTenantStandardsPath(tenantId)}/organization-types`;
}

export function getAdminOrganizationTypeDetailPath(tenantId: number, typeId: number): string {
  return `${getAdminOrganizationTypesPath(tenantId)}/${typeId}`;
}

export function getAdminTenantCertificationDetailPath(tenantId: number, configKey: string): string {
  return `${getAdminTenantStandardsPath(tenantId)}/certifications/${encodeURIComponent(configKey)}`;
}

export function getAdminTenantTrainingCourseTypeDetailPath(
  tenantId: number,
  configKey: string,
): string {
  return `${getAdminTenantStandardsPath(tenantId)}/training-courses/${encodeURIComponent(configKey)}`;
}

export function getAdminTenantDrivingLicenseDefinitionDetailPath(
  tenantId: number,
  configKey: string,
): string {
  return `${getAdminTenantStandardsPath(tenantId)}/driving-licenses/${encodeURIComponent(configKey)}`;
}

export function getAdminOrganizationDetailPath(tenantId: number, organizationId: number): string {
  return `/admin/tenants/${tenantId}/organizations/${organizationId}`;
}

export function getAdminRecordedOnboardingPath(
  tenantId: number,
  organizationId: number,
  invitationId: number,
  options?: { membershipId?: number },
): string {
  const base = `${getAdminOrganizationDetailPath(tenantId, organizationId)}/onboarding/${invitationId}`;
  if (options?.membershipId != null) {
    return `${base}?membershipId=${options.membershipId}`;
  }
  return base;
}

export function getAdminLeadershipGroupMembershipDetailPath(
  tenantId: number,
  organizationId: number,
  membershipId: number,
): string {
  return `${getAdminOrganizationDetailPath(tenantId, organizationId)}/leadership/memberships/${membershipId}`;
}
