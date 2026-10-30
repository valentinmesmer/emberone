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
import { ApiRequestError } from '@/lib/api/client';
import { workflowsApi } from '@/lib/api/workflows';
import {
  useJoinableOrganizations,
  useSubmitOrganizationJoinRequestWorkflow,
  useUpdateWorkflowPayload,
} from '@/lib/store';
import { isActiveOrganizationMembership } from '@/lib/utils/membership';
import type { UserDto } from '@/types/user';
import {
  type OrganizationJoinRequestWorkflowPayload,
  WORKFLOW_CLIENT_ERROR_CODES,
  WORKFLOW_TYPE_IDS,
} from '@/types/workflow';

export interface OrganizationJoinRequestModalProps {
  isOpen: boolean;
  onClose: () => void;
  userData: UserDto;
  editWorkflowId?: number | null;
  initialPayload?: OrganizationJoinRequestWorkflowPayload | null;
  requestMessage?: string | null;
}

export function OrganizationJoinRequestModal({
  isOpen,
  onClose,
  userData,
  editWorkflowId = null,
  initialPayload = null,
  requestMessage = null,
}: OrganizationJoinRequestModalProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const submitJoinRequest = useSubmitOrganizationJoinRequestWorkflow();
  const updatePayload = useUpdateWorkflowPayload();
  const { data: joinableOrganizations = [], isLoading: joinableLoading } =
    useJoinableOrganizations(isOpen);
  const isEdit = editWorkflowId != null && editWorkflowId > 0;
  const pending = isEdit ? updatePayload.isPending : submitJoinRequest.isPending;
  const hasMainOrganization = useMemo(
    () =>
      (userData.organizationMemberships ?? []).some(
        (membership) => membership.main && isActiveOrganizationMembership(membership),
      ),
    [userData.organizationMemberships],
  );

  const organizationOptions = useMemo(() => {
    const options = joinableOrganizations.map((org) => ({
      value: String(org.id),
      label: org.name,
    }));
    if (
      isEdit &&
      initialPayload?.targetOrganizationId != null &&
      !options.some((option) => option.value === String(initialPayload.targetOrganizationId))
    ) {
      options.unshift({
        value: String(initialPayload.targetOrganizationId),
        label: initialPayload.targetOrganizationName ?? `#${initialPayload.targetOrganizationId}`,
      });
    }
    return [
      { value: '', label: t('myArea.organizationJoinRequest.targetOrganizationPlaceholder') },
      ...options,
    ];
  }, [initialPayload, isEdit, joinableOrganizations, t]);

  const resolveInitial = () => ({
    targetOrganizationId:
      initialPayload?.targetOrganizationId != null
        ? String(initialPayload.targetOrganizationId)
        : '',
    reason: initialPayload?.reason ?? '',
    attachmentIds: initialPayload?.attachmentIds ?? [],
  });

  const [targetOrganizationId, setTargetOrganizationId] = useState(
    () => resolveInitial().targetOrganizationId,
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
      setTargetOrganizationId(initial.targetOrganizationId);
      setReason(initial.reason);
      setExistingAttachmentIds(initial.attachmentIds);
      setFiles([]);
      setPrevDepsKey(depsKey);
    }
  } else if (isOpen && depsKey !== prevDepsKey) {
    setPrevDepsKey(depsKey);
    const initial = resolveInitial();
    setTargetOrganizationId(initial.targetOrganizationId);
    setReason(initial.reason);
    setExistingAttachmentIds(initial.attachmentIds);
    setFiles([]);
  }

  const canSubmit =
    targetOrganizationId.trim().length > 0 &&
    !pending &&
    (isEdit || (!joinableLoading && organizationOptions.length > 0));

  const handleSubmit = async () => {
    if (!canSubmit) return;
    const organizationId = Number(targetOrganizationId);
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
                      WORKFLOW_TYPE_IDS.ORGANIZATION_JOIN_REQUEST,
                      file,
                    ),
                  ),
                )
              ).map((item) => item.id)
            : [];
        const attachmentIds = [...existingAttachmentIds, ...uploadedIds];
        const payload: OrganizationJoinRequestWorkflowPayload = {
          targetOrganizationId: organizationId,
          reason: reason.trim() || undefined,
          ...(attachmentIds.length > 0 ? { attachmentIds } : {}),
        };
        await updatePayload.mutateAsync({ workflowId: editWorkflowId, payload });
        showSuccess(t('myArea.workflowDetail.editInputSuccess'));
      } else {
        await submitJoinRequest.mutateAsync({
          files,
          payload: {
            targetOrganizationId: organizationId,
            reason: reason.trim() || undefined,
          },
        });
        showSuccess(t('common.request.successSubmitted'));
      }
      onClose();
    } catch (e) {
      const msg =
        e instanceof ApiRequestError &&
        e.code === WORKFLOW_CLIENT_ERROR_CODES.ORGANIZATION_JOIN_ALREADY_OPEN
          ? t('myArea.organizationJoinRequest.errorAlreadyOpen')
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
          : t('myArea.organizationJoinRequest.modalTitle')
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
            : t(
                hasMainOrganization
                  ? 'myArea.organizationJoinRequest.modalIntro'
                  : 'myArea.organizationJoinRequest.modalIntroWithoutMainOrganization',
              )}
        </p>

        {isEdit && requestMessage?.trim() ? (
          <WorkflowInputChangeRequestNote message={requestMessage} tone="neutral" />
        ) : null}

        <Select
          id="organization-join-target"
          label={t('myArea.organizationJoinRequest.targetOrganizationLabel')}
          value={targetOrganizationId}
          onChange={(event) => setTargetOrganizationId(event.target.value)}
          options={organizationOptions}
          disabled={joinableLoading && !isEdit}
          required
        />

        {!isEdit && !joinableLoading && organizationOptions.length === 0 ? (
          <p className="text-sm text-amber-700 dark:text-amber-300">
            {t('myArea.organizationJoinRequest.noJoinableOrganizations')}
          </p>
        ) : null}

        <Textarea
          id="organization-join-reason"
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
          inputId="organization-join-attachments"
          files={files}
          onChange={setFiles}
          label={t('common.request.attachmentsLabel')}
          hint={t('myArea.organizationJoinRequest.attachmentsHint')}
          optionalLabel={t('common.request.attachmentsOptional')}
          removeFileLabel={t('common.attachments.removeFile')}
        />
      </div>
    </Modal>
  );
}
