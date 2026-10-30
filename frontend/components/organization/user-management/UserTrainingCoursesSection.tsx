/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { parseDate } from '@internationalized/date';
import { Pencil, Trash2 } from 'lucide-react';
import { useMemo, useState } from 'react';
import { PendingAttachments } from '@/components/attachment';
import { TrainingCourseRecordEditModal } from '@/components/my-area/trainingCourses/TrainingCourseRecordEditModal';
import { TrainingCourseSubmissionListRow } from '@/components/my-area/trainingCourses/TrainingCourseSubmissionListRow';
import { TrainingCourseTypeSelectionField } from '@/components/my-area/trainingCourses/TrainingCourseTypeSelectionField';
import { Badge } from '@/components/ui/badge';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { DatePicker } from '@/components/ui/date-picker';
import { IconButton } from '@/components/ui/icon-button';
import { Input } from '@/components/ui/input';
import { ListContainer } from '@/components/ui/list';
import { ListLoadMore, ListNoResults } from '@/components/ui/list-filter';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import type { TrainingCourseTypeDto, TrainingCourseUserRecordDto } from '@/lib/api/trainingCourses';
import {
  getOrganizationUserTrainingCourseRecordDetailPath,
  getOrganizationUserWorkflowDetailPath,
} from '@/lib/navigation/userManagementSections';
import { usePermissions } from '@/lib/permission/PermissionContext';
import {
  useCreateTrainingCourseRecord,
  useDeleteTrainingCourseRecord,
  useOrgTrainingCourseCatalog,
  useTrainingCourseRecordListState,
  useUploadOrganizationUserDocument,
  useUserTrainingCoursePendingSubmissions,
  useUserTrainingCourseRecordList,
} from '@/lib/store';
import {
  getTodayIsoDate,
  isEndStrictlyBeforeStart,
  laterOfDateStrings,
  shiftEndDateWithStartDateChange,
} from '@/lib/utils/date';
import { PermissionTypeEnum } from '@/types/permission';

interface Props {
  organizationId: number;
  userId: number;
  canWrite: boolean;
  isAddOpen: boolean;
  onAddClose: () => void;
}

interface UserTrainingCourseAddModalProps {
  organizationId: number;
  userId: number;
  onClose: () => void;
  typeOptions: TrainingCourseTypeDto[];
}

