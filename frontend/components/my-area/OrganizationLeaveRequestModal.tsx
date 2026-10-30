/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ArrowRightLeft } from 'lucide-react';
import { useMemo, useState } from 'react';
import { PendingAttachments } from '@/components/attachment';
import { MainOrganizationChangeModal } from '@/components/my-area/MainOrganizationChangeModal';
import { WorkflowInputChangeRequestNote } from '@/components/my-area/workflows/WorkflowInputChangeRequestNote';
import { Button } from '@/components/ui/button';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Select } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { ApiRequestError } from '@/lib/api/client';
import { workflowsApi } from '@/lib/api/workflows';
import {
  useMainOrganizationChangeTargets,
  useMyBeginnableWorkflowTypes,
  useOrganizationLeaveTargets,
  useOrganizationTree,
  useSubmitOrganizationLeaveRequestWorkflow,
  useUpdateWorkflowPayload,
  useWorkflowOpenState,
} from '@/lib/store';
import { findMainOrganizationInTree } from '@/lib/utils/organizationTree';
import { resolveWorkflowErrorMessage } from '@/lib/utils/workflowErrors';
import type { UserDto } from '@/types/user';
import {
  type OrganizationLeaveRequestWorkflowPayload,
  WORKFLOW_CLIENT_ERROR_CODES,
  WORKFLOW_TYPE_IDS,
} from '@/types/workflow';

export interface OrganizationLeaveRequestModalProps {
  isOpen: boolean;
  onClose: () => void;
  userData: UserDto;
  editWorkflowId?: number | null;
  initialPayload?: OrganizationLeaveRequestWorkflowPayload | null;
  requestMessage?: string | null;
}

