/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useEffect, useRef } from 'react';
import { LoadingState } from '@/components/ui/loading';
import { useTranslation } from '@/hooks/useTranslation';
import { useAuth } from '@/lib/auth/AuthContext';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

export function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { isAuthenticated, isLoading, isLoggingOut, login, isOidcConfigured } = useAuth();
  const hasInitiatedLoginRef = useRef(false);
  const t = useTranslation();

  useEffect(() => {
    if (
      !isLoading &&
      isOidcConfigured &&
      !isAuthenticated &&
      !isLoggingOut &&
      !hasInitiatedLoginRef.current
    ) {
      hasInitiatedLoginRef.current = true;
      login().catch((error) => {
        console.error('Login redirect failed:', error);
        hasInitiatedLoginRef.current = false;
      });
    }
  }, [isLoading, isAuthenticated, isLoggingOut, login, isOidcConfigured]);

  if (isLoading || isLoggingOut) {
    return <LoadingState />;
  }

  if (!isOidcConfigured) {
    return <LoadingState message={t('auth.unavailable')} subMessage="" showSpinner={false} />;
  }

  if (!isAuthenticated) {
    return <LoadingState />;
  }

  return <>{children}</>;
}
