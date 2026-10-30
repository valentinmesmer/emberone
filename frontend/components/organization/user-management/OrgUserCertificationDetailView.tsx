/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Plus } from 'lucide-react';
import Link from 'next/link';
import { useMemo, useState } from 'react';
import { PendingAttachments } from '@/components/attachment';
import { CertificationDetailStatusCard } from '@/components/certifications/CertificationDetailStatusCard';
import { Button } from '@/components/ui/button';
import { DatePicker } from '@/components/ui/date-picker';
import { FieldDisplay } from '@/components/ui/field-display';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyState, NotFoundState } from '@/components/ui/message-cards';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { SectionActions } from '@/components/ui/section-header';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import type { CertificationStatus } from '@/lib/api/certifications';
import {
  allowsManualRecordCreation,
  requiresSubmissionAttachment,
} from '@/lib/certifications/evidenceTypes';
import {
  computeRecordCertificationStatus,
  isActiveCertificationRecord,
} from '@/lib/certifications/recordValidity';
import { CERTIFICATION_STATUS_ROW_TONE } from '@/lib/certifications/statusPresentation';
import { findOpenCertificationWorkflow } from '@/lib/certifications/submittableCertifications';
import {
  getOrganizationUserCertificationAssignmentDetailPath,
  getOrganizationUserCertificationRecordDetailPath,
  getOrganizationUserWorkflowDetailPath,
} from '@/lib/navigation/userManagementSections';
import { usePermissions } from '@/lib/permission/PermissionContext';
import {
  useCreateCertificationRecord,
  useOrganizationUserWorkflows,
  useUploadOrganizationUserDocument,
  useUserCertificationAssignments,
  useUserCertificationRecords,
  useUserCertificationStatus,
} from '@/lib/store';
import { formatDateLong } from '@/lib/utils';
import { PermissionTypeEnum } from '@/types/permission';

export interface OrgUserCertificationDetailViewProps {
  organizationId: number;
  userId: number;
  definitionId: number;
  canWrite: boolean;
}

