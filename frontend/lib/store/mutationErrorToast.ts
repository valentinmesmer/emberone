/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { getRequestErrorMessage, shouldSuppressMutationErrorToast } from '@/lib/api/requestError';
import type { AppMutationMeta } from '@/lib/store/appMutationMeta';

type MutationErrorToastContext = {
  showError: (message: string) => void;
  translate: (key: string) => string;
  fallbackMessage: string;
};

let context: MutationErrorToastContext | null = null;

export function setMutationErrorToastContext(next: MutationErrorToastContext | null): void {
  context = next;
}

export function handleMutationCacheError(error: unknown, meta?: AppMutationMeta): void {
  if (!meta?.showErrorToast || !context) {
    return;
  }
  if (shouldSuppressMutationErrorToast(error)) {
    return;
  }

  const fallback =
    meta.errorMessage ??
    (meta.errorMessageKey ? context.translate(meta.errorMessageKey) : context.fallbackMessage);

  context.showError(getRequestErrorMessage(error, fallback));
}
