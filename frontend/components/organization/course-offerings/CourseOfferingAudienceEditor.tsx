/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Trash2 } from 'lucide-react';
import { useState } from 'react';
import {
  CourseAudienceGroupModal,
  useCourseAudienceTargetName,
} from '@/components/organization/course-offerings/CourseAudienceGroupModal';
import { CourseAudienceQuotaModal } from '@/components/organization/course-offerings/CourseAudienceQuotaModal';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { IconButton } from '@/components/ui/icon-button';
import { ListContainer, ListRow } from '@/components/ui/list';
import { SectionActions, SectionHeader } from '@/components/ui/section-header';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useReplaceCourseOfferingAudiences } from '@/lib/store';
import { cn } from '@/lib/utils/cn';
import {
  canEditCourseOffering,
  courseAudienceEntriesToRequests,
  courseAudienceKindLabelKey,
  getOrganizationCourseAudienceDetailPath,
  offeringIsPrivate,
  offeringPublicRestrictions,
} from '@/lib/utils/courseOffering';
import type {
  CourseAudienceEntryDto,
  CourseAudienceEntryRequest,
  CourseOfferingDto,
} from '@/types/courseOffering';

type AudienceMode = 'everyone' | 'ownOrg' | 'groups';

const MODE_KEYS: Record<AudienceMode, { label: string; description: string }> = {
  everyone: {
    label: 'courseOfferings.audience.modeEveryone',
    description: 'courseOfferings.audience.descriptionEveryone',
  },
  ownOrg: {
    label: 'courseOfferings.audience.modeOwnOrg',
    description: 'courseOfferings.audience.descriptionOwnOrg',
  },
  groups: {
    label: 'courseOfferings.audience.modeGroups',
    description: 'courseOfferings.audience.descriptionGroups',
  },
};

const MODES = Object.keys(MODE_KEYS) as AudienceMode[];

interface Props {
  organizationId: number;
  offering: CourseOfferingDto;
  canWrite: boolean;
}

