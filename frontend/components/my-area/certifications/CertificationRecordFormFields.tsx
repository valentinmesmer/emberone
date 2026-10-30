/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { PendingAttachments } from '@/components/attachment';
import { DatePicker } from '@/components/ui/date-picker';
import { Select } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { useTranslation } from '@/hooks/useTranslation';
import type { CertificationDefinitionDto } from '@/lib/api/certifications';

export interface CertificationRecordFormFieldsProps {
  definitions?: CertificationDefinitionDto[];
  selectedDefinitionIds: number[];
  onSelectedDefinitionIdsChange: (ids: number[]) => void;
  completedDate: string;
  onCompletedDateChange: (value: string) => void;
  notes: string;
  onNotesChange: (value: string) => void;
  files: File[];
  onFilesChange: (files: File[]) => void;
  attachmentRequired: boolean;
  storedFileNames?: string[];
  onRemoveStored?: (index: number) => void;
  disabled?: boolean;
  attachmentsInputId?: string;
}

export function CertificationRecordFormFields({
  definitions = [],
  selectedDefinitionIds,
  onSelectedDefinitionIdsChange,
  completedDate,
  onCompletedDateChange,
  notes,
  onNotesChange,
  files,
  onFilesChange,
  attachmentRequired,
  storedFileNames = [],
  onRemoveStored,
  disabled = false,
  attachmentsInputId = 'certification-record-attachments',
}: CertificationRecordFormFieldsProps) {
  const t = useTranslation();

  return (
    <div className="space-y-4">
      <Select
        label={t('certifications.submit.definitionLabel')}
        name="tenantCertDefinitionId"
        value={selectedDefinitionIds[0] ? String(selectedDefinitionIds[0]) : ''}
        onChange={(event) =>
          onSelectedDefinitionIdsChange(
            event.target.value ? [Number.parseInt(event.target.value, 10)] : [],
          )
        }
        options={[
          { value: '', label: t('certifications.submit.chooseDefinition') },
          ...definitions.map((definition) => ({
            value: String(definition.id),
            label: definition.name,
          })),
        ]}
        required
        disabled={disabled}
      />
      <DatePicker
        label={t('certifications.submit.completedDateLabel')}
        name="completedDate"
        value={completedDate}
        onChange={(event) => onCompletedDateChange(event.target.value)}
        disabled={disabled}
        required
      />
      <Textarea
        label={t('certifications.submit.notesLabel')}
        value={notes}
        onChange={(event) => onNotesChange(event.target.value)}
        disabled={disabled}
      />
      <PendingAttachments
        variant="field"
        inputId={attachmentsInputId}
        files={files}
        onChange={onFilesChange}
        storedFileNames={storedFileNames}
        onRemoveStored={onRemoveStored}
        disabled={disabled}
        label={t('certifications.submit.attachmentsLabel')}
        hint={
          attachmentRequired
            ? t('certifications.submit.attachmentsHint')
            : t('certifications.submit.attachmentsOptionalHint')
        }
        required={attachmentRequired && storedFileNames.length === 0 && files.length === 0}
        optionalLabel={
          attachmentRequired && storedFileNames.length === 0 && files.length === 0
            ? undefined
            : t('certifications.submit.attachmentsOptional')
        }
        removeFileLabel={t('common.attachments.removeFile')}
      />
    </div>
  );
}
