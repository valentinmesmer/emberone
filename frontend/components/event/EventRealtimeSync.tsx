/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useAuth } from '@/lib/auth/AuthContext';
import { buildTenantEventsTopic, useRealtimeTopic } from '@/lib/realtime';

/**
 * Subscribes to tenant event changes app-wide so lists stay in sync.
 */
export function EventRealtimeSync() {
  const { isAuthenticated, tenantId } = useAuth();
  const topic = isAuthenticated && tenantId != null ? buildTenantEventsTopic(tenantId) : null;
  useRealtimeTopic(topic);
  return null;
}
