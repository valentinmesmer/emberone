/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo, useState } from 'react';
import { TrainingCourseRecordFormFields } from '@/components/my-area/trainingCourses/TrainingCourseRecordFormFields';
import { WorkflowInputChangeRequestNote } from '@/components/my-area/workflows/WorkflowInputChangeRequestNote';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { ApiRequestError } from '@/lib/api/client';
import type { TrainingCourseTypeDto } from '@/lib/api/trainingCourses';
import { workflowsApi } from '@/lib/api/workflows';
import {
  useSubmitTrainingCourseRecordWorkflow,
  useUpdateWorkflowPayload,
} from '@/lib/store/queries/workflows';
import { getTodayIsoDate, isEndStrictlyBeforeStart } from '@/lib/utils/date';
import {
  type TrainingCourseRecordWorkflowPayload,
  WORKFLOW_CLIENT_ERROR_CODES,
  WORKFLOW_TYPE_IDS,
} from '@/types/workflow';

export interface TrainingCourseRecordSubmitModalProps {
  isOpen: boolean;
  onClose: () => void;
  submittableTypes: TrainingCourseTypeDto[];
  /** Pre-selects type ids when opening the modal. */
  presetTypeIds?: number[];
  /** Hides the type picker and uses {@link presetTypeIds}. */
  lockTypes?: boolean;
  /** When set, updates an existing workflow payload instead of opening a new one. */
  editWorkflowId?: number | null;
  initialPayload?: TrainingCourseRecordWorkflowPayload | null;
  /** Comment from an open input-change request, shown while editing. */
  requestMessage?: string | null;
}

