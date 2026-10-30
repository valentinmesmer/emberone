/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { DEFAULT_LOCALE, toBcp47Locale } from '@/lib/i18n/locale';

const LOCALE = toBcp47Locale(DEFAULT_LOCALE);

const DATE_SHORT_OPTIONS: Intl.DateTimeFormatOptions = {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
};

const DATE_LONG_OPTIONS: Intl.DateTimeFormatOptions = {
  day: '2-digit',
  month: 'long',
  year: 'numeric',
};

const TIME_OPTIONS: Intl.DateTimeFormatOptions = {
  hour: '2-digit',
  minute: '2-digit',
};

function parseDate(dateString: string | undefined | null): Date | null {
  if (!dateString) return null;
  const date = new Date(dateString);
  return Number.isNaN(date.getTime()) ? null : date;
}

/**
 * Formats a date string to German locale (DD.MM.YYYY, always with leading zeros).
 */
export function formatDateShort(dateString: string | undefined | null): string {
  const date = parseDate(dateString);
  if (!date) return dateString ? String(dateString) : '-';
  return date.toLocaleDateString(LOCALE, DATE_SHORT_OPTIONS);
}

/**
 * Formats a date string to German locale (DD. Month YYYY, day with leading zeros).
 */
export function formatDateLong(dateString: string | undefined | null): string {
  const date = parseDate(dateString);
  if (!date) return dateString ? String(dateString) : '-';
  return date.toLocaleDateString(LOCALE, DATE_LONG_OPTIONS);
}

/**
 * Formats time (HH:mm, always with leading zeros).
 */
export function formatTime(dateString: string | undefined | null): string {
  const date = parseDate(dateString);
  if (!date) return '-';
  return date.toLocaleTimeString(LOCALE, TIME_OPTIONS);
}

/**
 * Formats date and time (DD.MM.YYYY, HH:mm).
 */
export function formatDateTime(dateString: string | undefined | null): string {
  if (!parseDate(dateString)) return '-';
  return `${formatDateShort(dateString)}, ${formatTime(dateString)}`;
}

/** En-dash with spaces - standard range separator in UI copy. */
const RANGE_SEP = ' - ';

/**
 * Formats a time range (HH:mm - HH:mm).
 */
export function formatTimeRange(
  start: string | undefined | null,
  end: string | undefined | null,
): string {
  return `${formatTime(start)}${RANGE_SEP}${formatTime(end)}`;
}

/**
 * Human-readable duration between two instants (e.g. "2 h", "45 min", "1 h 30 min").
 */
export function formatDurationBetween(start: string, end: string): string {
  const startDate = parseDate(start);
  const endDate = parseDate(end);
  if (!startDate || !endDate) return '-';
  const totalMinutes = Math.round((endDate.getTime() - startDate.getTime()) / 60000);
  const h = Math.floor(totalMinutes / 60);
  const m = totalMinutes % 60;
  if (h === 0) return `${m} min`;
  return m === 0 ? `${h} h` : `${h} h ${m} min`;
}

/**
 * List subtitle for a time entry: same-day range with optional duration, or start only when open.
 */
export function formatTimeEntryListSubtitle(start: string, end?: string | null): string {
  if (!end) return formatDateTime(start);
  return `${formatAppointmentDateTimeRange(start, end)} · ${formatDurationBetween(start, end)}`;
}

/**
 * Compact appointment range: same calendar day → "DD.MM.YYYY, HH:mm - HH:mm".
 */
export function formatAppointmentDateTimeRange(
  start: string | undefined | null,
  end: string | undefined | null,
): string {
  const startDate = parseDate(start);
  const endDate = parseDate(end);
  if (!startDate || !endDate) return '-';
  if (startDate.toDateString() === endDate.toDateString()) {
    return `${formatDateShort(start)}, ${formatTimeRange(start, end)}`;
  }
  return `${formatDateTime(start)}${RANGE_SEP}${formatDateTime(end)}`;
}

/**
 * Gets current date/time in LocalDateTime format (yyyy-MM-ddTHH:mm:ss)
 */
