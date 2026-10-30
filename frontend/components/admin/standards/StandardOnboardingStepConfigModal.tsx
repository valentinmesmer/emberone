/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { onboardingStepTypeKey } from '@/components/admin/standards/onboardingStepUtils';
import { StandardCatalogCheckboxList } from '@/components/admin/standards/StandardCatalogCheckboxList';
import { Input } from '@/components/ui/input';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useTranslation } from '@/hooks/useTranslation';
import type { OnboardingStepType } from '@/lib/api/standards';
import type { TenantOnboardingDocumentDefinitionDto } from '@/lib/api/tenantOnboardingDocuments';

export interface OnboardingStepConfigDraft {
  stepType: OnboardingStepType;
  documentKeys: string[];
  minimumCount: string;
}

interface StandardOnboardingStepConfigModalProps {
  isOpen: boolean;
  onClose: () => void;
  draft: OnboardingStepConfigDraft;
  onChange: (draft: OnboardingStepConfigDraft) => void;
  onSave: () => void;
  catalog: TenantOnboardingDocumentDefinitionDto[];
  catalogReady: boolean;
  saving?: boolean;
}

export function StandardOnboardingStepConfigModal({
  isOpen,
  onClose,
  draft,
  onChange,
  onSave,
  catalog,
  catalogReady,
  saving = false,
}: StandardOnboardingStepConfigModalProps) {
  const t = useTranslation();
  const stepKey = onboardingStepTypeKey(draft.stepType);

  const configValid = useMemo(() => {
    if (draft.stepType === 'ENTRY_DOCUMENTS') {
      return draft.documentKeys.length > 0;
    }
    if (draft.stepType === 'EMERGENCY_CONTACTS') {
      const parsed = Number.parseInt(draft.minimumCount, 10);
      return Number.isFinite(parsed) && parsed >= 1;
    }
    return true;
  }, [draft]);

  function toggleDocument(configKey: string) {
    const selected = new Set(draft.documentKeys);
    if (selected.has(configKey)) selected.delete(configKey);
    else selected.add(configKey);
    onChange({ ...draft, documentKeys: [...selected] });
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={t(`admin.standards.onboardingSteps.${stepKey}.title`)}
      size={draft.stepType === 'ENTRY_DOCUMENTS' ? 'lg' : 'sm'}
      footer={
        <ModalFooter onCancel={onClose} onSave={onSave} isValid={configValid} isLoading={saving} />
      }
    >
      <div className="space-y-3">
        <p className="text-sm text-[var(--text-secondary)]">
          {t(`admin.standards.onboardingSteps.${stepKey}.subtitle`)}
        </p>
        {draft.stepType === 'ENTRY_DOCUMENTS' ? (
          !catalogReady ? (
            <LoadingState variant="embedded" size="sm" />
          ) : catalog.length === 0 ? (
            <EmptyStateCard className="!p-3 text-sm">
              {t('admin.standards.onboardingSteps.entryDocuments.emptyCatalog')}
            </EmptyStateCard>
          ) : (
            <StandardCatalogCheckboxList
              idPrefix="onboarding-doc"
              maxHeightClassName="max-h-64"
              items={catalog.map((document) => ({
                id: document.configKey,
                label: document.name,
                subtitle: t('admin.tenantStandards.onboardingDocuments.abbreviation', {
                  key: document.abbreviation,
                }),
              }))}
              selected={new Set(draft.documentKeys)}
              onToggle={toggleDocument}
              disabled={saving}
            />
          )
        ) : null}
        {draft.stepType === 'EMERGENCY_CONTACTS' ? (
          <Input
            type="number"
            min={1}
            inputMode="numeric"
            required
            label={t('admin.standards.onboardingSteps.emergencyContacts.minimumCountLabel')}
            value={draft.minimumCount}
            disabled={saving}
            onChange={(e) => onChange({ ...draft, minimumCount: e.target.value })}
          />
        ) : null}
      </div>
    </Modal>
  );
}
