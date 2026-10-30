/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useAuth } from '@/lib/auth/AuthContext';
import { buildTenantAppointmentsTopic, useRealtimeTopic } from '@/lib/realtime';

/**
 * Subscribes to tenant appointment changes app-wide so lists, badges, and
 * unread counts stay in sync (same role as the todos user queue).
 */
export function AppointmentRealtimeSync() {
  const { isAuthenticated, tenantId } = useAuth();
  const topic = isAuthenticated && tenantId != null ? buildTenantAppointmentsTopic(tenantId) : null;
  useRealtimeTopic(topic);
  return null;
}
