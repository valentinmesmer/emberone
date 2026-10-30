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
  useOrganizationGroupLeaveTargets,
  useSubmitOrganizationGroupLeaveRequestWorkflow,
  useUpdateWorkflowPayload,
} from '@/lib/store';
import type { UserDto } from '@/types/user';
import {
  type OrganizationGroupLeaveRequestWorkflowPayload,
  WORKFLOW_CLIENT_ERROR_CODES,
  WORKFLOW_TYPE_IDS,
} from '@/types/workflow';

export interface OrganizationGroupLeaveRequestModalProps {
  isOpen: boolean;
  onClose: () => void;
  userData: UserDto;
  editWorkflowId?: number | null;
  initialPayload?: OrganizationGroupLeaveRequestWorkflowPayload | null;
  requestMessage?: string | null;
}

export function OrganizationGroupLeaveRequestModal({
  isOpen,
  onClose,
  userData: _userData,
  editWorkflowId = null,
  initialPayload = null,
  requestMessage = null,
}: OrganizationGroupLeaveRequestModalProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const submitLeaveRequest = useSubmitOrganizationGroupLeaveRequestWorkflow();
  const updatePayload = useUpdateWorkflowPayload();
  const { data: leaveTargets = [], isLoading: targetsLoading } =
    useOrganizationGroupLeaveTargets(isOpen);
  const isEdit = editWorkflowId != null && editWorkflowId > 0;
  const pending = isEdit ? updatePayload.isPending : submitLeaveRequest.isPending;

  const resolveInitial = () => ({
    organizationId:
      initialPayload?.organizationId != null ? String(initialPayload.organizationId) : '',
    organizationGroupId:
      initialPayload?.organizationGroupId != null ? String(initialPayload.organizationGroupId) : '',
    reason: initialPayload?.reason ?? '',
    attachmentIds: initialPayload?.attachmentIds ?? [],
  });

  const [organizationId, setOrganizationId] = useState(() => resolveInitial().organizationId);
  const [organizationGroupId, setOrganizationGroupId] = useState(
    () => resolveInitial().organizationGroupId,
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
      setOrganizationId(initial.organizationId);
      setOrganizationGroupId(initial.organizationGroupId);
      setReason(initial.reason);
      setExistingAttachmentIds(initial.attachmentIds);
      setFiles([]);
      setPrevDepsKey(depsKey);
    }
  } else if (isOpen && depsKey !== prevDepsKey) {
    setPrevDepsKey(depsKey);
    const initial = resolveInitial();
    setOrganizationId(initial.organizationId);
    setOrganizationGroupId(initial.organizationGroupId);
    setReason(initial.reason);
    setExistingAttachmentIds(initial.attachmentIds);
    setFiles([]);
  }

  const organizationOptions = useMemo(() => {
    const seen = new Set<string>();
    const options: { value: string; label: string }[] = [];
    for (const target of leaveTargets) {
      const value = String(target.organizationId);
      if (seen.has(value)) continue;
      seen.add(value);
      options.push({ value, label: target.organizationName });
    }
    if (
      isEdit &&
      initialPayload?.organizationId != null &&
      !options.some((option) => option.value === String(initialPayload.organizationId))
    ) {
      options.unshift({
        value: String(initialPayload.organizationId),
        label: initialPayload.organizationName ?? `#${initialPayload.organizationId}`,
      });
    }
    return [
      { value: '', label: t('myArea.organizationGroupLeaveRequest.organizationPlaceholder') },
      ...options,
    ];
  }, [initialPayload, isEdit, leaveTargets, t]);

  const groupOptions = useMemo(() => {
    const options = leaveTargets
      .filter((target) => organizationId !== '' && String(target.organizationId) === organizationId)
      .map((target) => ({
        value: String(target.organizationGroupId),
        label: target.organizationGroupName,
      }));
    if (
      isEdit &&
      initialPayload?.organizationGroupId != null &&
      String(initialPayload.organizationId) === organizationId &&
      !options.some((option) => option.value === String(initialPayload.organizationGroupId))
    ) {
      options.unshift({
        value: String(initialPayload.organizationGroupId),
        label: initialPayload.organizationGroupName ?? `#${initialPayload.organizationGroupId}`,
      });
    }
    return [
      { value: '', label: t('myArea.organizationGroupLeaveRequest.organizationGroupPlaceholder') },
      ...options,
    ];
  }, [initialPayload, isEdit, leaveTargets, organizationId, t]);

  const canSubmit =
    organizationId.trim().length > 0 &&
    organizationGroupId.trim().length > 0 &&
    !pending &&
    (isEdit || (!targetsLoading && leaveTargets.length > 0));

  const handleOrganizationChange = (value: string) => {
    setOrganizationId(value);
    setOrganizationGroupId('');
  };

  const handleSubmit = async () => {
    if (!canSubmit) return;
    const selectedOrganizationId = Number(organizationId);
    const selectedGroupId = Number(organizationGroupId);
    if (
      !Number.isFinite(selectedOrganizationId) ||
      selectedOrganizationId <= 0 ||
      !Number.isFinite(selectedGroupId) ||
      selectedGroupId <= 0
    ) {
      return;
    }

    try {
      if (isEdit) {
        if (editWorkflowId == null) return;
        const uploadedIds =
          files.length > 0
            ? (
                await Promise.all(
                  files.map((file) =>
                    workflowsApi.uploadWorkflowDraftAttachment(
                      WORKFLOW_TYPE_IDS.ORGANIZATION_GROUP_LEAVE_REQUEST,
                      file,
                    ),
                  ),
                )
              ).map((item) => item.id)
            : [];
        const attachmentIds = [...existingAttachmentIds, ...uploadedIds];
        const payload: OrganizationGroupLeaveRequestWorkflowPayload = {
          organizationId: selectedOrganizationId,
          organizationGroupId: selectedGroupId,
          reason: reason.trim() || undefined,
          ...(attachmentIds.length > 0 ? { attachmentIds } : {}),
        };
        await updatePayload.mutateAsync({ workflowId: editWorkflowId, payload });
        showSuccess(t('myArea.workflowDetail.editInputSuccess'));
      } else {
        await submitLeaveRequest.mutateAsync({
          files,
          payload: {
            organizationId: selectedOrganizationId,
            organizationGroupId: selectedGroupId,
            reason: reason.trim() || undefined,
          },
        });
        showSuccess(t('common.request.successSubmitted'));
      }
      onClose();
    } catch (e) {
      const msg =
        e instanceof ApiRequestError &&
        e.code === WORKFLOW_CLIENT_ERROR_CODES.ORGANIZATION_GROUP_LEAVE_ALREADY_OPEN
          ? t('myArea.organizationGroupLeaveRequest.errorAlreadyOpen')
          : e instanceof ApiRequestError &&
              e.code === WORKFLOW_CLIENT_ERROR_CODES.LEADERSHIP_GROUP_LEAVE_NOT_ALLOWED
            ? t('myArea.organizationGroupLeaveRequest.errorLeadershipGroup')
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
          : t('myArea.organizationGroupLeaveRequest.modalTitle')
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
            : t('myArea.organizationGroupLeaveRequest.modalIntro')}
        </p>

        {isEdit && requestMessage?.trim() ? (
          <WorkflowInputChangeRequestNote message={requestMessage} tone="neutral" />
        ) : null}

        <Select
          id="organization-group-leave-organization"
          label={t('myArea.organizationGroupLeaveRequest.organizationLabel')}
          value={organizationId}
          onChange={(event) => handleOrganizationChange(event.target.value)}
          options={organizationOptions}
          disabled={targetsLoading && !isEdit}
          required
        />

        <Select
          id="organization-group-leave-group"
          label={t('myArea.organizationGroupLeaveRequest.organizationGroupLabel')}
          value={organizationGroupId}
          onChange={(event) => setOrganizationGroupId(event.target.value)}
          options={groupOptions}
          disabled={!organizationId || (targetsLoading && !isEdit)}
          required
        />

        {!isEdit && !targetsLoading && leaveTargets.length === 0 ? (
          <p className="text-sm text-amber-700 dark:text-amber-300">
            {t('myArea.organizationGroupLeaveRequest.noLeaveTargets')}
          </p>
        ) : null}

        <Textarea
          id="organization-group-leave-reason"
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
          inputId="organization-group-leave-attachments"
          files={files}
          onChange={setFiles}
          label={t('common.request.attachmentsLabel')}
          hint={t('myArea.organizationGroupLeaveRequest.attachmentsHint')}
          optionalLabel={t('common.request.attachmentsOptional')}
          removeFileLabel={t('common.attachments.removeFile')}
        />
      </div>
    </Modal>
  );
}
