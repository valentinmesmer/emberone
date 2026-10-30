/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { ListRowTone } from '@/components/ui/tones';
import {
  type AppointmentDto,
  AppointmentResponseMode,
  AppointmentResponseValue,
  type MyAppointmentResponseDto,
} from '@/types/appointment';

/** My own response colors the row (green = yes, red = no/cancelled, neutral = uncertain). */
export function getAppointmentResponseTone(
  appointment: AppointmentDto,
  myResponse: MyAppointmentResponseDto | undefined,
): ListRowTone {
  if (appointment.responseMode === AppointmentResponseMode.NONE) {
    return 'default';
  }
  const value = myResponse?.value;
  if (value === AppointmentResponseValue.YES) return 'success';
  if (value === AppointmentResponseValue.NO) return 'danger';
  if (value === AppointmentResponseValue.UNCERTAIN) return 'neutral';
  return 'default';
}

/**
 * List row tone: response status wins; otherwise unread uses `info`
 * (not `active`/red - that reads as „Nein“ next to response colors).
 */
export function getAppointmentListTone(
  appointment: AppointmentDto,
  myResponse: MyAppointmentResponseDto | undefined,
): ListRowTone {
  const responseTone = getAppointmentResponseTone(appointment, myResponse);
  if (responseTone !== 'default') return responseTone;
  return appointment.unread ? 'info' : 'default';
}
