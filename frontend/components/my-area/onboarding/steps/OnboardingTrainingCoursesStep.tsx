/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Download, FileText, MapPin, Pencil, Plus, Trash2 } from 'lucide-react';
import { forwardRef, useCallback, useImperativeHandle, useMemo, useState } from 'react';
import { TrainingCourseRecordFormFields } from '@/components/my-area/trainingCourses/TrainingCourseRecordFormFields';
import { formatGrantedTrainingCourseTypeNames } from '@/components/my-area/trainingCourses/TrainingCourseTypeSelectionField';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { IconButton } from '@/components/ui/icon-button';
import { ListContainer, ListRow, ListRowMeta, ListRowMetaItem } from '@/components/ui/list';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useTranslation } from '@/hooks/useTranslation';
import { onboardingInvitationApi } from '@/lib/api/onboardingInvitation';
import type { TrainingCourseTypeDto } from '@/lib/api/trainingCourses';
import {
  hasOnboardingAttachments,
  readOnboardingAttachmentFileNames,
  withOnboardingAttachmentFileNames,
} from '@/lib/onboarding/attachmentUtils';
import { readNoneDeclared, readTrainingCourses } from '@/lib/onboarding/stepPayloads';
import { formatDateLong, getTodayIsoDate, isEndStrictlyBeforeStart } from '@/lib/utils/date';
import type {
  OnboardingInvitationAccess,
  OnboardingTrainingCourseDraft,
} from '@/types/onboardingInvitation';
import { useDownloadOnboardingAttachment } from '../useDownloadOnboardingAttachment';
import { useOnboardingRecordAttachments } from '../useOnboardingRecordAttachments';
import type { OnboardingStepHandle, OnboardingStepProps } from './onboardingStepHandle';
import { payloadsEqual } from './onboardingStepHandle';

interface OnboardingTrainingCoursesStepProps extends OnboardingStepProps {
  access: OnboardingInvitationAccess;
  types: TrainingCourseTypeDto[];
}

type RecordRow = OnboardingTrainingCourseDraft & { key: string };

const toTrainingCoursesPayload = (rows: RecordRow[], declared: boolean) => ({
  records: rows.map((record) => ({
    recordKey: record.recordKey,
    tenantTrainingCourseTypeIds: record.tenantTrainingCourseTypeIds,
    name: record.name,
    startDate: record.startDate,
    endDate: record.endDate,
    location: record.location,
    hours: record.hours,
    notes: record.notes,
  })),
  noneDeclared: rows.length === 0 && declared,
});

const emptyDraft = (): OnboardingTrainingCourseDraft => ({
  recordKey: crypto.randomUUID(),
  tenantTrainingCourseTypeIds: [],
  name: '',
  startDate: getTodayIsoDate(),
  endDate: getTodayIsoDate(),
  location: '',
  hours: null,
  notes: '',
  fileName: null,
  fileNames: [],
  uploaded: false,
});

export const OnboardingTrainingCoursesStep = forwardRef<
  OnboardingStepHandle,
  OnboardingTrainingCoursesStepProps
