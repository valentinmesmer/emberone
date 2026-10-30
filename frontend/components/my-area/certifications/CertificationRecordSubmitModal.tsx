/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo, useState } from 'react';
import { CertificationRecordFormFields } from '@/components/my-area/certifications/CertificationRecordFormFields';
import { WorkflowInputChangeRequestNote } from '@/components/my-area/workflows/WorkflowInputChangeRequestNote';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import type { CertificationStatusItemDto } from '@/lib/api/certifications';
import { ApiRequestError } from '@/lib/api/client';
import { workflowsApi } from '@/lib/api/workflows';
import { requiresSubmissionAttachment } from '@/lib/certifications/evidenceTypes';
import {
  useSubmitCertificationRecordWorkflow,
  useUpdateWorkflowPayload,
} from '@/lib/store/queries/workflows';
import {
  type CertificationRecordWorkflowPayload,
  WORKFLOW_CLIENT_ERROR_CODES,
  WORKFLOW_TYPE_IDS,
} from '@/types/workflow';

function todayIsoDate(): string {
  return new Date().toLocaleDateString('en-CA');
}

export interface CertificationRecordSubmitModalProps {
  isOpen: boolean;
  onClose: () => void;
  submittableItems: CertificationStatusItemDto[];
  /** Pre-selects the definition when opening the modal. */
  presetDefinitionId?: number;
  /** When set, updates an existing workflow payload instead of opening a new one. */
  editWorkflowId?: number | null;
  initialPayload?: CertificationRecordWorkflowPayload | null;
  /** Comment from an open input-change request, shown while editing. */
  requestMessage?: string | null;
}

