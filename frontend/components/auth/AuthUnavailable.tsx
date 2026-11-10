/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useEffect, useState } from 'react';
import { LoadingState } from '@/components/ui/loading';
import { useTranslation } from '@/hooks/useTranslation';

/**
 * Shown when the OIDC config could not be loaded. Offline this is expected,
 * so say that instead of "unavailable" and reload once the connection is back.
 */
export function AuthUnavailable() {
  const t = useTranslation();
  const [isOffline, setIsOffline] = useState(() => !navigator.onLine);

  useEffect(() => {
    const goOffline = () => setIsOffline(true);
    const goOnline = () => window.location.reload();
    window.addEventListener('offline', goOffline);
    window.addEventListener('online', goOnline);
    return () => {
      window.removeEventListener('offline', goOffline);
      window.removeEventListener('online', goOnline);
    };
  }, []);

  if (isOffline) {
    return (
      <LoadingState
        message={t('pwa.offline.appOffline')}
        subMessage={t('pwa.offline.connectHint')}
        showSpinner={false}
      />
    );
  }

  return <LoadingState message={t('auth.unavailable')} subMessage="" showSpinner={false} />;
}
