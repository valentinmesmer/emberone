/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useAuth } from '@/lib/auth/AuthContext';
import { buildTenantOrganizationMembershipsTopic, useRealtimeTopic } from '@/lib/realtime';

/**
 * Subscribes to tenant org-membership changes app-wide so organization user
 * lists and my-organizations stay in sync after Eintritt / leave / main-change.
 */
export function OrganizationMembershipRealtimeSync() {
  const { isAuthenticated, tenantId } = useAuth();
  const topic =
    isAuthenticated && tenantId != null ? buildTenantOrganizationMembershipsTopic(tenantId) : null;
  useRealtimeTopic(topic);
  return null;
}