export function OrganizationLeaveRequestModal({
  isOpen,
  onClose,
  userData,
  editWorkflowId = null,
  initialPayload = null,
  requestMessage = null,
}: OrganizationLeaveRequestModalProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const submitLeaveRequest = useSubmitOrganizationLeaveRequestWorkflow();
  const updatePayload = useUpdateWorkflowPayload();
  const { data: leaveTargets = [], isLoading: targetsLoading } =
    useOrganizationLeaveTargets(isOpen);
  const { data: organizationTree } = useOrganizationTree();
  const { data: beginnableTypes = [] } = useMyBeginnableWorkflowTypes();
  const mainOrganizationChangeBeginnable = useMemo(
    () =>
      beginnableTypes.some(
        (type) => type.workflowTypeId === WORKFLOW_TYPE_IDS.MAIN_ORGANIZATION_CHANGE,
      ),
    [beginnableTypes],
  );
  const { data: mainOrganizationChangeOpenState } = useWorkflowOpenState(
    WORKFLOW_TYPE_IDS.MAIN_ORGANIZATION_CHANGE,
    isOpen && mainOrganizationChangeBeginnable,
  );
  const { data: mainChangeTargets = [], isLoading: mainChangeTargetsLoading } =
    useMainOrganizationChangeTargets(isOpen && mainOrganizationChangeBeginnable);
  const mainOrganizationChangeOpenBlocked = mainOrganizationChangeOpenState?.typeBlocked ?? false;
  const mainOrganizationChangeNoTargetsBlocked =
    mainChangeTargetsLoading || mainChangeTargets.length === 0;
  const mainOrganizationChangeLinkDisabled =
    mainOrganizationChangeOpenBlocked || mainOrganizationChangeNoTargetsBlocked;
  const mainOrganizationChangeLinkDisabledTitle = mainOrganizationChangeOpenBlocked
    ? t('myArea.mainOrganizationChange.blockedOpenRequest')
    : mainOrganizationChangeNoTargetsBlocked
      ? t('myArea.mainOrganizationChange.blockedNoTargetOrganizations')
      : undefined;

  const resolvedMainOrganizationId = useMemo(
    () => findMainOrganizationInTree(organizationTree)?.id ?? null,
    [organizationTree],
  );

  const [mainOrganizationChangeOpen, setMainOrganizationChangeOpen] = useState(false);
  const isEdit = editWorkflowId != null && editWorkflowId > 0;
  const pending = isEdit ? updatePayload.isPending : submitLeaveRequest.isPending;

  const organizationOptions = useMemo(() => {
    const options = leaveTargets.map((org) => ({
      value: String(org.id),
      label: org.name,
    }));
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
      { value: '', label: t('myArea.organizationLeaveRequest.organizationPlaceholder') },
      ...options,
    ];
  }, [initialPayload, isEdit, leaveTargets, t]);

  const resolveInitial = () => ({
    organizationId:
      initialPayload?.organizationId != null ? String(initialPayload.organizationId) : '',
    reason: initialPayload?.reason ?? '',
    attachmentIds: initialPayload?.attachmentIds ?? [],
  });

  const [organizationId, setOrganizationId] = useState(() => resolveInitial().organizationId);
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
      setReason(initial.reason);
      setExistingAttachmentIds(initial.attachmentIds);
      setFiles([]);
      setMainOrganizationChangeOpen(false);
      setPrevDepsKey(depsKey);
    }
  } else if (isOpen && depsKey !== prevDepsKey) {
    setPrevDepsKey(depsKey);
    const initial = resolveInitial();
    setOrganizationId(initial.organizationId);
    setReason(initial.reason);
    setExistingAttachmentIds(initial.attachmentIds);
    setFiles([]);
  }

  const selectedOrganizationId = Number(organizationId);
  const isMainOrganizationLeave =
    resolvedMainOrganizationId != null &&
    Number.isFinite(selectedOrganizationId) &&
    selectedOrganizationId === resolvedMainOrganizationId;

  const trimmedReason = reason.trim();
  const canSubmit =
    organizationId.trim().length > 0 &&
    trimmedReason.length > 0 &&
    !pending &&
    (isEdit || (!targetsLoading && organizationOptions.length > 1));

  const handleSubmit = async () => {
    if (!canSubmit) return;
    const selectedOrganizationId = Number(organizationId);
    if (!Number.isFinite(selectedOrganizationId) || selectedOrganizationId <= 0) return;

    try {
      if (isEdit) {
        if (editWorkflowId == null) return;
        const uploadedIds =
          files.length > 0
            ? (
                await Promise.all(
                  files.map((file) =>
                    workflowsApi.uploadWorkflowDraftAttachment(
                      WORKFLOW_TYPE_IDS.ORGANIZATION_LEAVE_REQUEST,
                      file,
                    ),
                  ),
                )
              ).map((item) => item.id)
            : [];
        const attachmentIds = [...existingAttachmentIds, ...uploadedIds];
        const payload: OrganizationLeaveRequestWorkflowPayload = {
          organizationId: selectedOrganizationId,
          reason: trimmedReason,
          ...(attachmentIds.length > 0 ? { attachmentIds } : {}),
        };
        await updatePayload.mutateAsync({ workflowId: editWorkflowId, payload });
        showSuccess(t('myArea.workflowDetail.editInputSuccess'));
      } else {
        await submitLeaveRequest.mutateAsync({
          files,
          payload: {
            organizationId: selectedOrganizationId,
            reason: trimmedReason,
          },
        });
        showSuccess(t('common.request.successSubmitted'));
      }
      onClose();
    } catch (e) {
      const msg =
        e instanceof ApiRequestError &&
        e.code === WORKFLOW_CLIENT_ERROR_CODES.LEADERSHIP_MEMBERSHIP_BLOCKS_LEAVE
          ? t('myArea.organizationLeaveRequest.errorLeadershipMember')
          : resolveWorkflowErrorMessage(
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
    <>
      <Modal
        isOpen
        onClose={onClose}
        title={
          isEdit
            ? t('myArea.workflowDetail.editInputTitle')
            : t('myArea.organizationLeaveRequest.modalTitle')
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
              : t('myArea.organizationLeaveRequest.modalIntro')}
          </p>

          {isEdit && requestMessage?.trim() ? (
            <WorkflowInputChangeRequestNote message={requestMessage} tone="neutral" />
          ) : null}

          <Select
            id="organization-leave-target"
            label={t('myArea.organizationLeaveRequest.organizationLabel')}
            value={organizationId}
            onChange={(event) => setOrganizationId(event.target.value)}
            options={organizationOptions}
            disabled={targetsLoading && !isEdit}
            required
          />

          {!isEdit && !targetsLoading && organizationOptions.length <= 1 ? (
            <p className="text-sm text-amber-700 dark:text-amber-300">
              {t('myArea.organizationLeaveRequest.noLeaveTargets')}
            </p>
          ) : null}

          {isMainOrganizationLeave ? (
            <div className="space-y-2 rounded-md border border-amber-600/40 bg-amber-50 px-3 py-2 text-sm text-amber-900 dark:bg-amber-950/30 dark:text-amber-200">
              <p>{t('myArea.organizationLeaveRequest.mainOrganizationWarning')}</p>
              {mainOrganizationChangeBeginnable ? (
                <div className="mt-1 space-y-2.5 border-t border-amber-600/30 pt-3">
                  <p className="text-amber-950/90 dark:text-amber-100/90">
                    {t('myArea.organizationLeaveRequest.mainOrganizationLeaveAlternativeHint')}
                  </p>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    fullWidth="responsive"
                    icon={<ArrowRightLeft className="h-4 w-4 shrink-0" aria-hidden />}
                    onClick={() => setMainOrganizationChangeOpen(true)}
                    disabled={mainOrganizationChangeLinkDisabled}
                    title={mainOrganizationChangeLinkDisabledTitle}
                    className="!border-amber-700/50 !text-amber-950 hover:!bg-amber-100/80 dark:!border-amber-500/40 dark:!text-amber-50 dark:hover:!bg-amber-900/40"
                  >
                    {t('myArea.mainOrganizationChange.requestButton')}
                  </Button>
                </div>
              ) : (
                <p className="border-t border-amber-600/30 pt-3">
                  {t('myArea.organizationLeaveRequest.mainOrganizationLeaveAlternativeHintPlain')}
                </p>
              )}
            </div>
          ) : null}

          <Textarea
            id="organization-leave-reason"
            label={t('common.request.reasonLabel')}
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            rows={4}
            maxLength={2000}
            required
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
            inputId="organization-leave-attachments"
            files={files}
            onChange={setFiles}
            label={t('common.request.attachmentsLabel')}
            hint={t('myArea.organizationLeaveRequest.attachmentsHint')}
            optionalLabel={t('common.request.attachmentsOptional')}
            removeFileLabel={t('common.attachments.removeFile')}
          />
        </div>
      </Modal>
      {mainOrganizationChangeBeginnable ? (
        <MainOrganizationChangeModal
          isOpen={mainOrganizationChangeOpen}
          onClose={() => setMainOrganizationChangeOpen(false)}
          userData={userData}
        />
      ) : null}
    </>
  );
}
