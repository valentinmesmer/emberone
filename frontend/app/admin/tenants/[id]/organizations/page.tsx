/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { Suspense, useCallback, useEffect, useState } from 'react';
import { AdminMobileLayout, OrgTreeSection } from '@/components/admin';
import { Card } from '@/components/ui/card';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';
import {
  adminApi,
  type SetupOrganizationRequest,
  type TenantAdminDetailDto,
} from '@/lib/api/admin';

const STANDARD_NOT_CONFIGURED_MESSAGE =
  'Clone the system standard and assign it to the organization type before creating organizations';

function TenantOrganizationsIndexInner() {
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
  const { showSuccess, showError } = useToast();

  const [tenant, setTenant] = useState<TenantAdminDetailDto | null>(null);
  const [loading, setLoading] = useState(false);
  const [creatingRootOrg, setCreatingRootOrg] = useState(false);

  const tenantOutOfScope =
    !isGlobalAdmin && idValid && (authTenantId == null || tenantId !== authTenantId);

  const loadTenant = useCallback(async () => {
    if (!idValid) return;
    setLoading(true);
    try {
      const data = await adminApi.getTenant(tenantId);
      setTenant(data);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.errors.tenantLoadFailed'));
      setTenant(null);
    } finally {
      setLoading(false);
    }
  }, [idValid, tenantId, showError, t]);

  useEffect(() => {
    if (!canAccessAdministration || authLoading || !idValid || tenantOutOfScope) {
      return;
    }
    const id = window.setTimeout(() => {
      void loadTenant();
    }, 0);
    return () => window.clearTimeout(id);
  }, [canAccessAdministration, authLoading, idValid, loadTenant, tenantOutOfScope]);

  async function handleCreateRootOrg(data: SetupOrganizationRequest) {
    if (!tenant) return;
    setCreatingRootOrg(true);
    try {
      const updated = await adminApi.createRootOrganization(tenant.id, data);
      setTenant(updated);
      showSuccess(t('admin.tenantUpdated'));
    } catch (e) {
      const message = e instanceof Error ? e.message : '';
      if (message.includes(STANDARD_NOT_CONFIGURED_MESSAGE)) {
        showError(t('admin.standards.errors.notConfiguredForOrgCreation'));
      } else {
        showError(message || t('admin.errors.rootOrgCreateFailed'));
      }
      throw e;
    } finally {
      setCreatingRootOrg(false);
    }
  }

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
      ) : !tenant ? (
        <NoticeCard>{t('admin.errors.tenantLoadFailed')}</NoticeCard>
      ) : (
        <div className="w-full space-y-4">
          <Card className="p-3 sm:p-4">
            <div className="min-w-0">
              <h1 className="break-words text-lg font-semibold text-[var(--text-primary)]">
                {t('admin.orgTree.orgStructurePageTitle')}
              </h1>
              <p className="mt-1 text-sm text-[var(--text-secondary)]">{tenant.name}</p>
            </div>
          </Card>

          <OrgTreeSection
            tenantId={tenant.id}
            rootOrganizations={tenant.rootOrganizations}
            isGlobalAdmin={isGlobalAdmin}
            standardsConfiguredForOrgCreation={tenant.standardsConfiguredForOrgCreation}
            onRootOrgCreated={handleCreateRootOrg}
            creatingRootOrg={creatingRootOrg}
          />
        </div>
      )}
    </AdminMobileLayout>
  );
}

export default function TenantOrganizationsIndexPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[var(--page-bg)] pt-20">
          <LoadingState variant="embedded" />
        </div>
      }
    >
      <TenantOrganizationsIndexInner />
    </Suspense>
  );
}
