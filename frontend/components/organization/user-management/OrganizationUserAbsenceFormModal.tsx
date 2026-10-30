/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { parseDate } from '@internationalized/date';
import { useMemo, useState } from 'react';
import { DatePicker } from '@/components/ui/date-picker';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Select } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import {
  ABSENCE_REASON_OPTION_VALUES,
  getAbsenceReasonLabelKey,
} from '@/lib/my-area/absenceReasonOptions';
import {
  isEndStrictlyBeforeStart,
  laterOfDateStrings,
  shiftEndDateWithStartDateChange,
} from '@/lib/utils/date';
import type { CreateUserAbsenceRequest, UserAbsenceDto } from '@/types/absence';
import { AbsenceReason } from '@/types/enums/absenceReason';

interface OrganizationUserAbsenceFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  organizationId: number;
  absence?: UserAbsenceDto;
  onSave: (data: CreateUserAbsenceRequest) => Promise<void>;
}

function getLocalDateIsoString(date: Date = new Date()): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export function OrganizationUserAbsenceFormModal({
  isOpen,
  onClose,
  organizationId,
  absence,
  onSave,
}: OrganizationUserAbsenceFormModalProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const [isLoading, setIsLoading] = useState(false);

  const [startDate, setStartDate] = useState(() => absence?.startDate ?? getLocalDateIsoString());
  const [endDate, setEndDate] = useState(() => absence?.endDate ?? getLocalDateIsoString());
  const [reason, setReason] = useState<AbsenceReason>(absence?.reason ?? AbsenceReason.PERSONAL);
  const [comment, setComment] = useState(absence?.comment ?? '');

  const reasonOptions = useMemo(
    () =>
      ABSENCE_REASON_OPTION_VALUES.map((r) => ({
        value: r,
        label: t(getAbsenceReasonLabelKey(r)),
      })),
    [t],
  );

  const isFormValid =
    !!startDate.trim() && !!endDate.trim() && !isEndStrictlyBeforeStart(startDate, endDate);

  const endDateMinValue = useMemo(() => {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(startDate)) return undefined;
    try {
      return parseDate(startDate);
    } catch {
      return undefined;
    }
  }, [startDate]);

  const dateOrderError =
    startDate && endDate && isEndStrictlyBeforeStart(startDate, endDate)
      ? t('common.validation.endNotBeforeStart')
      : undefined;

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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isFormValid) return;

    const payload: CreateUserAbsenceRequest = {
      organizationIds: [organizationId],
      startDate,
      endDate,
      reason,
      comment: comment.trim() || undefined,
    };

    setIsLoading(true);
    try {
      await onSave(payload);
      showSuccess(
        absence ? t('myArea.absence.success.updated') : t('myArea.absence.success.created'),
      );
      onClose();
    } catch (error) {
      showError(error instanceof Error ? error.message : t('myArea.error.loadFailed'));
    } finally {
      setIsLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={absence ? t('myArea.absence.editTitle') : t('myArea.absence.addTitle')}
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => {
            const form = document.getElementById('org-user-absence-form') as HTMLFormElement;
            form?.requestSubmit();
          }}
          isValid={isFormValid}
          isLoading={isLoading}
        />
      }
    >
      <form id="org-user-absence-form" onSubmit={(e) => void handleSubmit(e)}>
        <div className="space-y-4">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <DatePicker
              label={t('myArea.absence.fields.startDate')}
              name="startDate"
              value={startDate}
              onChange={handleStartDateChange}
              required
            />
            <DatePicker
              label={t('myArea.absence.fields.endDate')}
              name="endDate"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              minValue={endDateMinValue}
              required
              error={dateOrderError}
            />
          </div>

          <Select
            label={t('myArea.absence.fields.reason')}
            name="reason"
            value={reason}
            options={reasonOptions}
            onChange={(e) => setReason(e.target.value as AbsenceReason)}
          />

          <Textarea
            label={t('myArea.absence.fields.comment')}
            name="comment"
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            rows={3}
            hint={t('myArea.absence.commentHealthHint')}
          />
        </div>
      </form>
    </Modal>
  );
}
