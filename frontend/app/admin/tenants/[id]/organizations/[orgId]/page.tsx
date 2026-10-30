/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import { ArrowRightLeft, Pencil, Power } from 'lucide-react';
import { useParams, useRouter } from 'next/navigation';
import { Suspense, useCallback, useEffect, useState } from 'react';
import {
  AdminDeactivateOrganizationModal,
  AdminMobileLayout,
  AdminMoveOrganizationModal,
} from '@/components/admin';
import { AdminLeadershipGroupSection } from '@/components/admin/AdminLeadershipGroupSection';
import { AdminOrganizationCourseOfferingTypeSection } from '@/components/admin/AdminOrganizationCourseOfferingTypeSection';
import { AdminOrganizationManagementSection } from '@/components/admin/AdminOrganizationManagementSection';
import { AdminOrganizationOnboardingDocumentSection } from '@/components/admin/AdminOrganizationOnboardingDocumentSection';
import { CreateChildOrgModal } from '@/components/admin/CreateChildOrgModal';
import { EditOrgModal } from '@/components/admin/EditOrgModal';
import { OrganizationStandardCard } from '@/components/admin/standards/OrganizationStandardCard';
import {
  StandardDetailAction,
  StandardDetailCardHeader,
} from '@/components/admin/standards/StandardDetailCardHeader';
import { CreateUserModal } from '@/components/organization/CreateUserModal';
import { ActionRowIcon, ActionsSection } from '@/components/ui/actions-section';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyState, NoticeCard } from '@/components/ui/message-cards';
import { SectionActions, SectionHeader } from '@/components/ui/section-header';
import { useToast } from '@/components/ui/toast';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';
import { type AdminOrganizationDetailDto, adminApi } from '@/lib/api/admin';
import { type OrganizationTypeDto, organizationTypesApi } from '@/lib/api/organizationTypes';
import { useRegisterAdminOrgBackHref } from '@/lib/navigation/backHrefOverride';
import {
  adminCourseOfferingTypeKeys,
  adminManagementAudienceKeys,
  invalidateAdminLeadershipGroupMemberships,
} from '@/lib/store';
import { getAdminRecordedOnboardingPath } from '@/lib/utils/adminPaths';
import type {
  CreateChildOrganizationRequest,
  UpdateOrganizationRequest,
} from '@/types/organization';
import type { CreateUserRequest } from '@/types/user';

const STANDARD_NOT_CONFIGURED_MESSAGE =
  'Clone the system standard and assign it to the organization type before creating organizations';

