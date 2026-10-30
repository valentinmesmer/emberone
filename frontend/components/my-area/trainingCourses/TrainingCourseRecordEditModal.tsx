/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { parseDate } from '@internationalized/date';
import { useEffect, useMemo, useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { DatePicker } from '@/components/ui/date-picker';
import { Input } from '@/components/ui/input';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import type { TrainingCourseUserRecordDto } from '@/lib/api/trainingCourses';
import { useUpdateTrainingCourseRecord } from '@/lib/store';
import {
  isEndStrictlyBeforeStart,
  laterOfDateStrings,
  shiftEndDateWithStartDateChange,
} from '@/lib/utils/date';

export interface TrainingCourseRecordEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  organizationId: number;
  userId: number;
  record: TrainingCourseUserRecordDto;
}

export function TrainingCourseRecordEditModal({
  isOpen,
  onClose,
  organizationId,
  userId,
  record,
}: TrainingCourseRecordEditModalProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const updateRecord = useUpdateTrainingCourseRecord(organizationId, userId);

  const [name, setName] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [location, setLocation] = useState('');
  const [hours, setHours] = useState('');
  const [notes, setNotes] = useState('');

  const requiresName = (record.grantedTypes?.length ?? 0) > 1;

  useEffect(() => {
    if (!isOpen) return;
    queueMicrotask(() => {
      setName(record.name ?? '');
      setStartDate(record.startDate);
      setEndDate(record.endDate);
      setLocation(record.location ?? '');
      setHours(record.hours != null ? String(record.hours) : '');
      setNotes(record.notes ?? '');
    });
  }, [isOpen, record]);

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

  const canSave =
    (!requiresName || name.trim().length > 0) &&
    startDate.trim().length > 0 &&
    endDate.trim().length > 0 &&
    !dateOrderInvalid &&
    location.trim().length > 0 &&
    !updateRecord.isPending;

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

  const handleSave = async () => {
    if (!canSave) return;
    try {
      const parsedHours = hours.trim() ? Number.parseFloat(hours) : null;
      await updateRecord.mutateAsync({
        recordId: record.id,
        data: {
          name: requiresName ? name.trim() : undefined,
          startDate,
          endDate,
          location: location.trim(),
          hours: parsedHours,
          notes: notes.trim() || null,
        },
      });
      showSuccess(t('trainingCourse.record.editSuccess'));
      onClose();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('trainingCourse.record.editError'));
    }
  };

  if (!isOpen) {
    return null;
  }

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={t('trainingCourse.record.editTitle')}
      size="md"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleSave()}
          isValid={canSave}
          isLoading={updateRecord.isPending}
          showSave
        />
      }
    >
      <div className="space-y-4">
        {requiresName ? (
          <Input
            label={t('trainingCourse.submissionName.label')}
            value={name}
            onChange={(e) => setName(e.target.value)}
            disabled={updateRecord.isPending}
            required
          />
        ) : null}
        <div className="space-y-2">
          <p className="text-sm font-semibold text-[var(--text-primary)]">
            {t('trainingCourse.grantedTypes.label')}
          </p>
          <div className="flex flex-wrap gap-1.5">
            {(record.grantedTypes ?? []).map((type) => (
              <Badge key={type.tenantTrainingCourseTypeId} tone="neutral">
                {type.courseTypeName ?? `#${type.tenantTrainingCourseTypeId}`}
              </Badge>
            ))}
          </div>
        </div>
        <DatePicker
          label={t('trainingCourse.record.startDateLabel')}
          name="startDate"
          value={startDate}
          onChange={handleStartDateChange}
          disabled={updateRecord.isPending}
          required
        />
        <DatePicker
          label={t('trainingCourse.record.endDateLabel')}
          name="endDate"
          value={endDate}
          onChange={(e) => setEndDate(e.target.value)}
          minValue={endDateMinValue}
          error={dateOrderError}
          disabled={updateRecord.isPending}
          required
        />
        <Input
          label={t('trainingCourse.record.locationLabel')}
          name="location"
          value={location}
          onChange={(e) => setLocation(e.target.value)}
          placeholder={t('trainingCourse.record.locationPlaceholder')}
          disabled={updateRecord.isPending}
          required
        />
        <Input
          label={t('trainingCourse.record.hoursLabel')}
          value={hours}
          onChange={(e) => setHours(e.target.value)}
          inputMode="decimal"
          disabled={updateRecord.isPending}
        />
        <Textarea
          label={t('common.record.notesLabel')}
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          disabled={updateRecord.isPending}
        />
      </div>
    </Modal>
  );
}
