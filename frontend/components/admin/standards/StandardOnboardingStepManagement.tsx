/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Plus } from 'lucide-react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  defaultOnboardingStepConfig,
  type LocalOnboardingStep,
  normalizeOnboardingSteps,
  onboardingStepHasConfig,
  readOnboardingDocumentKeys,
  readOnboardingMinimumCount,
} from '@/components/admin/standards/onboardingStepUtils';
import { StandardOnboardingStepAddModal } from '@/components/admin/standards/StandardOnboardingStepAddModal';
import { StandardOnboardingStepCard } from '@/components/admin/standards/StandardOnboardingStepCard';
import {
  type OnboardingStepConfigDraft,
  StandardOnboardingStepConfigModal,
} from '@/components/admin/standards/StandardOnboardingStepConfigModal';
import { useOnboardingStepReorder } from '@/components/admin/standards/useOnboardingStepReorder';
import type { StandardContentState } from '@/components/admin/standards/useStandardContent';
import { OrganizationManageSection } from '@/components/organization/OrganizationManageSection';
import { Button } from '@/components/ui/button';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { ListContainer } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { SectionActions } from '@/components/ui/section-header';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { type OnboardingStepType, standardsApi } from '@/lib/api/standards';
import { tenantOnboardingDocumentsApi } from '@/lib/api/tenantOnboardingDocuments';
import { getAdminStandardOnboardingStepPath } from '@/lib/navigation/adminWorkspace';
import { cn } from '@/lib/utils/cn';

interface Props {
  state: StandardContentState;
  embedded?: boolean;
}