export function TrainingCourseRecordSubmitModal({
  isOpen,
  onClose,
  submittableTypes,
  presetTypeIds,
  lockTypes = false,
  editWorkflowId = null,
  initialPayload = null,
  requestMessage = null,
}: TrainingCourseRecordSubmitModalProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const submit = useSubmitTrainingCourseRecordWorkflow();
  const updatePayload = useUpdateWorkflowPayload();
  const isEdit = editWorkflowId != null && editWorkflowId > 0;
  const pending = isEdit ? updatePayload.isPending : submit.isPending;

  const presetTypes = useMemo(() => {
    if (!presetTypeIds?.length) return [];
    const allowed = new Set(presetTypeIds);
    return submittableTypes.filter((type) => allowed.has(type.id));
  }, [presetTypeIds, submittableTypes]);

  const resolveInitial = () => {
    if (isEdit && initialPayload) {
      const typeIds = initialPayload.tenantTrainingCourseTypeIds ?? [];
      return {
        selectedTypeIds: typeIds,
        name: initialPayload.name ?? '',
        startDate: initialPayload.startDate,
        endDate: initialPayload.endDate,
        location: initialPayload.location,
        hours:
          initialPayload.hours != null && !Number.isNaN(initialPayload.hours)
            ? String(initialPayload.hours)
            : '',
        notes: initialPayload.notes ?? '',
        attachmentIds: initialPayload.attachmentIds ?? [],
      };
    }
    if (presetTypes.length > 0) {
      return {
        selectedTypeIds: presetTypes.map((type) => type.id),
        name: '',
        startDate: getTodayIsoDate(),
        endDate: getTodayIsoDate(),
        location: '',
        hours: '',
        notes: '',
        attachmentIds: [] as number[],
      };
    }
    return {
      selectedTypeIds: submittableTypes.length === 1 ? [submittableTypes[0].id] : [],
      name: '',
      startDate: getTodayIsoDate(),
      endDate: getTodayIsoDate(),
      location: '',
      hours: '',
      notes: '',
      attachmentIds: [] as number[],
    };
  };

  const [selectedTypeIds, setSelectedTypeIds] = useState(() => resolveInitial().selectedTypeIds);
  const [name, setName] = useState(() => resolveInitial().name);
  const [startDate, setStartDate] = useState(() => resolveInitial().startDate);
  const [endDate, setEndDate] = useState(() => resolveInitial().endDate);
  const [location, setLocation] = useState(() => resolveInitial().location);
  const [hours, setHours] = useState(() => resolveInitial().hours);
  const [notes, setNotes] = useState(() => resolveInitial().notes);
  const [existingAttachmentIds, setExistingAttachmentIds] = useState(
    () => resolveInitial().attachmentIds,
  );
  const [files, setFiles] = useState<File[]>([]);
  const [prevIsOpen, setPrevIsOpen] = useState(isOpen);
  const depsKey = `${editWorkflowId ?? 'new'}:${JSON.stringify(initialPayload)}:${presetTypes.map((d) => d.id).join(',')}:${submittableTypes.map((d) => d.id).join(',')}`;
  const [prevDepsKey, setPrevDepsKey] = useState(depsKey);

  if (isOpen !== prevIsOpen) {
    setPrevIsOpen(isOpen);
    if (isOpen) {
      const initial = resolveInitial();
      setSelectedTypeIds(initial.selectedTypeIds);
      setName(initial.name);
      setStartDate(initial.startDate);
      setEndDate(initial.endDate);
      setLocation(initial.location);
      setHours(initial.hours);
      setNotes(initial.notes);
      setExistingAttachmentIds(initial.attachmentIds);
      setFiles([]);
      setPrevDepsKey(depsKey);
    }
  } else if (isOpen && depsKey !== prevDepsKey) {
    setPrevDepsKey(depsKey);
    const initial = resolveInitial();
    setSelectedTypeIds(initial.selectedTypeIds);
    setName(initial.name);
    setStartDate(initial.startDate);
    setEndDate(initial.endDate);
    setLocation(initial.location);
    setHours(initial.hours);
    setNotes(initial.notes);
    setExistingAttachmentIds(initial.attachmentIds);
    setFiles([]);
  }

  const requiresName = selectedTypeIds.length > 1;

  const dateOrderInvalid =
    !!startDate.trim() && !!endDate.trim() && isEndStrictlyBeforeStart(startDate, endDate);

  const hasAttachments = existingAttachmentIds.length > 0 || files.length > 0;

  const canSubmit =
    selectedTypeIds.length > 0 &&
    (!requiresName || name.trim().length > 0) &&
    startDate.trim().length > 0 &&
    endDate.trim().length > 0 &&
    !dateOrderInvalid &&
    location.trim().length > 0 &&
    hasAttachments &&
    !pending;

  const handleSubmit = async () => {
    if (!canSubmit) return;
    try {
      const parsedHours = hours.trim() ? Number.parseFloat(hours) : null;
      if (isEdit) {
        if (editWorkflowId == null) return;
        const uploadedIds =
          files.length > 0
            ? (
                await Promise.all(
                  files.map((file) =>
                    workflowsApi.uploadWorkflowDraftAttachment(
                      WORKFLOW_TYPE_IDS.TRAINING_COURSE_RECORD,
                      file,
                    ),
                  ),
                )
              ).map((item) => item.id)
            : [];
        const payload: TrainingCourseRecordWorkflowPayload = {
          tenantTrainingCourseTypeIds: selectedTypeIds,
          name: requiresName ? name.trim() : undefined,
          startDate,
          endDate,
          location: location.trim(),
          hours: parsedHours,
          notes: notes.trim() || undefined,
          attachmentIds: [...existingAttachmentIds, ...uploadedIds],
        };
        await updatePayload.mutateAsync({ workflowId: editWorkflowId, payload });
        showSuccess(t('myArea.workflowDetail.editInputSuccess'));
      } else {
        await submit.mutateAsync({
          files,
          payload: {
            tenantTrainingCourseTypeIds: selectedTypeIds,
            name: requiresName ? name.trim() : undefined,
            startDate,
            endDate,
            location: location.trim(),
            hours: parsedHours,
            notes: notes.trim() || undefined,
          },
        });
        showSuccess(t('trainingCourse.submit.success'));
      }
      onClose();
    } catch (e) {
      if (
        e instanceof ApiRequestError &&
        e.code === WORKFLOW_CLIENT_ERROR_CODES.TRAINING_COURSE_TYPE_ALREADY_OPEN
      ) {
        showError(t('trainingCourse.submit.errorAlreadyOpen'));
        return;
      }
      showError(
        e instanceof Error
          ? e.message
          : isEdit
            ? t('myArea.workflowDetail.editInputError')
            : t('trainingCourse.submit.error'),
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
      title={isEdit ? t('myArea.workflowDetail.editInputTitle') : t('trainingCourse.submit.title')}
      size="md"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleSubmit()}
          isValid={canSubmit}
          isLoading={pending}
          saveText={
            isEdit ? t('myArea.workflowDetail.editInputSubmit') : t('trainingCourse.submit.action')
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

        <TrainingCourseRecordFormFields
          types={submittableTypes}
          selectedTypeIds={selectedTypeIds}
          onSelectedTypeIdsChange={setSelectedTypeIds}
          lockTypes={lockTypes}
          name={name}
          onNameChange={setName}
          startDate={startDate}
          onStartDateChange={setStartDate}
          endDate={endDate}
          onEndDateChange={setEndDate}
          location={location}
          onLocationChange={setLocation}
          hours={hours}
          onHoursChange={setHours}
          notes={notes}
          onNotesChange={setNotes}
          files={files}
          onFilesChange={setFiles}
          disabled={pending}
          attachmentsInputId="training-course-submit-attachments"
        />
      </div>
    </Modal>
  );
}
