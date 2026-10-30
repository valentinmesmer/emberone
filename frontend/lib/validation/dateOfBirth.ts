/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { type CalendarDate, getLocalTimeZone, parseDate, today } from '@internationalized/date';
import { DATE_OF_BIRTH_MAX_AGE_YEARS } from './constants';

/** Inclusive bounds for date of birth (today − max age … today). */
export function getDateOfBirthBounds(): { min: CalendarDate; max: CalendarDate } {
  const max = today(getLocalTimeZone());
  return { min: max.subtract({ years: DATE_OF_BIRTH_MAX_AGE_YEARS }), max };
}

/**
 * Non-empty `yyyy-MM-dd` within {@link getDateOfBirthBounds}. Rejects short years such as year 4
 * (`0004-…`). Aligned with backend `DateOfBirthRules`.
 */
export function isValidDateOfBirth(value: string): boolean {
  const trimmed = value.trim();
  if (!trimmed) {
    return false;
  }
  try {
    const date = parseDate(trimmed);
    const { min, max } = getDateOfBirthBounds();
    return date.compare(min) >= 0 && date.compare(max) <= 0;
  } catch {
    return false;
  }
}
