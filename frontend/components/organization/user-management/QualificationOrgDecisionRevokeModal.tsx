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
import { statusDangerSolidCtaClass } from '@/lib/utils/statusActionClasses';

export interface QualificationOrgDecisionRevokeModalProps {
  isOpen: boolean;
  onClose: () => void;
  qualificationName: string;
  intervalStartDate: string;
  currentEndDate?: string | null;
  isLoading?: boolean;
  onConfirm: (params: { endDate: string }) => void | Promise<void>;
}

function QualificationOrgDecisionRevokeModalOpen({
  onClose,
  qualificationName,
  intervalStartDate,
  currentEndDate,
  isLoading = false,
  onConfirm,
}: Omit<QualificationOrgDecisionRevokeModalProps, 'isOpen'>) {
  const t = useTranslation();
  const [endDate, setEndDate] = useState(() => currentEndDate?.trim() || getTodayIsoDate());

  const dateInvalid = isEndStrictlyBeforeStart(intervalStartDate, endDate);
  const isValid = endDate.trim().length > 0 && !dateInvalid;

  const endDateMinValue = useMemo(() => {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(intervalStartDate)) return undefined;
    try {
      return parseDate(intervalStartDate);
    } catch {
      return undefined;
    }
  }, [intervalStartDate]);

  const handleConfirm = async () => {
    if (!isValid) return;
    await onConfirm({ endDate });
  };

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={t('qualifications.orgDecision.revokeModalTitle')}
      size="md"
      bodyClassName="pb-1"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={handleConfirm}
          isValid={isValid}
          isLoading={isLoading}
          saveText={t('qualifications.orgDecision.revoke')}
          saveButtonClassName={statusDangerSolidCtaClass}
        />
      }
    >
      <div className="space-y-4 py-1">
        <p className="text-sm text-[var(--text-secondary)]">
          {t('qualifications.orgDecision.revokeModalDescription', { name: qualificationName })}
        </p>
        <DatePicker
          label={t('qualifications.orgDecision.endDateLabel')}
          value={endDate}
          onChange={(e) => setEndDate(e.target.value)}
          minValue={endDateMinValue}
          error={dateInvalid ? t('common.validation.endNotBeforeStart') : undefined}
          required
        />
        {!dateInvalid ? (
          <p className="text-xs text-[var(--text-secondary)]">
            {t('qualifications.orgDecision.endDateOptionalHint')}
          </p>
        ) : null}
      </div>
    </Modal>
  );
}

export function QualificationOrgDecisionRevokeModal({
  isOpen,
  ...props
}: QualificationOrgDecisionRevokeModalProps) {
  if (!isOpen) return null;
  return <QualificationOrgDecisionRevokeModalOpen {...props} />;
}