export function CourseOfferingAudienceEditor({ organizationId, offering, canWrite }: Props) {
  const t = useTranslation();
  const { showSuccess, showError } = useToast();
  const replaceAudiences = useReplaceCourseOfferingAudiences(organizationId, offering.id);
  const editable = canWrite && canEditCourseOffering(offering.status);

  const isPrivate = offeringIsPrivate(offering.audiences);
  const restrictions = offeringPublicRestrictions(offering.audiences);
  const mode: AudienceMode = isPrivate ? 'ownOrg' : restrictions.length > 0 ? 'groups' : 'everyone';
  const targetName = useCourseAudienceTargetName(organizationId);

  const [addOpen, setAddOpen] = useState(false);
  const [quotaOpen, setQuotaOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<CourseAudienceEntryDto | null>(null);
  const [switchTarget, setSwitchTarget] = useState<AudienceMode | null>(null);

  const applyMode = async (next: AudienceMode) => {
    try {
      const entries: CourseAudienceEntryRequest[] =
        next === 'ownOrg' ? [{ kind: 'OWN_ORG', refId: null, quota: null }] : [];
      await replaceAudiences.mutateAsync(entries);
      showSuccess(t('courseOfferings.audience.saved'));
      setSwitchTarget(null);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('courseOfferings.audience.saveFailed'));
    }
  };

  const selectMode = (next: AudienceMode) => {
    if (next === mode) return;
    if (next === 'groups') {
      setAddOpen(true);
    } else if (restrictions.length > 0) {
      setSwitchTarget(next);
    } else {
      void applyMode(next);
    }
  };

  const hasQuotas = restrictions.some((entry) => entry.quota != null);

  const saveGroup = async (request: CourseAudienceEntryRequest) => {
    try {
      await replaceAudiences.mutateAsync([
        ...courseAudienceEntriesToRequests(restrictions, false),
        request,
      ]);
      showSuccess(t('courseOfferings.audience.saved'));
      setAddOpen(false);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('courseOfferings.audience.saveFailed'));
    }
  };

  const saveQuotas = async (entries: CourseAudienceEntryRequest[]) => {
    try {
      await replaceAudiences.mutateAsync(entries);
      showSuccess(t('courseOfferings.audience.saved'));
      setQuotaOpen(false);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('courseOfferings.audience.saveFailed'));
    }
  };

  const confirmDelete = async () => {
    if (!deleteTarget) return;
    try {
      const entries = courseAudienceEntriesToRequests(
        restrictions.filter((entry) => entry.id !== deleteTarget.id),
        false,
      );
      await replaceAudiences.mutateAsync(entries);
      showSuccess(
        entries.length === 0
          ? t('courseOfferings.audience.nowEveryone')
          : t('courseOfferings.audience.deleted'),
      );
      setDeleteTarget(null);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('courseOfferings.audience.deleteFailed'));
    }
  };

  const groupSubtitle = (entry: (typeof restrictions)[number]) =>
    [
      targetName(entry),
      entry.quota != null ? t('courseOfferings.audience.quota', { count: entry.quota }) : null,
    ]
      .filter(Boolean)
      .join(' · ') || undefined;

  return (
    <Card className="p-3 sm:p-4">
      <SectionHeader
        title={t('courseOfferings.audience.title')}
        subtitle={t('courseOfferings.audience.subtitle')}
      />

      {editable ? (
        <ListContainer
          className="mt-3"
          variant="inset"
          label={t('courseOfferings.audience.modeLabel')}
        >
          {MODES.map((value) => (
            <ListRow
              key={value}
              accent={false}
              chevron={false}
              selected={value === mode}
              title={t(MODE_KEYS[value].label)}
              subtitle={t(MODE_KEYS[value].description)}
              rootProps={{ 'aria-current': value === mode ? 'true' : undefined }}
              leading={
                <span
                  aria-hidden="true"
                  className={cn(
                    'flex h-4 w-4 shrink-0 items-center justify-center rounded-full border',
                    value === mode ? 'border-[var(--fire-red)]' : 'border-[var(--border-color)]',
                  )}
                >
                  {value === mode ? (
                    <span className="h-2 w-2 rounded-full bg-[var(--fire-red)]" />
                  ) : null}
                </span>
              }
              onClick={replaceAudiences.isPending ? undefined : () => selectMode(value)}
            />
          ))}
        </ListContainer>
      ) : (
        <ListContainer className="mt-3">
          <ListRow title={t(MODE_KEYS[mode].label)} subtitle={t(MODE_KEYS[mode].description)} />
        </ListContainer>
      )}

      {mode === 'groups' ? (
        <div className="mt-5 border-t border-[var(--border-color)] pt-4">
          <SectionHeader
            title={t('courseOfferings.audience.groupsTitle')}
            action={
              editable ? (
                <SectionActions layout="inline" className="sm:justify-end">
                  <Button
                    type="button"
                    variant="secondary"
                    size="xs"
                    fullWidth="responsive"
                    onClick={() => setQuotaOpen(true)}
                  >
                    {t('courseOfferings.audience.quotaButton')}
                  </Button>
                  <Button
                    type="button"
                    variant="secondary"
                    size="xs"
                    fullWidth="responsive"
                    onClick={() => setAddOpen(true)}
                  >
                    {t('courseOfferings.audience.addGroup')}
                  </Button>
                </SectionActions>
              ) : undefined
            }
          />
          <ListContainer className="mt-3" variant="inset">
            {restrictions.map((entry) => (
              <ListRow
                key={entry.id}
                accent={false}
                href={getOrganizationCourseAudienceDetailPath(
                  organizationId,
                  offering.id,
                  entry.id,
                )}
                title={t(courseAudienceKindLabelKey(entry.kind))}
                subtitle={groupSubtitle(entry)}
                actions={
                  editable ? (
                    <IconButton
                      variant="action"
                      aria-label={t('common.delete')}
                      icon={<Trash2 className="h-4 w-4" aria-hidden />}
                      onClick={() => setDeleteTarget(entry)}
                    />
                  ) : undefined
                }
              />
            ))}
          </ListContainer>
        </div>
      ) : null}

      {addOpen ? (
        <CourseAudienceGroupModal
          organizationId={organizationId}
          taken={restrictions}
          resetsQuotas={hasQuotas}
          isSaving={replaceAudiences.isPending}
          onClose={() => setAddOpen(false)}
          onSave={(request) => void saveGroup(request)}
        />
      ) : null}

      {quotaOpen ? (
        <CourseAudienceQuotaModal
          organizationId={organizationId}
          groups={restrictions}
          maxParticipants={offering.maxParticipants}
          isSaving={replaceAudiences.isPending}
          onClose={() => setQuotaOpen(false)}
          onSave={(entries) => void saveQuotas(entries)}
        />
      ) : null}

      <ConfirmDialog
        isOpen={deleteTarget != null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => void confirmDelete()}
        title={t('courseOfferings.audience.deleteTitle')}
        message={`${t('courseOfferings.audience.deleteConfirm')}${hasQuotas ? ` ${t('courseOfferings.audience.quotasReset')}` : ''}`}
        confirmText={t('common.delete')}
        variant="danger"
        isLoading={replaceAudiences.isPending}
      />

      <ConfirmDialog
        isOpen={switchTarget != null}
        onClose={() => setSwitchTarget(null)}
        onConfirm={() => switchTarget && void applyMode(switchTarget)}
        title={t('courseOfferings.audience.switchTitle')}
        message={t('courseOfferings.audience.switchConfirm')}
        confirmText={t('common.confirm')}
        variant="danger"
        isLoading={replaceAudiences.isPending}
      />
    </Card>
  );
}
