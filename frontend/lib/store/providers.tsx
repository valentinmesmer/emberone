/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

// React Query Provider Component
// Wrap your app with this to enable React Query

import { QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { useEffect } from 'react';
import { revokeResolvedImageOnRemoval } from './queries/profileMedia';
import { queryClient } from './query-client';

export function QueryProvider({ children }: { children: React.ReactNode }) {
  useEffect(() => queryClient.getQueryCache().subscribe(revokeResolvedImageOnRemoval), []);

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      {/* React Query DevTools - development only */}
      {process.env.NODE_ENV === 'development' && <ReactQueryDevtools initialIsOpen={false} />}
    </QueryClientProvider>
  );
}