>(function OnboardingTrainingCoursesStep({ access, payload, types, editable, isSaving }, ref) {
  const t = useTranslation();
  const downloadAttachment = useDownloadOnboardingAttachment();
  const [records, setRecords] = useState<RecordRow[]>(() =>
    readTrainingCourses(payload).map((record) => ({ ...record, key: crypto.randomUUID() })),
  );
  const [noneDeclared, setNoneDeclared] = useState(
    () => readNoneDeclared(payload) && readTrainingCourses(payload).length === 0,
  );
  const [formOpen, setFormOpen] = useState(false);
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const [draft, setDraft] = useState<OnboardingTrainingCourseDraft>(emptyDraft);
  const [hoursInput, setHoursInput] = useState('');

  const applyRecordFileNames = useCallback(
    (fileNames: string[]) => {
      setDraft((current) => withOnboardingAttachmentFileNames(current, fileNames));
      setRecords((current) =>
        current.map((record) =>
          record.recordKey === draft.recordKey
            ? withOnboardingAttachmentFileNames(record, fileNames)
            : record,
        ),
      );
    },
    [draft.recordKey],
  );

  const { files, setFiles, uploading, removingIndex, handleRemoveStored, uploadPendingFiles } =
    useOnboardingRecordAttachments({
      stepType: 'TRAINING_COURSES',
      access,
      recordKey: draft.recordKey,
      draft,
      editable,
      onFileNamesChange: applyRecordFileNames,
    });

  const toPayload = toTrainingCoursesPayload;

  const savedSnapshot = useState(() =>
    toTrainingCoursesPayload(
      readTrainingCourses(payload).map((record) => ({ ...record, key: '' })),
      readNoneDeclared(payload) && readTrainingCourses(payload).length === 0,
    ),
  )[0];

  useImperativeHandle(
    ref,
    () => ({
      getPayload: () => toPayload(records, noneDeclared),
      hasChanges: () => !payloadsEqual(savedSnapshot, toPayload(records, noneDeclared)),
    }),
    [noneDeclared, records, savedSnapshot],
  );

  const requiresName = draft.tenantTrainingCourseTypeIds.length > 1;
  const dateOrderInvalid =
    !!draft.startDate.trim() &&
    !!draft.endDate.trim() &&
    isEndStrictlyBeforeStart(draft.startDate, draft.endDate);
  const hasAttachment = hasOnboardingAttachments(readOnboardingAttachmentFileNames(draft), files);

  const isValid = useMemo(
    () =>
      draft.tenantTrainingCourseTypeIds.length > 0 &&
      (!requiresName || (draft.name?.trim() ?? '').length > 0) &&
      draft.startDate.trim() !== '' &&
      draft.endDate.trim() !== '' &&
      !dateOrderInvalid &&
      draft.location.trim() !== '' &&
      hasAttachment,
    [dateOrderInvalid, draft, hasAttachment, requiresName],
  );

  const openCreate = () => {
    setEditingIndex(null);
    const nextDraft = emptyDraft();
    setDraft(nextDraft);
    setHoursInput('');
    setFiles([]);
    setFormOpen(true);
  };

  const openEdit = (index: number, record: RecordRow) => {
    setEditingIndex(index);
    setDraft(record);
    setHoursInput(record.hours == null ? '' : String(record.hours));
    setFiles([]);
    setFormOpen(true);
  };

  return (
    <div className="space-y-3">
      {editable ? (
        <p className="text-sm text-[var(--text-secondary)]">
          {t('myArea.onboarding.wizard.trainingCourses.hint')}
        </p>
      ) : null}
      {records.length === 0 ? (
        <Checkbox
          label={t('myArea.onboarding.wizard.trainingCourses.noneDeclared')}
          checked={noneDeclared}
          onCheckedChange={(checked) => {
            if (!editable || isSaving) return;
            setNoneDeclared(checked);
          }}
          disabled={!editable || isSaving}
        />
      ) : null}
      {noneDeclared ? null : records.length === 0 ? (
        <EmptyStateCard>{t('myArea.onboarding.wizard.trainingCourses.empty')}</EmptyStateCard>
      ) : (
        <ListContainer label={t('trainingCourse.title')}>
          {records.map((record, index) => (
            <ListRow
              key={record.key}
              title={
                record.name?.trim() ||
                formatGrantedTrainingCourseTypeNames(record.tenantTrainingCourseTypeIds, types)
              }
              subtitle={`${formatDateLong(record.startDate)} - ${formatDateLong(record.endDate)}`}
              meta={
                editable ? undefined : (
                  <ListRowMeta>
                    {record.location.trim() ? (
                      <ListRowMetaItem
                        icon={
                          <MapPin
                            className="mt-0.5 h-3 w-3 shrink-0 text-[var(--fire-red)]"
                            aria-hidden
                          />
                        }
                      >
                        {record.location}
                      </ListRowMetaItem>
                    ) : null}
                    {record.hours != null ? (
                      <ListRowMetaItem>
                        {t('trainingCourse.record.hours', { hours: record.hours })}
                      </ListRowMetaItem>
                    ) : null}
                    {record.notes?.trim() ? (
                      <ListRowMetaItem>{record.notes}</ListRowMetaItem>
                    ) : null}
                    {readOnboardingAttachmentFileNames(record).map((fileName) => (
                      <ListRowMetaItem
                        key={fileName}
                        icon={
                          <FileText
                            className="mt-0.5 h-3 w-3 shrink-0 text-[var(--fire-red)]"
                            aria-hidden
                          />
                        }
                      >
                        {fileName}
                      </ListRowMetaItem>
                    ))}
                  </ListRowMeta>
                )
              }
              actions={
                editable ? (
                  <div className="flex items-center gap-1">
                    <IconButton
                      icon={<Pencil className="h-4 w-4" />}
                      aria-label={t('common.edit')}
                      disabled={isSaving}
                      onClick={() => openEdit(index, record)}
                    />
                    <IconButton
                      icon={<Trash2 className="h-4 w-4" />}
                      aria-label={t('common.delete')}
                      disabled={isSaving}
                      onClick={() => {
                        setRecords(records.filter((_, itemIndex) => itemIndex !== index));
                        setNoneDeclared(false);
                      }}
                    />
                  </div>
                ) : readOnboardingAttachmentFileNames(record).length > 0 ? (
                  <IconButton
                    icon={<Download className="h-4 w-4" />}
                    aria-label={t('common.attachments.download')}
                    title={
                      readOnboardingAttachmentFileNames(record)[0] ??
                      t('common.attachments.download')
                    }
                    onClick={() =>
                      void downloadAttachment(() =>
                        onboardingInvitationApi.downloadRecordAttachment(
                          access,
                          'TRAINING_COURSES',
                          record.recordKey,
                          readOnboardingAttachmentFileNames(record)[0] ?? 'attachment',
                        ),
                      )
                    }
                  />
                ) : undefined
              }
            />
          ))}
        </ListContainer>
      )}
      {editable && !noneDeclared ? (
        <Button
          variant="secondary"
          icon={<Plus className="h-4 w-4" />}
          disabled={isSaving}
          onClick={openCreate}
        >
          {t('trainingCourse.record.createAction')}
        </Button>
      ) : null}
      {formOpen ? (
        <Modal
          isOpen
          onClose={() => setFormOpen(false)}
          title={t('trainingCourse.record.createTitle')}
          footer={
            <ModalFooter
              onCancel={() => setFormOpen(false)}
              onSave={async () => {
                if (!isValid || uploading) return;
                const savedDraft = await uploadPendingFiles();
                if (savedDraft == null) return;
                const nextRecord = {
                  ...savedDraft,
                  hours: hoursInput.trim() ? Number(hoursInput) : null,
                };
                const next =
                  editingIndex == null
                    ? [...records, { ...nextRecord, key: crypto.randomUUID() }]
                    : records.map((item, index) =>
                        index === editingIndex ? { ...nextRecord, key: item.key } : item,
                      );
                setRecords(next);
                setNoneDeclared(false);
                setFormOpen(false);
              }}
              isValid={isValid && !uploading}
              isLoading={uploading}
            />
          }
        >
          <TrainingCourseRecordFormFields
            types={types}
            selectedTypeIds={draft.tenantTrainingCourseTypeIds}
            onSelectedTypeIdsChange={(selectedIds) =>
              setDraft((current) => ({ ...current, tenantTrainingCourseTypeIds: selectedIds }))
            }
            name={draft.name ?? ''}
            onNameChange={(value) => setDraft((current) => ({ ...current, name: value }))}
            startDate={draft.startDate}
            onStartDateChange={(value) => setDraft((current) => ({ ...current, startDate: value }))}
            endDate={draft.endDate}
            onEndDateChange={(value) => setDraft((current) => ({ ...current, endDate: value }))}
            location={draft.location}
            onLocationChange={(value) => setDraft((current) => ({ ...current, location: value }))}
            hours={hoursInput}
            onHoursChange={setHoursInput}
            notes={draft.notes ?? ''}
            onNotesChange={(value) => setDraft((current) => ({ ...current, notes: value }))}
            files={files}
            onFilesChange={setFiles}
            storedFileNames={readOnboardingAttachmentFileNames(draft)}
            onRemoveStored={editable ? handleRemoveStored : undefined}
            disabled={!editable || isSaving || uploading || removingIndex != null}
            attachmentsInputId="onboarding-training-course-attachments"
          />
        </Modal>
      ) : null}
    </div>
  );
});
