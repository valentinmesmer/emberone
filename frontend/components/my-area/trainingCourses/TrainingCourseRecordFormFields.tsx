/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { parseDate } from '@internationalized/date';
import { useMemo } from 'react';
import { PendingAttachments } from '@/components/attachment';
import { TrainingCourseTypeSelectionField } from '@/components/my-area/trainingCourses/TrainingCourseTypeSelectionField';
import { DatePicker } from '@/components/ui/date-picker';
import { FieldDisplay } from '@/components/ui/field-display';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { useTranslation } from '@/hooks/useTranslation';
import type { TrainingCourseTypeDto } from '@/lib/api/trainingCourses';
import { isEndStrictlyBeforeStart, shiftEndDateWithStartDateChange } from '@/lib/utils/date';

export interface TrainingCourseRecordFormFieldsProps {
  types: TrainingCourseTypeDto[];
  selectedTypeIds: number[];
  onSelectedTypeIdsChange: (ids: number[]) => void;
  lockTypes?: boolean;
  name: string;
  onNameChange: (value: string) => void;
  startDate: string;
  onStartDateChange: (value: string) => void;
  endDate: string;
  onEndDateChange: (value: string) => void;
  location: string;
  onLocationChange: (value: string) => void;
  hours: string;
  onHoursChange: (value: string) => void;
  notes: string;
  onNotesChange: (value: string) => void;
  files: File[];
  onFilesChange: (files: File[]) => void;
  storedFileNames?: string[];
  onRemoveStored?: (index: number) => void;
  disabled?: boolean;
  attachmentsInputId?: string;
}

export function TrainingCourseRecordFormFields({
  types,
  selectedTypeIds,
  onSelectedTypeIdsChange,
  lockTypes = false,
  name,
  onNameChange,
  startDate,
  onStartDateChange,
  endDate,
  onEndDateChange,
  location,
  onLocationChange,
  hours,
  onHoursChange,
  notes,
  onNotesChange,
  files,
  onFilesChange,
  storedFileNames = [],
  onRemoveStored,
  disabled = false,
  attachmentsInputId = 'training-course-record-attachments',
}: TrainingCourseRecordFormFieldsProps) {
  const t = useTranslation();
  const requiresName = selectedTypeIds.length > 1;
  const lockedTypeLabel = useMemo(
    () =>
      types
        .filter((type) => selectedTypeIds.includes(type.id))
        .map((type) => type.name)
        .join(', ') || null,
    [selectedTypeIds, types],
  );

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

  const handleStartDateChange = (value: string) => {
    const prevStart = startDate;
    onStartDateChange(value);
    if (!endDate || !value) return;
    if (
      /^\d{4}-\d{2}-\d{2}$/.test(endDate) &&
      /^\d{4}-\d{2}-\d{2}$/.test(value) &&
      /^\d{4}-\d{2}-\d{2}$/.test(prevStart)
    ) {
      onEndDateChange(shiftEndDateWithStartDateChange(prevStart, value, endDate));
      return;
    }
    if (endDate < value) {
      onEndDateChange(value);
    }
  };

  return (
    <div className="space-y-4">
      {lockTypes && lockedTypeLabel ? (
        <FieldDisplay label={t('trainingCourse.submit.typeLabel')} value={lockedTypeLabel} />
      ) : (
        <TrainingCourseTypeSelectionField
          types={types}
          selectedIds={selectedTypeIds}
          onChange={onSelectedTypeIdsChange}
          disabled={disabled}
        />
      )}
      {requiresName ? (
        <Input
          label={t('trainingCourse.submissionName.label')}
          name="submissionName"
          value={name}
          onChange={(event) => onNameChange(event.target.value)}
          placeholder={t('trainingCourse.submissionName.placeholder')}
          hint={t('trainingCourse.submissionName.hint')}
          disabled={disabled}
          required
        />
      ) : null}
      <DatePicker
        label={t('trainingCourse.submit.startDateLabel')}
        name="startDate"
        value={startDate}
        onChange={(event) => handleStartDateChange(event.target.value)}
        disabled={disabled}
        required
      />
      <DatePicker
        label={t('trainingCourse.submit.endDateLabel')}
        name="endDate"
        value={endDate}
        onChange={(event) => onEndDateChange(event.target.value)}
        minValue={endDateMinValue}
        error={dateOrderError}
        disabled={disabled}
        required
      />
      <Input
        label={t('trainingCourse.submit.locationLabel')}
        name="location"
        value={location}
        onChange={(event) => onLocationChange(event.target.value)}
        placeholder={t('trainingCourse.submit.locationPlaceholder')}
        disabled={disabled}
        required
      />
      <Input
        label={t('trainingCourse.submit.hoursLabel')}
        value={hours}
        onChange={(event) => onHoursChange(event.target.value)}
        inputMode="decimal"
        disabled={disabled}
      />
      <Textarea
        label={t('trainingCourse.submit.notesLabel')}
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
        label={t('trainingCourse.submit.attachmentsLabel')}
        hint={t('trainingCourse.submit.attachmentsHint')}
        required={storedFileNames.length === 0 && files.length === 0}
        removeFileLabel={t('common.attachments.removeFile')}
      />
    </div>
  );
}
