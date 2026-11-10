/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useState } from 'react';
import { StandardWorkflowEditFormFields } from '@/components/admin/standards/StandardWorkflowEditFormFields';
import type { StandardContentState } from '@/components/admin/standards/useStandardContent';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useTranslation } from '@/hooks/useTranslation';
import {
  type StandardContentItemDto,
  type StandardWorkflowDto,
  type StandardWorkflowSlotDto,
  standardsApi,
} from '@/lib/api/standards';
import { formBlockers } from '@/lib/utils';
import { workflowSlotRoleLabel } from '@/lib/workflows/slotRoles';

interface StandardWorkflowEditModalProps {
  state: StandardContentState;
  workflow: StandardWorkflowDto;
  /** Workflow type enum key; selects the type-specific role names. */
  typeKey: string;
  orgGroupItems: StandardContentItemDto[];
  onClose: () => void;
  /** Called after the workflow was saved, before the modal closes. */
  onSaved: () => void;
}

/** Edits the approver slot roles of a workflow; the enabled flag is sent unchanged. */
export function StandardWorkflowEditModal({
  state,
  workflow,
  typeKey,
  orgGroupItems,
  onClose,
  onSaved,
}: StandardWorkflowEditModalProps) {
  const t = useTranslation();
  const { tenantId, standardId, busy, runMutation } = state;
  const [slots, setSlots] = useState<StandardWorkflowSlotDto[]>(workflow.slots);

  const handleSlotChange = (slotRole: string, patch: Partial<StandardWorkflowSlotDto>) => {
    setSlots((current) =>
      current.map((slot) => (slot.slotRole === slotRole ? { ...slot, ...patch } : slot)),
    );
  };

  const blockers = formBlockers(
    slots.map((slot) => [
      `${workflowSlotRoleLabel(slot.slotRole, t, typeKey)}: ${t('admin.standards.approverLabel')}`,
      Boolean(slot.approverOrgGroupConfigKey),
    ]),
  );

  const handleSave = async () => {
    const ok = await runMutation(
      () =>
        standardsApi.updateWorkflow(tenantId, standardId, workflow.configKey, {
          enabled: workflow.enabled,
          slots,
        }),
      'admin.standards.workflowSaved',
      'admin.standards.errors.saveFailed',
    );
    if (ok) {
      onSaved();
      onClose();
    }
  };

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={t('admin.standards.editWorkflow')}
      size="lg"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleSave()}
          blockers={blockers}
          isLoading={busy}
        />
      }
    >
      <StandardWorkflowEditFormFields
        typeKey={typeKey}
        slots={slots}
        onSlotChange={handleSlotChange}
        orgGroupItems={orgGroupItems}
        disabled={busy}
      />
    </Modal>
  );
}
