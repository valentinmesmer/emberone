/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useState } from 'react';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Select } from '@/components/ui/select';
import { useTranslation } from '@/hooks/useTranslation';
import { useCourseAudienceOptions } from '@/lib/store';
import { courseAudienceKindLabelKey } from '@/lib/utils/courseOffering';
import type {
  CourseAudienceEntryDto,
  CourseAudienceEntryRequest,
  CourseAudienceKind,
} from '@/types/courseOffering';

const GROUP_KINDS: CourseAudienceKind[] = ['ORG', 'ORG_SUBTREE', 'ORG_TYPE'];

/** Name of the organization / organization type an audience group points at. */
export function useCourseAudienceTargetName(organizationId: number) {
  const { data } = useCourseAudienceOptions(organizationId);
  return (entry: Pick<CourseAudienceEntryDto, 'kind' | 'refId'>): string | undefined => {
    if (entry.kind === 'ORG_TYPE') {
      return data?.organizationTypes.find((type) => type.id === entry.refId)?.name;
    }
    return data?.organizations.find((org) => org.id === entry.refId)?.name;
  };
}

interface Props {
  organizationId: number;
  /** Groups already on the offering; their targets are not offered again. */
  taken: Pick<CourseAudienceEntryDto, 'kind' | 'refId'>[];
  /** Existing quotas are reset when a group is added. */
  resetsQuotas: boolean;
  isSaving: boolean;
  onClose: () => void;
  onSave: (request: CourseAudienceEntryRequest) => void;
}

export function CourseAudienceGroupModal({
  organizationId,
  taken,
  resetsQuotas,
  isSaving,
  onClose,
  onSave,
}: Props) {
  const t = useTranslation();
  const { data: options } = useCourseAudienceOptions(organizationId);
  const [kind, setKind] = useState<CourseAudienceKind>('ORG');
  const [refId, setRefId] = useState<number | null>(null);

  const targets = (
    kind === 'ORG_TYPE' ? (options?.organizationTypes ?? []) : (options?.organizations ?? [])
  ).filter((target) => !taken.some((e) => e.kind === kind && e.refId === target.id));

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={t('courseOfferings.audience.addGroup')}
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => onSave({ kind, refId, quota: null })}
          isValid={refId != null}
          isLoading={isSaving}
        />
      }
    >
      <div className="space-y-4">
        <p className="text-xs leading-relaxed text-[var(--text-secondary)]">
          {t('courseOfferings.audience.groupHint')}
          {resetsQuotas ? ` ${t('courseOfferings.audience.quotasReset')}` : ''}
        </p>
        <Select
          label={t('courseOfferings.audience.kind')}
          value={kind}
          onChange={(e) => {
            setKind(e.target.value as CourseAudienceKind);
            setRefId(null);
          }}
          options={GROUP_KINDS.map((value) => ({
            value,
            label: t(courseAudienceKindLabelKey(value)),
          }))}
          required
        />
        <Select
          label={
            kind === 'ORG_TYPE'
              ? t('courseOfferings.audience.orgType')
              : t('courseOfferings.audience.organization')
          }
          value={refId != null ? String(refId) : ''}
          onChange={(e) => setRefId(e.target.value === '' ? null : Number(e.target.value))}
          options={targets.map((target) => ({
            value: String(target.id),
            label: target.name,
          }))}
          required
        />
      </div>
    </Modal>
  );
}