export function CertificationRecordSubmitModal({
  isOpen,
  onClose,
  submittableItems,
  presetDefinitionId,
  editWorkflowId = null,
  initialPayload = null,
  requestMessage = null,
}: CertificationRecordSubmitModalProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const submit = useSubmitCertificationRecordWorkflow();
  const updatePayload = useUpdateWorkflowPayload();
  const isEdit = editWorkflowId != null && editWorkflowId > 0;
  const pending = isEdit ? updatePayload.isPending : submit.isPending;

  const resolveInitial = () => {
    if (isEdit && initialPayload) {
      return {
        selectedDefinitionId: initialPayload.tenantCertDefinitionId ?? 0,
        completedDate: initialPayload.completedDate,
        notes: initialPayload.notes ?? '',
        attachmentIds: initialPayload.attachmentIds ?? [],
      };
    }
    if (presetDefinitionId != null) {
      return {
        selectedDefinitionId: presetDefinitionId,
        completedDate: todayIsoDate(),
        notes: '',
        attachmentIds: [] as number[],
      };
    }
    return {
      selectedDefinitionId:
        submittableItems.length === 1 ? submittableItems[0].tenantCertDefinitionId : 0,
      completedDate: todayIsoDate(),
      notes: '',
      attachmentIds: [] as number[],
    };
  };

  const [selectedDefinitionId, setSelectedDefinitionId] = useState(
    () => resolveInitial().selectedDefinitionId,
  );
  const [completedDate, setCompletedDate] = useState(() => resolveInitial().completedDate);
  const [notes, setNotes] = useState(() => resolveInitial().notes);
  const [existingAttachmentIds, setExistingAttachmentIds] = useState(
    () => resolveInitial().attachmentIds,
  );
  const [files, setFiles] = useState<File[]>([]);
  const [prevIsOpen, setPrevIsOpen] = useState(isOpen);
  const depsKey = `${editWorkflowId ?? 'new'}:${JSON.stringify(initialPayload)}:${presetDefinitionId ?? ''}:${submittableItems.map((item) => item.tenantCertDefinitionId).join(',')}`;
  const [prevDepsKey, setPrevDepsKey] = useState(depsKey);

  if (isOpen !== prevIsOpen) {
    setPrevIsOpen(isOpen);
    if (isOpen) {
      const initial = resolveInitial();
      setSelectedDefinitionId(initial.selectedDefinitionId);
      setCompletedDate(initial.completedDate);
      setNotes(initial.notes);
      setExistingAttachmentIds(initial.attachmentIds);
      setFiles([]);
      setPrevDepsKey(depsKey);
    }
  } else if (isOpen && depsKey !== prevDepsKey) {
    setPrevDepsKey(depsKey);
    const initial = resolveInitial();
    setSelectedDefinitionId(initial.selectedDefinitionId);
    setCompletedDate(initial.completedDate);
    setNotes(initial.notes);
    setExistingAttachmentIds(initial.attachmentIds);
    setFiles([]);
  }

  const selectedItem = useMemo(
    () => submittableItems.find((item) => item.tenantCertDefinitionId === selectedDefinitionId),
    [selectedDefinitionId, submittableItems],
  );

  const attachmentRequired = requiresSubmissionAttachment(selectedItem?.evidenceTypes);

  const hasAttachments = existingAttachmentIds.length > 0 || files.length > 0;

  const canSubmit =
    selectedDefinitionId > 0 &&
    completedDate.trim().length > 0 &&
    (!attachmentRequired || hasAttachments) &&
    !pending;

  const handleSubmit = async () => {
    if (!canSubmit) return;
    try {
      if (isEdit) {
        if (editWorkflowId == null) return;
        const uploadedIds =
          files.length > 0
            ? (
                await Promise.all(
                  files.map((file) =>
                    workflowsApi.uploadWorkflowDraftAttachment(
                      WORKFLOW_TYPE_IDS.CERTIFICATION_RECORD,
                      file,
                    ),
                  ),
                )
              ).map((item) => item.id)
            : [];
        const payload: CertificationRecordWorkflowPayload = {
          tenantCertDefinitionId: selectedDefinitionId,
          completedDate,
          notes: notes.trim() || undefined,
          attachmentIds: [...existingAttachmentIds, ...uploadedIds],
        };
        await updatePayload.mutateAsync({ workflowId: editWorkflowId, payload });
        showSuccess(t('myArea.workflowDetail.editInputSuccess'));
      } else {
        await submit.mutateAsync({
          files,
          payload: {
            tenantCertDefinitionId: selectedDefinitionId,
            completedDate,
            notes: notes.trim() || undefined,
          },
        });
        showSuccess(t('certifications.submit.success'));
      }
      onClose();
    } catch (e) {
      if (
        e instanceof ApiRequestError &&
        e.code === WORKFLOW_CLIENT_ERROR_CODES.CERTIFICATION_DEFINITION_ALREADY_OPEN
      ) {
        showError(t('certifications.submit.errorAlreadyOpen'));
        return;
      }
      if (
        e instanceof ApiRequestError &&
        e.code === WORKFLOW_CLIENT_ERROR_CODES.TYPE_ALREADY_OPEN
      ) {
        showError(t('certifications.submit.errorAlreadyOpen'));
        return;
      }
      showError(
        e instanceof Error
          ? e.message
          : isEdit
            ? t('myArea.workflowDetail.editInputError')
            : t('certifications.submit.error'),
      );
    }
  };

  if (!isOpen) {
    return null;
  }

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={isEdit ? t('myArea.workflowDetail.editInputTitle') : t('certifications.submit.title')}
      size="md"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleSubmit()}
          isValid={canSubmit}
          isLoading={pending}
          saveText={
            isEdit ? t('myArea.workflowDetail.editInputSubmit') : t('certifications.submit.action')
          }
          showSave
        />
      }
    >
      <div className="space-y-4">
        {isEdit ? (
          <p className="text-sm text-[var(--text-secondary)]">
            {t('myArea.workflowDetail.editInputIntro')}
          </p>
        ) : null}

        {isEdit && requestMessage?.trim() ? (
          <WorkflowInputChangeRequestNote message={requestMessage} tone="neutral" />
        ) : null}

        {isEdit && existingAttachmentIds.length > 0 ? (
          <p className="text-xs text-[var(--text-secondary)]">
            {t('myArea.workflowDetail.editInputKeepAttachments', {
              count: existingAttachmentIds.length,
            })}
          </p>
        ) : null}

        <CertificationRecordFormFields
          definitions={submittableItems.map((item) => ({
            id: item.tenantCertDefinitionId,
            name: item.certDefinitionName,
            description: null,
            intervalDays: null,
            reminderDaysBeforeExpiry: null,
            active: true,
            configKey: null,
            evidenceTypes: item.evidenceTypes,
          }))}
          selectedDefinitionIds={selectedDefinitionId > 0 ? [selectedDefinitionId] : []}
          onSelectedDefinitionIdsChange={(ids) => setSelectedDefinitionId(ids[0] ?? 0)}
          completedDate={completedDate}
          onCompletedDateChange={setCompletedDate}
          notes={notes}
          onNotesChange={setNotes}
          files={files}
          onFilesChange={setFiles}
          attachmentRequired={attachmentRequired}
          disabled={pending}
          attachmentsInputId="certification-attachments"
        />
      </div>
    </Modal>
  );
}
