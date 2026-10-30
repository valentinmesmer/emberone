/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { onboardingStepTypeKey } from '@/components/admin/standards/onboardingStepUtils';
import { ListContainer, ListRow } from '@/components/ui/list';
import { Modal } from '@/components/ui/modal';
import { useTranslation } from '@/hooks/useTranslation';
import type { OnboardingStepType } from '@/lib/api/standards';

interface StandardOnboardingStepAddModalProps {
  isOpen: boolean;
  onClose: () => void;
  unusedTypes: OnboardingStepType[];
  onSelect: (type: OnboardingStepType) => void;
  disabled?: boolean;
}

export function StandardOnboardingStepAddModal({
  isOpen,
  onClose,
  unusedTypes,
  onSelect,
  disabled = false,
}: StandardOnboardingStepAddModalProps) {
  const t = useTranslation();

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={t('admin.standards.onboardingSteps.addStep')}
      size="md"
    >
      <div className="space-y-3">
        <p className="text-sm text-[var(--text-secondary)]">
          {t('admin.standards.onboardingSteps.addModalHint')}
        </p>
        <ListContainer label={t('admin.standards.onboardingSteps.addStepLabel')} variant="inset">
          {unusedTypes.map((type) => {
            const stepKey = onboardingStepTypeKey(type);
            return (
              <ListRow
                key={type}
                compact
                title={t(`admin.standards.onboardingSteps.${stepKey}.title`)}
                subtitle={t(`admin.standards.onboardingSteps.${stepKey}.subtitle`)}
                chevron={false}
                onClick={() => {
                  if (disabled) return;
                  onSelect(type);
                }}
                ariaLabel={t(`admin.standards.onboardingSteps.${stepKey}.title`)}
              />
            );
          })}
        </ListContainer>
      </div>
    </Modal>
  );
}
