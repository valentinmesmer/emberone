/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useState } from 'react';
import { PendingAttachments } from '@/components/attachment';
import { WorkflowInputChangeRequestNote } from '@/components/my-area/workflows/WorkflowInputChangeRequestNote';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Select } from '@/components/ui/select';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { workflowsApi } from '@/lib/api/workflows';
import { useMyAvailableRanks, useSubmitRankRequestWorkflow } from '@/lib/store';
import { useUpdateWorkflowPayload } from '@/lib/store/queries/workflows';
import { type RankRequestWorkflowPayload, WORKFLOW_TYPE_IDS } from '@/types/workflow';

export interface RankRequestSubmitModalProps {
  isOpen: boolean;
  onClose: () => void;
  editWorkflowId?: number | null;
  initialPayload?: RankRequestWorkflowPayload | null;
  requestMessage?: string | null;
  /** Optional rank list override (workflow edit host may include the selected rank). */
  ranks?: { id: number; name: string }[];
}

function modalBodyKey(editWorkflowId: number | null | undefined) {
  return editWorkflowId != null && editWorkflowId > 0 ? `edit-${editWorkflowId}` : 'submit';
}

export function RankRequestSubmitModal({
  isOpen,
  onClose,
  editWorkflowId = null,
  initialPayload = null,
  requestMessage = null,
  ranks: ranksOverride,
}: RankRequestSubmitModalProps) {
  if (!isOpen) return null;

  return (
    <RankRequestSubmitModalBody
      key={modalBodyKey(editWorkflowId)}
      isOpen={isOpen}
      onClose={onClose}
      editWorkflowId={editWorkflowId}
      initialPayload={initialPayload}
      requestMessage={requestMessage}
      ranksOverride={ranksOverride}
    />
  );
}

function RankRequestSubmitModalBody({
  isOpen,
  onClose,
  editWorkflowId = null,
  initialPayload = null,
  requestMessage = null,
  ranksOverride,
}: RankRequestSubmitModalProps & { ranksOverride?: { id: number; name: string }[] }) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const isEdit = editWorkflowId != null && editWorkflowId > 0;
  const { data: catalogRanks = [] } = useMyAvailableRanks(isOpen && !ranksOverride);
  const ranks = ranksOverride ?? catalogRanks;
  const submit = useSubmitRankRequestWorkflow();
  const updatePayload = useUpdateWorkflowPayload();
  const pending = isEdit ? updatePayload.isPending : submit.isPending;

  const [tenantRankId, setTenantRankId] = useState(() =>
    initialPayload?.tenantRankId != null ? String(initialPayload.tenantRankId) : '',
  );
  const [existingAttachmentIds] = useState(initialPayload?.attachmentIds ?? []);
  const [files, setFiles] = useState<File[]>([]);

  const handleSave = async () => {
    const parsed = Number.parseInt(tenantRankId, 10);
    if (!Number.isFinite(parsed)) return;
    const payload = { tenantRankId: parsed };

    try {
      if (isEdit && editWorkflowId != null) {
        const uploadedIds =
          files.length > 0
            ? (
                await Promise.all(
                  files.map((file) =>
                    workflowsApi.uploadWorkflowDraftAttachment(
                      WORKFLOW_TYPE_IDS.RANK_REQUEST,
                      file,
                    ),
                  ),
                )
              ).map((item) => item.id)
            : [];
        await updatePayload.mutateAsync({
          workflowId: editWorkflowId,
          payload: {
            ...payload,
            attachmentIds: [...existingAttachmentIds, ...uploadedIds],
          },
        });
        showSuccess(t('workflows.inputChange.updateSuccess'));
        onClose();
        return;
      }

      await submit.mutateAsync({ files, payload });
      showSuccess(t('ranks.request.success'));
      onClose();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('ranks.request.failed'));
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isEdit ? t('ranks.request.editTitle') : t('ranks.request.title')}
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleSave()}
          isValid={Boolean(tenantRankId)}
          isLoading={pending}
        />
      }
    >
      <div className="space-y-3">
        {requestMessage ? <WorkflowInputChangeRequestNote message={requestMessage} /> : null}
        <Select
          label={t('ranks.request.rank')}
          value={tenantRankId}
          onChange={(e) => setTenantRankId(e.target.value)}
          options={ranks.map((rank) => ({ value: String(rank.id), label: rank.name }))}
          disabled={pending}
        />
        <PendingAttachments
          variant="field"
          label={t('ranks.request.proofOptional')}
          files={files}
          onChange={setFiles}
          multiple={false}
          disabled={pending}
        />
      </div>
    </Modal>
  );
}
