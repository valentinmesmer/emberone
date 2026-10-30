/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { ensureUserManager } from '@/lib/auth/oidcConfig';
import {
  clearRememberedReturnPath,
  consumeRememberedReturnPath,
} from '@/lib/auth/postLoginRedirect';
import { LoadingState } from '@/components/ui/loading';

export default function AuthCallbackPage() {
  const router = useRouter();

  useEffect(() => {
    let cancelled = false;

    (async () => {
      try {
        const userManager = await ensureUserManager();
        if (!userManager) {
          clearRememberedReturnPath();
          if (!cancelled) router.replace('/');
          return;
        }
        await userManager.signinRedirectCallback();
        const target = consumeRememberedReturnPath('/');
        if (!cancelled) router.replace(target);
      } catch (error) {
        console.error('OIDC callback error:', error);
        clearRememberedReturnPath();
        if (!cancelled) router.replace('/');
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [router]);

  return <LoadingState />;
}
