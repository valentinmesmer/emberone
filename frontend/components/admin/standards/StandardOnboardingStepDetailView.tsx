/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import { Pencil, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useCallback, useMemo, useState } from 'react';
import {
  type LocalOnboardingStep,
  normalizeOnboardingSteps,
  onboardingStepHasConfig,
  onboardingStepTypeKey,
  readOnboardingDocumentKeys,
  readOnboardingMinimumCount,
} from '@/components/admin/standards/onboardingStepUtils';
import {
  StandardDetailAction,
  StandardDetailCardHeader,
} from '@/components/admin/standards/StandardDetailCardHeader';
import {
  type OnboardingStepConfigDraft,
  StandardOnboardingStepConfigModal,
} from '@/components/admin/standards/StandardOnboardingStepConfigModal';
import type { StandardContentState } from '@/components/admin/standards/useStandardContent';
import { ActionRowIcon, ActionsSection } from '@/components/ui/actions-section';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { FieldDisplay } from '@/components/ui/field-display';
import { ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import type { OnboardingStepType } from '@/lib/api/standards';
import { standardsApi } from '@/lib/api/standards';
import {
  adminTenantCatalogKeys,
  useQueryErrorToast,
  useStandardOnboardingSteps,
  useTenantOnboardingDocuments,
} from '@/lib/store';
import { getAdminStandardDetailPath } from '@/lib/navigation/adminWorkspace';

interface StandardOnboardingStepDetailViewProps {
  state: StandardContentState;
  stepType: OnboardingStepType;
}

export function StandardOnboardingStepDetailView({
  state,
  stepType,
}: StandardOnboardingStepDetailViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError } = useToast();
  const { tenantId, standardId, editable } = state;

  const [saving, setSaving] = useState(false);
  const [catalogRequested, setCatalogRequested] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [configDraft, setConfigDraft] = useState<OnboardingStepConfigDraft | null>(null);

  const queryClient = useQueryClient();
  const stepsQuery = useStandardOnboardingSteps(tenantId, standardId);
  useQueryErrorToast(stepsQuery.error, { messageKey: 'admin.standards.errors.saveFailed' });
  const loading = stepsQuery.isPending;
  const steps = useMemo(
    () =>
      normalizeOnboardingSteps(
        [...(stepsQuery.data?.steps ?? [])].sort((a, b) => a.sortOrder - b.sortOrder),
      ),
    [stepsQuery.data],
  );
  const catalogWanted =
    catalogRequested || steps.some((item) => item.stepType === 'ENTRY_DOCUMENTS');
  const catalogQuery = useTenantOnboardingDocuments(tenantId, catalogWanted);
  useQueryErrorToast(catalogQuery.error, {
    messageKey: 'admin.standards.onboardingSteps.loadCatalogFailed',
  });
  const catalog = useMemo(
    () =>
      (catalogQuery.data ?? []).filter(
        (document) => !document.archived && document.active && document.hasTemplate,
      ),
    [catalogQuery.data],
  );
  const catalogReady = !loading && (!catalogWanted || !catalogQuery.isPending);

  const step = useMemo(() => steps.find((item) => item.stepType === stepType), [steps, stepType]);

  const stepKey = onboardingStepTypeKey(stepType);
  const configurable = onboardingStepHasConfig(stepType);

  const persistSteps = useCallback(
    async (nextSteps: LocalOnboardingStep[]): Promise<boolean> => {
      if (!editable) return false;
      const normalized = normalizeOnboardingSteps(nextSteps);
      setSaving(true);
      try {
        const saved = await standardsApi.replaceOnboardingSteps(tenantId, standardId, {
          steps: normalized,
        });
        queryClient.setQueryData(
          adminTenantCatalogKeys.standardOnboardingSteps(tenantId, standardId),
          saved,
        );
        return true;
      } catch (e) {
        showError(e instanceof Error ? e.message : t('admin.standards.errors.saveFailed'));
        return false;
      } finally {
        setSaving(false);
      }
    },
    [editable, queryClient, showError, standardId, t, tenantId],
  );

  function openEdit() {
    if (!step) return;
    setConfigDraft({
      stepType: step.stepType,
      documentKeys: readOnboardingDocumentKeys(step.config),
      minimumCount: String(readOnboardingMinimumCount(step.config)),
    });
    if (step.stepType === 'ENTRY_DOCUMENTS') {
      setCatalogRequested(true);
    }
    setEditOpen(true);
  }

  async function saveConfig() {
    if (!configDraft || !step) return;
    const config: Record<string, unknown> =
      configDraft.stepType === 'ENTRY_DOCUMENTS'
        ? { documentConfigKeys: configDraft.documentKeys }
        : configDraft.stepType === 'EMERGENCY_CONTACTS'
          ? { minimumCount: Number.parseInt(configDraft.minimumCount, 10) }
          : {};

    const next = steps.map((item) =>
      item.stepType === step.stepType ? { ...item, config } : item,
    );
    const ok = await persistSteps(next);
    if (ok) {
      setEditOpen(false);
      setConfigDraft(null);
    }
  }

  async function handleDelete() {
    if (!step) return;
    const ok = await persistSteps(steps.filter((item) => item.stepType !== step.stepType));
    if (ok) {
      setDeleteOpen(false);
      router.push(getAdminStandardDetailPath(tenantId, standardId));
    }
  }

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!step) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  return (
    <div className="w-full space-y-3">
      <Card className="p-3 sm:p-4">
        <StandardDetailCardHeader
          title={t(`admin.standards.onboardingSteps.${stepKey}.title`)}
          subtitle={t(`admin.standards.onboardingSteps.${stepKey}.subtitle`)}
          actions={
            editable ? (
              configurable ? (
                <StandardDetailAction
                  label={t('admin.standards.onboardingSteps.editConfig')}
                  icon={<Pencil className="h-4 w-4" aria-hidden />}
                  onClick={openEdit}
                  disabled={saving}
                />
              ) : null
            ) : undefined
          }
        />

        <div className="space-y-3">
          <FieldDisplay
            label={t('admin.standards.onboardingSteps.sortOrderLabel')}
            value={String(step.sortOrder + 1)}
          />
          {step.stepType === 'ENTRY_DOCUMENTS' ? (
            <FieldDisplay
              label={t('admin.standards.onboardingSteps.entryDocuments.selectedDocuments')}
              value={
                readOnboardingDocumentKeys(step.config).length > 0
                  ? readOnboardingDocumentKeys(step.config)
                      .map(
                        (key) =>
                          catalog.find((document) => document.configKey === key)?.name ?? key,
                      )
                      .join(', ')
                  : t('admin.standards.onboardingSteps.entryDocuments.noneSelected')
              }
            />
          ) : null}
          {step.stepType === 'ORGANIZATION_ENTRY_DOCUMENTS' ? (
            <FieldDisplay
              label={t('admin.standards.onboardingSteps.organizationEntryDocuments.title')}
              value={t('admin.standards.onboardingSteps.organizationEntryDocuments.subtitle')}
            />
          ) : null}
          {step.stepType === 'EMERGENCY_CONTACTS' ? (
            <FieldDisplay
              label={t('admin.standards.onboardingSteps.emergencyContacts.minimumCountLabel')}
              value={String(readOnboardingMinimumCount(step.config))}
            />
          ) : null}
        </div>
      </Card>

      {editable ? (
        <ActionsSection>
          <ListRow
            leading={<ActionRowIcon icon={Trash2} />}
            title={t('admin.standards.onboardingSteps.removeStep')}
            subtitle={t('common.deleteHint')}
            onClick={saving ? undefined : () => setDeleteOpen(true)}
          />
        </ActionsSection>
      ) : null}

      {configDraft && editOpen ? (
        <StandardOnboardingStepConfigModal
          isOpen={editOpen}
          onClose={() => {
            if (saving) return;
            setEditOpen(false);
            setConfigDraft(null);
          }}
          draft={configDraft}
          onChange={setConfigDraft}
          onSave={() => void saveConfig()}
          catalog={catalog}
          catalogReady={catalogReady}
          saving={saving}
        />
      ) : null}

      <ConfirmDialog
        isOpen={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onConfirm={() => void handleDelete()}
        title={t('admin.standards.onboardingSteps.deleteConfirmTitle')}
        message={t('admin.standards.onboardingSteps.deleteConfirm')}
        confirmText={t('admin.standards.onboardingSteps.removeStep')}
        isLoading={saving}
        variant="danger"
      />
    </div>
  );
}
