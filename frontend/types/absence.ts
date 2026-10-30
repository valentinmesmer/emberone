/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { AbsenceReason } from './enums/absenceReason';
import type { PageResponse } from './page';

export interface UserAbsenceDto {
  id: number;
  userId: number;
  organizationIds: number[];
  startDate: string;
  endDate: string;
  reason: AbsenceReason;
  comment?: string;
}

export interface CreateUserAbsenceRequest {
  organizationIds: number[];
  startDate: string;
  endDate: string;
  reason: AbsenceReason;
  comment?: string;
}

export type UpdateUserAbsenceRequest = CreateUserAbsenceRequest;

export type UserAbsencePageDto = PageResponse<UserAbsenceDto>;
