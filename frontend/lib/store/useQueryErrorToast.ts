/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useEffect } from 'react';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { getRequestErrorMessage } from '@/lib/api/requestError';

type UseQueryErrorToastOptions = {
  enabled?: boolean;
  /** i18n key when the API returns no message (default: `common.errors.requestFailed`). */
  messageKey?: string;
};

/**
 * Shows a one-shot error toast when a query `error` is set (list/detail loads).
 * Prefer inline error UI where it fits; use this for consistent list-page feedback.
 */
export function useQueryErrorToast(
  error: unknown,
  { enabled = true, messageKey = 'common.errors.requestFailed' }: UseQueryErrorToastOptions = {},
) {
  const { showError } = useToast();
  const t = useTranslation();

  useEffect(() => {
    if (!enabled || !error) {
      return;
    }
    const fallback = t(messageKey);
    showError(getRequestErrorMessage(error, fallback));
  }, [enabled, error, messageKey, showError, t]);
}
