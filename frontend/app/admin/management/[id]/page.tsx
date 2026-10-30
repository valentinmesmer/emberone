/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { AdminManagementActivationDetailView, AdminMobileLayout } from '@/components/admin';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';

export default function AdminManagementActivationDetailPage() {
  const params = useParams();
  const idParam = params?.id as string | undefined;
  const activationId = idParam ? Number.parseInt(idParam, 10) : NaN;
  const { canAccessAdministration, isLoading: authLoading } = useAuth();
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

  if (!Number.isFinite(activationId) || activationId <= 0) {
    return (
      <AdminMobileLayout>
        <NoticeCard>{t('admin.managementActivations.detail.notFound')}</NoticeCard>
      </AdminMobileLayout>
    );
  }

  return (
    <AdminMobileLayout>
      <AdminManagementActivationDetailView activationId={activationId} />
    </AdminMobileLayout>
  );
}
