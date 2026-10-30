/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

// Permission API endpoints

import type { UserPermissionDto } from '@/types/permission';
import { apiClient } from './client';

export const permissionApi = {
  async getSelfPermissions(): Promise<UserPermissionDto> {
    return apiClient.get<UserPermissionDto>('/api/v1/self/permissions');
  },
};