export function OrgUserCertificationDetailView({
  organizationId,
  userId,
  definitionId,
  canWrite,
}: OrgUserCertificationDetailViewProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const { hasPermission } = usePermissions();
  const createRecord = useCreateCertificationRecord(organizationId, userId);
  const uploadDocument = useUploadOrganizationUserDocument(organizationId, userId);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [completedDate, setCompletedDate] = useState('');
  const [notes, setNotes] = useState('');
  const [files, setFiles] = useState<File[]>([]);

  const { data: status, isLoading: statusLoading } = useUserCertificationStatus(
    organizationId,
    userId,
  );
  const { data: allRecords = [], isLoading: recordsLoading } = useUserCertificationRecords(
    organizationId,
    userId,
  );
  const { data: assignments = [] } = useUserCertificationAssignments(organizationId, userId);
  const canReadWorkflows = hasPermission(organizationId, PermissionTypeEnum.WORKFLOW_READ);
  const { data: workflows = [] } = useOrganizationUserWorkflows(
    canReadWorkflows ? organizationId : null,
    canReadWorkflows ? userId : null,
    { fetchAll: true },
  );

  const item = status?.items.find((i) => i.tenantCertDefinitionId === definitionId);

  const individualAssignment = useMemo(
    () => assignments.find((a) => a.tenantCertDefinitionId === definitionId) ?? null,
    [assignments, definitionId],
  );

  const records = useMemo(
    () =>
      allRecords
        .filter((r) => r.tenantCertDefinitionId === definitionId)
        .sort((a, b) => b.completedDate.localeCompare(a.completedDate)),
    [allRecords, definitionId],
  );
  const openWorkflow = useMemo(
    () => findOpenCertificationWorkflow(workflows, definitionId),
    [definitionId, workflows],
  );
  const showPendingSubmission = item?.status === 'IN_REVIEW';
  const canOpenWorkflow = canReadWorkflows && openWorkflow != null;
  const hasHistoryContent = records.length > 0 || showPendingSubmission;

  const statusLabel = (s: CertificationStatus) => t(`certifications.status.${s}`);
  const loading = statusLoading || recordsLoading;
  const canCreateRecord =
    canWrite &&
    item?.status !== 'ARCHIVED' &&
    item?.assigned !== false &&
    allowsManualRecordCreation(item?.evidenceTypes);
  const attachmentRequired = requiresSubmissionAttachment(item?.evidenceTypes);

  const resetCreateForm = () => {
    setCompletedDate(new Date().toLocaleDateString('en-CA'));
    setNotes('');
    setFiles([]);
  };

  const openCreateModal = () => {
    resetCreateForm();
    setIsCreateOpen(true);
  };

  const handleCreateRecord = async () => {
    if (!item || !completedDate || (attachmentRequired && files.length === 0)) return;
    try {
      const uploadedDocuments =
        files.length > 0
          ? await Promise.all(
              files.map((file) =>
                uploadDocument.mutateAsync({
                  file,
                  title: file.name.replace(/\.[^.]+$/, ''),
                }),
              ),
            )
          : [];
      await createRecord.mutateAsync({
        tenantCertDefinitionId: item.tenantCertDefinitionId,
        completedDate,
        userDocumentIds: uploadedDocuments.map((doc) => doc.id),
        notes: notes.trim() || null,
      });
      showSuccess(t('certifications.record.createSuccess'));
      setIsCreateOpen(false);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('certifications.record.createError'));
    }
  };

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!item) {
    return <NotFoundState message={t('certifications.detail.notFound')} />;
  }
  const historical = item.assigned === false || item.archived || item.status === 'ARCHIVED';

  const assignmentHref = individualAssignment
    ? getOrganizationUserCertificationAssignmentDetailPath(
        organizationId,
        userId,
        individualAssignment.id,
      )
    : null;

  const assignmentLabel = individualAssignment ? t('certifications.assign.definitionBadge') : null;

  return (
    <div className="w-full space-y-4">
      <CertificationDetailStatusCard
        title={item.certDefinitionName}
        status={item.status}
        statusLabel={historical ? undefined : statusLabel(item.status)}
        meta={
          assignmentLabel ? (
            <p className="mt-1 text-xs text-[var(--text-secondary)]">
              {assignmentHref ? (
                <Link
                  href={assignmentHref}
                  className="transition-colors hover:text-[var(--fire-red)]"
                >
                  {assignmentLabel}
                </Link>
              ) : (
                assignmentLabel
              )}
            </p>
          ) : null
        }
        hints={
          <>
            {item.status === 'ARCHIVED' ? (
              <p className="mt-1.5 text-xs text-[var(--text-secondary)]">
                {t('certifications.detail.archivedHint')}
              </p>
            ) : null}
            {item.assigned === false && item.status !== 'ARCHIVED' ? (
              <p className="mt-1.5 text-xs text-[var(--text-secondary)]">
                {t('certifications.detail.unassignedHint')}
              </p>
            ) : null}
          </>
        }
        footer={
          item.completedDate || item.expiryDate ? (
            <div className="mt-3 grid grid-cols-1 gap-3 border-t border-[var(--border-color)]/30 pt-3 sm:grid-cols-2">
              {item.completedDate ? (
                <FieldDisplay
                  label={t('certifications.detail.completedDate')}
                  value={formatDateLong(item.completedDate)}
                />
              ) : null}
              {item.expiryDate ? (
                <FieldDisplay
                  label={t('certifications.detail.validUntil')}
                  value={formatDateLong(item.expiryDate)}
                />
              ) : null}
            </div>
          ) : null
        }
      />

      <section className="space-y-1.5">
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center sm:gap-x-3">
          <h2 className="text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
            {t('certifications.historyTitle')}
          </h2>
          {canCreateRecord ? (
            <SectionActions layout="inline" className="sm:justify-end">
              <Button
                type="button"
                size="xs"
                variant="outline"
                fullWidth="responsive"
                onClick={openCreateModal}
              >
                <Plus className="h-3.5 w-3.5" aria-hidden />
                {t('certifications.record.createAction')}
              </Button>
            </SectionActions>
          ) : null}
        </div>
        {!hasHistoryContent ? (
          <EmptyState>{t('certifications.noRecords')}</EmptyState>
        ) : (
          <ListContainer label={t('certifications.historyTitle')} rowSeparator="inset">
            {showPendingSubmission ? (
              <ListRow
                compact
                tone="warning"
                href={
                  canOpenWorkflow
                    ? getOrganizationUserWorkflowDetailPath(organizationId, userId, openWorkflow.id)
                    : undefined
                }
                chevron={canOpenWorkflow}
                title={t('certifications.detail.pendingSubmission')}
                subtitle={t('certifications.detail.pendingSubmissionHintOrg')}
              />
            ) : null}
            {records.map((rec) => {
              const isCurrent = isActiveCertificationRecord(rec);
              const recordStatus = computeRecordCertificationStatus(rec);

              return (
                <ListRow
                  key={rec.id}
                  compact
                  tone={isCurrent ? CERTIFICATION_STATUS_ROW_TONE[recordStatus] : 'default'}
                  href={getOrganizationUserCertificationRecordDetailPath(
                    organizationId,
                    userId,
                    definitionId,
                    rec.id,
                  )}
                  chevron
                  title={t('certifications.detail.recordCompleted', {
                    date: formatDateLong(rec.completedDate),
                  })}
                  subtitle={
                    rec.expiryDate
                      ? t('certifications.detail.recordValidUntil', {
                          date: formatDateLong(rec.expiryDate),
                        })
                      : undefined
                  }
                />
              );
            })}
          </ListContainer>
        )}
      </section>

      <Modal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        title={t('certifications.record.createTitle')}
        size="md"
        footer={
          <ModalFooter
            onCancel={() => setIsCreateOpen(false)}
            onSave={() => void handleCreateRecord()}
            isValid={
              !!completedDate &&
              (!attachmentRequired || files.length > 0) &&
              !createRecord.isPending &&
              !uploadDocument.isPending
            }
            isLoading={createRecord.isPending || uploadDocument.isPending}
            saveText={t('certifications.record.createAction')}
            showSave
          />
        }
      >
        <div className="space-y-4">
          <FieldDisplay
            label={t('certifications.submit.definitionLabel')}
            value={item.certDefinitionName}
          />
          <DatePicker
            label={t('certifications.record.completedDateLabel')}
            name="completedDate"
            value={completedDate}
            onChange={(e) => setCompletedDate(e.target.value)}
            disabled={createRecord.isPending || uploadDocument.isPending}
            required
          />
          <Textarea
            label={t('common.record.notesLabel')}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            disabled={createRecord.isPending || uploadDocument.isPending}
          />
          <PendingAttachments
            variant="field"
            inputId="manual-certification-attachments"
            files={files}
            onChange={setFiles}
            label={t('certifications.submit.attachmentsLabel')}
            hint={
              attachmentRequired
                ? t('certifications.submit.attachmentsHint')
                : t('certifications.submit.attachmentsOptionalHint')
            }
            removeFileLabel={t('common.attachments.removeFile')}
            required={attachmentRequired}
            optionalLabel={
              attachmentRequired ? undefined : t('certifications.submit.attachmentsOptional')
            }
          />
        </div>
      </Modal>
    </div>
  );
}
