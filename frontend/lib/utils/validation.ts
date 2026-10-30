/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { isEndNotAfterStart, isStartInTheFuture } from './date';

export type TranslationFunction = (key: string, params?: Record<string, string | number>) => string;
export type ShowErrorFunction = (message: string) => void;

/**
 * Validates that end is not before start (`yyyy-MM-dd` or `yyyy-MM-ddTHH:mm:ss` form values).
 * @returns true if valid, false otherwise
 */
export function validateDateRange(
  startDate: string,
  endDate: string | undefined,
  t: TranslationFunction,
  showError: ShowErrorFunction,
): boolean {
  if (endDate && isEndNotAfterStart(startDate, endDate)) {
    showError(t('common.validation.endAfterStart'));
    return false;
  }
  return true;
}

/**
 * Validates that start is not in the future (`yyyy-MM-dd` or `yyyy-MM-ddTHH:mm:ss` form values).
 * @returns true if valid, false otherwise
 */
export function validateStartNotInFuture(
  startDate: string,
  t: TranslationFunction,
  showError: ShowErrorFunction,
): boolean {
  if (isStartInTheFuture(startDate)) {
    showError(t('common.validation.startNotInFuture'));
    return false;
  }
  return true;
}

/**
 * Validates that all required fields are filled
 * @returns true if all required fields are filled, false otherwise
 */
export function validateRequiredFields(
  fields: Record<string, string | undefined>,
  t: TranslationFunction,
  showError: ShowErrorFunction,
  errorKey: string,
): boolean {
  const emptyFields = Object.entries(fields).filter(([, value]) => !value?.trim());

  if (emptyFields.length > 0) {
    showError(t(errorKey));
    return false;
  }
  return true;
}

/** Empty string is valid; otherwise must be a positive integer (e.g. days). */
export function isOptionalPositiveInteger(value: string): boolean {
  const trimmed = value.trim();
  if (!trimmed) {
    return true;
  }
  const parsed = Number.parseInt(trimmed, 10);
  return Number.isInteger(parsed) && parsed > 0 && String(parsed) === trimmed;
}
