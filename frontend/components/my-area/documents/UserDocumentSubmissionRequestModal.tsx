/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo, useState } from 'react';
import { PendingAttachments } from '@/components/attachment';
import { WorkflowInputChangeRequestNote } from '@/components/my-area/workflows/WorkflowInputChangeRequestNote';
import { Input } from '@/components/ui/input';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { ApiRequestError } from '@/lib/api/client';
import { workflowsApi } from '@/lib/api/workflows';
import { useSubmitUserDocumentSubmissionWorkflow, useUpdateWorkflowPayload } from '@/lib/store';
import {
  type UserDocumentSubmissionWorkflowPayload,
  WORKFLOW_CLIENT_ERROR_CODES,
  WORKFLOW_TYPE_IDS,
} from '@/types/workflow';
import { DocumentTagInput } from './DocumentTagInput';

function titleFromFileName(fileName: string): string {
  const lastDot = fileName.lastIndexOf('.');
  if (lastDot <= 0) return fileName;
  return fileName.slice(0, lastDot);
}

export interface UserDocumentSubmissionRequestModalProps {
  isOpen: boolean;
  onClose: () => void;
  tagSuggestions?: string[];
  editWorkflowId?: number | null;
  initialPayload?: UserDocumentSubmissionWorkflowPayload | null;
  requestMessage?: string | null;
}

export function UserDocumentSubmissionRequestModal({
  isOpen,
  onClose,
  tagSuggestions = [],
  editWorkflowId = null,
  initialPayload = null,
  requestMessage = null,
}: UserDocumentSubmissionRequestModalProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const submitWorkflow = useSubmitUserDocumentSubmissionWorkflow();
  const updatePayload = useUpdateWorkflowPayload();
  const isEdit = editWorkflowId != null && editWorkflowId > 0;
  const pending = isEdit ? updatePayload.isPending : submitWorkflow.isPending;

  const resolveInitial = () => {
    if (isEdit && initialPayload) {
      return {
        title: initialPayload.title,
        description: initialPayload.description ?? '',
        tags: initialPayload.tags ?? [],
        attachmentIds: initialPayload.attachmentIds ?? [],
      };
    }
    return { title: '', description: '', tags: [] as string[], attachmentIds: [] as number[] };
  };

  const [title, setTitle] = useState(() => resolveInitial().title);
  const [description, setDescription] = useState(() => resolveInitial().description);
  const [tags, setTags] = useState(() => resolveInitial().tags);
  const [existingAttachmentIds, setExistingAttachmentIds] = useState(
    () => resolveInitial().attachmentIds,
  );
  const [files, setFiles] = useState<File[]>([]);
  const [prevIsOpen, setPrevIsOpen] = useState(isOpen);
  const depsKey = `${editWorkflowId ?? 'new'}:${JSON.stringify(initialPayload)}`;
  const [prevDepsKey, setPrevDepsKey] = useState(depsKey);

  if (isOpen !== prevIsOpen) {
    setPrevIsOpen(isOpen);
    if (isOpen) {
      const initial = resolveInitial();
      setTitle(initial.title);
      setDescription(initial.description);
      setTags(initial.tags);
      setExistingAttachmentIds(initial.attachmentIds);
      setFiles([]);
      setPrevDepsKey(depsKey);
    }
  } else if (isOpen && depsKey !== prevDepsKey) {
    setPrevDepsKey(depsKey);
    const initial = resolveInitial();
    setTitle(initial.title);
    setDescription(initial.description);
    setTags(initial.tags);
    setExistingAttachmentIds(initial.attachmentIds);
    setFiles([]);
  }

  const handleFilesChange = (selected: File[]) => {
    setFiles(selected);
    if (!title.trim() && selected.length > 0) {
      setTitle(titleFromFileName(selected[0].name));
    }
  };

  const hasAttachments = existingAttachmentIds.length > 0 || files.length > 0;

  const canSubmit = useMemo(
    () => title.trim().length > 0 && hasAttachments && !pending,
    [title, hasAttachments, pending],
  );

  const handleSubmit = async () => {
    if (!canSubmit) return;
    try {
      const payloadBody = {
        title: title.trim(),
        description: description.trim() || undefined,
        tags: tags.length > 0 ? tags : undefined,
      };
      if (isEdit) {
        if (editWorkflowId == null) return;
        const uploadedIds =
          files.length > 0
            ? (
                await Promise.all(
                  files.map((file) =>
                    workflowsApi.uploadWorkflowDraftAttachment(
                      WORKFLOW_TYPE_IDS.USER_DOCUMENT_SUBMISSION,
                      file,
                    ),
                  ),
                )
              ).map((item) => item.id)
            : [];
        const payload: UserDocumentSubmissionWorkflowPayload = {
          ...payloadBody,
          title: payloadBody.title,
          attachmentIds: [...existingAttachmentIds, ...uploadedIds],
        };
        await updatePayload.mutateAsync({ workflowId: editWorkflowId, payload });
        showSuccess(t('myArea.workflowDetail.editInputSuccess'));
      } else {
        await submitWorkflow.mutateAsync({
          files,
          payload: payloadBody,
        });
        showSuccess(t('common.request.successSubmitted'));
      }
      onClose();
    } catch (e) {
      const msg =
        e instanceof ApiRequestError && e.code === WORKFLOW_CLIENT_ERROR_CODES.TYPE_ALREADY_OPEN
          ? t('myArea.userDocumentSubmission.errorAlreadyOpen')
          : e instanceof Error
            ? e.message
            : isEdit
              ? t('myArea.workflowDetail.editInputError')
              : t('common.request.errorSubmit');
      showError(msg);
    }
  };

  if (!isOpen) {
    return null;
  }

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={
        isEdit
          ? t('myArea.workflowDetail.editInputTitle')
          : t('myArea.userDocumentSubmission.modalTitle')
      }
      size="md"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleSubmit()}
          isValid={canSubmit}
          isLoading={pending}
          saveText={
            isEdit
              ? t('myArea.workflowDetail.editInputSubmit')
              : t('myArea.userDocumentSubmission.submit')
          }
          showSave
        />
      }
    >
      <div className="space-y-4">
        <p className="text-sm text-[var(--text-secondary)]">
          {isEdit
            ? t('myArea.workflowDetail.editInputIntro')
            : t('myArea.userDocumentSubmission.modalIntro')}
        </p>

        {isEdit && requestMessage?.trim() ? (
          <WorkflowInputChangeRequestNote message={requestMessage} tone="neutral" />
        ) : null}

        <PendingAttachments
          variant="field"
          inputId="user-document-submission-attachments"
          files={files}
          onChange={handleFilesChange}
          label={t('myArea.documents.fileLabel')}
          hint={t('myArea.userDocumentSubmission.attachmentsHint')}
          required={!isEdit || existingAttachmentIds.length === 0}
          removeFileLabel={t('common.attachments.removeFile')}
        />

        {isEdit && existingAttachmentIds.length > 0 ? (
          <p className="text-xs text-[var(--text-secondary)]">
            {t('myArea.workflowDetail.editInputKeepAttachments', {
              count: existingAttachmentIds.length,
            })}
          </p>
        ) : null}

        <Input
          id="user-document-submission-title"
          label={t('myArea.documents.titleLabel')}
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
        />

        <Input
          id="user-document-submission-description"
          label={t('myArea.documents.descriptionLabel')}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />

        <DocumentTagInput tags={tags} suggestions={tagSuggestions} onChange={setTags} />
      </div>
    </Modal>
  );
}