export function formatLocalDateTimeFromDate(d: Date): string {
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  const seconds = String(d.getSeconds()).padStart(2, '0');
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
}

/**
 * Gets current date/time in LocalDateTime format (yyyy-MM-ddTHH:mm:ss)
 */
export function getCurrentDateTime(): string {
  return formatLocalDateTimeFromDate(new Date());
}

/**
 * Returns the current date/time as organization wall-clock string ({@code yyyy-MM-dd'T'HH:mm:ss}).
 * Use for API payloads and optimistic cache updates of {@code @OrganizationWallClock} fields.
 */
export function getOrganizationWallClockNow(): string {
  return getCurrentDateTime();
}

/**
 * First local minute strictly after "now" truncated to whole minutes (matches appointment backend truncation).
 */
export function getNextMinuteDateTime(): string {
  const d = new Date();
  d.setSeconds(0, 0);
  d.setMilliseconds(0);
  d.setMinutes(d.getMinutes() + 1);
  return formatLocalDateTimeFromDate(d);
}

/** Adds hours to a `yyyy-MM-ddTHH:mm:ss` local datetime string. */
export function addHoursToLocalDateTime(dateTime: string, hours: number): string {
  const date = new Date(dateTime);
  date.setHours(date.getHours() + hours);
  return formatLocalDateTimeFromDate(date);
}

export function getEndDateTime(): string {
  const now = new Date();
  now.setHours(now.getHours() + 2);
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  const hours = String(now.getHours()).padStart(2, '0');
  const minutes = String(now.getMinutes()).padStart(2, '0');
  const seconds = String(now.getSeconds()).padStart(2, '0');
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
}

/**
 * Formats a date string to LocalDateTime format (yyyy-MM-ddTHH:mm:ss)
 */
export function formatToLocalDateTime(dateString: string): string {
  if (!dateString) return '';
  if (dateString.match(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/)) {
    return dateString;
  }
  const date = parseDate(dateString);
  if (!date) return '';
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
}

const LOCAL_DT_RE = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})$/;

function parseFormLocalDateTime(s: string): Date | null {
  if (!s) return null;
  const m = s.match(LOCAL_DT_RE);
  if (m) {
    const [, y, mo, d, h, mi, se] = m;
    const dt = new Date(Number(y), Number(mo) - 1, Number(d), Number(h), Number(mi), Number(se));
    return Number.isNaN(dt.getTime()) ? null : dt;
  }
  return parseDate(s);
}

function formatFormLocalDateTime(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

function startOfLocalCalendarDay(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate());
}

function calendarDaysDelta(prevStart: Date, nextStart: Date): number {
  const msPerDay = 24 * 60 * 60 * 1000;
  return Math.round(
    (startOfLocalCalendarDay(nextStart).getTime() - startOfLocalCalendarDay(prevStart).getTime()) /
      msPerDay,
  );
}

/**
 * When the start **calendar day** moves, shift `end` by the same number of days (wall-clock time on end unchanged).
 * If that would place end before or equal to the new start, keep the previous start→end duration from the old values.
 */
export function shiftEndWithStartDateChange(
  previousStart: string,
  nextStart: string,
  end: string,
): string {
  const prevS = parseFormLocalDateTime(previousStart);
  const nextS = parseFormLocalDateTime(nextStart);
  const endD = parseFormLocalDateTime(end);
  if (!prevS || !nextS || !endD) return end;

  const deltaDays = calendarDaysDelta(prevS, nextS);
  if (deltaDays === 0) return end;

  const shifted = new Date(endD);
  shifted.setDate(shifted.getDate() + deltaDays);

  if (shifted.getTime() <= nextS.getTime()) {
    const durationMs = Math.max(endD.getTime() - prevS.getTime(), 60 * 60 * 1000);
    return formatFormLocalDateTime(new Date(nextS.getTime() + durationMs));
  }

  return formatFormLocalDateTime(shifted);
}

const DEFAULT_END_OFFSET_HOURS = 1;

