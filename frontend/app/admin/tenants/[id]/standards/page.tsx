/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { CheckCircle2, Circle, Copy, Trash2 } from 'lucide-react';
import { useParams, useRouter } from 'next/navigation';
import { Suspense, useCallback, useEffect, useMemo, useState } from 'react';
import { AdminMobileLayout } from '@/components/admin';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { IconButton } from '@/components/ui/icon-button';
import { Input } from '@/components/ui/input';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyState, NoticeCard, statusIconToneClasses } from '@/components/ui/message-cards';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { SectionHeader } from '@/components/ui/section-header';
import { Select } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';
import {
  formatStandardSwitchImpactMessage,
  switchImpactRequiresConfirmation,
} from '@/lib/admin/standardImpact';
import { type AdminOrganizationTreeNodeDto, adminApi } from '@/lib/api/admin';
import { type OrganizationTypeDto, organizationTypesApi } from '@/lib/api/organizationTypes';
import {
  type OrganizationStandardOverrideDto,
  type StandardAssignmentDto,
  type StandardDto,
  standardsApi,
} from '@/lib/api/standards';
import { compareText } from '@/lib/i18n/locale';
import { hasCustomStandard, isStandardsReadyForOrgCreation } from '@/lib/standards/readiness';
import { cn } from '@/lib/utils/cn';

