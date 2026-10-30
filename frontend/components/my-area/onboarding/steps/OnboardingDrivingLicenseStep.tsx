/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { forwardRef, useCallback, useImperativeHandle, useMemo, useState } from 'react';
import { DrivingLicenseRecordFormFields } from '@/components/my-area/drivingLicenses/DrivingLicenseRecordFormFields';
import { Checkbox } from '@/components/ui/checkbox';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import type { DrivingLicenseDefinitionDto } from '@/lib/api/drivingLicenses';
import {
  hasOnboardingAttachments,
  readDrivingLicenseAttachmentFileNamesFromDetail,
  readOnboardingAttachmentFileNames,
  uploadOnboardingPendingFiles,
  withOnboardingAttachmentFileNames,
} from '@/lib/onboarding/attachmentUtils';
import { readDrivingLicense, readNoneDeclared } from '@/lib/onboarding/stepPayloads';
import {
  useDeleteOnboardingDrivingLicenseAttachment,
  useUploadOnboardingDrivingLicenseAttachment,
} from '@/lib/store/queries/onboardingInvitation';
import type {
  OnboardingDrivingLicenseDraft,
  OnboardingInvitationAccess,
} from '@/types/onboardingInvitation';
import type { OnboardingStepHandle, OnboardingStepProps } from './onboardingStepHandle';
import { payloadsEqual } from './onboardingStepHandle';

interface OnboardingDrivingLicenseStepProps extends OnboardingStepProps {
  access: OnboardingInvitationAccess;
  definitions: DrivingLicenseDefinitionDto[];
}

const emptyLicense = (): OnboardingDrivingLicenseDraft => ({
  issueDate: '',
  documentExpirationDate: null,
  licenseNumber: '',
  issuingAuthority: '',
  classes: [],
  notes: '',
  fileName: null,
  fileNames: [],
  uploaded: false,
});

const buildDrivingLicensePayload = (declared: boolean, value: OnboardingDrivingLicenseDraft) => ({
  license: declared ? null : value,
  noneDeclared: declared,
});

export const OnboardingDrivingLicenseStep = forwardRef<
  OnboardingStepHandle,
  OnboardingDrivingLicenseStepProps
>(function OnboardingDrivingLicenseStep({ access, payload, definitions, editable, isSaving }, ref) {
  const t = useTranslation();
  const { showError } = useToast();
  const uploadAttachment = useUploadOnboardingDrivingLicenseAttachment(access);
  const deleteAttachment = useDeleteOnboardingDrivingLicenseAttachment(access);
  const initial = readDrivingLicense(payload);
  const [noneDeclared, setNoneDeclared] = useState(
    () => readNoneDeclared(payload) && initial == null,
  );
  const [license, setLicense] = useState<OnboardingDrivingLicenseDraft>(initial ?? emptyLicense());
  const [files, setFiles] = useState<File[]>([]);
  const [uploading, setUploading] = useState(false);
  const [removingIndex, setRemovingIndex] = useState<number | null>(null);

  const payloadLicense = readDrivingLicense(payload);
  const storedFileNames = readOnboardingAttachmentFileNames(payloadLicense ?? license);

  const savedSnapshot = useState(() => {
    const savedLicense = readDrivingLicense(payload);
    const savedNoneDeclared = readNoneDeclared(payload) && savedLicense == null;
    return buildDrivingLicensePayload(savedNoneDeclared, savedLicense ?? emptyLicense());
  })[0];

  const uploadPendingFiles = useCallback(async () => {
    if (files.length === 0) return true;
    setUploading(true);
    try {
      await uploadOnboardingPendingFiles(files, async (file) => {
        await uploadAttachment.mutateAsync(file);
      });
      setFiles([]);
      return true;
    } catch (error) {
      showError(error instanceof Error ? error.message : t('myArea.error.loadFailed'));
      return false;
    } finally {
      setUploading(false);
    }
  }, [files, showError, t, uploadAttachment]);

  useImperativeHandle(
    ref,
    () => ({
      getPayload: () => buildDrivingLicensePayload(noneDeclared, license),
      hasChanges: () =>
        !payloadsEqual(savedSnapshot, buildDrivingLicensePayload(noneDeclared, license)) ||
        files.length > 0,
      prepareSave: uploadPendingFiles,
    }),
    [files.length, license, noneDeclared, savedSnapshot, uploadPendingFiles],
  );

  const hasAttachment = hasOnboardingAttachments(storedFileNames, files);

  const handleRemoveStored = useCallback(
    async (index: number) => {
      if (!editable || removingIndex != null) return;
      setRemovingIndex(index);
      try {
        const detail = await deleteAttachment.mutateAsync(index);
        setLicense((current) =>
          withOnboardingAttachmentFileNames(
            current,
            readDrivingLicenseAttachmentFileNamesFromDetail(detail),
          ),
        );
      } catch (error) {
        showError(error instanceof Error ? error.message : t('myArea.error.loadFailed'));
      } finally {
        setRemovingIndex(null);
      }
    },
    [deleteAttachment, editable, removingIndex, showError, t],
  );

  const isValid = useMemo(
    () =>
      noneDeclared ||
      (license.issueDate.trim() !== '' &&
        license.licenseNumber.trim() !== '' &&
        license.issuingAuthority.trim() !== '' &&
        license.classes.length > 0 &&
        definitions.length > 0 &&
        hasAttachment),
    [definitions.length, hasAttachment, license, noneDeclared],
  );

  return (
    <div className="space-y-4">
      {editable ? (
        <p className="text-sm text-[var(--text-secondary)]">
          {t('myArea.onboarding.wizard.drivingLicense.hint')}
        </p>
      ) : null}
      <Checkbox
        label={t('myArea.onboarding.wizard.drivingLicense.noLicense')}
        checked={noneDeclared}
        onCheckedChange={(checked) => {
          if (!editable || isSaving) return;
          setNoneDeclared(checked);
        }}
        disabled={!editable || isSaving}
      />
      {noneDeclared ? null : (
        <DrivingLicenseRecordFormFields
          classDefinitions={definitions}
          issueDate={license.issueDate}
          onIssueDateChange={(value) => setLicense((current) => ({ ...current, issueDate: value }))}
          documentExpirationDate={license.documentExpirationDate ?? ''}
          onDocumentExpirationDateChange={(value) =>
            setLicense((current) => ({
              ...current,
              documentExpirationDate: value.trim() || null,
            }))
          }
          licenseNumber={license.licenseNumber}
          onLicenseNumberChange={(value) =>
            setLicense((current) => ({ ...current, licenseNumber: value }))
          }
          issuingAuthority={license.issuingAuthority}
          onIssuingAuthorityChange={(value) =>
            setLicense((current) => ({ ...current, issuingAuthority: value }))
          }
          classes={license.classes}
          onClassesChange={(classes) => setLicense((current) => ({ ...current, classes }))}
          notes={license.notes ?? ''}
          onNotesChange={(value) => setLicense((current) => ({ ...current, notes: value }))}
          files={files}
          onFilesChange={setFiles}
          storedFileNames={storedFileNames}
          onRemoveStored={editable ? handleRemoveStored : undefined}
          attachmentsReadOnly={!editable}
          disabled={!editable || isSaving || uploading || removingIndex != null}
          showAttachments
          attachmentsInputId="onboarding-driving-license-attachments"
        />
      )}
      {editable && !noneDeclared && !isValid ? (
        <p className="text-xs text-[var(--text-secondary)]">
          {t('drivingLicense.submit.attachmentsHint')}
        </p>
      ) : null}
    </div>
  );
});
