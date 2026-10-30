/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { PermissionTypeEnum } from './permission';

/** An open management activation of the current user (display and client gating, not the access check). */
export interface ManagementActivationDto {
  id: number;
  managingOrganizationId: number;
  managingOrganizationName: string | null;
  targetOrganizationId: number;
  targetOrganizationName: string | null;
  activatedAt: string;
  validUntil: string;
  permissionTypes: PermissionTypeEnum[];
  reason: string;
}

/** A target the caller may open through one managing organization, with the largest scope and duration. */
export interface ManagementTargetOrganizationDto {
  organizationId: number;
  organizationName: string;
  managingOrganizationId: number;
  managingOrganizationName: string;
  maxDurationMinutes: number;
  permissionTypes: PermissionTypeEnum[];
}

export interface ManagementActivationRequest {
  managingOrganizationId: number;
  targetOrganizationId: number;
  durationMinutes: number;
  permissionTypes: PermissionTypeEnum[];
  reason: string;
}
