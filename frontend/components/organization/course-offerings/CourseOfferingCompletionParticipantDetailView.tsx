/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useLayoutEffect, useMemo } from 'react';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { DetailCardHeader } from '@/components/ui/detail-card-header';
import { LoadingState } from '@/components/ui/loading';
import { ErrorState, NotFoundState } from '@/components/ui/message-cards';
import { SwitchField } from '@/components/ui/switch';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useSetBackHref } from '@/lib/navigation/backHrefOverride';
import { usePermissions } from '@/lib/permission/PermissionContext';
import {
  useCourseOfferingCompletion,
  useOrganizationCourseOffering,
  useSetCourseFinalResult,
} from '@/lib/store';
import {
  canSetCourseOfferingResults,
  getOrganizationCourseCompletionPath,
} from '@/lib/utils/courseOffering';
import { formatDateShort } from '@/lib/utils/date';
import { PermissionTypeEnum } from '@/types/permission';

interface Props {
  organizationId: number;
  offeringId: number;
  registrationId: number;
}

/** One participant: whether completing the offering credits the course (yes/no). */
export function CourseOfferingCompletionParticipantDetailView({
  organizationId,
  offeringId,
  registrationId,
}: Props) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const { hasPermission } = usePermissions();
  const setBackHref = useSetBackHref();

  const canRead = hasPermission(organizationId, PermissionTypeEnum.COURSE_OFFERING_READ);
  const canWrite = hasPermission(organizationId, PermissionTypeEnum.COURSE_OFFERING_WRITE);

  const listPath = useMemo(
    () => getOrganizationCourseCompletionPath(organizationId, offeringId),
    [organizationId, offeringId],
  );

  useLayoutEffect(() => {
    setBackHref(listPath);
    return () => setBackHref(null);
  }, [listPath, setBackHref]);

  const { data: offering, isPending: offeringPending } = useOrganizationCourseOffering(
    canRead ? organizationId : null,
    offeringId,
    canRead,
  );
  const {
    data: completion,
    isPending,
    error,
  } = useCourseOfferingCompletion(organizationId, offeringId);
  const setResult = useSetCourseFinalResult(organizationId, offeringId);

  if (isPending || offeringPending) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (error || !completion) {
    return <ErrorState message={t('courseOfferings.completion.loadFailed')} />;
  }

  if (!offering) {
    return <NotFoundState message={t('courseOfferings.detail.notFound')} />;
  }

  const participant = completion.participants.find((p) => p.registrationId === registrationId);
  if (!participant) {
    return <NotFoundState message={t('courseOfferings.completion.notFound')} />;
  }

  const isCompleted = completion.status === 'COMPLETED';
  const resultsOpen = canSetCourseOfferingResults(offering);
  const canEdit = canWrite && !isCompleted && resultsOpen;
  const credited = participant.finalPassed === true;

  const onCreditChange = async (checked: boolean) => {
    try {
      await setResult.mutateAsync({ registrationId, passed: checked });
      showSuccess(t('courseOfferings.completion.setResultSuccess'));
    } catch {
      showError(t('courseOfferings.completion.setResultFailed'));
    }
  };

  return (
    <div className="space-y-3">
      <Card className="p-3 sm:p-4">
        <DetailCardHeader
          className="mb-2"
          eyebrow={offering.trainingCourseTypeName}
          title={participant.userDisplayName}
          badges={
            participant.finalPassed == null ? (
              <Badge tone="warning" pill>
                {t('courseOfferings.completion.undecided')}
              </Badge>
            ) : participant.finalPassed ? (
              <Badge tone="success" pill>
                {t('courseOfferings.completion.passed')}
              </Badge>
            ) : (
              <Badge tone="neutral" pill>
                {t('courseOfferings.completion.failed')}
              </Badge>
            )
          }
          subtitle={
            completion.completedAt
              ? t('courseOfferings.completion.completedAt', {
                  date: formatDateShort(completion.completedAt),
                })
              : canWrite && !isCompleted && !resultsOpen
                ? t('courseOfferings.completion.resultsBlockedNotStarted')
                : undefined
          }
        />

        {canWrite && !isCompleted && !resultsOpen ? null : (
          <div className="mt-3 border-t border-[var(--border-color)] pt-3">
            <SwitchField
              label={t('courseOfferings.completion.creditSwitch')}
              description={t('courseOfferings.completion.creditSwitchHint')}
              checked={credited}
              disabled={!canEdit || setResult.isPending}
              onCheckedChange={(checked) => void onCreditChange(checked)}
            />
          </div>
        )}
      </Card>
    </div>
  );
}
