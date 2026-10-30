/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

export type ResponseDeadlineUnit = 'MINUTES' | 'HOURS' | 'DAYS';

const MINUTES_PER_HOUR = 60;
const MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR;

export const DEFAULT_RESPONSE_DEADLINE_MINUTES = 24 * MINUTES_PER_HOUR;

export function hasResponseDeadline(minutes: number | null | undefined): boolean {
  return (minutes ?? 0) > 0;
}

export function formValueToMinutes(amount: number, unit: ResponseDeadlineUnit): number {
  const value = Math.max(0, Math.floor(amount));
  if (value === 0) {
    return 0;
  }
  switch (unit) {
    case 'DAYS':
      return value * MINUTES_PER_DAY;
    case 'HOURS':
      return value * MINUTES_PER_HOUR;
    default:
      return value;
  }
}

export function minutesToFormValue(minutes: number): {
  amount: number;
  unit: ResponseDeadlineUnit;
} {
  if (minutes <= 0) {
    return { amount: 0, unit: 'HOURS' };
  }
  const safeMinutes = minutes;
  if (safeMinutes % MINUTES_PER_DAY === 0) {
    return { amount: safeMinutes / MINUTES_PER_DAY, unit: 'DAYS' };
  }
  if (safeMinutes % MINUTES_PER_HOUR === 0) {
    return { amount: safeMinutes / MINUTES_PER_HOUR, unit: 'HOURS' };
  }
  return { amount: safeMinutes, unit: 'MINUTES' };
}

export function isResponseDeadlinePassed(
  start: string,
  minutesBefore: number | null | undefined,
): boolean {
  const deadline = computeResponseDeadline(
    start,
    hasResponseDeadline(minutesBefore),
    minutesBefore,
  );
  if (!deadline) return false;
  return new Date(deadline).getTime() < Date.now();
}

export function computeResponseDeadline(
  start: string,
  enabled: boolean,
  minutesBefore?: number | null,
): string | null {
  if (!enabled || minutesBefore == null) {
    return null;
  }
  const date = new Date(start);
  date.setMinutes(date.getMinutes() - minutesBefore);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
}
