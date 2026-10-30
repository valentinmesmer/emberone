/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

// React Query Client Configuration
// Handles server state caching and synchronization

import { MutationCache, QueryClient } from '@tanstack/react-query';
import type { AppMutationMeta } from '@/lib/store/appMutationMeta';
import { handleMutationCacheError } from '@/lib/store/mutationErrorToast';

export const queryClient = new QueryClient({
  mutationCache: new MutationCache({
    onError: (error, _variables, _context, mutation) => {
      handleMutationCacheError(error, mutation.meta as AppMutationMeta | undefined);
    },
  }),
  defaultOptions: {
    queries: {
      // Cache data for 5 minutes by default
      staleTime: 5 * 60 * 1000,
      // Keep unused data in cache for 10 minutes
      gcTime: 10 * 60 * 1000,
      // Retry failed requests once
      retry: 1,
      // Refetch on window focus (can be disabled per query if needed)
      refetchOnWindowFocus: false,
      // Don't refetch on reconnect by default (can be enabled per query)
      refetchOnReconnect: false,
    },
    mutations: {
      // Retry failed mutations once
      retry: 1,
    },
  },
});
