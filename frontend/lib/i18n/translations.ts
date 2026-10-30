/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { DEFAULT_LOCALE, type Locale } from '@/lib/i18n/locale';
import de from '@/locales/de.json';

export type { Locale };

const translationsByLocale: Record<Locale, typeof de> = {
  de,
};

export function getTranslation(
  key: string,
  locale: Locale = DEFAULT_LOCALE,
  params?: Record<string, string | number>,
): string {
  const translations = translationsByLocale[locale];
  if (!translations) {
    return key;
  }

  const keys = key.split('.');
  let value: unknown = translations;

  for (const k of keys) {
    if (value && typeof value === 'object' && k in value) {
      value = value[k as keyof typeof value];
    } else {
      return key;
    }
  }

  let result = typeof value === 'string' ? value : key;

  if (params) {
    Object.entries(params).forEach(([paramKey, paramValue]) => {
      result = result.replace(new RegExp(`\\{\\{${paramKey}\\}\\}`, 'g'), String(paramValue));
    });
  }

  return result;
}

export function t(
  key: string,
  locale: Locale = DEFAULT_LOCALE,
  params?: Record<string, string | number>,
): string {
  return getTranslation(key, locale, params);
}
