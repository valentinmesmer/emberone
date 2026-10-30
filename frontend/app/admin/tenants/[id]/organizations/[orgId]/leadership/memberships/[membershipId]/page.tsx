/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { Suspense } from 'react';
import { AdminMobileLayout } from '@/components/admin';
import { AdminLeadershipGroupMembershipDetailView } from '@/components/admin/AdminLeadershipGroupMembershipDetailView';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState, NoticeCard } from '@/components/ui/message-cards';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';

function AdminLeadershipMembershipDetailPageInner() {
  const params = useParams();
  const tenantParam = params?.id as string | undefined;
  const orgParam = params?.orgId as string | undefined;
  const membershipParam = params?.membershipId as string | undefined;
  const tenantId = tenantParam ? Number.parseInt(tenantParam, 10) : NaN;
  const organizationId = orgParam ? Number.parseInt(orgParam, 10) : NaN;
  const membershipId = membershipParam ? Number.parseInt(membershipParam, 10) : NaN;
  const idsValid =
    Number.isFinite(tenantId) &&
    tenantId > 0 &&
    Number.isFinite(organizationId) &&
    organizationId > 0 &&
    Number.isFinite(membershipId) &&
    membershipId > 0;

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
        <NotFoundState message={t('userManagement.errors.notFound')} />
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
      <AdminLeadershipGroupMembershipDetailView
        tenantId={tenantId}
        organizationId={organizationId}
        membershipId={membershipId}
      />
    </AdminMobileLayout>
  );
}

export default function AdminLeadershipMembershipDetailPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[var(--page-bg)] pt-20">
          <LoadingState variant="embedded" />
        </div>
      }
    >
      <AdminLeadershipMembershipDetailPageInner />
    </Suspense>
  );
}
