/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo, useState } from 'react';
import { PendingAttachments } from '@/components/attachment';
import { WorkflowInputChangeRequestNote } from '@/components/my-area/workflows/WorkflowInputChangeRequestNote';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Select } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { workflowsApi } from '@/lib/api/workflows';
import {
  useMainOrganizationChangeTargets,
  useSubmitMainOrganizationChangeWorkflow,
  useUpdateWorkflowPayload,
} from '@/lib/store';
import { isActiveOrganizationMembership } from '@/lib/utils/membership';
import { resolveWorkflowErrorMessage } from '@/lib/utils/workflowErrors';
import type { UserDto } from '@/types/user';
import { type MainOrganizationChangeWorkflowPayload, WORKFLOW_TYPE_IDS } from '@/types/workflow';

export interface MainOrganizationChangeModalProps {
  isOpen: boolean;
  onClose: () => void;
  userData: UserDto;
  editWorkflowId?: number | null;
  initialPayload?: MainOrganizationChangeWorkflowPayload | null;
  requestMessage?: string | null;
}

export function MainOrganizationChangeModal({
  isOpen,
  onClose,
  userData,
  editWorkflowId = null,
  initialPayload = null,
  requestMessage = null,
}: MainOrganizationChangeModalProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const submitMainOrganizationChange = useSubmitMainOrganizationChangeWorkflow();
  const updatePayload = useUpdateWorkflowPayload();
  const { data: targetOrganizations = [], isLoading: targetsLoading } =
    useMainOrganizationChangeTargets(isOpen);
  const isEdit = editWorkflowId != null && editWorkflowId > 0;
  const pending = isEdit ? updatePayload.isPending : submitMainOrganizationChange.isPending;
  const hasMainOrganization = useMemo(
    () =>
      (userData.organizationMemberships ?? []).some(
        (membership) => membership.main && isActiveOrganizationMembership(membership),
      ),
    [userData.organizationMemberships],
  );

  const organizationOptions = useMemo(() => {
    const options = targetOrganizations.map((org) => ({
      value: String(org.id),
      label: org.name,
    }));
    if (
      isEdit &&
      initialPayload?.newMainOrganizationId != null &&
      !options.some((option) => option.value === String(initialPayload.newMainOrganizationId))
    ) {
      options.unshift({
        value: String(initialPayload.newMainOrganizationId),
        label: initialPayload.newMainOrganizationName ?? `#${initialPayload.newMainOrganizationId}`,
      });
    }
    return [
      { value: '', label: t('myArea.mainOrganizationChange.newMainOrganizationPlaceholder') },
      ...options,
    ];
  }, [initialPayload, isEdit, targetOrganizations, t]);

  const resolveInitial = () => ({
    newMainOrganizationId:
      initialPayload?.newMainOrganizationId != null
        ? String(initialPayload.newMainOrganizationId)
        : '',
    reason: initialPayload?.reason ?? '',
    attachmentIds: initialPayload?.attachmentIds ?? [],
  });

  const [newMainOrganizationId, setNewMainOrganizationId] = useState(
    () => resolveInitial().newMainOrganizationId,
  );
  const [reason, setReason] = useState(() => resolveInitial().reason);
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
      setNewMainOrganizationId(initial.newMainOrganizationId);
      setReason(initial.reason);
      setExistingAttachmentIds(initial.attachmentIds);
      setFiles([]);
      setPrevDepsKey(depsKey);
    }
  } else if (isOpen && depsKey !== prevDepsKey) {
    setPrevDepsKey(depsKey);
    const initial = resolveInitial();
    setNewMainOrganizationId(initial.newMainOrganizationId);
    setReason(initial.reason);
    setExistingAttachmentIds(initial.attachmentIds);
    setFiles([]);
  }

  const canSubmit =
    newMainOrganizationId.trim().length > 0 &&
    !pending &&
    (isEdit || (!targetsLoading && organizationOptions.length > 0));

  const handleSubmit = async () => {
    if (!canSubmit) return;
    const organizationId = Number(newMainOrganizationId);
    if (!Number.isFinite(organizationId) || organizationId <= 0) return;

    try {
      if (isEdit) {
        if (editWorkflowId == null) return;
        const uploadedIds =
          files.length > 0
            ? (
                await Promise.all(
                  files.map((file) =>
                    workflowsApi.uploadWorkflowDraftAttachment(
                      WORKFLOW_TYPE_IDS.MAIN_ORGANIZATION_CHANGE,
                      file,
                    ),
                  ),
                )
              ).map((item) => item.id)
            : [];
        const attachmentIds = [...existingAttachmentIds, ...uploadedIds];
        const payload: MainOrganizationChangeWorkflowPayload = {
          newMainOrganizationId: organizationId,
          reason: reason.trim() || undefined,
          ...(attachmentIds.length > 0 ? { attachmentIds } : {}),
        };
        await updatePayload.mutateAsync({ workflowId: editWorkflowId, payload });
        showSuccess(t('myArea.workflowDetail.editInputSuccess'));
      } else {
        await submitMainOrganizationChange.mutateAsync({
          files,
          payload: {
            newMainOrganizationId: organizationId,
            reason: reason.trim() || undefined,
          },
        });
        showSuccess(t('common.request.successSubmitted'));
      }
      onClose();
    } catch (e) {
      const msg = resolveWorkflowErrorMessage(
        e,
        t,
        isEdit ? t('myArea.workflowDetail.editInputError') : t('common.request.errorSubmit'),
        'self',
      );
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
          : t('myArea.mainOrganizationChange.modalTitle')
      }
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
          {isEdit
            ? t('myArea.workflowDetail.editInputIntro')
            : hasMainOrganization
              ? t('myArea.mainOrganizationChange.modalIntro')
              : t('myArea.mainOrganizationChange.modalIntroWithoutMainOrganization')}
        </p>

        {isEdit && requestMessage?.trim() ? (
          <WorkflowInputChangeRequestNote message={requestMessage} tone="neutral" />
        ) : null}

        <Select
          id="main-organization-change-target"
          label={t('myArea.mainOrganizationChange.newMainOrganizationLabel')}
          value={newMainOrganizationId}
          onChange={(event) => setNewMainOrganizationId(event.target.value)}
          options={organizationOptions}
          disabled={targetsLoading && !isEdit}
          required
        />

        {!isEdit && !targetsLoading && organizationOptions.length === 0 ? (
          <p className="text-sm text-amber-700 dark:text-amber-300">
            {t('myArea.mainOrganizationChange.noTargetOrganizations')}
          </p>
        ) : null}

        <Textarea
          id="main-organization-change-reason"
          label={t('common.request.reasonLabel')}
          hint={t('common.request.reasonOptional')}
          value={reason}
          onChange={(event) => setReason(event.target.value)}
          rows={4}
          maxLength={2000}
        />

        {isEdit && existingAttachmentIds.length > 0 ? (
          <p className="text-xs text-[var(--text-secondary)]">
            {t('myArea.workflowDetail.editInputKeepAttachments', {
              count: existingAttachmentIds.length,
            })}
          </p>
        ) : null}

        <PendingAttachments
          variant="field"
          inputId="main-organization-change-attachments"
          files={files}
          onChange={setFiles}
          label={t('common.request.attachmentsLabel')}
          hint={t('myArea.mainOrganizationChange.attachmentsHint')}
          optionalLabel={t('common.request.attachmentsOptional')}
          removeFileLabel={t('common.attachments.removeFile')}
        />
      </div>
    </Modal>
  );
}
