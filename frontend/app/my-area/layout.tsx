/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import Link from 'next/link';
import { useTranslation } from '@/hooks/useTranslation';
import { useAuth } from '@/lib/auth/AuthContext';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { MobileNavigationLayout } from '@/components/layout/MobileNavigationLayout';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';

export default function ProfileLayout({ children }: { children: React.ReactNode }) {
  const t = useTranslation();
  const { isLoading: authLoading, isAuthenticated } = useAuth();
  const hasEmberOneUserId = useHasEmberOneUserId();

  if (authLoading) {
    return (
      <MobileNavigationLayout>
        <LoadingState variant="embedded" size="sm" />
      </MobileNavigationLayout>
    );
  }

  if (isAuthenticated && !hasEmberOneUserId) {
    return (
      <MobileNavigationLayout>
        <NoticeCard>{t('myArea.error.noEmployeeId')}</NoticeCard>
        <p className="mt-4 text-center">
          <Link
            href="/"
            className="text-sm font-medium text-[var(--fire-red)] underline-offset-2 hover:underline"
          >
            {t('myArea.layout.backToHome')}
          </Link>
        </p>
      </MobileNavigationLayout>
    );
  }

  return <MobileNavigationLayout>{children}</MobileNavigationLayout>;
}
