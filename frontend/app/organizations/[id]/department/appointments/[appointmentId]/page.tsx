/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useParams } from 'next/navigation';
import { AppointmentDetailView } from '@/components/appointment/AppointmentDetailView';

export default function AppointmentDetailPage() {
  const params = useParams();
  const orgId = params?.id ? parseInt(params.id as string, 10) : null;
  const appointmentId = params?.appointmentId ? parseInt(params.appointmentId as string, 10) : null;

  if (!orgId || !appointmentId || Number.isNaN(orgId) || Number.isNaN(appointmentId)) {
    return null;
  }

  return <AppointmentDetailView organizationId={orgId} appointmentId={appointmentId} />;
}
