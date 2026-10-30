/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useIsMutating, useQueryClient } from '@tanstack/react-query';
import {
  createContext,
  type ReactNode,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from 'react';

export type RefreshStatus = 'idle' | 'refreshing' | 'success' | 'offline' | 'error';

/** One refresh path for the mobile button and gesture. */
function useRefreshController() {
  const queryClient = useQueryClient();
  const mutations = useIsMutating();
  const [status, setStatus] = useState<RefreshStatus>('idle');
  const busy = useRef(false);
  const mounted = useRef(false);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  useEffect(() => {
    if (status === 'idle' || status === 'refreshing') return;
    const timeout = window.setTimeout(() => setStatus('idle'), status === 'success' ? 1800 : 6000);
    return () => window.clearTimeout(timeout);
  }, [status]);

  const refresh = useCallback(async () => {
    if (busy.current || queryClient.isMutating() > 0) return;
    if (!navigator.onLine) {
      setStatus('offline');
      return;
    }
    busy.current = true;
    setStatus('refreshing');
    const started = Date.now();
    let result: RefreshStatus = 'success';
    let onOffline = () => {};
    const offline = new Promise<never>((_, reject) => {
      onOffline = () => reject(new Error('Connection lost during refresh'));
    });
    window.addEventListener('offline', onOffline, { once: true });
    try {
      // Reuse running fetches. Mounted data refreshes; other cached data becomes stale.
      // A fetch may pause indefinitely when connectivity disappears between retries.
      await Promise.race([
        queryClient.invalidateQueries(
          { refetchType: 'active' },
          { throwOnError: true, cancelRefetch: false },
        ),
        offline,
      ]);
      if (
        !navigator.onLine ||
        queryClient
          .getQueryCache()
          .findAll({ type: 'active' })
          .some((query) => query.state.fetchStatus === 'paused')
      )
        result = 'offline';
    } catch {
      result = navigator.onLine ? 'error' : 'offline';
    } finally {
      window.removeEventListener('offline', onOffline);
      // Prevent a flash of loading UI and absorb accidental double taps.
      await new Promise((resolve) =>
        window.setTimeout(resolve, Math.max(0, 600 - (Date.now() - started))),
      );
      busy.current = false;
      if (mounted.current) setStatus(result);
    }
  }, [queryClient]);

  return { status, refresh, disabled: status === 'refreshing' || mutations > 0 };
}

const RefreshContext = createContext<ReturnType<typeof useRefreshController> | null>(null);

export function RefreshServerCacheProvider({ children }: { children: ReactNode }) {
  const refresh = useRefreshController();
  return <RefreshContext.Provider value={refresh}>{children}</RefreshContext.Provider>;
}

export function useRefreshServerCache() {
  const refresh = useContext(RefreshContext);
  if (!refresh) throw new Error('useRefreshServerCache requires RefreshServerCacheProvider');
  return refresh;
}
