/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useRouter } from 'next/navigation';
import { useEffect } from 'react';
import { LoadingState } from '@/components/ui/loading';
import { ensureUserManager } from '@/lib/auth/oidcConfig';
import {
  clearRememberedReturnPath,
  consumeRememberedReturnPath,
} from '@/lib/auth/postLoginRedirect';

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
