/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Pencil, Trash2 } from 'lucide-react';
import { useMemo, useState } from 'react';
import { PendingAttachments } from '@/components/attachment';
import { DrivingLicenseClassesEditor } from '@/components/my-area/drivingLicenses/DrivingLicenseClassesEditor';
import {
  DrivingLicensePendingWorkflowCard,
  mapPendingWorkflowClasses,
} from '@/components/my-area/drivingLicenses/DrivingLicensePendingWorkflowCard';
import { DrivingLicenseRecordEditModal } from '@/components/my-area/drivingLicenses/DrivingLicenseRecordEditModal';
import { DrivingLicenseView } from '@/components/my-area/drivingLicenses/DrivingLicenseView';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { DatePicker } from '@/components/ui/date-picker';
import { IconButton } from '@/components/ui/icon-button';
import { Input } from '@/components/ui/input';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import type {
  DrivingLicenseClassInput,
  DrivingLicenseDefinitionDto,
} from '@/lib/api/drivingLicenses';
import {
  getOrganizationUserDocumentDetailPath,
  getOrganizationUserWorkflowDetailPath,
} from '@/lib/navigation/userManagementSections';
import { usePermissions } from '@/lib/permission/PermissionContext';
import {
  useCreateDrivingLicense,
  useDeleteDrivingLicense,
  useOrganizationUserDocuments,
  useOrgDrivingLicenseCatalog,
  useUpdateDrivingLicense,
  useUploadOrganizationUserDocument,
  useUserDrivingLicense,
  useUserDrivingLicensePendingWorkflow,
} from '@/lib/store';
import { getTodayIsoDate } from '@/lib/utils/date';
import { PermissionTypeEnum } from '@/types/permission';

interface Props {
  organizationId: number;
  userId: number;
  canWrite: boolean;
  isAddOpen: boolean;
  onAddClose: () => void;
}

interface UserDrivingLicenseAddModalProps {
  organizationId: number;
  userId: number;
  onClose: () => void;
  classOptions: DrivingLicenseDefinitionDto[];
  renewal?: boolean;
}

function UserDrivingLicenseAddModal({
  organizationId,
  userId,
  onClose,
  classOptions,
  renewal = false,
}: UserDrivingLicenseAddModalProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const createLicense = useCreateDrivingLicense(organizationId, userId);
  const updateLicense = useUpdateDrivingLicense(organizationId, userId);
  const uploadDocument = useUploadOrganizationUserDocument(organizationId, userId);

  const [issueDate, setIssueDate] = useState(getTodayIsoDate);
  const [documentExpirationDate, setDocumentExpirationDate] = useState('');
  const [licenseNumber, setLicenseNumber] = useState('');
  const [issuingAuthority, setIssuingAuthority] = useState('');
  const [notes, setNotes] = useState('');
  const [classes, setClasses] = useState<DrivingLicenseClassInput[]>([]);
  const [files, setFiles] = useState<File[]>([]);

  const canSubmit =
    issueDate.trim().length > 0 &&
    licenseNumber.trim().length > 0 &&
    issuingAuthority.trim().length > 0 &&
    classes.length > 0 &&
    classOptions.length > 0;

  const handleCreate = async () => {
    if (!canSubmit) return;
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
      const payload = {
        issueDate,
        documentExpirationDate: documentExpirationDate.trim() || null,
        licenseNumber: licenseNumber.trim(),
        issuingAuthority: issuingAuthority.trim(),
        classes: classes.map((entry) => ({
          tenantDrivingLicenseDefinitionId: entry.tenantDrivingLicenseDefinitionId,
          grantDate: entry.grantDate?.trim() || null,
          expirationDate: entry.expirationDate?.trim() || null,
        })),
        userDocumentIds,
        notes: notes.trim() || null,
      };
      if (renewal) {
        await updateLicense.mutateAsync(payload);
      } else {
        await createLicense.mutateAsync(payload);
      }
      showSuccess(
        renewal
          ? t('drivingLicense.record.renewSuccess')
          : t('drivingLicense.record.createSuccess'),
      );
      onClose();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('drivingLicense.record.createError'));
    }
  };

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={
        renewal ? t('drivingLicense.record.renewTitle') : t('drivingLicense.record.createTitle')
      }
      size="lg"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleCreate()}
          isValid={canSubmit}
          isLoading={createLicense.isPending || updateLicense.isPending || uploadDocument.isPending}
          saveText={
            renewal
              ? t('drivingLicense.record.renewAction')
              : t('drivingLicense.record.createAction')
          }
          showSave
        />
      }
    >
      <div className="space-y-4">
        {renewal ? (
          <p className="text-sm text-[var(--text-secondary)]">
            {t('drivingLicense.record.renewHint')}
          </p>
        ) : null}
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <DatePicker
            label={t('drivingLicense.record.issueDateLabel')}
            value={issueDate}
            onChange={(event) => setIssueDate(event.target.value)}
            disabled={createLicense.isPending || updateLicense.isPending}
            required
          />
          <DatePicker
            label={t('drivingLicense.record.documentExpirationDateLabel')}
            value={documentExpirationDate}
            onChange={(event) => setDocumentExpirationDate(event.target.value)}
            disabled={createLicense.isPending || updateLicense.isPending}
          />
        </div>
        <Input
          label={t('drivingLicense.record.licenseNumberLabel')}
          value={licenseNumber}
          onChange={(event) => setLicenseNumber(event.target.value)}
          disabled={createLicense.isPending || updateLicense.isPending}
          required
        />
        <Input
          label={t('drivingLicense.record.issuingAuthorityLabel')}
          value={issuingAuthority}
          onChange={(event) => setIssuingAuthority(event.target.value)}
          disabled={createLicense.isPending || updateLicense.isPending}
          required
        />
        <DrivingLicenseClassesEditor
          definitions={classOptions}
          value={classes}
          onChange={setClasses}
          disabled={createLicense.isPending || uploadDocument.isPending}
        />
        <Textarea
          label={t('common.record.notesLabel')}
          value={notes}
          onChange={(event) => setNotes(event.target.value)}
          disabled={createLicense.isPending || updateLicense.isPending}
        />
        <PendingAttachments
          variant="field"
          inputId="driving-license-attachments"
          files={files}
          onChange={setFiles}
          label={t('drivingLicense.record.attachmentsLabel')}
          hint={t('drivingLicense.record.attachmentsHint')}
          removeFileLabel={t('common.attachments.removeFile')}
        />
      </div>
    </Modal>
  );
}