function StandardsPageInner() {
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

  const [standards, setStandards] = useState<StandardDto[]>([]);
  const [assignments, setAssignments] = useState<StandardAssignmentDto[]>([]);
  const [overrides, setOverrides] = useState<OrganizationStandardOverrideDto[]>([]);
  const [organizationTypes, setOrganizationTypes] = useState<OrganizationTypeDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [assigningType, setAssigningType] = useState<number | null>(null);
  const [pendingAssign, setPendingAssign] = useState<{
    organizationTypeId: number;
    standardId: number;
  } | null>(null);
  const [assignImpactMessage, setAssignImpactMessage] = useState('');

  const [addOverrideOpen, setAddOverrideOpen] = useState(false);
  const [addOverrideOrgId, setAddOverrideOrgId] = useState('');
  const [addOverrideStandardId, setAddOverrideStandardId] = useState('');
  const [addOverrideOrgs, setAddOverrideOrgs] = useState<AdminOrganizationTreeNodeDto[]>([]);
  const [addOverrideOrgsLoading, setAddOverrideOrgsLoading] = useState(false);
  const [pendingOverrideAdd, setPendingOverrideAdd] = useState<{
    organizationId: number;
    standardId: number;
  } | null>(null);
  const [overrideImpactMessage, setOverrideImpactMessage] = useState('');

  const [cloneSource, setCloneSource] = useState<StandardDto | null>(null);
  const [cloneName, setCloneName] = useState('');
  const [cloneDescription, setCloneDescription] = useState('');
  const [deleteTarget, setDeleteTarget] = useState<StandardDto | null>(null);

  const tenantOutOfScope =
    !isGlobalAdmin && idValid && (authTenantId == null || tenantId !== authTenantId);

  const assignedStandardIds = useMemo(
    () => new Set(assignments.map((assignment) => assignment.standardId)),
    [assignments],
  );

  const organizationTypeNames = useMemo(
    () => new Map(organizationTypes.map((type) => [type.id, type.name])),
    [organizationTypes],
  );

  const load = useCallback(async () => {
    if (!idValid) return;
    setLoading(true);
    try {
      const [std, asg, ovr, types] = await Promise.all([
        standardsApi.listStandards(tenantId),
        standardsApi.listAssignments(tenantId),
        standardsApi.listOrganizationOverrides(tenantId),
        organizationTypesApi.list(tenantId),
      ]);
      setStandards(std);
      setAssignments(asg);
      setOverrides(ovr);
      setOrganizationTypes(types);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.standards.errors.loadFailed'));
    } finally {
      setLoading(false);
    }
  }, [idValid, tenantId, showError, t]);

  useEffect(() => {
    if (!canAccessAdministration || authLoading || !idValid || tenantOutOfScope) {
      return;
    }
    const id = window.setTimeout(() => {
      void load();
    }, 0);
    return () => window.clearTimeout(id);
  }, [canAccessAdministration, authLoading, idValid, tenantOutOfScope, load]);

  function openDetail(standard: StandardDto) {
    router.push(`/admin/tenants/${tenantId}/standards/${standard.id}`);
  }

  function openClone(source: StandardDto) {
    setCloneSource(source);
    setCloneName('');
    setCloneDescription('');
  }

  async function submitClone() {
    if (!cloneSource || !cloneName.trim()) return;
    setBusy(true);
    try {
      await standardsApi.cloneStandard(tenantId, {
        baseStandardId: cloneSource.id,
        name: cloneName.trim(),
        description: cloneDescription.trim() || undefined,
      });
      setCloneSource(null);
      showSuccess(t('admin.standards.created'));
      await load();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.standards.errors.cloneFailed'));
    } finally {
      setBusy(false);
    }
  }

  async function confirmDelete() {
    if (!deleteTarget || assignedStandardIds.has(deleteTarget.id)) return;
    setBusy(true);
    try {
      await standardsApi.deleteStandard(tenantId, deleteTarget.id);
      setDeleteTarget(null);
      showSuccess(t('admin.standards.deleted'));
      await load();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.standards.errors.deleteFailed'));
    } finally {
      setBusy(false);
    }
  }

  async function applyAssign(organizationTypeId: number, standardId: number) {
    setAssigningType(organizationTypeId);
    setBusy(true);
    try {
      await standardsApi.assignStandard(tenantId, organizationTypeId, { standardId });
      showSuccess(t('admin.standards.assigned'));
      await load();
    } catch (e) {
      const message = e instanceof Error ? e.message : '';
      if (message.includes('cannot be assigned')) {
        showError(t('admin.standards.errors.assignSystemFailed'));
      } else {
        showError(message || t('admin.standards.errors.assignFailed'));
      }
      await load();
    } finally {
      setBusy(false);
      setAssigningType(null);
    }
  }

  async function handleAssign(organizationTypeId: number, standardId: number) {
    setAssigningType(organizationTypeId);
    setBusy(true);
    try {
      const impact = await standardsApi.getAssignmentImpact(
        tenantId,
        organizationTypeId,
        standardId,
      );
      if (switchImpactRequiresConfirmation(impact)) {
        setPendingAssign({ organizationTypeId, standardId });
        setAssignImpactMessage(formatStandardSwitchImpactMessage(impact, t));
        return;
      }
      await applyAssign(organizationTypeId, standardId);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.standards.errors.assignFailed'));
      await load();
    } finally {
      setAssigningType(null);
      setBusy(false);
    }
  }

  async function confirmAssign() {
    if (!pendingAssign) return;
    const next = pendingAssign;
    setPendingAssign(null);
    await applyAssign(next.organizationTypeId, next.standardId);
  }

  const flattenOrgTree = useCallback((nodes: AdminOrganizationTreeNodeDto[]) => {
    const result: AdminOrganizationTreeNodeDto[] = [];
    const walk = (list: AdminOrganizationTreeNodeDto[]) => {
      for (const node of list) {
        result.push(node);
        if (node.children.length > 0) walk(node.children);
      }
    };
    walk(nodes);
    return result;
  }, []);

  const overrideOrgIds = useMemo(
    () => new Set(overrides.map((override) => override.organizationId)),
    [overrides],
  );

  const addOverrideCandidates = useMemo(
    () =>
      flattenOrgTree(addOverrideOrgs)
        .filter((org) => !overrideOrgIds.has(org.id))
        .sort((a, b) => compareText(a.name, b.name)),
    [addOverrideOrgs, overrideOrgIds, flattenOrgTree],
  );

  async function openAddOverride() {
    setAddOverrideOpen(true);
    setAddOverrideOrgId('');
    setAddOverrideStandardId(customStandards[0] ? String(customStandards[0].id) : '');
    setAddOverrideOrgsLoading(true);
    try {
      const tree = await adminApi.getAdminOrganizationTree(tenantId);
      setAddOverrideOrgs(tree.roots);
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.standards.overridesSection.loadOrgsFailed'),
      );
      setAddOverrideOpen(false);
    } finally {
      setAddOverrideOrgsLoading(false);
    }
  }

  function closeAddOverride() {
    setAddOverrideOpen(false);
    setAddOverrideOrgId('');
    setAddOverrideStandardId('');
    setAddOverrideOrgs([]);
  }

  async function applyAddOverride(organizationId: number, standardId: number) {
    setBusy(true);
    try {
      await standardsApi.setOrganizationStandard(tenantId, organizationId, { standardId });
      closeAddOverride();
      showSuccess(t('admin.standards.orgOverride.saved'));
      await load();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.standards.orgOverride.saveFailed'));
    } finally {
      setBusy(false);
    }
  }

  async function submitAddOverride() {
    const organizationId = Number.parseInt(addOverrideOrgId, 10);
    const standardId = Number.parseInt(addOverrideStandardId, 10);
    if (!Number.isFinite(organizationId) || !Number.isFinite(standardId)) return;

    setBusy(true);
    try {
      const impact = await standardsApi.getOrganizationStandardImpact(
        tenantId,
        organizationId,
        standardId,
      );
      if (switchImpactRequiresConfirmation(impact)) {
        setPendingOverrideAdd({ organizationId, standardId });
        setOverrideImpactMessage(formatStandardSwitchImpactMessage(impact, t));
        return;
      }
      await applyAddOverride(organizationId, standardId);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.standards.orgOverride.saveFailed'));
    } finally {
      setBusy(false);
    }
  }

  async function confirmOverrideAdd() {
    if (!pendingOverrideAdd) return;
    const next = pendingOverrideAdd;
    setPendingOverrideAdd(null);
    await applyAddOverride(next.organizationId, next.standardId);
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
      <AdminMobileLayout>
        <NoticeCard>{t('admin.accessDenied')}</NoticeCard>
      </AdminMobileLayout>
    );
  }

  if (!idValid || tenantOutOfScope) {
    return (
      <AdminMobileLayout>
        <NoticeCard>{t('admin.errors.tenantScopeDenied')}</NoticeCard>
      </AdminMobileLayout>
    );
  }

  const assignmentFor = (organizationTypeId: number): number | null =>
    assignments.find((a) => a.organizationTypeId === organizationTypeId)?.standardId ?? null;

  const systemStandards = standards.filter((s) => s.system);
  const customStandards = standards.filter((s) => !s.system);

  const assignableStandards = customStandards;
  const setupComplete = isStandardsReadyForOrgCreation(organizationTypes, standards, assignments);
  const cloneStepDone = hasCustomStandard(standards);

  const assignmentOptionsFor = (organizationTypeId: number) => {
    const assignedId = assignmentFor(organizationTypeId);
    const options: { value: string; label: string; disabled?: boolean }[] = [];

    if (assignedId == null) {
      options.push({ value: '', label: t('admin.standards.notAssigned'), disabled: true });
    }

    for (const standard of assignableStandards) {
      options.push({ value: String(standard.id), label: standard.name });
    }

    return options;
  };

  function renderStandardRow(standard: StandardDto) {
    const isAssigned = assignedStandardIds.has(standard.id);
    const canDelete = !standard.system && !isAssigned;

    return (
      <ListRow
        key={standard.id}
        compact
        onClick={() => openDetail(standard)}
        title={standard.name}
        subtitle={
          standard.description ? (
            <span className="line-clamp-1">{standard.description}</span>
          ) : undefined
        }
        badges={
          <Badge tone={standard.system ? 'neutral' : 'accent'}>
            {standard.system ? t('admin.standards.system') : t('admin.standards.custom')}
          </Badge>
        }
        actions={
          <>
            <IconButton
              variant="action"
              icon={<Copy className="h-4 w-4" aria-hidden />}
              aria-label={t('admin.standards.clone')}
              title={t('admin.standards.clone')}
              onClick={(event) => {
                event.stopPropagation();
                openClone(standard);
              }}
              disabled={busy}
            />
            {canDelete ? (
              <IconButton
                variant="action"
                icon={<Trash2 className="h-4 w-4" aria-hidden />}
                aria-label={t('common.delete')}
                title={t('common.delete')}
                onClick={(event) => {
                  event.stopPropagation();
                  setDeleteTarget(standard);
                }}
                disabled={busy}
              />
            ) : null}
          </>
        }
      />
    );
  }

  return (
    <AdminMobileLayout>
      <div className="w-full space-y-4">
        <Card className="p-3 sm:p-4">
          <div>
            <h1 className="text-base font-semibold text-[var(--text-primary)] sm:text-lg">
              {t('admin.standards.title')}
            </h1>
            <p className="mt-0.5 text-xs text-[var(--text-secondary)] sm:text-sm">
              {t('admin.standards.subtitle')}
            </p>
          </div>
        </Card>

        {loading ? (
          <LoadingState variant="embedded" size="sm" />
        ) : (
          <>
            {setupComplete ? (
              <Card className="p-3 sm:p-4">
                <div className="flex items-start gap-2.5">
                  <CheckCircle2
                    className={cn('mt-0.5 h-5 w-5 shrink-0', statusIconToneClasses.success)}
                    aria-hidden
                  />
                  <h2 className="text-sm font-semibold text-[var(--text-primary)]">
                    {t('admin.standards.setupCompleteTitle')}
                  </h2>
                </div>
              </Card>
            ) : null}

            <Card className="p-3 sm:p-4">
              <SectionHeader
                className="mb-3"
                title={t('admin.standards.step1Title')}
                subtitle={t('admin.standards.bundlesHint')}
              />
              {standards.length === 0 ? (
                <EmptyState>{t('admin.standards.empty')}</EmptyState>
              ) : (
                <div className="space-y-4">
                  {systemStandards.length > 0 ? (
                    <div>
                      <h3 className="text-xs font-semibold uppercase tracking-wide text-[var(--text-secondary)]">
                        {t('admin.standards.systemSectionTitle')}
                      </h3>
                      <ListContainer
                        label={t('admin.standards.systemSectionTitle')}
                        variant="inset"
                        className="mt-2"
                      >
                        {systemStandards.map(renderStandardRow)}
                      </ListContainer>
                    </div>
                  ) : null}

                  <div>
                    <h3 className="text-xs font-semibold uppercase tracking-wide text-[var(--text-secondary)]">
                      {t('admin.standards.customSectionTitle')}
                    </h3>
                    {customStandards.length === 0 ? (
                      <p className="mt-2 rounded-lg border border-dashed border-[var(--border-color)] px-3 py-2.5 text-sm text-[var(--text-secondary)]">
                        {t('admin.standards.customEmpty')}
                      </p>
                    ) : (
                      <ListContainer
                        label={t('admin.standards.customSectionTitle')}
                        variant="inset"
                        className="mt-2"
                      >
                        {customStandards.map(renderStandardRow)}
                      </ListContainer>
                    )}
                  </div>
                </div>
              )}
            </Card>

            <Card className="p-3 sm:p-4">
              <SectionHeader
                className="mb-3"
                title={t('admin.standards.step2Title')}
                subtitle={t('admin.standards.assignmentHint')}
              />
              <ListContainer label={t('admin.standards.assignmentsTitle')} variant="inset">
                {organizationTypes
                  .filter((type) => type.active)
                  .map((orgType) => {
                    const typeLabel = orgType.name;
                    const assignedId = assignmentFor(orgType.id);
                    const isSaving = assigningType === orgType.id;
                    const isAssigned = assignedId != null;
                    return (
                      <ListRow
                        key={orgType.id}
                        accent={false}
                        chevron={false}
                        compact
                        title={typeLabel}
                        trailing={
                          isAssigned ? (
                            <CheckCircle2
                              className={`h-3.5 w-3.5 shrink-0 ${statusIconToneClasses.success}`}
                              aria-hidden
                            />
                          ) : (
                            <Circle
                              className="h-3.5 w-3.5 shrink-0 text-[var(--text-secondary)]"
                              aria-hidden
                            />
                          )
                        }
                        actions={
                          <Select
                            size="sm"
                            width="auto"
                            aria-label={`${typeLabel} - ${t('admin.standards.assignmentsTitle')}`}
                            value={assignedId != null ? String(assignedId) : ''}
                            disabled={busy || assignableStandards.length === 0 || isSaving}
                            title={
                              assignableStandards.length === 0
                                ? t('admin.standards.assignmentNeedsClone')
                                : undefined
                            }
                            options={assignmentOptionsFor(orgType.id)}
                            onChange={(e) => {
                              const nextId = Number.parseInt(e.target.value, 10);
                              if (!Number.isFinite(nextId) || nextId === assignedId) return;
                              void handleAssign(orgType.id, nextId);
                            }}
                          />
                        }
                      />
                    );
                  })}
              </ListContainer>

              <div className="mt-4 border-t border-[var(--border-color)] pt-4">
                <SectionHeader
                  className="mb-3"
                  title={t('admin.standards.overridesSection.title')}
                  subtitle={t('admin.standards.overridesSection.hint')}
                  action={
                    <Button
                      type="button"
                      variant="primary"
                      size="xs"
                      onClick={() => void openAddOverride()}
                      disabled={busy || !cloneStepDone || customStandards.length === 0}
                      title={
                        !cloneStepDone || customStandards.length === 0
                          ? t('admin.standards.assignmentNeedsClone')
                          : undefined
                      }
                      fullWidth="responsive"
                    >
                      {t('common.add')}
                    </Button>
                  }
                />
                {overrides.length === 0 ? (
                  <EmptyState>{t('admin.standards.overridesSection.empty')}</EmptyState>
                ) : (
                  <ListContainer
                    label={t('admin.standards.overridesSection.title')}
                    variant="inset"
                  >
                    {overrides.map((override) => (
                      <ListRow
                        key={override.organizationId}
                        compact
                        onClick={() =>
                          router.push(
                            `/admin/tenants/${tenantId}/organizations/${override.organizationId}`,
                          )
                        }
                        title={override.organizationName}
                        subtitle={organizationTypeNames.get(override.organizationTypeId) ?? ''}
                        meta={
                          override.standardName ? (
                            <div className="mt-1 flex flex-wrap gap-1.5">
                              <Badge tone="accent">{override.standardName}</Badge>
                            </div>
                          ) : undefined
                        }
                      />
                    ))}
                  </ListContainer>
                )}
              </div>
            </Card>
          </>
        )}
      </div>

      {cloneSource ? (
        <Modal
          isOpen
          onClose={() => setCloneSource(null)}
          title={t('admin.standards.cloneTitle')}
          footer={
            <ModalFooter
              onCancel={() => setCloneSource(null)}
              onSave={() => void submitClone()}
              isLoading={busy}
              isValid={cloneName.trim().length > 0}
              cancelText={t('common.cancel')}
              saveText={t('common.save')}
            />
          }
        >
          <div className="space-y-3">
            <p className="text-sm text-[var(--text-secondary)]">
              {t('admin.standards.cloneFrom')}: <strong>{cloneSource.name}</strong>
            </p>
            <Input
              label={t('admin.standards.name')}
              value={cloneName}
              onChange={(e) => setCloneName(e.target.value)}
              disabled={busy}
            />
            <Textarea
              label={t('admin.standards.description')}
              value={cloneDescription}
              onChange={(e) => setCloneDescription(e.target.value)}
              disabled={busy}
            />
          </div>
        </Modal>
      ) : null}

      <ConfirmDialog
        isOpen={pendingAssign != null}
        onClose={() => setPendingAssign(null)}
        onConfirm={() => void confirmAssign()}
        title={t('admin.standards.impact.switch.title')}
        message={assignImpactMessage}
        confirmText={t('admin.standards.impact.switch.confirm')}
        cancelText={t('common.cancel')}
        variant="danger"
        isLoading={busy}
      />

      <ConfirmDialog
        isOpen={pendingOverrideAdd != null}
        onClose={() => setPendingOverrideAdd(null)}
        onConfirm={() => void confirmOverrideAdd()}
        title={t('admin.standards.impact.switch.title')}
        message={overrideImpactMessage}
        confirmText={t('admin.standards.impact.switch.confirm')}
        cancelText={t('common.cancel')}
        variant="danger"
        isLoading={busy}
      />

      {addOverrideOpen ? (
        <Modal
          isOpen
          onClose={closeAddOverride}
          title={t('admin.standards.overridesSection.addTitle')}
          footer={
            <ModalFooter
              onCancel={closeAddOverride}
              onSave={() => void submitAddOverride()}
              isLoading={busy}
              isValid={
                !addOverrideOrgsLoading &&
                Boolean(addOverrideOrgId) &&
                Boolean(addOverrideStandardId) &&
                addOverrideCandidates.length > 0
              }
              cancelText={t('common.cancel')}
              saveText={t('common.save')}
            />
          }
        >
          <div className="space-y-3">
            <p className="text-sm text-[var(--text-secondary)]">
              {t('admin.standards.overridesSection.addHint')}
            </p>
            {addOverrideOrgsLoading ? (
              <LoadingState variant="embedded" size="sm" />
            ) : addOverrideCandidates.length === 0 ? (
              <p className="text-sm text-[var(--text-secondary)]">
                {t('admin.standards.overridesSection.noOrgsAvailable')}
              </p>
            ) : (
              <>
                <Select
                  label={t('admin.standards.overridesSection.orgLabel')}
                  value={addOverrideOrgId}
                  options={[
                    {
                      value: '',
                      label: t('admin.standards.overridesSection.orgPlaceholder'),
                      disabled: true,
                    },
                    ...addOverrideCandidates.map((org) => ({
                      value: String(org.id),
                      label: `${org.name} (${organizationTypeNames.get(org.organizationTypeId) ?? ''})`,
                    })),
                  ]}
                  onChange={(e) => setAddOverrideOrgId(e.target.value)}
                  disabled={busy}
                />
                <Select
                  label={t('admin.standards.orgOverride.selectLabel')}
                  value={addOverrideStandardId}
                  options={customStandards.map((standard) => ({
                    value: String(standard.id),
                    label: standard.name,
                  }))}
                  onChange={(e) => setAddOverrideStandardId(e.target.value)}
                  disabled={busy || customStandards.length === 0}
                />
              </>
            )}
          </div>
        </Modal>
      ) : null}

      <ConfirmDialog
        isOpen={deleteTarget != null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => void confirmDelete()}
        message={t('admin.standards.deleteConfirm')}
        confirmText={t('common.delete')}
        cancelText={t('common.cancel')}
        isLoading={busy}
      />
    </AdminMobileLayout>
  );
}

export default function StandardsPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[var(--page-bg)] pt-20">
          <LoadingState variant="embedded" />
        </div>
      }
    >
      <StandardsPageInner />
    </Suspense>
  );
}