export function StandardOnboardingStepManagement({ state, embedded = false }: Props) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const { tenantId, standardId, editable } = state;

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [steps, setSteps] = useState<LocalOnboardingStep[]>([]);
  const [availableTypes, setAvailableTypes] = useState<OnboardingStepType[]>([]);
  const [catalog, setCatalog] = useState<
    Awaited<ReturnType<typeof tenantOnboardingDocumentsApi.list>>
  >([]);
  const [catalogReady, setCatalogReady] = useState(false);
  const [addOpen, setAddOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [configDraft, setConfigDraft] = useState<OnboardingStepConfigDraft | null>(null);
  const [pendingAddType, setPendingAddType] = useState<OnboardingStepType | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<OnboardingStepType | null>(null);

  const loadCatalog = useCallback(async () => {
    try {
      const documents = await tenantOnboardingDocumentsApi.list(tenantId);
      setCatalog(
        documents.filter(
          (document) => !document.archived && document.active && document.hasTemplate,
        ),
      );
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.standards.onboardingSteps.loadCatalogFailed'),
      );
      setCatalog([]);
    } finally {
      setCatalogReady(true);
    }
  }, [showError, t, tenantId]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [stepsResponse, typesResponse] = await Promise.all([
        standardsApi.getOnboardingSteps(tenantId, standardId),
        standardsApi.listOnboardingStepTypes(tenantId),
      ]);
      const loaded = normalizeOnboardingSteps(
        [...stepsResponse.steps].sort((a, b) => a.sortOrder - b.sortOrder),
      );
      setSteps(loaded);
      setAvailableTypes(typesResponse.stepTypes.map((item) => item.stepType));
      if (loaded.some((step) => step.stepType === 'ENTRY_DOCUMENTS')) {
        await loadCatalog();
      }
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.standards.errors.saveFailed'));
      setSteps([]);
    } finally {
      setLoading(false);
    }
  }, [loadCatalog, showError, standardId, t, tenantId]);

  useEffect(() => {
    void load();
  }, [load]);

  const persistSteps = useCallback(
    async (nextSteps: LocalOnboardingStep[]): Promise<boolean> => {
      if (!editable) return false;
      const normalized = normalizeOnboardingSteps(nextSteps);
      setSaving(true);
      try {
        const saved = await standardsApi.replaceOnboardingSteps(tenantId, standardId, {
          steps: normalized,
        });
        setSteps(
          normalizeOnboardingSteps([...saved.steps].sort((a, b) => a.sortOrder - b.sortOrder)),
        );
        return true;
      } catch (e) {
        showError(e instanceof Error ? e.message : t('admin.standards.errors.saveFailed'));
        return false;
      } finally {
        setSaving(false);
      }
    },
    [editable, showError, standardId, t, tenantId],
  );

  const unusedTypes = useMemo(
    () => availableTypes.filter((type) => !steps.some((step) => step.stepType === type)),
    [availableTypes, steps],
  );

  const reorder = useOnboardingStepReorder({
    steps,
    disabled: saving || !editable,
    onReorder: async (orderedTypes) => {
      const byType = new Map(steps.map((step) => [step.stepType, step]));
      const next = orderedTypes
        .map((type) => byType.get(type))
        .filter((step): step is LocalOnboardingStep => step != null);
      const ok = await persistSteps(next);
      if (ok) showSuccess(t('admin.standards.onboardingSteps.reordered'));
    },
  });

  async function handleAddStep(type: OnboardingStepType) {
    if (!editable) return;
    if (onboardingStepHasConfig(type)) {
      setAddOpen(false);
      if (type === 'ENTRY_DOCUMENTS' && !catalogReady) {
        await loadCatalog();
      }
      setConfigDraft({
        stepType: type,
        documentKeys: [],
        minimumCount: '1',
      });
      setPendingAddType(type);
      setEditOpen(true);
      return;
    }
    const config = defaultOnboardingStepConfig(type);
    const next = [...steps, { stepType: type, sortOrder: steps.length, config }];
    const ok = await persistSteps(next);
    if (!ok) return;
    setAddOpen(false);
  }

  async function handleDelete() {
    if (deleteTarget == null) return;
    const ok = await persistSteps(steps.filter((step) => step.stepType !== deleteTarget));
    if (ok) {
      showSuccess(t('admin.standards.onboardingSteps.deleted'));
      setDeleteTarget(null);
    }
  }

  function openEdit(step: LocalOnboardingStep) {
    setConfigDraft({
      stepType: step.stepType,
      documentKeys: readOnboardingDocumentKeys(step.config),
      minimumCount: String(readOnboardingMinimumCount(step.config)),
    });
    if (step.stepType === 'ENTRY_DOCUMENTS' && !catalogReady) {
      void loadCatalog();
    }
    setEditOpen(true);
  }

  function closeConfigModal() {
    if (saving) return;
    setEditOpen(false);
    setConfigDraft(null);
    setPendingAddType(null);
  }

  async function saveConfig() {
    if (!configDraft) return;
    const config: Record<string, unknown> =
      configDraft.stepType === 'ENTRY_DOCUMENTS'
        ? { documentConfigKeys: configDraft.documentKeys }
        : configDraft.stepType === 'EMERGENCY_CONTACTS'
          ? { minimumCount: Number.parseInt(configDraft.minimumCount, 10) }
          : {};

    const isNewStep = pendingAddType === configDraft.stepType;
    const next = isNewStep
      ? [...steps, { stepType: configDraft.stepType, sortOrder: steps.length, config }]
      : steps.map((item) => (item.stepType === configDraft.stepType ? { ...item, config } : item));
    const ok = await persistSteps(next);
    if (ok) {
      showSuccess(
        isNewStep
          ? t('admin.standards.onboardingSteps.added')
          : t('admin.standards.onboardingSteps.configSaved'),
      );
      closeConfigModal();
    }
  }

  const subtitle =
    steps.length > 1
      ? `${t('admin.standards.onboardingSteps.count', { count: steps.length })} · ${t('admin.standards.onboardingSteps.dragHint')}`
      : steps.length === 1
        ? t('admin.standards.onboardingSteps.count', { count: steps.length })
        : t('admin.standards.onboardingSteps.subtitle');

  const addAction =
    editable && unusedTypes.length > 0 ? (
      <SectionActions layout="inline">
        <Button
          type="button"
          variant="primary"
          size="xs"
          disabled={saving}
          fullWidth="responsive"
          icon={<Plus className="h-3.5 w-3.5" aria-hidden />}
          onClick={() => setAddOpen(true)}
        >
          {t('common.add')}
        </Button>
      </SectionActions>
    ) : undefined;

  const body = loading ? (
    <LoadingState variant="embedded" size="sm" />
  ) : steps.length === 0 ? (
    <EmptyStateCard>{t('admin.standards.onboardingSteps.empty')}</EmptyStateCard>
  ) : (
    <ListContainer
      label={t('admin.standards.onboardingSteps.title')}
      className={cn(reorder.isDragging && 'select-none')}
    >
      {steps.map((step, index) => (
        <StandardOnboardingStepCard
          key={step.stepType}
          step={step}
          sortIndex={index}
          href={getAdminStandardOnboardingStepPath(tenantId, standardId, step.stepType)}
          configurable={onboardingStepHasConfig(step.stepType)}
          isReordering={saving}
          isDragging={reorder.draggedType === step.stepType}
          isDragOver={reorder.dragOverType === step.stepType}
          reorderable={editable && steps.length > 1}
          onReorderPointerDown={(event) => reorder.onHandlePointerDown(step.stepType, event)}
          onEdit={
            editable && onboardingStepHasConfig(step.stepType) ? () => openEdit(step) : undefined
          }
          onDelete={() => setDeleteTarget(step.stepType)}
          actionsDisabled={saving || !editable}
        />
      ))}
    </ListContainer>
  );

  return (
    <>
      <OrganizationManageSection
        embedded={embedded}
        title={t('admin.standards.onboardingSteps.title')}
        subtitle={subtitle}
        action={addAction}
      >
        {body}
      </OrganizationManageSection>

      <StandardOnboardingStepAddModal
        isOpen={addOpen}
        onClose={() => setAddOpen(false)}
        unusedTypes={unusedTypes}
        onSelect={(type) => void handleAddStep(type)}
        disabled={saving}
      />

      {configDraft && editOpen ? (
        <StandardOnboardingStepConfigModal
          isOpen={editOpen}
          onClose={closeConfigModal}
          draft={configDraft}
          onChange={setConfigDraft}
          onSave={() => void saveConfig()}
          catalog={catalog}
          catalogReady={catalogReady}
          saving={saving}
        />
      ) : null}

      <ConfirmDialog
        isOpen={deleteTarget != null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => void handleDelete()}
        title={t('admin.standards.onboardingSteps.deleteConfirmTitle')}
        message={t('admin.standards.onboardingSteps.deleteConfirm')}
        confirmText={t('common.delete')}
        isLoading={saving}
        variant="danger"
      />
    </>
  );
}
