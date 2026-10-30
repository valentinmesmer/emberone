/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { I18nProvider } from '@react-aria/i18n';
import { createContext, type ReactNode, useContext, useEffect, useMemo } from 'react';
import { DEFAULT_LOCALE, type Locale, parseLocale, toBcp47Locale } from '@/lib/i18n/locale';
import { usePermissions } from '@/lib/permission';

const LocaleContext = createContext<
  | {
      locale: Locale;
    }
  | undefined
>(undefined);

export function LocaleProvider({ children }: { children: ReactNode }) {
  const { permissions } = usePermissions();

  const locale = useMemo(
    () => parseLocale(permissions?.tenantLocale ?? DEFAULT_LOCALE),
    [permissions?.tenantLocale],
  );
  const value = useMemo(() => ({ locale }), [locale]);

  useEffect(() => {
    if (typeof document !== 'undefined') {
      document.documentElement.lang = locale;
    }
  }, [locale]);

  return (
    <LocaleContext.Provider value={value}>
      <I18nProvider locale={toBcp47Locale(locale)}>{children}</I18nProvider>
    </LocaleContext.Provider>
  );
}

export function useLocale() {
  const context = useContext(LocaleContext);
  if (context === undefined) {
    throw new Error('useLocale must be used within a LocaleProvider');
  }
  return context;
}
