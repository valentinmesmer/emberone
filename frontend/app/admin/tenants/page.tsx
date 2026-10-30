/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import { Pencil } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { Suspense, useEffect, useState } from 'react';
import {
  AdminMobileLayout,
  emptyTenantMasterDataForm,
  TenantMasterDataModal,
  tenantDetailToMasterDataForm,
} from '@/components/admin';
import { Button } from '@/components/ui/button';
import { IconButton } from '@/components/ui/icon-button';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';
import { PageContentSection } from '@/components/ui/page-content-section';
import { PageHeader } from '@/components/ui/page-header';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { useToast } from '@/components/ui/toast';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';
import { adminApi, type TenantAdminDetailDto, type TenantDto } from '@/lib/api/admin';
import type { FederalState } from '@/lib/federal-state';
import { useUrlSyncedListState } from '@/lib/list';
import { adminTenantListDefinition } from '@/lib/list/adminTenantList';
import { getAdminTenantDetailPath } from '@/lib/navigation/adminWorkspace';
import { invalidateAdminTenantList, useAdminTenants } from '@/lib/store';

export default function AdminTenantsPage() {
  return (
    <Suspense fallback={<LoadingState variant="embedded" />}>
      <AdminTenantsListPage />
    </Suspense>
  );
}

function AdminTenantsListPage() {
  const {
    canAccessAdministration,
    isGlobalAdmin,
    tenantId: authTenantId,
    isLoading: authLoading,
  } = useAuth();
  const router = useRouter();
  const t = useTranslation();
  const { showSuccess, showError } = useToast();

  const queryClient = useQueryClient();
  const listState = useUrlSyncedListState(adminTenantListDefinition);
  const {
    data: tenants,
    isPending: tenantsLoading,
    error: tenantsError,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useAdminTenants({
    listQuery: listState.listQuery,
    enabled: canAccessAdministration && !authLoading && isGlobalAdmin,
  });

  const [addTenantOpen, setAddTenantOpen] = useState(false);
  const [configuredFederalStates, setConfiguredFederalStates] = useState<FederalState[]>([
    'BAYERN',
  ]);
  const [isSubmittingTenant, setIsSubmittingTenant] = useState(false);

  const [editDetail, setEditDetail] = useState<TenantAdminDetailDto | null>(null);
  const [editDetailLoadingId, setEditDetailLoadingId] = useState<number | null>(null);
  const [isUpdatingTenant, setIsUpdatingTenant] = useState(false);

  useEffect(() => {
    if (tenantsError) {
      showError(
        tenantsError instanceof Error ? tenantsError.message : t('admin.errors.tenantsLoadFailed'),
      );
    }
  }, [tenantsError, showError, t]);

  useEffect(() => {
    if (authLoading || !canAccessAdministration || isGlobalAdmin || authTenantId == null) {
      return;
    }
    router.replace(getAdminTenantDetailPath(authTenantId));
  }, [authLoading, authTenantId, canAccessAdministration, isGlobalAdmin, router]);

  useEffect(() => {
    if (!canAccessAdministration || authLoading || !isGlobalAdmin) {
      return;
    }
    const id = window.setTimeout(() => {
      void adminApi
        .listConfiguredFederalStates()
        .then(setConfiguredFederalStates)
        .catch(() => undefined);
    }, 0);
    return () => window.clearTimeout(id);
  }, [canAccessAdministration, authLoading, isGlobalAdmin]);

  async function openEditTenant(tenant: TenantDto) {
    setEditDetailLoadingId(tenant.id);
    try {
      const detail = await adminApi.getTenant(tenant.id);
      setEditDetail(detail);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.errors.tenantLoadFailed'));
    } finally {
      setEditDetailLoadingId(null);
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

  return (
    <AdminMobileLayout>
      <PageHeader
        title={isGlobalAdmin ? t('admin.tenantsListTitle') : t('admin.tenantsListTitleScoped')}
        description={isGlobalAdmin ? t('admin.subtitleGlobal') : t('admin.subtitleTenant')}
        size="compact"
        action={
          isGlobalAdmin ? (
            <Button
              variant="primary"
              size="xs"
              onClick={() => setAddTenantOpen(true)}
              fullWidth="responsive"
            >
              {t('common.add')}
            </Button>
          ) : undefined
        }
      />

      <PageContentSection>
        {isGlobalAdmin && addTenantOpen ? (
          <TenantMasterDataModal
            key="create-tenant"
            mode="create"
            isOpen
            onClose={() => setAddTenantOpen(false)}
            isLoading={isSubmittingTenant}
            initialValues={emptyTenantMasterDataForm(configuredFederalStates[0] ?? 'BAYERN')}
            configuredFederalStates={configuredFederalStates}
            onCreate={async (body) => {
              setIsSubmittingTenant(true);
              try {
                await adminApi.createTenant(body);
                showSuccess(t('admin.tenantCreatedMinimal'));
                setAddTenantOpen(false);
                await invalidateAdminTenantList(queryClient);
              } catch (e) {
                showError(e instanceof Error ? e.message : t('admin.errors.createTenantFailed'));
                throw e;
              } finally {
                setIsSubmittingTenant(false);
              }
            }}
          />
        ) : null}

        {editDetail ? (
          <TenantMasterDataModal
            key={`edit-${editDetail.id}`}
            mode="edit"
            tenantId={editDetail.id}
            isOpen
            onClose={() => setEditDetail(null)}
            isLoading={isUpdatingTenant}
            initialValues={tenantDetailToMasterDataForm(editDetail)}
            onPatch={async (id, patch) => {
              setIsUpdatingTenant(true);
              try {
                await adminApi.patchTenant(id, patch);
                showSuccess(t('admin.tenantUpdated'));
                setEditDetail(null);
                await invalidateAdminTenantList(queryClient);
              } catch (e) {
                showError(e instanceof Error ? e.message : t('admin.errors.updateTenantFailed'));
                throw e;
              } finally {
                setIsUpdatingTenant(false);
              }
            }}
          />
        ) : null}

        {isGlobalAdmin ? (
          <PaginatedListView
            definition={adminTenantListDefinition}
            state={listState}
            totalItems={tenants.length}
            isPending={tenantsLoading}
            emptyState={paginatedListEmptyCard(t('admin.tenantsEmpty'))}
            hasNextPage={!!hasNextPage}
            isFetchingNextPage={isFetchingNextPage}
            onLoadMore={() => void fetchNextPage()}
          >
            <ListContainer label={t('admin.tenantsListTitle')} className="mt-1 sm:mt-0">
              {tenants.map((tenant) => (
                <ListRow
                  key={tenant.id}
                  href={`/admin/tenants/${tenant.id}`}
                  title={tenant.name}
                  subtitle={`${t('admin.tenantIdLabel')}: ${tenant.id}`}
                  actions={
                    <IconButton
                      variant="action"
                      icon={<Pencil className="h-4 w-4" aria-hidden />}
                      aria-label={t('admin.editTenant')}
                      title={t('admin.editTenant')}
                      onClick={() => void openEditTenant(tenant)}
                      disabled={isUpdatingTenant || editDetailLoadingId === tenant.id}
                    />
                  }
                />
              ))}
            </ListContainer>
          </PaginatedListView>
        ) : null}
      </PageContentSection>
    </AdminMobileLayout>
  );
}
