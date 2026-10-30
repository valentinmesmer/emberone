/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useEffect } from 'react';
import { useLocale } from '@/components/locale/LocaleProvider';
import { useToast } from '@/components/ui/toast';
import { t } from '@/lib/i18n/translations';
import { setMutationErrorToastContext } from '@/lib/store/mutationErrorToast';

/** Wires React Query mutation errors to the toast UI (mounted under `ToastProvider`). */
export function MutationErrorToastBridge() {
  const { showError } = useToast();
  const { locale } = useLocale();

  useEffect(() => {
    setMutationErrorToastContext({
      showError,
      translate: (key) => t(key, locale),
      fallbackMessage: t('common.errors.requestFailed', locale),
    });
    return () => setMutationErrorToastContext(null);
  }, [showError, locale]);

  return null;
}
