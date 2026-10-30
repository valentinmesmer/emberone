/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Pencil, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useMemo, useState } from 'react';
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
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { FieldDisplay } from '@/components/ui/field-display';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import type { OnboardingStepType } from '@/lib/api/standards';
import { standardsApi } from '@/lib/api/standards';
import { tenantOnboardingDocumentsApi } from '@/lib/api/tenantOnboardingDocuments';
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

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [steps, setSteps] = useState<LocalOnboardingStep[]>([]);
  const [catalog, setCatalog] = useState<
    Awaited<ReturnType<typeof tenantOnboardingDocumentsApi.list>>
  >([]);
  const [catalogReady, setCatalogReady] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [configDraft, setConfigDraft] = useState<OnboardingStepConfigDraft | null>(null);

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
      const stepsResponse = await standardsApi.getOnboardingSteps(tenantId, standardId);
      const loaded = normalizeOnboardingSteps(
        [...stepsResponse.steps].sort((a, b) => a.sortOrder - b.sortOrder),
      );
      setSteps(loaded);
      if (loaded.some((step) => step.stepType === 'ENTRY_DOCUMENTS')) {
        await loadCatalog();
      } else {
        setCatalogReady(true);
      }
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.standards.errors.saveFailed'));
      setSteps([]);
      setCatalogReady(true);
    } finally {
      setLoading(false);
    }
  }, [loadCatalog, showError, standardId, t, tenantId]);

  useEffect(() => {
    void load();
  }, [load]);

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

  function openEdit() {
    if (!step) return;
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
              <>
                {configurable ? (
                  <StandardDetailAction
                    label={t('admin.standards.onboardingSteps.editConfig')}
                    icon={<Pencil className="h-4 w-4" aria-hidden />}
                    onClick={openEdit}
                    disabled={saving}
                  />
                ) : null}
                <StandardDetailAction
                  label={t('admin.standards.onboardingSteps.removeStep')}
                  icon={<Trash2 className="h-4 w-4" aria-hidden />}
                  onClick={() => setDeleteOpen(true)}
                  disabled={saving}
                />
              </>
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
