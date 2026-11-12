/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useParams } from 'next/navigation';
import { Suspense, useMemo } from 'react';
import { AdminMobileLayout, AdminOrgTreeView } from '@/components/admin';
import { Card } from '@/components/ui/card';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';
import { SectionHeader } from '@/components/ui/section-header';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';
import {
  useAdminOrganizationTree,
  useQueryErrorToast,
  useTenantOrganizationTypes,
} from '@/lib/store';

function TenantOrganizationsTreeInner() {
  const params = useParams();
  const idParam = params?.id as string | undefined;
  const tenantId = idParam ? Number.parseInt(idParam, 10) : NaN;
  const idValid = Number.isFinite(tenantId) && tenantId > 0;

  const {
    canAccessAdministration,
    isGlobalAdmin,
    tenantId: authTenantId,
    isLoading: authLoading,
  } = useAuth();
  const t = useTranslation();

  const tenantOutOfScope =
    !isGlobalAdmin && idValid && (authTenantId == null || tenantId !== authTenantId);

  const queryEnabled = canAccessAdministration && !authLoading && idValid && !tenantOutOfScope;
  const treeQuery = useAdminOrganizationTree(tenantId, queryEnabled);
  const typesQuery = useTenantOrganizationTypes(tenantId, queryEnabled);
  useQueryErrorToast(treeQuery.error ?? typesQuery.error, {
    messageKey: 'admin.errors.orgTreeLoadFailed',
  });
  const data = treeQuery.data ?? null;
  const loadFailed = treeQuery.isError || typesQuery.isError;
  const loading = treeQuery.isLoading || typesQuery.isLoading;

  const organizationTypeNames = useMemo(
    () => new Map((typesQuery.data ?? []).map((type) => [type.id, type.name])),
    [typesQuery.data],
  );

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

  if (!idValid) {
    return (
      <AdminMobileLayout>
        <NoticeCard>{t('admin.errors.tenantLoadFailed')}</NoticeCard>
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
      {loading ? (
        <LoadingState variant="embedded" size="sm" />
      ) : loadFailed || !data ? (
        <NoticeCard>{t('admin.errors.orgTreeLoadFailed')}</NoticeCard>
      ) : (
        <div className="w-full space-y-4">
          <Card className="p-3 sm:p-4">
            <div className="min-w-0">
              <h1 className="break-words text-lg font-semibold text-[var(--text-primary)]">
                {t('admin.orgTree.orgStructureTreePageTitle')}
              </h1>
              <p className="mt-1 text-sm text-[var(--text-secondary)]">{data.tenantName}</p>
            </div>
          </Card>

          <Card className="p-3 sm:p-4">
            <SectionHeader
              className="mb-3"
              title={t('admin.orgTree.orgStructureTreeCardTitle')}
              subtitle={t('admin.orgTree.orgStructureTreeIntro')}
            />
            <AdminOrgTreeView
              tenantId={data.tenantId}
              nodes={data.roots}
              organizationTypeNames={organizationTypeNames}
            />
          </Card>
        </div>
      )}
    </AdminMobileLayout>
  );
}

export default function TenantOrganizationsTreePage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[var(--page-bg)] pt-20">
          <LoadingState variant="embedded" />
        </div>
      }
    >
      <TenantOrganizationsTreeInner />
    </Suspense>
  );
}
