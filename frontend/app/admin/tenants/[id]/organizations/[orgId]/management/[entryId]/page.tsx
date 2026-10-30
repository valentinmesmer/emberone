/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { Suspense } from 'react';
import { AdminMobileLayout } from '@/components/admin';
import { AdminManagementAudienceDetailView } from '@/components/admin/AdminManagementAudienceDetailView';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState, NoticeCard } from '@/components/ui/message-cards';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';

function AdminManagementAudienceDetailPageInner() {
  const params = useParams();
  const tenantParam = params?.id as string | undefined;
  const orgParam = params?.orgId as string | undefined;
  const entryParam = params?.entryId as string | undefined;
  const tenantId = tenantParam ? Number.parseInt(tenantParam, 10) : NaN;
  const organizationId = orgParam ? Number.parseInt(orgParam, 10) : NaN;
  const entryId = entryParam ? Number.parseInt(entryParam, 10) : NaN;
  const idsValid =
    Number.isFinite(tenantId) &&
    tenantId > 0 &&
    Number.isFinite(organizationId) &&
    organizationId > 0 &&
    Number.isFinite(entryId) &&
    entryId > 0;

  const {
    canAccessAdministration,
    isGlobalAdmin,
    tenantId: authTenantId,
    isLoading: authLoading,
  } = useAuth();
  const t = useTranslation();

  const tenantOutOfScope =
    !isGlobalAdmin && idsValid && (authTenantId == null || tenantId !== authTenantId);

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

  if (!idsValid) {
    return (
      <AdminMobileLayout>
        <NotFoundState message={t('admin.managementAudience.notFound')} />
      </AdminMobileLayout>
    );
  }

  if (tenantOutOfScope) {
    return (
      <AdminMobileLayout>
        <NoticeCard>{t('admin.errors.tenantScopeDenied')}</NoticeCard>
      </AdminMobileLayout>
    );
  }

  return (
    <AdminMobileLayout>
      <AdminManagementAudienceDetailView
        tenantId={tenantId}
        organizationId={organizationId}
        entryId={entryId}
      />
    </AdminMobileLayout>
  );
}

export default function AdminManagementAudienceDetailPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[var(--page-bg)] pt-20">
          <LoadingState variant="embedded" />
        </div>
      }
    >
      <AdminManagementAudienceDetailPageInner />
    </Suspense>
  );
}