export function UserDrivingLicensesSection({
  organizationId,
  userId,
  canWrite,
  isAddOpen,
  onAddClose,
}: Props) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const { hasPermission } = usePermissions();

  const { data: license, isLoading: licenseLoading } = useUserDrivingLicense(
    organizationId,
    userId,
  );
  const { data: pendingWorkflow, isLoading: pendingLoading } = useUserDrivingLicensePendingWorkflow(
    organizationId,
    userId,
  );
  const { data: catalog } = useOrgDrivingLicenseCatalog(organizationId, userId);
  const { data: documents = [], isLoading: documentsLoading } = useOrganizationUserDocuments(
    organizationId,
    userId,
  );
  const canReadWorkflows = hasPermission(organizationId, PermissionTypeEnum.WORKFLOW_READ);
  const deleteLicense = useDeleteDrivingLicense(organizationId, userId);

  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);

  const classOptions = useMemo(
    () => catalog?.definitions.filter((definition) => definition.active) ?? [],
    [catalog?.definitions],
  );

  const resolveClassLabel = (definitionId: number) =>
    classOptions.find((definition) => definition.id === definitionId)?.name ?? `#${definitionId}`;

  const linkedDocuments = useMemo(() => {
    const documentIds = new Set(license?.userDocumentIds ?? []);
    return documents.filter((doc) => documentIds.has(doc.id));
  }, [documents, license?.userDocumentIds]);

  const handleDelete = async () => {
    try {
      await deleteLicense.mutateAsync();
      showSuccess(t('drivingLicense.record.deleteSuccess'));
      setDeleteOpen(false);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('drivingLicense.record.deleteError'));
    }
  };

  if (licenseLoading || pendingLoading || documentsLoading) {
    return <LoadingState />;
  }

  const hasContent = license != null || pendingWorkflow != null;

  return (
    <div className="space-y-4">
      {!hasContent ? (
        <EmptyStateCard>{t('drivingLicense.empty')}</EmptyStateCard>
      ) : (
        <>
          {pendingWorkflow ? (
            <DrivingLicensePendingWorkflowCard
              issueDate={pendingWorkflow.issueDate}
              documentExpirationDate={pendingWorkflow.documentExpirationDate}
              licenseNumber={pendingWorkflow.licenseNumber}
              issuingAuthority={pendingWorkflow.issuingAuthority}
              classes={mapPendingWorkflowClasses(pendingWorkflow.classes, resolveClassLabel)}
              href={
                canReadWorkflows
                  ? getOrganizationUserWorkflowDetailPath(
                      organizationId,
                      userId,
                      pendingWorkflow.workflowId,
                    )
                  : undefined
              }
            />
          ) : null}
          {license ? (
            <DrivingLicenseView
              license={license}
              linkedDocuments={linkedDocuments}
              navigationContext={{ scope: 'org', organizationId, userId }}
              getDocumentHref={(documentId) =>
                getOrganizationUserDocumentDetailPath(organizationId, userId, documentId)
              }
              headerActions={
                canWrite ? (
                  <>
                    <IconButton
                      variant="action"
                      icon={<Pencil className="h-4 w-4" aria-hidden />}
                      aria-label={t('drivingLicense.record.correctAction')}
                      title={t('drivingLicense.record.correctAction')}
                      onClick={() => setEditOpen(true)}
                      disabled={deleteLicense.isPending}
                    />
                    <IconButton
                      variant="action"
                      icon={<Trash2 className="h-4 w-4" aria-hidden />}
                      aria-label={t('drivingLicense.record.deleteAction')}
                      title={t('drivingLicense.record.deleteAction')}
                      onClick={() => setDeleteOpen(true)}
                      disabled={deleteLicense.isPending}
                    />
                  </>
                ) : undefined
              }
            />
          ) : null}
        </>
      )}

      {isAddOpen && pendingWorkflow == null ? (
        <UserDrivingLicenseAddModal
          key={`${license != null ? 'renew' : 'create'}-${classOptions.map((definition) => definition.id).join(',') || 'loading'}`}
          organizationId={organizationId}
          userId={userId}
          onClose={onAddClose}
          classOptions={classOptions}
          renewal={license != null}
        />
      ) : null}

      {canWrite && license ? (
        <DrivingLicenseRecordEditModal
          isOpen={editOpen}
          onClose={() => setEditOpen(false)}
          organizationId={organizationId}
          userId={userId}
          license={license}
        />
      ) : null}

      {canWrite ? (
        <ConfirmDialog
          isOpen={deleteOpen}
          onClose={() => setDeleteOpen(false)}
          onConfirm={() => void handleDelete()}
          title={t('drivingLicense.record.confirmDeleteTitle')}
          message={t('drivingLicense.record.confirmDeleteMessage')}
          isLoading={deleteLicense.isPending}
        />
      ) : null}
    </div>
  );
}
