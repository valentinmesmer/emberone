/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { Suspense, useCallback, useEffect, useMemo, useState } from 'react';
import { AdminMobileLayout, AdminOrgTreeView } from '@/components/admin';
import { Card } from '@/components/ui/card';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';
import { SectionHeader } from '@/components/ui/section-header';
import { useToast } from '@/components/ui/toast';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';
import { type AdminOrganizationTreeResponseDto, adminApi } from '@/lib/api/admin';
import { type OrganizationTypeDto, organizationTypesApi } from '@/lib/api/organizationTypes';

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
  const { showError } = useToast();

  const [data, setData] = useState<AdminOrganizationTreeResponseDto | null>(null);
  const [organizationTypes, setOrganizationTypes] = useState<OrganizationTypeDto[]>([]);
  const [loadFailed, setLoadFailed] = useState(false);
  const [loading, setLoading] = useState(true);

  const organizationTypeNames = useMemo(
    () => new Map(organizationTypes.map((type) => [type.id, type.name])),
    [organizationTypes],
  );

  const tenantOutOfScope =
    !isGlobalAdmin && idValid && (authTenantId == null || tenantId !== authTenantId);

  const loadTree = useCallback(async () => {
    if (!idValid) return;
    setLoading(true);
    setLoadFailed(false);
    setData(null);
    try {
      const [res, types] = await Promise.all([
        adminApi.getAdminOrganizationTree(tenantId),
        organizationTypesApi.list(tenantId),
      ]);
      setData(res);
      setOrganizationTypes(types);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.errors.orgTreeLoadFailed'));
      setLoadFailed(true);
    } finally {
      setLoading(false);
    }
  }, [idValid, tenantId, showError, t]);

  useEffect(() => {
    if (!canAccessAdministration || authLoading || !idValid || tenantOutOfScope) {
      return;
    }
    const id = window.setTimeout(() => {
      void loadTree();
    }, 0);
    return () => window.clearTimeout(id);
  }, [canAccessAdministration, authLoading, idValid, loadTree, tenantOutOfScope]);

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
