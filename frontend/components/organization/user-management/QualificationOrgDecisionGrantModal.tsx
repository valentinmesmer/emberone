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
import { useTranslation } from '@/hooks/useTranslation';
import { getTodayIsoDate, isEndStrictlyBeforeStart } from '@/lib/utils/date';

export interface QualificationOrgDecisionGrantModalProps {
  isOpen: boolean;
  onClose: () => void;
  qualificationName: string;
  isLoading?: boolean;
  onConfirm: (params: { startDate: string; endDate?: string }) => void | Promise<void>;
}

function QualificationOrgDecisionGrantModalOpen({
  onClose,
  qualificationName,
  isLoading = false,
  onConfirm,
}: Omit<QualificationOrgDecisionGrantModalProps, 'isOpen'>) {
  const t = useTranslation();
  const [startDate, setStartDate] = useState(() => getTodayIsoDate());
  const [endDate, setEndDate] = useState('');

  const dateInvalid = !!endDate.trim() && isEndStrictlyBeforeStart(startDate, endDate);
  const isValid = startDate.trim().length > 0 && !dateInvalid;

  const endDateMinValue = useMemo(() => {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(startDate)) return undefined;
    try {
      return parseDate(startDate);
    } catch {
      return undefined;
    }
  }, [startDate]);

  const handleConfirm = async () => {
    if (!isValid) return;
    await onConfirm({
      startDate,
      endDate: endDate.trim() ? endDate : undefined,
    });
  };

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={t('qualifications.orgDecision.grantModalTitle')}
      size="md"
      bodyClassName="pb-1"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={handleConfirm}
          isValid={isValid}
          isLoading={isLoading}
          saveText={t('qualifications.orgDecision.grant')}
        />
      }
    >
      <div className="space-y-4 py-1">
        <p className="text-sm text-[var(--text-secondary)]">
          {t('qualifications.orgDecision.grantModalDescription', { name: qualificationName })}
        </p>
        <div className="flex flex-col gap-4">
          <DatePicker
            label={t('qualifications.orgDecision.startDateLabel')}
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            required
          />
          <DatePicker
            label={t('qualifications.orgDecision.endDateLabel')}
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            minValue={endDateMinValue}
            error={dateInvalid ? t('common.validation.endNotBeforeStart') : undefined}
          />
        </div>
        {!dateInvalid ? (
          <p className="text-xs text-[var(--text-secondary)]">
            {t('qualifications.orgDecision.endDateOptionalHint')}
          </p>
        ) : null}
      </div>
    </Modal>
  );
}

export function QualificationOrgDecisionGrantModal({
  isOpen,
  ...props
}: QualificationOrgDecisionGrantModalProps) {
  if (!isOpen) return null;
  return <QualificationOrgDecisionGrantModalOpen {...props} />;
}
