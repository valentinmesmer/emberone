/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { Suspense } from 'react';
import { AdminMobileLayout } from '@/components/admin';
import { AdminOrganizationTypeDetailView } from '@/components/admin/AdminOrganizationTypeDetailView';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';

function OrganizationTypeDetailPageInner() {
  const params = useParams();
  const tenantParam = params?.id as string | undefined;
  const typeParam = params?.typeId as string | undefined;
  const tenantId = tenantParam ? Number.parseInt(tenantParam, 10) : NaN;
  const typeId = typeParam ? Number.parseInt(typeParam, 10) : NaN;
  const idsValid =
    Number.isFinite(tenantId) && tenantId > 0 && Number.isFinite(typeId) && typeId > 0;

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
      <AdminMobileLayout>
        <NoticeCard>{t('admin.accessDenied')}</NoticeCard>
      </AdminMobileLayout>
    );
  }

  if (!idsValid) {
    return (
      <AdminMobileLayout>
        <NoticeCard>{t('admin.organizationTypes.errors.loadFailed')}</NoticeCard>
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
      <AdminOrganizationTypeDetailView tenantId={tenantId} typeId={typeId} />
    </AdminMobileLayout>
  );
}

export default function OrganizationTypeDetailPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[var(--page-bg)] pt-20">
          <LoadingState variant="embedded" />
        </div>
      }
    >
      <OrganizationTypeDetailPageInner />
    </Suspense>
  );
}
