/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { type CountryCode, isValidPhoneNumber } from 'libphonenumber-js/core';
import metadata from 'libphonenumber-js/max/metadata';
import { DEFAULT_PHONE_REGION } from './constants';

/**
 * Empty/whitespace-only, or a number accepted by libphonenumber.
 * Uses the full metadata bundle to match the backend Google libphonenumber validator.
 */
export function isValidOptionalPhone(
  value: string,
  defaultCountry: CountryCode = DEFAULT_PHONE_REGION,
): boolean {
  const trimmed = value.trim();
  if (!trimmed) {
    return true;
  }
  try {
    if (trimmed.startsWith('+')) {
      return isValidPhoneNumber(trimmed, metadata);
    }
    if (isValidPhoneNumber(trimmed, defaultCountry, metadata)) {
      return true;
    }
    const digits = trimmed.replace(/\D/g, '');
    if (digits.length > 0) {
      return isValidPhoneNumber(`+${digits}`, metadata);
    }
    return false;
  } catch {
    return false;
  }
}

/** Non-empty and a valid phone number (required fields). */
export function isValidRequiredPhone(
  value: string,
  defaultCountry: CountryCode = DEFAULT_PHONE_REGION,
): boolean {
  const trimmed = value.trim();
  if (!trimmed) {
    return false;
  }
  return isValidOptionalPhone(trimmed, defaultCountry);
}
