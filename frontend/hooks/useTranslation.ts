/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useMemo } from 'react';
import { t } from '@/lib/i18n/translations';
import { useLocale } from '@/components/locale/LocaleProvider';

export function useTranslation() {
  const { locale } = useLocale();

  const translation = useMemo(() => {
    return (key: string, params?: Record<string, string | number>): string => {
      return t(key, locale, params);
    };
  }, [locale]);

  return translation;
}