function UserTrainingCourseAddModal({
  organizationId,
  userId,
  onClose,
  typeOptions,
}: UserTrainingCourseAddModalProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const createRecord = useCreateTrainingCourseRecord(organizationId, userId);
  const uploadDocument = useUploadOrganizationUserDocument(organizationId, userId);

  const [selectedTypeIds, setSelectedTypeIds] = useState<number[]>(() =>
    typeOptions.length === 1 ? [typeOptions[0].id] : [],
  );
  const [name, setName] = useState('');
  const [startDate, setStartDate] = useState(getTodayIsoDate);
  const [endDate, setEndDate] = useState(getTodayIsoDate);
  const [location, setLocation] = useState('');
  const [hours, setHours] = useState('');
  const [notes, setNotes] = useState('');
  const [files, setFiles] = useState<File[]>([]);

  const requiresName = selectedTypeIds.length > 1;

  const dateOrderInvalid =
    !!startDate.trim() && !!endDate.trim() && isEndStrictlyBeforeStart(startDate, endDate);

  const dateOrderError = dateOrderInvalid ? t('common.validation.endNotBeforeStart') : undefined;

  const endDateMinValue = useMemo(() => {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(startDate)) return undefined;
    try {
      return parseDate(startDate);
    } catch {
      return undefined;
    }
  }, [startDate]);

  const canSubmit =
    selectedTypeIds.length > 0 &&
    (!requiresName || name.trim().length > 0) &&
    startDate.trim().length > 0 &&
    endDate.trim().length > 0 &&
    !dateOrderInvalid &&
    location.trim().length > 0 &&
    typeOptions.length > 0;

  const handleStartDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const next = e.target.value;
    const prevStart = startDate;
    setStartDate(next);
    setEndDate((prevEnd) => {
      if (!prevEnd || !next) return prevEnd;
      if (
        /^\d{4}-\d{2}-\d{2}$/.test(prevEnd) &&
        /^\d{4}-\d{2}-\d{2}$/.test(next) &&
        /^\d{4}-\d{2}-\d{2}$/.test(prevStart)
      ) {
        return shiftEndDateWithStartDateChange(prevStart, next, prevEnd);
      }
      return laterOfDateStrings(prevEnd, next);
    });
  };

  const handleCreate = async () => {
    if (!canSubmit || selectedTypeIds.length === 0 || !startDate || !endDate) return;
    try {
      let userDocumentIds: number[] | undefined;
      if (files.length > 0) {
        const uploadedDocuments = await Promise.all(
          files.map((file) =>
            uploadDocument.mutateAsync({
              file,
              title: file.name.replace(/\.[^.]+$/, ''),
            }),
          ),
        );
        userDocumentIds = uploadedDocuments.map((doc) => doc.id);
      }
      const parsedHours = hours.trim() ? Number.parseFloat(hours) : null;
      await createRecord.mutateAsync({
        tenantTrainingCourseTypeIds: selectedTypeIds,
        name: requiresName ? name.trim() : undefined,
        startDate,
        endDate,
        location: location.trim(),
        hours: parsedHours,
        userDocumentIds,
        notes: notes.trim() || null,
      });
      showSuccess(t('trainingCourse.record.createSuccess'));
      onClose();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('trainingCourse.record.createError'));
    }
  };

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={t('trainingCourse.record.createTitle')}
      size="md"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleCreate()}
          isValid={canSubmit}
          isLoading={createRecord.isPending || uploadDocument.isPending}
          saveText={t('trainingCourse.record.createAction')}
          showSave
        />
      }
    >
      <div className="space-y-4">
        <TrainingCourseTypeSelectionField
          types={typeOptions}
          selectedIds={selectedTypeIds}
          onChange={setSelectedTypeIds}
          disabled={createRecord.isPending || uploadDocument.isPending}
        />
        {requiresName ? (
          <Input
            label={t('trainingCourse.submissionName.label')}
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder={t('trainingCourse.submissionName.placeholder')}
            hint={t('trainingCourse.submissionName.hint')}
            disabled={createRecord.isPending || uploadDocument.isPending}
            required
          />
        ) : null}
        <DatePicker
          label={t('trainingCourse.record.startDateLabel')}
          value={startDate}
          onChange={handleStartDateChange}
          disabled={createRecord.isPending}
          required
        />
        <DatePicker
          label={t('trainingCourse.record.endDateLabel')}
          value={endDate}
          onChange={(e) => setEndDate(e.target.value)}
          minValue={endDateMinValue}
          error={dateOrderError}
          disabled={createRecord.isPending}
          required
        />
        <Input
          label={t('trainingCourse.record.locationLabel')}
          value={location}
          onChange={(e) => setLocation(e.target.value)}
          placeholder={t('trainingCourse.record.locationPlaceholder')}
          disabled={createRecord.isPending}
          required
        />
        <Input
          label={t('trainingCourse.record.hoursLabel')}
          value={hours}
          onChange={(e) => setHours(e.target.value)}
          inputMode="decimal"
          disabled={createRecord.isPending}
        />
        <Textarea
          label={t('common.record.notesLabel')}
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          disabled={createRecord.isPending}
        />
        <PendingAttachments
          variant="field"
          inputId="training-course-attachments"
          files={files}
          onChange={setFiles}
          label={t('trainingCourse.record.attachmentsLabel')}
          hint={t('trainingCourse.record.attachmentsHint')}
          removeFileLabel={t('common.attachments.removeFile')}
        />
      </div>
    </Modal>
  );
}

