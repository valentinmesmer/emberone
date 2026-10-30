/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useEffect, useState } from 'react';
import { resolvePasswordSettingsUrl } from '@/lib/idp';
import { getRuntimeConfig } from '@/lib/runtimeConfig';

export function useIdpPasswordSettingsUrl(): { url: string | null; ready: boolean } {
  const [url, setUrl] = useState<string | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let cancelled = false;
    getRuntimeConfig().then((cfg) => {
      if (!cancelled) {
        setUrl(resolvePasswordSettingsUrl(cfg));
        setReady(true);
      }
    });
    return () => {
      cancelled = true;
    };
  }, []);

  return { url, ready };
}
