/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { AdminMobileLayout, AdminUserDetailView } from '@/components/admin';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';

export default function AdminUserDetailPage() {
  const params = useParams();
  const userParam = params?.userId as string | undefined;
  const userId = userParam ? Number.parseInt(userParam, 10) : NaN;
  const { canAccessAdministration, isGlobalAdmin, isLoading: authLoading } = useAuth();
  const t = useTranslation();

  if (authLoading) {
    return (
      <div className="min-h-screen bg-[var(--page-bg)] pt-20">
        <LoadingState variant="embedded" />
      </div>
    );
  }

  if (!canAccessAdministration) {
    return (
      <div className="min-h-screen bg-[var(--page-bg)] px-4 pt-24 pb-12">
        <div className="mx-auto max-w-lg">
          <NoticeCard>{t('admin.accessDenied')}</NoticeCard>
        </div>
      </div>
    );
  }

  if (!Number.isFinite(userId) || userId <= 0) {
    return (
      <AdminMobileLayout>
        <NoticeCard>{t('admin.userDetail.notFound')}</NoticeCard>
      </AdminMobileLayout>
    );
  }

  return (
    <AdminMobileLayout>
      <AdminUserDetailView userId={userId} showTenantFields={isGlobalAdmin} />
    </AdminMobileLayout>
  );
}
