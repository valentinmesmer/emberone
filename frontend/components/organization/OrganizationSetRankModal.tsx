/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useEffect, useMemo, useState } from 'react';
import { DatePicker } from '@/components/ui/date-picker';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Select } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import type { OrganizationRankMemberDto } from '@/lib/api/ranks';
import { useOrganizationAvailableRanks, useSetUserRank } from '@/lib/store';
import { getTodayIsoDate } from '@/lib/utils/date';

interface Props {
  organizationId: number;
  member: OrganizationRankMemberDto;
  isOpen: boolean;
  onClose: () => void;
}

export function OrganizationSetRankModal({ organizationId, member, isOpen, onClose }: Props) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const { data: ranks = [] } = useOrganizationAvailableRanks(organizationId, isOpen);
  const setRank = useSetUserRank(organizationId, member.userId);

  const [tenantRankId, setTenantRankId] = useState('');
  const [startDate, setStartDate] = useState('');
  const [reason, setReason] = useState('');

  useEffect(() => {
    if (!isOpen) return;
    setTenantRankId(member.currentRank ? String(member.currentRank.tenantRankId) : '');
    setStartDate(getTodayIsoDate());
    setReason('');
  }, [isOpen, member]);

  const rankOptions = useMemo(
    () => ranks.map((rank) => ({ value: String(rank.id), label: rank.name })),
    [ranks],
  );

  const handleSave = async () => {
    const parsedRankId = Number.parseInt(tenantRankId, 10);
    if (!Number.isFinite(parsedRankId) || !startDate) {
      return;
    }
    const trimmedReason = reason.trim();
    try {
      await setRank.mutateAsync({
        tenantRankId: parsedRankId,
        startDate,
        ...(trimmedReason ? { reason: trimmedReason } : {}),
      });
      showSuccess(t('ranks.setRank.success'));
      onClose();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('ranks.setRank.failed'));
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={t('ranks.setRank.title', { name: `${member.firstName} ${member.lastName}` })}
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleSave()}
          isValid={Boolean(tenantRankId && startDate)}
          isLoading={setRank.isPending}
        />
      }
    >
      <div className="space-y-3">
        <Select
          label={t('ranks.setRank.rank')}
          value={tenantRankId}
          onChange={(e) => setTenantRankId(e.target.value)}
          options={rankOptions}
          disabled={setRank.isPending}
        />
        <DatePicker
          label={t('ranks.setRank.startDate')}
          value={startDate}
          onChange={(e) => setStartDate(e.target.value)}
          disabled={setRank.isPending}
          required
        />
        <Textarea
          label={t('ranks.setRank.reason')}
          hint={t('common.request.reasonOptional')}
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          disabled={setRank.isPending}
        />
      </div>
    </Modal>
  );
}
