/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { FileText, Pencil, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useMemo, useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { DetailCardAction, DetailCardHeader } from '@/components/ui/detail-card-header';
import { FieldDisplay } from '@/components/ui/field-display';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyState, NotFoundState } from '@/components/ui/message-cards';
import { RecordSourceLinkSection } from '@/components/ui/record-source-link-section';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { MY_AREA_BASE_PATH } from '@/lib/my-area/myAreaSections';
import {
  getOrganizationUserDocumentDetailPath,
  getOrganizationUserSectionPath,
} from '@/lib/navigation/userManagementSections';
import { usePermissions } from '@/lib/permission/PermissionContext';
import {
  useDeleteTrainingCourseRecord,
  useMyTrainingCourseRecords,
  useOrganizationUserDocuments,
  useUserDocuments,
  useUserTrainingCourseRecords,
} from '@/lib/store';
import { formatDateLong } from '@/lib/utils';
import type { AppNavigationContext } from '@/lib/utils/appLink';
import { resolveOnboardingSourceHref, resolveWorkflowSourceHref } from '@/lib/utils/sourceLink';
import { formatFileSize } from '@/lib/utils/userDocumentFormat';
import { TrainingCourseRecordEditModal } from './TrainingCourseRecordEditModal';

export type TrainingCourseRecordDetailViewProps =
  | {
      scope: 'self';
      recordId: number;
    }
  | {
      scope: 'org';
      organizationId: number;
      userId: number;
      recordId: number;
      canWrite?: boolean;
    };

