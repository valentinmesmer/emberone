/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { addHoursToLocalDateTime, getNextMinuteDateTime } from '@/lib/utils/date';
import { anchorWeekday, parseRecurrenceRule, weekdayFromStart } from '@/lib/utils/recurrence';
import {
  DEFAULT_RESPONSE_DEADLINE_MINUTES,
  formValueToMinutes,
  minutesToFormValue,
} from '@/lib/utils/responseDeadline';
import {
  type AppointmentDto,
  AppointmentResponseMode,
  AppointmentType,
  AudienceScope,
} from '@/types/appointment';
import type { AppointmentFormState } from './AppointmentFormFields';

/** Optional rules: create requires a future start; edit blocks moving a still-future occurrence into the past. */
export type AppointmentFormValidityPolicy =
  | { mode: 'create' }
  | { mode: 'edit'; originalStart: string };

function toMinuteTimestamp(isoLocal: string): number {
  const d = new Date(isoLocal);
  if (Number.isNaN(d.getTime())) return NaN;
  d.setSeconds(0, 0);
  d.setMilliseconds(0);
  return d.getTime();
}

function nowTruncatedToMinuteTimestamp(): number {
  const d = new Date();
  d.setSeconds(0, 0);
  d.setMilliseconds(0);
  return d.getTime();
}

/** Minute resolution; end must be strictly after the current local minute (aligned with backend). */
export function isAppointmentEndInFuture(isoEnd: string): boolean {
  const endT = toMinuteTimestamp(isoEnd);
  const nowT = nowTruncatedToMinuteTimestamp();
  return !Number.isNaN(endT) && endT > nowT;
}

export function isAppointmentStartValidForEdit(isoStart: string, originalStart: string): boolean {
  const startT = toMinuteTimestamp(isoStart);
  const origT = toMinuteTimestamp(originalStart);
  const nowT = nowTruncatedToMinuteTimestamp();
  if (Number.isNaN(startT) || Number.isNaN(origT)) return false;
  const originalWasFuture = origT > nowT;
  if (originalWasFuture && startT <= nowT) return false;
  return true;
}

export function isAudienceRestricted(audienceScope: AudienceScope): boolean {
  return audienceScope === AudienceScope.SELECTED_GROUPS;
}

export function toggleAudienceGroupId(groupIds: number[], groupId: number): number[] {
  return groupIds.includes(groupId)
    ? groupIds.filter((id) => id !== groupId)
    : [...groupIds, groupId];
}

export function isAppointmentFormValid(
  form: AppointmentFormState,
  policy?: AppointmentFormValidityPolicy,
): boolean {
  if (!form.name.trim() || !form.start || !form.end || !form.type) {
    return false;
  }
  if (form.end < form.start) {
    return false;
  }
  if (policy?.mode === 'create' && !isAppointmentEndInFuture(form.end)) {
    return false;
  }
  if (
    policy?.mode === 'edit' &&
    !isAppointmentStartValidForEdit(form.start, policy.originalStart)
  ) {
    return false;
  }
  if (!form.responseMode || !form.audienceScope) {
    return false;
  }
  if (form.audienceScope === AudienceScope.SELECTED_GROUPS && form.audienceGroupIds.length === 0) {
    return false;
  }
  if (form.responseDeadlineBeforeAmount < 0) {
    return false;
  }
  if (
    form.recurrence.enabled &&
    form.recurrence.frequency === 'MONTHLY' &&
    form.recurrence.monthlyType === 'NTH_WEEKDAY' &&
    (form.recurrence.monthlyPositions.length === 0 || form.recurrence.monthlyWeekdays.length === 0)
  ) {
    return false;
  }
  if (form.recurrence.enabled && form.recurrence.interval < 1) {
    return false;
  }
  return true;
}

function formatLocationFromForm(form: Pick<AppointmentFormState, 'location'>): string | undefined {
  const trimmed = form.location.trim();
  return trimmed || undefined;
}

function locationFieldsFromStored(
  location?: string | null,
): Pick<AppointmentFormState, 'location'> {
  return { location: location?.trim() ?? '' };
}

const defaultDeadlineFormValue = minutesToFormValue(DEFAULT_RESPONSE_DEADLINE_MINUTES);

export function getDefaultAppointmentForm(): AppointmentFormState {
  const start = getNextMinuteDateTime();
  return {
    name: '',
    type: AppointmentType.SERVICE,
    start,
    end: addHoursToLocalDateTime(start, 2),
    location: '',
    description: '',
    responseMode: AppointmentResponseMode.YES_NO,
    responseDeadlineBeforeAmount: defaultDeadlineFormValue.amount,
    responseDeadlineBeforeUnit: defaultDeadlineFormValue.unit,
    audienceScope: AudienceScope.ALL,
    audienceGroupIds: [],
    recurrence: {
      enabled: false,
      frequency: 'WEEKLY',
      interval: 1,
      seriesEnd: null,
      weekDays: [anchorWeekday(start)],
      monthlyType: 'DAY_OF_MONTH',
      monthlyWeekdays: [weekdayFromStart(start)],
      monthlyPositions: [1],
    },
  };
}

function recurrenceFromAppointment(
  appointment: AppointmentDto,
): AppointmentFormState['recurrence'] {
  if (appointment.seriesId && appointment.recurrenceRule) {
    return parseRecurrenceRule(
      appointment.recurrenceRule,
      appointment.seriesEnd ?? null,
      appointment.start,
    );
  }

  return {
    enabled: false,
    frequency: 'WEEKLY',
    interval: 1,
    seriesEnd: null,
    weekDays: [anchorWeekday(appointment.start)],
    monthlyType: 'DAY_OF_MONTH',
    monthlyWeekdays: [weekdayFromStart(appointment.start)],
    monthlyPositions: [1],
  };
}

export function formFromAppointment(appointment: AppointmentDto): AppointmentFormState {
  const deadlineFormValue = minutesToFormValue(appointment.responseDeadlineMinutesBefore ?? 0);

  return {
    name: appointment.name,
    type: appointment.type,
    start: appointment.start,
    end: appointment.end,
    ...locationFieldsFromStored(appointment.location),
    description: appointment.description ?? '',
    responseMode: appointment.responseMode,
    responseDeadlineBeforeAmount: deadlineFormValue.amount,
    responseDeadlineBeforeUnit: deadlineFormValue.unit,
    audienceScope: appointment.audienceScope,
    audienceGroupIds: appointment.audienceGroupIds ?? [],
    recurrence: recurrenceFromAppointment(appointment),
  };
}

export function buildAppointmentPayloadFields(form: AppointmentFormState) {
  return {
    location: formatLocationFromForm(form),
    description: form.description.trim() || undefined,
    responseDeadlineMinutesBefore: formValueToMinutes(
      form.responseDeadlineBeforeAmount,
      form.responseDeadlineBeforeUnit,
    ),
  };
}
