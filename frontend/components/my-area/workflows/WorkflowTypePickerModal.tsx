/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ListContainer, ListRow } from '@/components/ui/list';
import { Modal } from '@/components/ui/modal';
import { useTranslation } from '@/hooks/useTranslation';
import { workflowTypeTranslationKey } from '@/lib/utils/workflowLabels';
import type { WorkflowTypeCatalogDto } from '@/types/workflow';

export interface WorkflowTypePickerModalProps {
  isOpen: boolean;
  onClose: () => void;
  types: WorkflowTypeCatalogDto[];
  onSelect: (workflowTypeId: number) => void;
}

export function WorkflowTypePickerModal({
  isOpen,
  onClose,
  types,
  onSelect,
}: WorkflowTypePickerModalProps) {
  const t = useTranslation();

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={t('myArea.workflowsOpened.pickTypeTitle')}
      size="md"
    >
      <p className="mb-4 text-sm text-[var(--text-secondary)]">
        {t('myArea.workflowsOpened.pickTypeIntro')}
      </p>
      <ListContainer label={t('myArea.workflowsOpened.pickTypeTitle')} variant="inset">
        {types.map((type) => {
          const label = t(workflowTypeTranslationKey(type.workflowTypeId));

          return (
            <ListRow
              key={type.workflowTypeId}
              title={label}
              onClick={() => onSelect(type.workflowTypeId)}
              chevron
            />
          );
        })}
      </ListContainer>
    </Modal>
  );
}