function TenantOrganizationDetailInner() {
  const params = useParams();
  const tenantParam = params?.id as string | undefined;
  const orgParam = params?.orgId as string | undefined;
  const tenantId = tenantParam ? Number.parseInt(tenantParam, 10) : NaN;
  const orgId = orgParam ? Number.parseInt(orgParam, 10) : NaN;
  const idsValid = Number.isFinite(tenantId) && tenantId > 0 && Number.isFinite(orgId) && orgId > 0;

  const {
    canAccessAdministration,
    isGlobalAdmin,
    tenantId: authTenantId,
    isLoading: authLoading,
  } = useAuth();
  const t = useTranslation();
  const { showSuccess, showError } = useToast();
  const queryClient = useQueryClient();
  const router = useRouter();

  const [org, setOrg] = useState<AdminOrganizationDetailDto | null>(null);
  const [children, setChildren] = useState<AdminOrganizationDetailDto[]>([]);
  const [organizationTypes, setOrganizationTypes] = useState<OrganizationTypeDto[]>([]);
  const [standardsConfiguredForOrgCreation, setStandardsConfiguredForOrgCreation] = useState(true);
  const [loading, setLoading] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);

  const [editOpen, setEditOpen] = useState(false);
  const [editLoading, setEditLoading] = useState(false);
  const [createChildOpen, setCreateChildOpen] = useState(false);
  const [createChildLoading, setCreateChildLoading] = useState(false);
  const [createLeaderOpen, setCreateLeaderOpen] = useState(false);
  const [createLeaderLoading, setCreateLeaderLoading] = useState(false);
  const [askOnboardingOpen, setAskOnboardingOpen] = useState(false);
  const [askOnboardingLoading, setAskOnboardingLoading] = useState(false);
  const [pendingLeaderUserId, setPendingLeaderUserId] = useState<number | null>(null);
  const [organizationStandardRevision, setOrganizationStandardRevision] = useState(0);
  const [moveOpen, setMoveOpen] = useState(false);
  const [moveLoading, setMoveLoading] = useState(false);
  const [deactivateOpen, setDeactivateOpen] = useState(false);
  const [deactivateLoading, setDeactivateLoading] = useState(false);

  const isDeactivated = Boolean(org?.deactivatedAt);

  const tenantOutOfScope =
    !isGlobalAdmin && idsValid && (authTenantId == null || tenantId !== authTenantId);

  const loadPage = useCallback(async () => {
    if (!idsValid) return;
    setLoading(true);
    setLoadFailed(false);
    try {
      const [orgData, childList, tenantData, orgTypes] = await Promise.all([
        adminApi.getAdminOrganization(tenantId, orgId),
        adminApi.getAdminChildOrganizations(tenantId, orgId),
        adminApi.getTenant(tenantId),
        organizationTypesApi.list(tenantId),
      ]);
      setOrg(orgData);
      setChildren(childList);
      setStandardsConfiguredForOrgCreation(tenantData.standardsConfiguredForOrgCreation);
      setOrganizationTypes(orgTypes);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.errors.orgDetailLoadFailed'));
      setOrg(null);
      setChildren([]);
      setLoadFailed(true);
    } finally {
      setLoading(false);
    }
  }, [idsValid, tenantId, orgId, showError, t]);

  useEffect(() => {
    if (!canAccessAdministration || authLoading || !idsValid || tenantOutOfScope) {
      return;
    }
    const id = window.setTimeout(() => {
      void loadPage();
    }, 0);
    return () => window.clearTimeout(id);
  }, [canAccessAdministration, authLoading, idsValid, loadPage, tenantOutOfScope]);

  const handleEdit = async (data: UpdateOrganizationRequest) => {
    if (!org) return;
    setEditLoading(true);
    try {
      const updated = await adminApi.updateAdminOrganization(tenantId, org.id, data);
      setOrg(updated);
      setEditOpen(false);
      showSuccess(t('organizationManagement.success.organizationUpdated'));
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.errors.orgUpdateFailed'));
      throw e;
    } finally {
      setEditLoading(false);
    }
  };

  const handleCreateChild = async (data: CreateChildOrganizationRequest) => {
    if (!org) return;
    setCreateChildLoading(true);
    try {
      const created = await adminApi.createAdminChildOrganization(tenantId, org.id, data);
      setChildren((prev) => [...prev, created]);
      setOrg((prev) => (prev ? { ...prev, hasChildren: true } : prev));
      setCreateChildOpen(false);
      showSuccess(t('organizationManagement.success.childCreated'));
    } catch (e) {
      const message = e instanceof Error ? e.message : '';
      if (message.includes(STANDARD_NOT_CONFIGURED_MESSAGE)) {
        showError(t('admin.standards.errors.notConfiguredForOrgCreation'));
      } else {
        showError(message || t('admin.errors.childOrgCreateFailed'));
      }
      throw e;
    } finally {
      setCreateChildLoading(false);
    }
  };

  const handleCreateLeaderUser = async (data: CreateUserRequest) => {
    if (!org) return;
    setCreateLeaderLoading(true);
    try {
      const updated = await adminApi.createAdminOrgLeaderUser(tenantId, org.id, data);
      setCreateLeaderOpen(false);
      showSuccess(t('admin.tenantUpdated'));
      invalidateAdminLeadershipGroupMemberships(queryClient, tenantId, org.id);
      await loadPage();

      const leaderUserId = updated.leader?.id ?? null;
      if (leaderUserId == null) return;

      const availability = await adminApi.getOnboardingAvailable(tenantId, org.id);
      if (!availability.available) return;

      setPendingLeaderUserId(leaderUserId);
      setAskOnboardingOpen(true);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.errors.leaderCreateFailed'));
      throw e;
    } finally {
      setCreateLeaderLoading(false);
    }
  };

  const handleMoveOrganization = async (newParentOrganizationId: number | null) => {
    if (!org) return;
    setMoveLoading(true);
    try {
      const updated = await adminApi.moveAdminOrganization(tenantId, org.id, {
        newParentOrganizationId,
      });
      setOrg(updated);
      setMoveOpen(false);
      showSuccess(t('admin.orgMove.success'));
      await loadPage();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.errors.orgUpdateFailed'));
      throw e;
    } finally {
      setMoveLoading(false);
    }
  };

  const handleDeactivateOrganization = async () => {
    if (!org) return;
    setDeactivateLoading(true);
    try {
      await adminApi.deactivateAdminOrganization(tenantId, org.id, {
        confirmationName: org.name,
      });
      setDeactivateOpen(false);
      showSuccess(t('admin.orgDeactivate.success'));
      await loadPage();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.errors.orgDeactivateFailed'));
      throw e;
    } finally {
      setDeactivateLoading(false);
    }
  };

  const handleStartOnboardingRecording = async () => {
    if (!org || pendingLeaderUserId == null) return;
    setAskOnboardingLoading(true);
    try {
      const detail = await adminApi.startAdminOnboardingRecording(
        tenantId,
        org.id,
        pendingLeaderUserId,
      );
      setAskOnboardingOpen(false);
      setPendingLeaderUserId(null);
      router.push(getAdminRecordedOnboardingPath(tenantId, org.id, detail.id));
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.leader.onboarding.startFailed'));
    } finally {
      setAskOnboardingLoading(false);
    }
  };

  // A standard change can add or remove the management, course offering and onboarding sections,
  // so reload the org (managementCapable) and drop the cached section data before re-rendering.
  const handleOrganizationStandardChanged = () => {
    setOrganizationStandardRevision((value) => value + 1);
    void queryClient.invalidateQueries({ queryKey: adminCourseOfferingTypeKeys.all });
    void queryClient.invalidateQueries({ queryKey: adminManagementAudienceKeys.all });
    void loadPage();
  };

  const backHrefActive = idsValid && canAccessAdministration && !authLoading && !tenantOutOfScope;
  useRegisterAdminOrgBackHref(tenantId, org, backHrefActive);

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
        <NoticeCard>{t('admin.errors.orgDetailLoadFailed')}</NoticeCard>
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
      {loading && !org ? (
        <LoadingState variant="embedded" size="sm" />
      ) : loadFailed || !org ? (
        <NoticeCard>{t('admin.errors.orgDetailLoadFailed')}</NoticeCard>
      ) : (
        <div className="w-full space-y-4">
          <Card className="p-3 sm:p-4">
            <StandardDetailCardHeader
              className="mb-0"
              title={org.name}
              subtitle={
                <>
                  {org.organizationCode} ·{' '}
                  {organizationTypes.find((type) => type.id === org.organizationTypeId)?.name ?? ''}
                </>
              }
              badges={
                isDeactivated ? (
                  <Badge tone="neutral">{t('admin.orgDeactivate.deactivatedBadge')}</Badge>
                ) : undefined
              }
              actions={
                !isDeactivated ? (
                  <StandardDetailAction
                    label={t('admin.orgTree.editOrg')}
                    icon={<Pencil className="h-4 w-4" aria-hidden />}
                    onClick={() => setEditOpen(true)}
                  />
                ) : undefined
              }
            />
          </Card>

          <AdminLeadershipGroupSection
            tenantId={tenantId}
            organizationId={org.id}
            onCreateUser={() => setCreateLeaderOpen(true)}
            onChanged={() => {
              void loadPage();
            }}
          />

          <OrganizationStandardCard
            tenantId={tenantId}
            organizationId={org.id}
            onStandardChanged={handleOrganizationStandardChanged}
          />

          <AdminOrganizationOnboardingDocumentSection
            key={organizationStandardRevision}
            tenantId={tenantId}
            organizationId={org.id}
          />

          <AdminOrganizationCourseOfferingTypeSection tenantId={tenantId} organizationId={org.id} />

          {org.managementCapable ? (
            <AdminOrganizationManagementSection tenantId={tenantId} organizationId={org.id} />
          ) : null}

          <Card className="p-3 sm:p-4">
            <SectionHeader
              className="mb-3"
              title={t('admin.orgTree.subOrganizationsTitle')}
              action={
                <SectionActions layout="inline">
                  <Button
                    type="button"
                    variant="primary"
                    size="xs"
                    onClick={() => setCreateChildOpen(true)}
                    fullWidth="responsive"
                    disabled={!standardsConfiguredForOrgCreation || isDeactivated}
                    title={
                      !standardsConfiguredForOrgCreation
                        ? t('admin.standards.orgCreationBlockedHint')
                        : undefined
                    }
                  >
                    {t('common.add')}
                  </Button>
                </SectionActions>
              }
            />
            {children.length === 0 ? (
              <EmptyState>{t('admin.orgTree.noChildren')}</EmptyState>
            ) : (
              <ListContainer label={t('admin.orgTree.subOrganizationsTitle')}>
                {children.map((child) => {
                  const showLeaderWarning = !child.leader && !child.deactivatedAt;
                  return (
                    <ListRow
                      key={child.id}
                      href={`/admin/tenants/${tenantId}/organizations/${child.id}`}
                      title={child.name}
                      badges={
                        showLeaderWarning ? (
                          <Badge tone="warning">{t('admin.orgTree.leaderMissingTitle')}</Badge>
                        ) : null
                      }
                      subtitle={<span className="font-mono">{child.organizationCode}</span>}
                    />
                  );
                })}
              </ListContainer>
            )}
          </Card>

          {!isDeactivated ? (
            <ActionsSection>
              <ListRow
                leading={<ActionRowIcon icon={ArrowRightLeft} />}
                title={t('admin.orgMove.action')}
                subtitle={t('admin.orgMove.actionHint')}
                onClick={() => setMoveOpen(true)}
              />
              <ListRow
                leading={<ActionRowIcon icon={Power} />}
                title={t('admin.orgDeactivate.action')}
                subtitle={t('admin.orgDeactivate.actionHint')}
                onClick={() => setDeactivateOpen(true)}
              />
            </ActionsSection>
          ) : null}

          <EditOrgModal
            isOpen={editOpen && !isDeactivated}
            onClose={() => setEditOpen(false)}
            org={org}
            onSave={handleEdit}
            isLoading={editLoading}
          />

          <CreateChildOrgModal
            isOpen={createChildOpen && standardsConfiguredForOrgCreation}
            onClose={() => setCreateChildOpen(false)}
            onCreate={handleCreateChild}
            isLoading={createChildLoading}
            organizationTypes={organizationTypes}
          />

          <CreateUserModal
            isOpen={createLeaderOpen}
            onClose={() => setCreateLeaderOpen(false)}
            onCreate={handleCreateLeaderUser}
            isLoading={createLeaderLoading}
          />

          <AdminMoveOrganizationModal
            isOpen={moveOpen}
            onClose={() => setMoveOpen(false)}
            tenantId={tenantId}
            organizationId={org.id}
            organizationName={org.name}
            isLoading={moveLoading}
            onMove={handleMoveOrganization}
          />

          <AdminDeactivateOrganizationModal
            isOpen={deactivateOpen}
            onClose={() => setDeactivateOpen(false)}
            organizationName={org.name}
            isLoading={deactivateLoading}
            onConfirm={handleDeactivateOrganization}
          />

          <ConfirmDialog
            isOpen={askOnboardingOpen}
            onClose={() => {
              if (askOnboardingLoading) return;
              setAskOnboardingOpen(false);
              setPendingLeaderUserId(null);
            }}
            onConfirm={() => void handleStartOnboardingRecording()}
            title={t('admin.leader.onboarding.askTitle')}
            message={t('admin.leader.onboarding.askMessage')}
            confirmText={t('admin.leader.onboarding.askConfirm')}
            cancelText={t('admin.leader.onboarding.askCancel')}
            variant="info"
            isLoading={askOnboardingLoading}
          />
        </div>
      )}
    </AdminMobileLayout>
  );
}

export default function TenantOrganizationDetailPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[var(--page-bg)] pt-20">
          <LoadingState variant="embedded" />
        </div>
      }
    >
      <TenantOrganizationDetailInner />
    </Suspense>
  );
}