/**
 * Keeps a paired end datetime aligned when start changes (form pickers).
 * Empty end → start plus {@link DEFAULT_END_OFFSET_HOURS} on the same calendar day.
 * Different start day → {@link shiftEndWithStartDateChange}.
 * Same day → keep end time-of-day when still after start; otherwise start plus offset.
 */
export function syncEndLocalDateTimeWithStartChange(
  previousStart: string,
  nextStart: string,
  end: string,
): string {
  if (!nextStart.trim()) return end;
  if (!end.trim()) {
    return addHoursToLocalDateTime(nextStart, DEFAULT_END_OFFSET_HOURS);
  }

  const prevS = parseFormLocalDateTime(previousStart);
  const nextS = parseFormLocalDateTime(nextStart);
  const endD = parseFormLocalDateTime(end);
  if (!nextS || !endD) return end;

  if (prevS && calendarDaysDelta(prevS, nextS) !== 0) {
    return shiftEndWithStartDateChange(previousStart, nextStart, end);
  }

  const merged = new Date(endD);
  merged.setFullYear(nextS.getFullYear(), nextS.getMonth(), nextS.getDate());
  if (merged.getTime() <= nextS.getTime()) {
    return addHoursToLocalDateTime(nextStart, DEFAULT_END_OFFSET_HOURS);
  }
  return formatFormLocalDateTime(merged);
}

const ISO_DATE_ONLY_RE = /^(\d{4})-(\d{2})-(\d{2})$/;

function parseIsoLocalDay(dateStr: string): Date | null {
  const m = dateStr.match(ISO_DATE_ONLY_RE);
  if (!m) return null;
  const [, y, mo, d] = m;
  const dt = new Date(Number(y), Number(mo) - 1, Number(d));
  return Number.isNaN(dt.getTime()) ? null : dt;
}

function formatIsoLocalDay(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/**
 * For `yyyy-MM-dd` start/end fields: when the start **calendar day** moves, shift `end` by the same
 * number of days. If that would place end before the new start day, snap end to the new start day.
 */
export function shiftEndDateWithStartDateChange(
  previousStart: string,
  nextStart: string,
  end: string,
): string {
  const prevS = parseIsoLocalDay(previousStart);
  const nextS = parseIsoLocalDay(nextStart);
  const endD = parseIsoLocalDay(end);
  if (!prevS || !nextS || !endD) return end;

  const deltaDays = calendarDaysDelta(prevS, nextS);
  if (deltaDays === 0) return end;

  const shifted = new Date(endD);
  shifted.setDate(shifted.getDate() + deltaDays);

  if (startOfLocalCalendarDay(shifted).getTime() < startOfLocalCalendarDay(nextS).getTime()) {
    return formatIsoLocalDay(nextS);
  }

  return formatIsoLocalDay(shifted);
}

export function getTodayIsoDate(): string {
  return formatIsoLocalDay(new Date());
}

/**
 * True when both values are non-empty and end is strictly before start.
 * Works for `yyyy-MM-dd` and `yyyy-MM-ddTHH:mm:ss` strings from our form pickers (lexicographic order).
 */
export function isEndStrictlyBeforeStart(start: string, end: string): boolean {
  const s = start?.trim();
  const e = end?.trim();
  return !!s && !!e && e < s;
}

/**
 * True when both values are non-empty and end is not after start (membership intervals are
 * half-open `[start, end)`, so the end must come after the start).
 */
export function isEndNotAfterStart(start: string, end: string): boolean {
  const s = start?.trim();
  const e = end?.trim();
  return !!s && !!e && e <= s;
}

/** Later picker value when both are non-empty (`yyyy-MM-dd` or `yyyy-MM-ddTHH:mm:ss`; lexicographic). */
export function laterOfDateStrings(a: string, b: string): string {
  if (a < b) return b;
  return a;
}

/** True when start is strictly after organization wall-clock now (`yyyy-MM-ddTHH:mm:ss`). */
export function isStartInTheFuture(startDate: string): boolean {
  const s = startDate?.trim();
  return !!s && s > getOrganizationWallClockNow();
}
