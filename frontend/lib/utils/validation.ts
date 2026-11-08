/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
 * Labels of the unmet requirements, in order. Pass the result to `ModalFooter` `blockers`: the
 * save button stays disabled while it is non-empty and its hover text names these labels.
 * Each entry is `[fieldLabel, met]`; use the same label as the field's `label` prop.
 */
export function formBlockers(
  requirements: ReadonlyArray<readonly [label: string, met: boolean]>,
): string[] {
  return requirements.filter(([, met]) => !met).map(([label]) => label);
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
