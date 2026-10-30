/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams, useSearchParams } from 'next/navigation';
import { Suspense, useMemo } from 'react';
import { AdminMobileLayout } from '@/components/admin';
import { AdminRecordedOnboardingWizard } from '@/components/admin/AdminRecordedOnboardingWizard';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';
import {
  getAdminLeadershipGroupMembershipDetailPath,
  getAdminOrganizationDetailPath,
} from '@/lib/utils/adminPaths';

function AdminRecordedOnboardingInner() {
  const params = useParams();
  const searchParams = useSearchParams();
  const tenantParam = params?.id as string | undefined;
  const orgParam = params?.orgId as string | undefined;
  const invitationParam = params?.invitationId as string | undefined;
  const tenantId = tenantParam ? Number.parseInt(tenantParam, 10) : NaN;
  const organizationId = orgParam ? Number.parseInt(orgParam, 10) : NaN;
  const invitationId = invitationParam ? Number.parseInt(invitationParam, 10) : NaN;
  const membershipParam = searchParams.get('membershipId');
  const membershipId = membershipParam ? Number.parseInt(membershipParam, 10) : NaN;
  const idsValid =
    Number.isFinite(tenantId) &&
    tenantId > 0 &&
    Number.isFinite(organizationId) &&
    organizationId > 0 &&
    Number.isFinite(invitationId) &&
    invitationId > 0;

  const {
    canAccessAdministration,
    isGlobalAdmin,
    tenantId: authTenantId,
    isLoading: authLoading,
  } = useAuth();
  const t = useTranslation();

  const tenantOutOfScope =
    !isGlobalAdmin && idsValid && (authTenantId == null || tenantId !== authTenantId);

  const returnHref = useMemo(() => {
    if (!idsValid) return undefined;
    if (Number.isFinite(membershipId) && membershipId > 0) {
      return getAdminLeadershipGroupMembershipDetailPath(tenantId, organizationId, membershipId);
    }
    return getAdminOrganizationDetailPath(tenantId, organizationId);
  }, [idsValid, membershipId, organizationId, tenantId]);

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

  if (!idsValid || tenantOutOfScope) {
    return (
      <AdminMobileLayout>
        <NoticeCard>
          {tenantOutOfScope
            ? t('admin.errors.tenantScopeDenied')
            : t('admin.errors.orgDetailLoadFailed')}
        </NoticeCard>
      </AdminMobileLayout>
    );
  }

  return (
    <AdminMobileLayout>
      <AdminRecordedOnboardingWizard
        tenantId={tenantId}
        organizationId={organizationId}
        invitationId={invitationId}
        returnHref={returnHref}
      />
    </AdminMobileLayout>
  );
}

export default function AdminRecordedOnboardingPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[var(--page-bg)] pt-20">
          <LoadingState variant="embedded" />
        </div>
      }
    >
      <AdminRecordedOnboardingInner />
    </Suspense>
  );
}
