/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import {
  type DefaultError,
  type UseMutationOptions,
  type UseMutationResult,
  useMutation,
} from '@tanstack/react-query';
import type { AppMutationMeta } from '@/lib/store/appMutationMeta';

type AppMutationOptions<
  TData = unknown,
  TError = DefaultError,
  TVariables = void,
  TContext = unknown,
> = UseMutationOptions<TData, TError, TVariables, TContext> & {
  meta?: AppMutationMeta;
};

/**
 * `useMutation` with default error toasts via the shared `MutationCache` handler.
 * Set `meta.showErrorToast: false` when the UI handles errors itself.
 */
export function useAppMutation<
  TData = unknown,
  TError = DefaultError,
  TVariables = void,
  TContext = unknown,
>(
  options: AppMutationOptions<TData, TError, TVariables, TContext>,
): UseMutationResult<TData, TError, TVariables, TContext> {
  const { meta, ...rest } = options;
  return useMutation({
    ...rest,
    meta: {
      showErrorToast: meta?.showErrorToast ?? true,
      ...meta,
    },
  });
}
