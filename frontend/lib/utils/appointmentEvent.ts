/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { type AppointmentDto, AppointmentType } from '@/types/appointment';
import { EventType } from '@/types/event';

export function eventTypeFromAppointmentType(type: AppointmentType): EventType {
  switch (type) {
    default:
      return EventType.SERVICE;
  }
}

export function sameScheduleInstant(
  isoA: string | undefined | null,
  isoB: string | undefined | null,
): boolean {
  if (!isoA || !isoB) return false;
  const ta = new Date(isoA).getTime();
  const tb = new Date(isoB).getTime();
  if (Number.isNaN(ta) || Number.isNaN(tb)) return false;
  return ta === tb;
}

export function hasAppointmentStarted(startIso: string): boolean {
  const t = new Date(startIso).getTime();
  if (Number.isNaN(t)) return false;
  return Date.now() >= t;
}

export function findAppointmentsMatchingEventSchedule(
  appointments: AppointmentDto[],
  params: { eventType: EventType; start: string; end?: string | null },
): AppointmentDto[] {
  const eventEnd = params.end ?? '';
  return appointments.filter(
    (a) =>
      !a.archived &&
      a.type === AppointmentType.SERVICE &&
      params.eventType === EventType.SERVICE &&
      sameScheduleInstant(a.start, params.start) &&
      sameScheduleInstant(a.end, eventEnd),
  );
}
