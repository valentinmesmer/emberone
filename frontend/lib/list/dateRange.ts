/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { ListDateRangeValue } from './types';

export const ListDatePreset = {
  THIS_MONTH: 'thisMonth',
  LAST_MONTH: 'lastMonth',
  THIS_YEAR: 'thisYear',
} as const;

export type ListDatePreset = (typeof ListDatePreset)[keyof typeof ListDatePreset];

export const LIST_DATE_PRESETS: readonly ListDatePreset[] = [
  ListDatePreset.THIS_MONTH,
  ListDatePreset.LAST_MONTH,
  ListDatePreset.THIS_YEAR,
];

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

export function isIsoDate(value: string | undefined): value is string {
  return value != null && ISO_DATE.test(value);
}

function pad(value: number): string {
  return String(value).padStart(2, '0');
}

/** `yyyy-MM-dd` in local time (never via toISOString, which is UTC). */
export function toIsoDate(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/** `yyyy-MM-dd` of the following calendar day (local calendar arithmetic, DST-safe). */
export function addDaysToIsoDate(iso: string, days: number): string {
  const [year, month, day] = iso.split('-').map(Number);
  return toIsoDate(new Date(year, month - 1, day + days));
}

/**
 * API form of an inclusive date range: organization wall-clock `from` (start of day) and an
 * exclusive `to` (start of the day after). See `time-handling.md`; the URL keeps plain dates.
 */
export function dateRangeToWallClock(range: ListDateRangeValue): { from?: string; to?: string } {
  return {
    from: isIsoDate(range.from) ? `${range.from}T00:00:00` : undefined,
    to: isIsoDate(range.to) ? `${addDaysToIsoDate(range.to, 1)}T00:00:00` : undefined,
  };
}

/** From/to (inclusive, `yyyy-MM-dd`, local time) for a preset. */
export function computePresetRange(
  preset: ListDatePreset,
  now: Date = new Date(),
): Required<ListDateRangeValue> {
  const year = now.getFullYear();
  const month = now.getMonth();
  switch (preset) {
    case ListDatePreset.THIS_MONTH:
      return {
        from: toIsoDate(new Date(year, month, 1)),
        to: toIsoDate(new Date(year, month + 1, 0)),
      };
    case ListDatePreset.LAST_MONTH:
      return {
        from: toIsoDate(new Date(year, month - 1, 1)),
        to: toIsoDate(new Date(year, month, 0)),
      };
    case ListDatePreset.THIS_YEAR:
      return { from: toIsoDate(new Date(year, 0, 1)), to: toIsoDate(new Date(year, 11, 31)) };
  }
}

/** Preset whose range equals `value`, so the selection survives a reload from the URL. */
export function detectDatePreset(
  value: ListDateRangeValue | undefined,
  now: Date = new Date(),
): ListDatePreset | undefined {
  if (!value?.from || !value.to) {
    return undefined;
  }
  return LIST_DATE_PRESETS.find((preset) => {
    const range = computePresetRange(preset, now);
    return range.from === value.from && range.to === value.to;
  });
}

export function isDateRangeActive(value: ListDateRangeValue | undefined): boolean {
  return Boolean(value?.from || value?.to);
}

/** `yyyy-MM-dd` → `dd.MM.yyyy` (string only, no timezone involved). */
export function formatIsoDateDe(iso: string): string {
  const [year, month, day] = iso.split('-');
  return `${day}.${month}.${year}`;
}
