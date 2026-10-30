/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Pencil, Trash2 } from 'lucide-react';
import { useParams, useRouter } from 'next/navigation';
import { Suspense, useCallback, useEffect, useMemo, useState } from 'react';
import {
  AdminMobileLayout,
  OrgTreeSection,
  TenantNameEditModal,
  TenantPurgeModal,
  TenantSetupStatusCard,
} from '@/components/admin';
import {
  StandardDetailAction,
  StandardDetailCardHeader,
} from '@/components/admin/standards/StandardDetailCardHeader';
import { ActionRowIcon, ActionsSection } from '@/components/ui/actions-section';
import { Card } from '@/components/ui/card';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';
import { hasLeaderIssueInRoots, summarizeLeaderIssuesFromTree } from '@/lib/admin/orgLeaderIssues';
import {
  type AdminOrganizationTreeNodeDto,
  adminApi,
  type SetupOrganizationRequest,
  type TenantAdminDetailDto,
} from '@/lib/api/admin';
import {
  getAdminStandardsPath,
  getAdminTenantRetentionPath,
  getAdminTenantStandardsPath,
} from '@/lib/navigation/adminWorkspace';

const STANDARD_NOT_CONFIGURED_MESSAGE =
  'Clone the system standard and assign it to the organization type before creating organizations';

function TenantDetailPageInner() {
  const params = useParams();
  const router = useRouter();
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
  const [treeRoots, setTreeRoots] = useState<AdminOrganizationTreeNodeDto[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [nameModalOpen, setNameModalOpen] = useState(false);
  const [savingName, setSavingName] = useState(false);
  const [creatingRootOrg, setCreatingRootOrg] = useState(false);
  const [purgeModalOpen, setPurgeModalOpen] = useState(false);
  const [purgingTenant, setPurgingTenant] = useState(false);

  const tenantOutOfScope =
    !isGlobalAdmin && idValid && (authTenantId == null || tenantId !== authTenantId);

  const loadTenant = useCallback(async () => {
    if (!idValid) return;
    setLoading(true);
    try {
      const data = await adminApi.getTenant(tenantId);
      setTenant(data);
      setTreeRoots(null);
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

  async function handlePurgeTenant() {
    if (!tenant) return;
    setPurgingTenant(true);
    try {
      await adminApi.purgeTenant(tenant.id, { confirmationName: tenant.name });
      showSuccess(t('admin.tenantPurge.success'));
      setPurgeModalOpen(false);
      router.push('/admin/tenants');
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.errors.purgeTenantFailed'));
      throw e;
    } finally {
      setPurgingTenant(false);
    }
  }

  async function handlePatchName(id: number, name: string) {
    setSavingName(true);
    try {
      const updated = await adminApi.patchTenant(id, { name });
      setTenant(updated);
      setNameModalOpen(false);
      showSuccess(t('admin.tenantUpdated'));
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.errors.updateTenantFailed'));
      throw e;
    } finally {
      setSavingName(false);
    }
  }

  const displayTitle = tenant?.name || (idValid ? `#${tenantId}` : '');

  const leaderSummary = useMemo(
    () => (treeRoots ? summarizeLeaderIssuesFromTree(treeRoots) : null),
    [treeRoots],
  );

  const hasLeaderIssueHint = tenant ? hasLeaderIssueInRoots(tenant.rootOrganizations) : false;

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
          {nameModalOpen ? (
            <TenantNameEditModal
              key={`name-${tenant.id}-${tenant.name}`}
              isOpen
              onClose={() => setNameModalOpen(false)}
              isLoading={savingName}
              initialName={tenant.name}
              onSave={(name) => handlePatchName(tenant.id, name)}
            />
          ) : null}

          <Card className="p-3 sm:p-4">
            <StandardDetailCardHeader
              className="mb-0"
              title={displayTitle}
              actions={
                <StandardDetailAction
                  label={t('admin.tenantForm.editNameModalTitle')}
                  icon={<Pencil className="h-4 w-4" aria-hidden />}
                  onClick={() => setNameModalOpen(true)}
                  disabled={savingName}
                />
              }
            />
            <div className="mt-2 space-y-0.5 text-sm text-[var(--text-secondary)]">
              <p>
                {t('admin.tenantIdLabel')}: {tenant.id}
              </p>
              <p>
                {t('admin.tenantForm.abbreviation')}: {tenant.abbreviation}
              </p>
              <p>
                {t('admin.tenantForm.locale')}: {t('locale.deutsch')}
              </p>
              <p>
                {t('admin.tenantForm.federalState')}: {t(`federalState.${tenant.federalState}`)}
              </p>
            </div>
          </Card>

          <TenantSetupStatusCard
            standardsConfigured={tenant.standardsConfiguredForOrgCreation}
            hasOrganizations={tenant.rootOrganizations.length > 0}
            leaderSummary={leaderSummary}
            hasLeaderIssueHint={hasLeaderIssueHint}
          />

          <ListContainer>
            <ListRow
              href={getAdminTenantStandardsPath(tenant.id)}
              title={t('admin.tenantStandards.manageLink')}
              subtitle={t('admin.tenantStandards.subtitle')}
            />
            <ListRow
              href={getAdminStandardsPath(tenant.id)}
              title={t('admin.standards.manageOrgStandardsLink')}
              subtitle={t('admin.standards.subtitle')}
            />
            <ListRow
              href={getAdminTenantRetentionPath(tenant.id)}
              title={t('admin.retention.manageLink')}
              subtitle={t('admin.retention.subtitle')}
            />
          </ListContainer>

          <OrgTreeSection
            tenantId={tenant.id}
            rootOrganizations={tenant.rootOrganizations}
            isGlobalAdmin={isGlobalAdmin}
            standardsConfiguredForOrgCreation={tenant.standardsConfiguredForOrgCreation}
            onRootOrgCreated={handleCreateRootOrg}
            creatingRootOrg={creatingRootOrg}
            onTreeLoaded={setTreeRoots}
            showLeaderIssueBanner={false}
          />

          {isGlobalAdmin ? (
            <>
              <ActionsSection>
                <ListRow
                  leading={<ActionRowIcon icon={Trash2} />}
                  title={t('admin.tenantPurge.action')}
                  subtitle={t('admin.tenantPurge.actionHint')}
                  onClick={() => setPurgeModalOpen(true)}
                />
              </ActionsSection>
              {purgeModalOpen ? (
                <TenantPurgeModal
                  isOpen
                  onClose={() => setPurgeModalOpen(false)}
                  tenantName={tenant.name}
                  isLoading={purgingTenant}
                  onConfirm={handlePurgeTenant}
                />
              ) : null}
            </>
          ) : null}
        </div>
      )}
    </AdminMobileLayout>
  );
}

export default function TenantDetailPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[var(--page-bg)] pt-20">
          <LoadingState variant="embedded" />
        </div>
      }
    >
      <TenantDetailPageInner />
    </Suspense>
  );
}
