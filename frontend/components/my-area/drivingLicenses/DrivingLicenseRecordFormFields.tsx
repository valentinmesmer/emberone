/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { PendingAttachments } from '@/components/attachment';
import { DrivingLicenseClassesEditor } from '@/components/my-area/drivingLicenses/DrivingLicenseClassesEditor';
import { DatePicker } from '@/components/ui/date-picker';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { useTranslation } from '@/hooks/useTranslation';
import type {
  DrivingLicenseClassInput,
  DrivingLicenseDefinitionDto,
} from '@/lib/api/drivingLicenses';

export interface DrivingLicenseRecordFormFieldsProps {
  classDefinitions: DrivingLicenseDefinitionDto[];
  issueDate: string;
  onIssueDateChange: (value: string) => void;
  documentExpirationDate: string;
  onDocumentExpirationDateChange: (value: string) => void;
  licenseNumber: string;
  onLicenseNumberChange: (value: string) => void;
  issuingAuthority: string;
  onIssuingAuthorityChange: (value: string) => void;
  classes: DrivingLicenseClassInput[];
  onClassesChange: (classes: DrivingLicenseClassInput[]) => void;
  notes: string;
  onNotesChange: (value: string) => void;
  files: File[];
  onFilesChange: (files: File[]) => void;
  storedFileNames?: string[];
  onRemoveStored?: (index: number) => void;
  attachmentsReadOnly?: boolean;
  disabled?: boolean;
  showAttachments?: boolean;
  attachmentsInputId?: string;
}

export function DrivingLicenseRecordFormFields({
  classDefinitions,
  issueDate,
  onIssueDateChange,
  documentExpirationDate,
  onDocumentExpirationDateChange,
  licenseNumber,
  onLicenseNumberChange,
  issuingAuthority,
  onIssuingAuthorityChange,
  classes,
  onClassesChange,
  notes,
  onNotesChange,
  files,
  onFilesChange,
  storedFileNames = [],
  onRemoveStored,
  attachmentsReadOnly = false,
  disabled = false,
  showAttachments = true,
  attachmentsInputId = 'driving-license-record-attachments',
}: DrivingLicenseRecordFormFieldsProps) {
  const t = useTranslation();

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <DatePicker
          label={t('drivingLicense.record.issueDateLabel')}
          value={issueDate}
          onChange={(event) => onIssueDateChange(event.target.value)}
          disabled={disabled}
          required
        />
        <DatePicker
          label={t('drivingLicense.record.documentExpirationDateLabel')}
          value={documentExpirationDate}
          onChange={(event) => onDocumentExpirationDateChange(event.target.value)}
          disabled={disabled}
        />
      </div>
      <Input
        label={t('drivingLicense.record.licenseNumberLabel')}
        value={licenseNumber}
        onChange={(event) => onLicenseNumberChange(event.target.value)}
        disabled={disabled}
        required
      />
      <Input
        label={t('drivingLicense.record.issuingAuthorityLabel')}
        value={issuingAuthority}
        onChange={(event) => onIssuingAuthorityChange(event.target.value)}
        disabled={disabled}
        required
      />
      <DrivingLicenseClassesEditor
        definitions={classDefinitions}
        value={classes}
        onChange={onClassesChange}
        disabled={disabled}
      />
      <Textarea
        label={t('common.record.notesLabel')}
        value={notes}
        onChange={(event) => onNotesChange(event.target.value)}
        disabled={disabled}
      />
      {showAttachments ? (
        <PendingAttachments
          variant="field"
          inputId={attachmentsInputId}
          files={files}
          onChange={onFilesChange}
          storedFileNames={storedFileNames}
          onRemoveStored={onRemoveStored}
          readOnly={attachmentsReadOnly}
          disabled={disabled}
          label={t('drivingLicense.submit.attachmentsLabel')}
          hint={t('drivingLicense.submit.attachmentsHint')}
          required={storedFileNames.length === 0 && files.length === 0}
          removeFileLabel={t('common.attachments.removeFile')}
        />
      ) : null}
    </div>
  );
}
