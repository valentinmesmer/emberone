/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { usePathname, useRouter, useSearchParams } from 'next/navigation';
import { useEffect, useRef } from 'react';
import { CREATE_INTENT_PARAM } from '@/lib/navigation/createIntent';

/**
 * Opens a create dialog once when the page was reached via {@link withCreateIntent} and the user
 * may create here (`enabled`). The parameter is removed afterwards so reloads and back navigation
 * do not reopen the dialog.
 */
export function useCreateIntent(enabled: boolean, open: () => void): void {
  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const handled = useRef(false);
  const requested = searchParams.get(CREATE_INTENT_PARAM) === '1';

  useEffect(() => {
    if (!requested || !enabled || handled.current) {
      return;
    }
    handled.current = true;
    open();
    const rest = new URLSearchParams(searchParams.toString());
    rest.delete(CREATE_INTENT_PARAM);
    const query = rest.toString();
    router.replace(query ? `${pathname}?${query}` : pathname, { scroll: false });
  }, [requested, enabled, open, searchParams, router, pathname]);
}
