/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Download, FileText, Pencil, Plus, Trash2 } from 'lucide-react';
import { forwardRef, useCallback, useImperativeHandle, useMemo, useState } from 'react';
import { CertificationRecordFormFields } from '@/components/my-area/certifications/CertificationRecordFormFields';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { IconButton } from '@/components/ui/icon-button';
import { ListContainer, ListRow, ListRowMeta, ListRowMetaItem } from '@/components/ui/list';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useTranslation } from '@/hooks/useTranslation';
import type { CertificationDefinitionDto } from '@/lib/api/certifications';
import { onboardingInvitationApi } from '@/lib/api/onboardingInvitation';
import { requiresSubmissionAttachment } from '@/lib/certifications/evidenceTypes';
import {
  hasOnboardingAttachments,
  readOnboardingAttachmentFileNames,
  withOnboardingAttachmentFileNames,
} from '@/lib/onboarding/attachmentUtils';
import { readCertifications, readNoneDeclared } from '@/lib/onboarding/stepPayloads';
import { formatDateLong } from '@/lib/utils';
import { getTodayIsoDate } from '@/lib/utils/date';
import type {
  OnboardingCertificationDraft,
  OnboardingInvitationAccess,
} from '@/types/onboardingInvitation';
import { useDownloadOnboardingAttachment } from '../useDownloadOnboardingAttachment';
import { useOnboardingRecordAttachments } from '../useOnboardingRecordAttachments';
import type { OnboardingStepHandle, OnboardingStepProps } from './onboardingStepHandle';
import { payloadsEqual } from './onboardingStepHandle';

interface OnboardingCertificationsStepProps extends OnboardingStepProps {
  access: OnboardingInvitationAccess;
  definitions: CertificationDefinitionDto[];
}

type RecordRow = OnboardingCertificationDraft & { key: string };

const toCertificationsPayload = (rows: RecordRow[], declared: boolean) => ({
  records: rows.map((record) => ({
    recordKey: record.recordKey,
    tenantCertDefinitionId: record.tenantCertDefinitionId,
    completedDate: record.completedDate,
    notes: record.notes,
  })),
  noneDeclared: rows.length === 0 && declared,
});

const emptyDraft = (): OnboardingCertificationDraft => ({
  recordKey: crypto.randomUUID(),
  tenantCertDefinitionId: 0,
  completedDate: getTodayIsoDate(),
  notes: '',
  fileName: null,
  fileNames: [],
  uploaded: false,
});

export const OnboardingCertificationsStep = forwardRef<
  OnboardingStepHandle,
  OnboardingCertificationsStepProps
