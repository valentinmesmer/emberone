/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo, useState } from 'react';
import { DrivingLicenseRecordFormFields } from '@/components/my-area/drivingLicenses/DrivingLicenseRecordFormFields';
import { WorkflowInputChangeRequestNote } from '@/components/my-area/workflows/WorkflowInputChangeRequestNote';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { ApiRequestError } from '@/lib/api/client';
import type {
  DrivingLicenseClassInput,
  DrivingLicenseDefinitionDto,
} from '@/lib/api/drivingLicenses';
import { workflowsApi } from '@/lib/api/workflows';
import {
  useSubmitDrivingLicenseRecordWorkflow,
  useUpdateWorkflowPayload,
} from '@/lib/store/queries/workflows';
import { getTodayIsoDate } from '@/lib/utils/date';
import {
  type DrivingLicenseRecordWorkflowPayload,
  WORKFLOW_CLIENT_ERROR_CODES,
  WORKFLOW_TYPE_IDS,
} from '@/types/workflow';

const EMPTY_PRESET_CLASSES: DrivingLicenseClassInput[] = [];

export interface DrivingLicenseRecordSubmitModalProps {
  isOpen: boolean;
  onClose: () => void;
  classDefinitions: DrivingLicenseDefinitionDto[];
  /** Pre-select classes for first-time submission (e.g. from qualification requirements). */
  presetClasses?: DrivingLicenseClassInput[];
  /** User already has an active license - submit archives the old record and creates a new one. */
  renewal?: boolean;
  editWorkflowId?: number | null;
  initialPayload?: DrivingLicenseRecordWorkflowPayload | null;
  requestMessage?: string | null;
}

function resolveSubmitInitialValues(
  isEdit: boolean,
  initialPayload: DrivingLicenseRecordWorkflowPayload | null,
  presetClasses: DrivingLicenseClassInput[],
) {
  if (isEdit && initialPayload) {
    return {
      issueDate: initialPayload.issueDate,
      documentExpirationDate: initialPayload.documentExpirationDate ?? '',
      licenseNumber: initialPayload.licenseNumber ?? '',
      issuingAuthority: initialPayload.issuingAuthority ?? '',
      notes: initialPayload.notes ?? '',
      classes: initialPayload.classes ?? [],
      attachmentIds: initialPayload.attachmentIds ?? [],
    };
  }
  return {
    issueDate: getTodayIsoDate(),
    documentExpirationDate: '',
    licenseNumber: '',
    issuingAuthority: '',
    notes: '',
    classes: presetClasses,
    attachmentIds: [] as number[],
  };
}

function submitModalBodyKey(
  editWorkflowId: number | null | undefined,
  renewal: boolean,
  presetClasses: DrivingLicenseClassInput[],
) {
  if (editWorkflowId != null && editWorkflowId > 0) {
    return `edit-${editWorkflowId}`;
  }
  const presetKey = presetClasses
    .map((entry) => entry.tenantDrivingLicenseDefinitionId)
    .sort((left, right) => left - right)
    .join('-');
  return `submit-${renewal ? 'renewal' : 'new'}-${presetKey}`;
}

export function DrivingLicenseRecordSubmitModal({
  isOpen,
  classDefinitions,
  presetClasses = EMPTY_PRESET_CLASSES,
  renewal = false,
  editWorkflowId = null,
  initialPayload = null,
  requestMessage = null,
  ...props
}: DrivingLicenseRecordSubmitModalProps) {
  if (!isOpen) {
    return null;
  }

  return (
    <DrivingLicenseRecordSubmitModalBody
      key={submitModalBodyKey(editWorkflowId, renewal, presetClasses)}
      isOpen={isOpen}
      classDefinitions={classDefinitions}
      presetClasses={presetClasses}
      renewal={renewal}
      editWorkflowId={editWorkflowId}
      initialPayload={initialPayload}
      requestMessage={requestMessage}
      {...props}
    />
  );
}

