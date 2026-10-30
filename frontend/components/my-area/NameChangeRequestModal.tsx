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
import { useSubmitNameChangeWorkflow, useUpdateWorkflowPayload } from '@/lib/store';
import type { UserDto } from '@/types/user';
import {
  type NameChangeWorkflowPayload,
  WORKFLOW_CLIENT_ERROR_CODES,
  WORKFLOW_TYPE_IDS,
} from '@/types/workflow';

export interface NameChangeRequestModalProps {
  isOpen: boolean;
  onClose: () => void;
  userData: UserDto;
  /** When set, updates an existing workflow payload instead of opening a new one. */
  editWorkflowId?: number | null;
  initialPayload?: NameChangeWorkflowPayload | null;
  /** Comment from an open input-change request, shown while editing. */
  requestMessage?: string | null;
}

export function NameChangeRequestModal({
  isOpen,
  onClose,
  userData,
  editWorkflowId = null,
  initialPayload = null,
  requestMessage = null,
}: NameChangeRequestModalProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const submitNameChange = useSubmitNameChangeWorkflow();
  const updatePayload = useUpdateWorkflowPayload();
  const isEdit = editWorkflowId != null && editWorkflowId > 0;
  const pending = isEdit ? updatePayload.isPending : submitNameChange.isPending;

  const resolveInitial = () => {
    if (isEdit && initialPayload) {
      return {
        firstName: initialPayload.firstName,
        lastName: initialPayload.lastName,
        attachmentIds: initialPayload.attachmentIds ?? [],
      };
    }
    return {
      firstName: userData.firstName,
      lastName: userData.lastName,
      attachmentIds: [] as number[],
    };
  };

  const [firstName, setFirstName] = useState(() => resolveInitial().firstName);
  const [lastName, setLastName] = useState(() => resolveInitial().lastName);
  const [existingAttachmentIds, setExistingAttachmentIds] = useState(
    () => resolveInitial().attachmentIds,
  );
  const [files, setFiles] = useState<File[]>([]);
  const [prevIsOpen, setPrevIsOpen] = useState(isOpen);
  const depsKey = `${editWorkflowId ?? 'new'}:${JSON.stringify(initialPayload)}:${userData.firstName}:${userData.lastName}`;
  const [prevDepsKey, setPrevDepsKey] = useState(depsKey);

  if (isOpen !== prevIsOpen) {
    setPrevIsOpen(isOpen);
    if (isOpen) {
      const initial = resolveInitial();
      setFirstName(initial.firstName);
      setLastName(initial.lastName);
      setExistingAttachmentIds(initial.attachmentIds);
      setFiles([]);
      setPrevDepsKey(depsKey);
    }
  } else if (isOpen && depsKey !== prevDepsKey) {
    setPrevDepsKey(depsKey);
    const initial = resolveInitial();
    setFirstName(initial.firstName);
    setLastName(initial.lastName);
    setExistingAttachmentIds(initial.attachmentIds);
    setFiles([]);
  }

  const namesUnchanged = useMemo(() => {
    if (isEdit) return false;
    const fn = firstName.trim();
    const ln = lastName.trim();
    return (
      fn.toLowerCase() === userData.firstName.trim().toLowerCase() &&
      ln.toLowerCase() === userData.lastName.trim().toLowerCase()
    );
  }, [firstName, lastName, isEdit, userData.firstName, userData.lastName]);

  const hasAttachments = existingAttachmentIds.length > 0 || files.length > 0;

  const canSubmit =
    firstName.trim().length > 0 &&
    lastName.trim().length > 0 &&
    hasAttachments &&
    !namesUnchanged &&
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
                    workflowsApi.uploadWorkflowDraftAttachment(WORKFLOW_TYPE_IDS.NAME_CHANGE, file),
                  ),
                )
              ).map((item) => item.id)
            : [];
        const payload: NameChangeWorkflowPayload = {
          firstName: firstName.trim(),
          lastName: lastName.trim(),
          attachmentIds: [...existingAttachmentIds, ...uploadedIds],
        };
        await updatePayload.mutateAsync({ workflowId: editWorkflowId, payload });
        showSuccess(t('myArea.workflowDetail.editInputSuccess'));
      } else {
        await submitNameChange.mutateAsync({
          files,
          payload: { firstName: firstName.trim(), lastName: lastName.trim() },
        });
        showSuccess(t('common.request.successSubmitted'));
      }
      onClose();
    } catch (e) {
      const msg =
        e instanceof ApiRequestError && e.code === WORKFLOW_CLIENT_ERROR_CODES.TYPE_ALREADY_OPEN
          ? t('myArea.nameChange.errorAlreadyOpen')
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
      title={isEdit ? t('myArea.workflowDetail.editInputTitle') : t('myArea.nameChange.modalTitle')}
      size="md"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleSubmit()}
          isValid={canSubmit}
          isLoading={pending}
          saveText={
            isEdit ? t('myArea.workflowDetail.editInputSubmit') : t('common.request.submit')
          }
          showSave
        />
      }
    >
      <div className="space-y-4">
        <p className="text-sm text-[var(--text-secondary)]">
          {isEdit ? t('myArea.workflowDetail.editInputIntro') : t('myArea.nameChange.modalIntro')}
        </p>

        {isEdit && requestMessage?.trim() ? (
          <WorkflowInputChangeRequestNote message={requestMessage} tone="neutral" />
        ) : null}

        <Input
          id="name-change-first"
          label={t('setup.user.firstName')}
          value={firstName}
          onChange={(e) => setFirstName(e.target.value)}
          autoComplete="given-name"
          required
        />
        <Input
          id="name-change-last"
          label={t('setup.user.lastName')}
          value={lastName}
          onChange={(e) => setLastName(e.target.value)}
          autoComplete="family-name"
          required
        />

        {!isEdit && namesUnchanged && (
          <p className="text-sm text-amber-700 dark:text-amber-300">
            {t('myArea.nameChange.hintNamesUnchanged')}
          </p>
        )}

        {isEdit && existingAttachmentIds.length > 0 ? (
          <p className="text-xs text-[var(--text-secondary)]">
            {t('myArea.workflowDetail.editInputKeepAttachments', {
              count: existingAttachmentIds.length,
            })}
          </p>
        ) : null}

        <PendingAttachments
          variant="field"
          inputId="name-change-attachments"
          files={files}
          onChange={setFiles}
          label={t('myArea.nameChange.attachmentsLabel')}
          hint={t('myArea.nameChange.attachmentsHint')}
          required={!isEdit || existingAttachmentIds.length === 0}
          removeFileLabel={t('common.attachments.removeFile')}
        />
      </div>
    </Modal>
  );
}
