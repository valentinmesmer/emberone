/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/** BCP 47 language tags supported by the app (matches {@link com.emberone.admin.tenant.domain.SupportedLocale}). */
export type Locale = 'de';

export const DEFAULT_LOCALE: Locale = 'de';

export function parseLocale(value: string | null | undefined): Locale {
  if (value === 'de') {
    return 'de';
  }
  return DEFAULT_LOCALE;
}

/** BCP 47 locale for Intl APIs and React Aria (date order, calendar). */
export function toBcp47Locale(locale: Locale): string {
  switch (locale) {
    case 'de':
      return 'de-DE';
    default:
      return 'de-DE';
  }
}

/** BCP 47 tag of the app's default locale, for Intl and `toLocale*` calls. */
export const APP_LOCALE_TAG = toBcp47Locale(DEFAULT_LOCALE);

/** Locale-aware string comparison for sorting display text. */
export function compareText(a: string, b: string): number {
  return a.localeCompare(b, APP_LOCALE_TAG);
}
