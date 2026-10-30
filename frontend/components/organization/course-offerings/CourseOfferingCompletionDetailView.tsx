/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Check, X } from 'lucide-react';
import { useLayoutEffect, useMemo } from 'react';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { DetailCardHeader } from '@/components/ui/detail-card-header';
import { IconButton } from '@/components/ui/icon-button';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard, ErrorState, NotFoundState } from '@/components/ui/message-cards';
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
  getOrganizationCourseCompletionParticipantPath,
  getOrganizationCourseOfferingDetailPath,
} from '@/lib/utils/courseOffering';
import { formatDateShort } from '@/lib/utils/date';
import { PermissionTypeEnum } from '@/types/permission';

interface Props {
  organizationId: number;
  offeringId: number;
}

/** Participant results list: set pass/credit on the detail page (mobile) or via row actions. */
export function CourseOfferingCompletionDetailView({ organizationId, offeringId }: Props) {
  const t = useTranslation();
  const { showError } = useToast();
  const { hasPermission } = usePermissions();
  const setBackHref = useSetBackHref();

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
    isLoading: offeringLoading,
    isError: offeringError,
  } = useOrganizationCourseOffering(canRead ? organizationId : null, offeringId, canRead);
  const {
    data: completion,
    isPending: completionPending,
    error: completionError,
  } = useCourseOfferingCompletion(organizationId, offeringId, canRead);
  const setResult = useSetCourseFinalResult(organizationId, offeringId);

  if (offeringLoading || completionPending) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (offeringError || completionError || !completion) {
    return (
      <ErrorState
        message={
          completionError || !completion
            ? t('courseOfferings.completion.loadFailed')
            : t('courseOfferings.list.loadFailed')
        }
      />
    );
  }

  if (!offering) {
    return <NotFoundState message={t('courseOfferings.detail.notFound')} />;
  }

  const isCompleted = completion.status === 'COMPLETED';
  const resultsOpen = canSetCourseOfferingResults(offering);
  const canEditResults = canWrite && !isCompleted && resultsOpen;

  const setPassed = async (registrationId: number, passed: boolean) => {
    try {
      await setResult.mutateAsync({ registrationId, passed });
    } catch {
      showError(t('courseOfferings.completion.setResultFailed'));
    }
  };

  return (
    <div className="space-y-3">
      <Card className="p-3 sm:p-4">
        <DetailCardHeader
          className="mb-1"
          eyebrow={offering.trainingCourseTypeName}
          title={t('courseOfferings.detail.completion')}
          subtitle={
            canWrite && !isCompleted && !resultsOpen
              ? t('courseOfferings.completion.resultsBlockedNotStarted')
              : completion.completedAt
                ? t('courseOfferings.completion.completedAt', {
                    date: formatDateShort(completion.completedAt),
                  })
                : undefined
          }
        />
      </Card>

      {completion.participants.length === 0 ? (
        <EmptyStateCard>{t('courseOfferings.completion.empty')}</EmptyStateCard>
      ) : (
        <ListContainer label={t('courseOfferings.completion.listTitle')}>
          {completion.participants.map((participant) => (
            <ListRow
              key={participant.registrationId}
              href={getOrganizationCourseCompletionParticipantPath(
                organizationId,
                offeringId,
                participant.registrationId,
              )}
              title={participant.userDisplayName}
              badges={<CompletionResultBadge finalPassed={participant.finalPassed} />}
              actions={
                canEditResults ? (
                  <>
                    <IconButton
                      variant="action"
                      icon={<Check />}
                      aria-label={t('courseOfferings.completion.markPassed')}
                      title={t('courseOfferings.completion.markPassed')}
                      disabled={setResult.isPending || participant.finalPassed === true}
                      onClick={() => void setPassed(participant.registrationId, true)}
                    />
                    <IconButton
                      variant="action"
                      icon={<X />}
                      aria-label={t('courseOfferings.completion.markFailed')}
                      title={t('courseOfferings.completion.markFailed')}
                      disabled={setResult.isPending || participant.finalPassed === false}
                      onClick={() => void setPassed(participant.registrationId, false)}
                    />
                  </>
                ) : undefined
              }
            />
          ))}
        </ListContainer>
      )}
    </div>
  );
}

function CompletionResultBadge({ finalPassed }: { finalPassed: boolean | null }) {
  const t = useTranslation();
  if (finalPassed == null) {
    return (
      <Badge tone="warning" pill>
        {t('courseOfferings.completion.undecided')}
      </Badge>
    );
  }
  if (finalPassed) {
    return (
      <Badge tone="success" pill>
        {t('courseOfferings.completion.passed')}
      </Badge>
    );
  }
  return (
    <Badge tone="neutral" pill>
      {t('courseOfferings.completion.failed')}
    </Badge>
  );
}
