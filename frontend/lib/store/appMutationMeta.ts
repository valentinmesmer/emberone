/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import '@tanstack/react-query';

/** React Query mutation meta for app-wide error toasts (see `useAppMutation`). */
export type AppMutationMeta = {
  /** When true (default for `useAppMutation`), failed mutations show an error toast. */
  showErrorToast?: boolean;
  /** i18n key used when the API returns no usable message. */
  errorMessageKey?: string;
  /** Fixed message; overrides `errorMessageKey` when set. */
  errorMessage?: string;
};

declare module '@tanstack/react-query' {
  interface Register {
    mutationMeta: AppMutationMeta;
  }
}