function DrivingLicenseRecordSubmitModalBody({
  isOpen,
  onClose,
  classDefinitions,
  presetClasses = EMPTY_PRESET_CLASSES,
  renewal = false,
  editWorkflowId = null,
  initialPayload = null,
  requestMessage = null,
}: DrivingLicenseRecordSubmitModalProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const submit = useSubmitDrivingLicenseRecordWorkflow();
  const updatePayload = useUpdateWorkflowPayload();
  const isEdit = editWorkflowId != null && editWorkflowId > 0;
  const pending = isEdit ? updatePayload.isPending : submit.isPending;

  const initial = resolveSubmitInitialValues(isEdit, initialPayload, presetClasses);

  const [issueDate, setIssueDate] = useState(initial.issueDate);
  const [documentExpirationDate, setDocumentExpirationDate] = useState(
    initial.documentExpirationDate,
  );
  const [licenseNumber, setLicenseNumber] = useState(initial.licenseNumber);
  const [issuingAuthority, setIssuingAuthority] = useState(initial.issuingAuthority);
  const [notes, setNotes] = useState(initial.notes);
  const [classes, setClasses] = useState<DrivingLicenseClassInput[]>(initial.classes);
  const [existingAttachmentIds] = useState(initial.attachmentIds);
  const [files, setFiles] = useState<File[]>([]);

  const hasAttachments = existingAttachmentIds.length > 0 || files.length > 0;

  const canSubmit = useMemo(
    () =>
      issueDate.trim().length > 0 &&
      licenseNumber.trim().length > 0 &&
      issuingAuthority.trim().length > 0 &&
      classes.length > 0 &&
      classDefinitions.length > 0 &&
      hasAttachments,
    [
      issueDate,
      licenseNumber,
      issuingAuthority,
      classes.length,
      classDefinitions.length,
      hasAttachments,
    ],
  );

  const handleSubmit = async () => {
    if (!canSubmit) return;
    const payload: Omit<DrivingLicenseRecordWorkflowPayload, 'attachmentIds'> = {
      issueDate,
      documentExpirationDate: documentExpirationDate.trim() || null,
      licenseNumber: licenseNumber.trim(),
      issuingAuthority: issuingAuthority.trim(),
      classes: classes.map((entry) => ({
        tenantDrivingLicenseDefinitionId: entry.tenantDrivingLicenseDefinitionId,
        grantDate: entry.grantDate?.trim() || null,
        expirationDate: entry.expirationDate?.trim() || null,
      })),
      notes: notes.trim() || null,
    };

    try {
      if (isEdit && editWorkflowId != null) {
        const uploadedIds =
          files.length > 0
            ? (
                await Promise.all(
                  files.map((file) =>
                    workflowsApi.uploadWorkflowDraftAttachment(
                      WORKFLOW_TYPE_IDS.DRIVING_LICENSE_RECORD,
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
      showSuccess(
        renewal ? t('drivingLicense.update.success') : t('drivingLicense.submit.success'),
      );
      onClose();
    } catch (error) {
      if (
        error instanceof ApiRequestError &&
        error.code === WORKFLOW_CLIENT_ERROR_CODES.TYPE_ALREADY_OPEN
      ) {
        showError(t('drivingLicense.submit.alreadyOpen'));
        return;
      }
      showError(error instanceof Error ? error.message : t('drivingLicense.submit.error'));
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={
        isEdit
          ? t('drivingLicense.submit.editTitle')
          : renewal
            ? t('drivingLicense.update.title')
            : t('drivingLicense.submit.title')
      }
      size="lg"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleSubmit()}
          isValid={canSubmit}
          isLoading={pending}
          saveText={
            isEdit
              ? t('common.save')
              : renewal
                ? t('drivingLicense.update.action')
                : t('drivingLicense.submit.action')
          }
          showSave
        />
      }
    >
      <div className="space-y-4">
        {requestMessage ? <WorkflowInputChangeRequestNote message={requestMessage} /> : null}
        {renewal && !isEdit ? (
          <p className="text-sm text-[var(--text-secondary)]">{t('drivingLicense.update.hint')}</p>
        ) : null}
        {isEdit && existingAttachmentIds.length > 0 ? (
          <p className="text-xs text-[var(--text-secondary)]">
            {t('myArea.workflowDetail.editInputKeepAttachments', {
              count: existingAttachmentIds.length,
            })}
          </p>
        ) : null}
        <DrivingLicenseRecordFormFields
          classDefinitions={classDefinitions}
          issueDate={issueDate}
          onIssueDateChange={setIssueDate}
          documentExpirationDate={documentExpirationDate}
          onDocumentExpirationDateChange={setDocumentExpirationDate}
          licenseNumber={licenseNumber}
          onLicenseNumberChange={setLicenseNumber}
          issuingAuthority={issuingAuthority}
          onIssuingAuthorityChange={setIssuingAuthority}
          classes={classes}
          onClassesChange={setClasses}
          notes={notes}
          onNotesChange={setNotes}
          files={files}
          onFilesChange={setFiles}
          disabled={pending}
          attachmentsInputId="driving-license-workflow-attachments"
        />
      </div>
    </Modal>
  );
}
