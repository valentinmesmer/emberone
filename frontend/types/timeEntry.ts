/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { OrganizationGroupType } from './organizationGroup';
import type { PageResponse } from './page';

export interface UserTimeEntryDto {
  id: number;
  userId: number;
  organizationId: number;
  organizationName: string;
  organizationGroupId?: number;
  organizationGroupName?: string;
  organizationGroupType?: OrganizationGroupType;
  startTime: string;
  endTime?: string;
  description?: string;
}

export interface CreateUserTimeEntryRequest {
  organizationId: number;
  organizationGroupId?: number;
  startTime: string;
  endTime?: string;
  description?: string;
}

export type UpdateUserTimeEntryRequest = CreateUserTimeEntryRequest;

export type UserTimeEntryPageDto = PageResponse<UserTimeEntryDto>;
