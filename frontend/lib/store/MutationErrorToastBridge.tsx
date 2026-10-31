/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
