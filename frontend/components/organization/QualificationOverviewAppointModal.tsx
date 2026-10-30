/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { parseDate } from '@internationalized/date';
import { useMemo, useState } from 'react';
import { OrganizationMemberSelectField } from '@/components/organization/OrganizationMemberSelectField';
import { DatePicker } from '@/components/ui/date-picker';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useTranslation } from '@/hooks/useTranslation';
import type { QualificationOverviewMemberDto } from '@/lib/api/qualifications';
import { getTodayIsoDate, isEndStrictlyBeforeStart } from '@/lib/utils/date';
import type { UserDto } from '@/types/user';

export interface QualificationOverviewAppointModalProps {
  isOpen: boolean;
  onClose: () => void;
  qualificationName: string;
  appointableMembers: QualificationOverviewMemberDto[];
  isLoading?: boolean;
  onConfirm: (params: {
    userId: number;
    startDate: string;
    endDate?: string;
  }) => void | Promise<void>;
}

function QualificationOverviewAppointModalOpen({
  onClose,
  qualificationName,
  appointableMembers,
  isLoading = false,
  onConfirm,
}: Omit<QualificationOverviewAppointModalProps, 'isOpen'>) {
  const t = useTranslation();
  const [userId, setUserId] = useState<number | undefined>(() =>
    appointableMembers.length === 1 ? appointableMembers[0].userId : undefined,
  );
  const [startDate, setStartDate] = useState(() => getTodayIsoDate());
  const [endDate, setEndDate] = useState('');

  const dateInvalid = !!endDate.trim() && isEndStrictlyBeforeStart(startDate, endDate);
  const isValid = userId != null && userId > 0 && startDate.trim().length > 0 && !dateInvalid;

  const endDateMinValue = useMemo(() => {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(startDate)) return undefined;
    try {
      return parseDate(startDate);
    } catch {
      return undefined;
    }
  }, [startDate]);

  const pickerUsers = useMemo((): UserDto[] => {
    return appointableMembers.map((member) => ({
      id: member.userId,
      personalNumber: 0,
      firstName: member.userDisplayName,
      lastName: '',
    }));
  }, [appointableMembers]);

  const handleConfirm = async () => {
    if (!isValid || userId == null) return;
    await onConfirm({
      userId,
      startDate,
      endDate: endDate.trim() ? endDate : undefined,
    });
  };

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={t('qualifications.overview.appointModalTitle')}
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
          {t('qualifications.overview.appointModalDescription', { name: qualificationName })}
        </p>
        <OrganizationMemberSelectField
          label={t('qualifications.overview.memberLabel')}
          users={pickerUsers}
          value={userId}
          onChange={setUserId}
          required
          disabled={isLoading}
        />
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

export function QualificationOverviewAppointModal({
  isOpen,
  ...props
}: QualificationOverviewAppointModalProps) {
  if (!isOpen) return null;
  return <QualificationOverviewAppointModalOpen key={props.qualificationName} {...props} />;
}
