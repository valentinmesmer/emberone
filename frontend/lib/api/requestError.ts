/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { ApiRequestError } from '@/lib/api/authorizedFetch';

/**
 * User-facing message from a failed API call or mutation (prefers server `error` body).
 */
export function getRequestErrorMessage(error: unknown, fallbackMessage: string): string {
  if (error instanceof ApiRequestError) {
    const trimmed = error.message.trim();
    if (trimmed) {
      return trimmed;
    }
  }
  if (error instanceof Error) {
    const trimmed = error.message.trim();
    if (trimmed) {
      return trimmed;
    }
  }
  return fallbackMessage;
}

/** Skip global mutation error toasts for auth recovery flows. */
export function shouldSuppressMutationErrorToast(error: unknown): boolean {
  return error instanceof ApiRequestError && error.status === 401;
}
