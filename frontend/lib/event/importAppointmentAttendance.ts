/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { appointmentApi } from '@/lib/api/appointment';
import { eventApi } from '@/lib/api/event';
import { fetchAllPageContent } from '@/lib/api/paged';
import { eventTypeToUrlSegment } from '@/lib/utils/event';
import type { EventType } from '@/types/event';

/**
 * Adds event attendance for each user who answered "Ja" on the appointment (skips duplicates).
 */
export async function importAppointmentYesResponsesAsEventAttendance(
  organizationId: number,
  eventType: EventType,
  eventId: number,
  appointmentId: number,
): Promise<number> {
  const category = eventTypeToUrlSegment(eventType);
  const [stats, existingRows] = await Promise.all([
    appointmentApi.getParticipationStats(organizationId, appointmentId),
    fetchAllPageContent((page) =>
      eventApi.listEventAttendances(organizationId, category, eventId, { page }),
    ),
  ]);
  const existingUserIds = new Set(existingRows.map((r) => r.userId));
  const yesParticipants = stats.yesParticipants ?? [];
  let added = 0;
  for (const p of yesParticipants) {
    if (existingUserIds.has(p.userId)) continue;
    await eventApi.createEventAttendance(organizationId, category, eventId, { userId: p.userId });
    existingUserIds.add(p.userId);
    added += 1;
  }
  return added;
}