>(function OnboardingCertificationsStep({ access, payload, definitions, editable, isSaving }, ref) {
  const t = useTranslation();
  const downloadAttachment = useDownloadOnboardingAttachment();
  const [records, setRecords] = useState<RecordRow[]>(() =>
    readCertifications(payload).map((record) => ({ ...record, key: crypto.randomUUID() })),
  );
  const [noneDeclared, setNoneDeclared] = useState(
    () => readNoneDeclared(payload) && readCertifications(payload).length === 0,
  );
  const [formOpen, setFormOpen] = useState(false);
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const [draft, setDraft] = useState<OnboardingCertificationDraft>(emptyDraft);

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

  const {
    files,
    setFiles,
    uploading,
    removingIndex,
    handleRemoveStored,
    uploadPendingFiles,
    storedFileNames,
  } = useOnboardingRecordAttachments({
    stepType: 'CERTIFICATIONS',
    access,
    recordKey: draft.recordKey,
    draft,
    editable,
    onFileNamesChange: applyRecordFileNames,
  });

  const toPayload = toCertificationsPayload;

  const savedSnapshot = useState(() =>
    toCertificationsPayload(
      readCertifications(payload).map((record) => ({ ...record, key: '' })),
      readNoneDeclared(payload) && readCertifications(payload).length === 0,
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

  const selectedDefinition = useMemo(
    () => definitions.find((definition) => definition.id === draft.tenantCertDefinitionId),
    [definitions, draft.tenantCertDefinitionId],
  );

  const attachmentRequired = requiresSubmissionAttachment(selectedDefinition?.evidenceTypes);
  const hasAttachment = hasOnboardingAttachments(storedFileNames, files);

  const isValid = useMemo(
    () =>
      draft.tenantCertDefinitionId > 0 &&
      draft.completedDate.trim() !== '' &&
      (!attachmentRequired || hasAttachment),
    [attachmentRequired, draft, hasAttachment],
  );

  const definitionName = (id: number) =>
    definitions.find((definition) => definition.id === id)?.name ?? `#${id}`;

  return (
    <div className="space-y-3">
      {editable ? (
        <p className="text-sm text-[var(--text-secondary)]">
          {t('myArea.onboarding.wizard.certifications.hint')}
        </p>
      ) : null}
      {records.length === 0 ? (
        <Checkbox
          label={t('myArea.onboarding.wizard.certifications.noneDeclared')}
          checked={noneDeclared}
          onCheckedChange={(checked) => {
            if (!editable || isSaving) return;
            setNoneDeclared(checked);
          }}
          disabled={!editable || isSaving}
        />
      ) : null}
      {noneDeclared ? null : records.length === 0 ? (
        <EmptyStateCard>{t('myArea.onboarding.wizard.certifications.empty')}</EmptyStateCard>
      ) : (
        <ListContainer label={t('certifications.title')}>
          {records.map((record, index) => (
            <ListRow
              key={record.key}
              title={definitionName(record.tenantCertDefinitionId)}
              subtitle={formatDateLong(record.completedDate)}
              meta={
                editable ? undefined : (
                  <ListRowMeta>
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
                      onClick={() => {
                        setEditingIndex(index);
                        setDraft(record);
                        setFiles([]);
                        setFormOpen(true);
                      }}
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
                          'CERTIFICATIONS',
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
          onClick={() => {
            setEditingIndex(null);
            setDraft(emptyDraft());
            setFiles([]);
            setFormOpen(true);
          }}
        >
          {t('certifications.record.createAction')}
        </Button>
      ) : null}
      {formOpen ? (
        <Modal
          isOpen
          onClose={() => setFormOpen(false)}
          title={t('certifications.record.createTitle')}
          footer={
            <ModalFooter
              onCancel={() => setFormOpen(false)}
              onSave={async () => {
                if (!isValid || uploading) return;
                const savedDraft = await uploadPendingFiles();
                if (savedDraft == null) return;
                const next =
                  editingIndex == null
                    ? [...records, { ...savedDraft, key: crypto.randomUUID() }]
                    : records.map((item, index) =>
                        index === editingIndex ? { ...savedDraft, key: item.key } : item,
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
          <CertificationRecordFormFields
            definitions={definitions}
            selectedDefinitionIds={
              draft.tenantCertDefinitionId ? [draft.tenantCertDefinitionId] : []
            }
            onSelectedDefinitionIdsChange={(ids) =>
              setDraft((current) => ({
                ...current,
                tenantCertDefinitionId: ids[0] ?? 0,
              }))
            }
            completedDate={draft.completedDate}
            onCompletedDateChange={(value) =>
              setDraft((current) => ({ ...current, completedDate: value }))
            }
            notes={draft.notes ?? ''}
            onNotesChange={(value) => setDraft((current) => ({ ...current, notes: value }))}
            files={files}
            onFilesChange={setFiles}
            attachmentRequired={attachmentRequired}
            storedFileNames={storedFileNames}
            onRemoveStored={editable ? handleRemoveStored : undefined}
            disabled={!editable || isSaving || uploading || removingIndex != null}
            attachmentsInputId="onboarding-certification-attachments"
          />
        </Modal>
      ) : null}
    </div>
  );
});