export function TrainingCourseRecordDetailView(props: TrainingCourseRecordDetailViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();
  const { hasPermission } = usePermissions();
  const isSelf = props.scope === 'self';
  const navigationContext: AppNavigationContext = isSelf
    ? { scope: 'self' }
    : { scope: 'org', organizationId: props.organizationId, userId: props.userId };
  const canWrite = !isSelf && props.canWrite === true;
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);

  const deleteRecord = useDeleteTrainingCourseRecord(
    isSelf ? 0 : props.organizationId,
    isSelf ? 0 : props.userId,
  );

  const { data: myRecords = [], isLoading: myRecordsLoading } = useMyTrainingCourseRecords();
  const { data: orgRecords = [], isLoading: orgRecordsLoading } = useUserTrainingCourseRecords(
    isSelf ? 0 : props.organizationId,
    isSelf ? 0 : props.userId,
    !isSelf,
  );
  const { data: myDocuments = [], isLoading: myDocumentsLoading } = useUserDocuments();
  const { data: orgDocuments = [], isLoading: orgDocumentsLoading } = useOrganizationUserDocuments(
    isSelf ? null : props.organizationId,
    isSelf ? null : props.userId,
  );

  const records = isSelf ? myRecords : orgRecords;
  const documents = isSelf ? myDocuments : orgDocuments;
  const loading = isSelf
    ? myRecordsLoading || myDocumentsLoading
    : orgRecordsLoading || orgDocumentsLoading;

  const record = useMemo(
    () => records.find((entry) => entry.id === props.recordId),
    [props.recordId, records],
  );

  const linkedDocuments = useMemo(() => {
    const documentIds = new Set(record?.userDocumentIds ?? []);
    return documents.filter((doc) => documentIds.has(doc.id));
  }, [documents, record?.userDocumentIds]);

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!record) {
    return <NotFoundState message={t('trainingCourse.record.notFound')} />;
  }

  const getDocumentHref = (documentId: number) =>
    isSelf
      ? `${MY_AREA_BASE_PATH}/documents/${documentId}`
      : getOrganizationUserDocumentDetailPath(props.organizationId, props.userId, documentId);

  const handleDelete = async () => {
    if (isSelf || !record) return;
    try {
      await deleteRecord.mutateAsync(record.id);
      showSuccess(t('trainingCourse.record.deleteSuccess'));
      router.push(
        getOrganizationUserSectionPath(props.organizationId, props.userId, 'training-courses'),
      );
    } catch (e) {
      showError(e instanceof Error ? e.message : t('trainingCourse.record.deleteError'));
    }
  };

  const workflowHref = resolveWorkflowSourceHref(record, navigationContext, hasPermission);
  const onboardingHref = resolveOnboardingSourceHref(record, navigationContext, hasPermission);

  return (
    <div className="w-full space-y-4">
      <Card density="compact">
        <DetailCardHeader
          className="mb-0"
          title={record.name}
          badges={
            record.grantedTypes.some((type) => type.typeArchived) ? (
              <Badge tone="neutral" pill className="shrink-0">
                {t('trainingCourse.archivedTag')}
              </Badge>
            ) : undefined
          }
          actions={
            canWrite ? (
              <>
                <DetailCardAction
                  label={t('common.edit')}
                  icon={<Pencil className="h-4 w-4" aria-hidden />}
                  onClick={() => setIsEditOpen(true)}
                />
                <DetailCardAction
                  label={t('common.delete')}
                  icon={<Trash2 className="h-4 w-4" aria-hidden />}
                  onClick={() => setIsDeleteOpen(true)}
                  disabled={deleteRecord.isPending}
                />
              </>
            ) : undefined
          }
        />
        {(record.grantedTypes?.length ?? 0) > 1 ? (
          <div className="mt-3 border-t border-[var(--border-color)]/30 pt-3">
            <p className="mb-2 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
              {t('trainingCourse.grantedTypes.label')}
            </p>
            <div className="flex flex-wrap gap-1.5">
              {record.grantedTypes.map((type) => (
                <Badge key={type.tenantTrainingCourseTypeId} tone="neutral">
                  {type.courseTypeName ?? `#${type.tenantTrainingCourseTypeId}`}
                </Badge>
              ))}
            </div>
          </div>
        ) : null}
        <div className="mt-3 grid grid-cols-1 gap-3 border-t border-[var(--border-color)]/30 pt-3 sm:grid-cols-2">
          <FieldDisplay
            label={t('trainingCourse.record.startDateLabel')}
            value={formatDateLong(record.startDate)}
          />
          <FieldDisplay
            label={t('trainingCourse.record.endDateLabel')}
            value={formatDateLong(record.endDate)}
          />
          {record.location?.trim() ? (
            <FieldDisplay
              label={t('trainingCourse.record.locationLabel')}
              value={record.location}
            />
          ) : null}
          {record.hours != null ? (
            <FieldDisplay
              label={t('trainingCourse.record.hoursLabel')}
              value={t('trainingCourse.record.hours', { hours: record.hours })}
            />
          ) : null}
        </div>
        {record.notes?.trim() ? (
          <div className="mt-3 border-t border-[var(--border-color)]/30 pt-3">
            <FieldDisplay label={t('common.record.notesLabel')} value={record.notes} />
          </div>
        ) : null}
      </Card>

      {workflowHref ? <RecordSourceLinkSection kind="WORKFLOW" href={workflowHref} /> : null}
      {onboardingHref ? <RecordSourceLinkSection kind="ONBOARDING" href={onboardingHref} /> : null}

      <section>
        <h2 className="mb-1.5 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
          {t('common.record.documentsTitle')}
        </h2>
        {linkedDocuments.length === 0 ? (
          <EmptyState>{t('common.record.noDocuments')}</EmptyState>
        ) : (
          <ListContainer label={t('common.record.documentsTitle')} rowSeparator="inset">
            {linkedDocuments.map((doc) => (
              <ListRow
                key={doc.id}
                compact
                href={getDocumentHref(doc.id)}
                chevron
                leading={
                  <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-[var(--surface-muted)] text-[var(--text-secondary)]">
                    <FileText className="h-4 w-4" aria-hidden />
                  </div>
                }
                title={doc.title}
                subtitle={
                  <span className="flex min-w-0 flex-wrap items-center gap-x-1.5 gap-y-0.5">
                    <span className="truncate">{doc.fileName}</span>
                    <span className="shrink-0 text-[var(--text-secondary)]/50" aria-hidden>
                      ·
                    </span>
                    <span className="shrink-0 tabular-nums">{formatFileSize(doc.sizeBytes)}</span>
                  </span>
                }
              />
            ))}
          </ListContainer>
        )}
      </section>

      {canWrite ? (
        <>
          <TrainingCourseRecordEditModal
            isOpen={isEditOpen}
            onClose={() => setIsEditOpen(false)}
            organizationId={props.organizationId}
            userId={props.userId}
            record={record}
          />
          <ConfirmDialog
            isOpen={isDeleteOpen}
            onClose={() => setIsDeleteOpen(false)}
            onConfirm={() => void handleDelete()}
            title={t('trainingCourse.record.confirmDeleteTitle')}
            message={t('trainingCourse.record.confirmDeleteMessage', {
              name: record.name,
            })}
            isLoading={deleteRecord.isPending}
          />
        </>
      ) : null}
    </div>
  );
}
