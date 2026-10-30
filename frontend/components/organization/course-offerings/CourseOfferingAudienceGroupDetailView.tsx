/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useLayoutEffect, useMemo, useState } from 'react';
import { useCourseAudienceTargetName } from '@/components/organization/course-offerings/CourseAudienceGroupModal';
import { ActionRowIcon, ActionsSection } from '@/components/ui/actions-section';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { DetailCardHeader } from '@/components/ui/detail-card-header';
import { FieldDisplay } from '@/components/ui/field-display';
import { ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { ErrorState, NotFoundState } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useSetBackHref } from '@/lib/navigation/backHrefOverride';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { useOrganizationCourseOffering, useReplaceCourseOfferingAudiences } from '@/lib/store';
import {
  canEditCourseOffering,
  courseAudienceEntriesToRequests,
  courseAudienceKindLabelKey,
  getOrganizationCourseOfferingDetailPath,
} from '@/lib/utils/courseOffering';
import { PermissionTypeEnum } from '@/types/permission';

interface Props {
  organizationId: number;
  offeringId: number;
  audienceId: number;
}

export function CourseOfferingAudienceGroupDetailView({
  organizationId,
  offeringId,
  audienceId,
}: Props) {
  const t = useTranslation();
  const router = useRouter();
  const { showSuccess, showError } = useToast();
  const { hasPermission } = usePermissions();
  const setBackHref = useSetBackHref();
  const targetName = useCourseAudienceTargetName(organizationId);

  const canRead = hasPermission(organizationId, PermissionTypeEnum.COURSE_OFFERING_READ);
  const canWrite = hasPermission(organizationId, PermissionTypeEnum.COURSE_OFFERING_WRITE);

  const offeringPath = useMemo(
    () => getOrganizationCourseOfferingDetailPath(organizationId, offeringId),
    [organizationId, offeringId],
  );

  useLayoutEffect(() => {
    setBackHref(offeringPath);
    return () => setBackHref(null);
  }, [offeringPath, setBackHref]);

  const {
    data: offering,
    isLoading,
    isError,
  } = useOrganizationCourseOffering(canRead ? organizationId : null, offeringId, canRead);
  const replaceAudiences = useReplaceCourseOfferingAudiences(organizationId, offeringId);

  const [deleteOpen, setDeleteOpen] = useState(false);

  const group = offering?.audiences.find((entry) => entry.id === audienceId) ?? null;
  const editable = Boolean(canWrite && offering && canEditCourseOffering(offering.status));

  const confirmDelete = async () => {
    try {
      const entries = courseAudienceEntriesToRequests(
        (offering?.audiences ?? []).filter((entry) => entry.id !== audienceId),
        false,
      );
      await replaceAudiences.mutateAsync(entries);
      showSuccess(
        entries.length === 0
          ? t('courseOfferings.audience.nowEveryone')
          : t('courseOfferings.audience.deleted'),
      );
      setDeleteOpen(false);
      router.push(offeringPath);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('courseOfferings.audience.deleteFailed'));
    }
  };

  if (isLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (isError) {
    return <ErrorState message={t('courseOfferings.list.loadFailed')} />;
  }

  if (!offering || !group) {
    return <NotFoundState message={t('courseOfferings.audience.notFound')} />;
  }

  const kindLabel = t(courseAudienceKindLabelKey(group.kind));

  return (
    <div className="space-y-3">
      <Card className="p-3 sm:p-4">
        <DetailCardHeader
          className="mb-2"
          eyebrow={t('courseOfferings.audience.title')}
          title={kindLabel}
          subtitle={<p>{targetName(group)}</p>}
        />

        <div className="grid grid-cols-1 gap-3 border-t border-[var(--border-color)] pt-4 text-sm sm:grid-cols-2">
          <FieldDisplay label={t('courseOfferings.audience.kind')} value={kindLabel} />
          <FieldDisplay
            label={
              group.kind === 'ORG_TYPE'
                ? t('courseOfferings.audience.orgType')
                : t('courseOfferings.audience.organization')
            }
            value={targetName(group)}
          />
          <FieldDisplay
            label={t('courseOfferings.audience.quotaLabel')}
            value={
              group.quota != null
                ? t('courseOfferings.audience.quota', { count: group.quota })
                : undefined
            }
            fallback={t('courseOfferings.audience.noQuota')}
          />
        </div>
      </Card>

      {editable ? (
        <ActionsSection title={t('courseOfferings.actions.title')}>
          <ListRow
            leading={<ActionRowIcon icon={Trash2} />}
            title={t('courseOfferings.audience.deleteAction')}
            subtitle={t('courseOfferings.audience.deleteConfirm')}
            onClick={() => setDeleteOpen(true)}
          />
        </ActionsSection>
      ) : null}

      <ConfirmDialog
        isOpen={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onConfirm={() => void confirmDelete()}
        title={t('courseOfferings.audience.deleteTitle')}
        message={t('courseOfferings.audience.deleteConfirm')}
        confirmText={t('common.delete')}
        variant="danger"
        isLoading={replaceAudiences.isPending}
      />
    </div>
  );
}