export function UserTrainingCoursesSection({
  organizationId,
  userId,
  canWrite,
  isAddOpen,
  onAddClose,
}: Props) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const { hasPermission } = usePermissions();

  const listState = useTrainingCourseRecordListState();
  const {
    data: records = [],
    isPending: recordsLoading,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useUserTrainingCourseRecordList({
    organizationId,
    userId,
    listQuery: listState.listQuery,
  });
  const { data: pendingSubmissions = [], isLoading: pendingLoading } =
    useUserTrainingCoursePendingSubmissions(organizationId, userId);
  const { data: catalog } = useOrgTrainingCourseCatalog(organizationId, userId);
  const canReadWorkflows = hasPermission(organizationId, PermissionTypeEnum.WORKFLOW_READ);
  const deleteRecord = useDeleteTrainingCourseRecord(organizationId, userId);

  const [editTarget, setEditTarget] = useState<TrainingCourseUserRecordDto | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<TrainingCourseUserRecordDto | null>(null);

  const typeOptions = useMemo(
    () => catalog?.types.filter((type) => type.active) ?? [],
    [catalog?.types],
  );
  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteRecord.mutateAsync(deleteTarget.id);
      showSuccess(t('trainingCourse.record.deleteSuccess'));
      setDeleteTarget(null);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('trainingCourse.record.deleteError'));
    }
  };

  if (recordsLoading || pendingLoading) {
    return <LoadingState />;
  }

  return (
    <div className="space-y-6">
      <section>
        {records.length === 0 && pendingSubmissions.length === 0 ? (
          listState.hasActiveFilters ? (
            <ListNoResults onReset={listState.reset} />
          ) : (
            <EmptyStateCard>{t('trainingCourse.empty')}</EmptyStateCard>
          )
        ) : (
          <ListContainer label={t('trainingCourse.title')}>
            {pendingSubmissions.map((pending) => (
              <TrainingCourseSubmissionListRow
                key={pending.workflowId}
                record={{
                  id: pending.workflowId,
                  userId,
                  name:
                    pending.name ??
                    (pending.courseTypeNames.length > 0
                      ? pending.courseTypeNames.join(', ')
                      : t('trainingCourse.record.detailTitle')),
                  startDate: pending.startDate,
                  endDate: pending.endDate,
                  location: pending.location,
                  hours: pending.hours,
                  userDocumentIds: [],
                  grantedTypes: pending.tenantTrainingCourseTypeIds.map((typeId, index) => ({
                    tenantTrainingCourseTypeId: typeId,
                    courseTypeName: pending.courseTypeNames[index] ?? null,
                    typeArchived: false,
                  })),
                  notes: null,
                  appLinkType: null,
                  appLinkId: pending.workflowId,
                  navigationPath: getOrganizationUserWorkflowDetailPath(
                    organizationId,
                    userId,
                    pending.workflowId,
                  ),
                }}
                href={
                  canReadWorkflows
                    ? getOrganizationUserWorkflowDetailPath(
                        organizationId,
                        userId,
                        pending.workflowId,
                      )
                    : undefined
                }
                chevron={canReadWorkflows}
                tone="warning"
                badges={<Badge tone="warning">{t('certifications.status.IN_REVIEW')}</Badge>}
              />
            ))}
            {records.map((record) => (
              <TrainingCourseSubmissionListRow
                key={record.id}
                record={record}
                href={getOrganizationUserTrainingCourseRecordDetailPath(
                  organizationId,
                  userId,
                  record.id,
                )}
                badges={
                  record.grantedTypes.some((type) => type.typeArchived) ? (
                    <Badge tone="neutral">{t('trainingCourse.archivedTag')}</Badge>
                  ) : undefined
                }
                actions={
                  canWrite ? (
                    <>
                      <IconButton
                        variant="action"
                        icon={<Pencil className="h-4 w-4" aria-hidden />}
                        aria-label={t('common.edit')}
                        title={t('common.edit')}
                        onClick={() => setEditTarget(record)}
                        disabled={deleteRecord.isPending}
                      />
                      <IconButton
                        variant="action"
                        icon={<Trash2 className="h-4 w-4" aria-hidden />}
                        aria-label={t('common.delete')}
                        title={t('common.delete')}
                        onClick={() => setDeleteTarget(record)}
                        disabled={deleteRecord.isPending}
                      />
                    </>
                  ) : undefined
                }
              />
            ))}
          </ListContainer>
        )}
        <ListLoadMore
          hasNextPage={hasNextPage}
          isFetchingNextPage={isFetchingNextPage}
          onLoadMore={() => void fetchNextPage()}
        />
      </section>

      {isAddOpen ? (
        <UserTrainingCourseAddModal
          key={typeOptions.map((type) => type.id).join(',') || 'loading'}
          organizationId={organizationId}
          userId={userId}
          onClose={onAddClose}
          typeOptions={typeOptions}
        />
      ) : null}

      {canWrite && editTarget ? (
        <TrainingCourseRecordEditModal
          isOpen
          onClose={() => setEditTarget(null)}
          organizationId={organizationId}
          userId={userId}
          record={editTarget}
        />
      ) : null}

      {canWrite ? (
        <ConfirmDialog
          isOpen={deleteTarget !== null}
          onClose={() => setDeleteTarget(null)}
          onConfirm={() => void handleDelete()}
          title={t('trainingCourse.record.confirmDeleteTitle')}
          message={
            deleteTarget
              ? t('trainingCourse.record.confirmDeleteMessage', {
                  name: deleteTarget.name,
                })
              : ''
          }
          isLoading={deleteRecord.isPending}
        />
      ) : null}
    </div>
  );
}
