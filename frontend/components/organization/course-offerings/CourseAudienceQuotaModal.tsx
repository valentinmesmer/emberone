/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useState } from 'react';
import { useCourseAudienceTargetName } from '@/components/organization/course-offerings/CourseAudienceGroupModal';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { Input } from '@/components/ui/input';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useTranslation } from '@/hooks/useTranslation';
import { courseAudienceKindLabelKey } from '@/lib/utils/courseOffering';
import type { CourseAudienceEntryDto, CourseAudienceEntryRequest } from '@/types/courseOffering';

interface Props {
  organizationId: number;
  groups: CourseAudienceEntryDto[];
  maxParticipants: number;
  isSaving: boolean;
  onClose: () => void;
  onSave: (entries: CourseAudienceEntryRequest[]) => void;
}

/** Quotas of all groups at once: all empty (no quota) or all set and adding up to the maximum. */
export function CourseAudienceQuotaModal({
  organizationId,
  groups,
  maxParticipants,
  isSaving,
  onClose,
  onSave,
}: Props) {
  const t = useTranslation();
  const targetName = useCourseAudienceTargetName(organizationId);
  const [quotas, setQuotas] = useState<Record<number, number | null>>(() =>
    Object.fromEntries(groups.map((group) => [group.id, group.quota])),
  );
  const [confirmOpen, setConfirmOpen] = useState(false);

  const values = groups.map((group) => quotas[group.id] ?? null);
  const filled = values.filter((value) => value != null);
  const sum = filled.reduce((total, value) => total + value, 0);
  const noQuotas = filled.length === 0;
  const remaining = maxParticipants - sum;
  const isValid =
    noQuotas ||
    (filled.length === groups.length && filled.every((value) => value >= 1) && remaining >= 0);

  const save = () =>
    onSave(
      groups.map((group) => ({
        kind: group.kind,
        refId: group.refId,
        quota: quotas[group.id] ?? null,
      })),
    );

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={t('courseOfferings.audience.quotaTitle')}
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => (remaining > 0 ? setConfirmOpen(true) : save())}
          isValid={isValid}
          isLoading={isSaving}
        />
      }
    >
      <div className="space-y-4">
        <p className="text-xs leading-relaxed text-[var(--text-secondary)]">
          {t('courseOfferings.audience.quotaHint', { max: maxParticipants })}
        </p>
        {groups.map((group) => (
          <Input
            key={group.id}
            label={[t(courseAudienceKindLabelKey(group.kind)), targetName(group)]
              .filter(Boolean)
              .join(' · ')}
            type="number"
            min={0}
            value={quotas[group.id] ?? ''}
            onChange={(e) => {
              const raw = e.target.value.trim();
              setQuotas((prev) => ({
                ...prev,
                [group.id]: raw === '' ? null : Math.max(0, parseInt(raw, 10) || 0),
              }));
            }}
          />
        ))}
        {noQuotas ? null : (
          <p
            className={
              isValid
                ? 'text-sm text-[var(--text-secondary)]'
                : 'text-sm text-[var(--status-danger-accent)]'
            }
          >
            {filled.length < groups.length
              ? t('courseOfferings.audience.quotaIncomplete')
              : remaining > 0
                ? t('courseOfferings.audience.quotaRemaining', { count: remaining })
                : remaining < 0
                  ? t('courseOfferings.audience.quotaOver', { count: -remaining })
                  : t('courseOfferings.audience.quotaSum', { sum, max: maxParticipants })}
          </p>
        )}
      </div>
      <ConfirmDialog
        isOpen={confirmOpen}
        onClose={() => setConfirmOpen(false)}
        onConfirm={() => {
          setConfirmOpen(false);
          save();
        }}
        title={t('courseOfferings.audience.quotaConfirmTitle')}
        message={t('courseOfferings.audience.quotaConfirmMessage', { count: remaining })}
        confirmText={t('courseOfferings.audience.quotaConfirmAction')}
        variant="info"
        isLoading={isSaving}
      />
    </Modal>
  );
}
