/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type { ReactNode } from 'react';
import { AdminMobileLayout } from '@/components/admin';
import type { StandardContentState } from '@/components/admin/standards/useStandardContent';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

interface StandardPageGateProps {
  state: Pick<
    StandardContentState,
    | 'authLoading'
    | 'canAccessAdministration'
    | 'idsValid'
    | 'tenantOutOfScope'
    | 'loading'
    | 'standard'
  >;
  children: ReactNode;
  /** When true, wait for standard to load before rendering children. */
  requireStandard?: boolean;
}

export function StandardPageGate({
  state,
  children,
  requireStandard = false,
}: StandardPageGateProps) {
  const t = useTranslation();

  if (state.authLoading) {
    return (
      <div className="min-h-screen bg-[var(--page-bg)] pt-20">
        <LoadingState variant="embedded" />
      </div>
    );
  }

  if (!state.canAccessAdministration) {
    return (
      <AdminMobileLayout>
        <NoticeCard>{t('admin.accessDenied')}</NoticeCard>
      </AdminMobileLayout>
    );
  }

  if (!state.idsValid || state.tenantOutOfScope) {
    return (
      <AdminMobileLayout>
        <NoticeCard>{t('admin.errors.tenantScopeDenied')}</NoticeCard>
      </AdminMobileLayout>
    );
  }

  if (requireStandard && (state.loading || !state.standard)) {
    return (
      <AdminMobileLayout>
        <LoadingState variant="embedded" size="sm" />
      </AdminMobileLayout>
    );
  }

  return <AdminMobileLayout>{children}</AdminMobileLayout>;
}
