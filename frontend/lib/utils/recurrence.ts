/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { ICalWeekday, RecurrenceFormState } from '@/types/appointment';

const WEEKDAY_FROM_JS: readonly ICalWeekday[] = ['SU', 'MO', 'TU', 'WE', 'TH', 'FR', 'SA'];

export const ICAL_WEEKDAYS: readonly ICalWeekday[] = ['MO', 'TU', 'WE', 'TH', 'FR', 'SA', 'SU'];

export const MONTHLY_POSITIONS = [1, 2, 3, 4] as const;

export function weekdayFromStart(isoLocal: string): ICalWeekday {
  return WEEKDAY_FROM_JS[new Date(isoLocal).getDay()];
}

export function anchorWeekday(seriesStart: string): ICalWeekday {
  return weekdayFromStart(seriesStart);
}

export function ensureAnchorWeekday(weekDays: ICalWeekday[], seriesStart: string): ICalWeekday[] {
  const anchor = anchorWeekday(seriesStart);
  return weekDays.includes(anchor) ? weekDays : [...weekDays, anchor];
}

export function toggleWeekdaySelection(
  weekDays: ICalWeekday[],
  day: ICalWeekday,
  seriesStart: string,
): ICalWeekday[] {
  if (day === anchorWeekday(seriesStart)) {
    return weekDays;
  }
  const next = weekDays.includes(day) ? weekDays.filter((d) => d !== day) : [...weekDays, day];
  return ensureAnchorWeekday(next, seriesStart);
}

function formatUntilUtc(isoLocal: string): string {
  const date = new Date(isoLocal);
  const pad = (n: number) => String(n).padStart(2, '0');
  return (
    date.getUTCFullYear() +
    pad(date.getUTCMonth() + 1) +
    pad(date.getUTCDate()) +
    'T' +
    pad(date.getUTCHours()) +
    pad(date.getUTCMinutes()) +
    pad(date.getUTCSeconds()) +
    'Z'
  );
}

export function buildRecurrenceRule(state: RecurrenceFormState, seriesStart: string): string {
  const parts = [`FREQ=${state.frequency}`, `INTERVAL=${state.interval}`];

  if (state.frequency === 'WEEKLY') {
    const days = ensureAnchorWeekday(state.weekDays, seriesStart);
    parts.push(`BYDAY=${days.join(',')}`);
  }

  if (state.frequency === 'MONTHLY' && state.monthlyType === 'NTH_WEEKDAY') {
    const weekdays = ensureAnchorWeekday(state.monthlyWeekdays, seriesStart);
    const positions = state.monthlyPositions.length > 0 ? state.monthlyPositions : [1];
    const byday = positions.flatMap((pos) => weekdays.map((wd) => `${pos}${wd}`)).join(',');
    parts.push(`BYDAY=${byday}`);
  }

  if (state.seriesEnd) {
    parts.push(`UNTIL=${formatUntilUtc(state.seriesEnd)}`);
  }

  return parts.join(';');
}

function parseRuleParts(rule: string): Record<string, string> {
  return Object.fromEntries(
    rule.split(';').map((part) => {
      const separator = part.indexOf('=');
      if (separator === -1) {
        return [part, ''];
      }
      return [part.slice(0, separator), part.slice(separator + 1)];
    }),
  );
}

function parseUntilToLocalDateTime(until: string): string | null {
  const match = until.match(/^(\d{4})(\d{2})(\d{2})T(\d{2})(\d{2})(\d{2})Z?$/);
  if (!match) {
    return null;
  }
  const [, year, month, day, hour, minute, second] = match;
  return `${year}-${month}-${day}T${hour}:${minute}:${second}`;
}

export function parseRecurrenceRule(
  rule: string,
  seriesEnd: string | null | undefined,
  seriesStart: string,
): RecurrenceFormState {
  const parts = parseRuleParts(rule);
  const frequency = (parts.FREQ as RecurrenceFormState['frequency']) || 'WEEKLY';
  const interval = parseInt(parts.INTERVAL ?? '1', 10) || 1;
  const untilFromRule = parts.UNTIL ? parseUntilToLocalDateTime(parts.UNTIL) : null;

  const defaults: RecurrenceFormState = {
    enabled: true,
    frequency,
    interval,
    seriesEnd: seriesEnd ?? untilFromRule,
    weekDays: [anchorWeekday(seriesStart)],
    monthlyType: 'DAY_OF_MONTH',
    monthlyWeekdays: [weekdayFromStart(seriesStart)],
    monthlyPositions: [1],
  };

  if (!parts.BYDAY) {
    return defaults;
  }

  const byDay = parts.BYDAY.split(',');
  const nthWeekday = byDay.some((token) => /^\d/.test(token));

  if (frequency === 'WEEKLY') {
    return {
      ...defaults,
      weekDays: ensureAnchorWeekday(byDay as ICalWeekday[], seriesStart),
    };
  }

  if (frequency === 'MONTHLY' && nthWeekday) {
    const monthlyPositions: number[] = [];
    const monthlyWeekdays: ICalWeekday[] = [];
    for (const token of byDay) {
      const match = token.match(/^(-?\d+)(MO|TU|WE|TH|FR|SA|SU)$/);
      if (match) {
        monthlyPositions.push(parseInt(match[1], 10));
        monthlyWeekdays.push(match[2] as ICalWeekday);
      }
    }
    return {
      ...defaults,
      monthlyType: 'NTH_WEEKDAY',
      monthlyPositions: monthlyPositions.length > 0 ? monthlyPositions : [1],
      monthlyWeekdays: ensureAnchorWeekday(monthlyWeekdays, seriesStart),
    };
  }

  return defaults;
}

/**
 * Series end is date-only in the UI: store end-of-day so occurrences on that calendar day
 * are still included by RRULE UNTIL / materialization cutoff.
 */
export function seriesEndFromDate(dateOnly: string): string {
  return `${dateOnly}T23:59:59`;
}

export function buildRecurrenceRequest(state: RecurrenceFormState, seriesStart: string) {
  const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/Berlin';
  const request: { recurrenceRule: string; seriesEnd?: string; timezone: string } = {
    recurrenceRule: buildRecurrenceRule(state, seriesStart),
    timezone,
  };
  if (state.seriesEnd) {
    request.seriesEnd = state.seriesEnd;
  }
  return request;
}
