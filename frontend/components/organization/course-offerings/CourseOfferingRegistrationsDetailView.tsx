/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ArrowUpToLine, UserMinus } from 'lucide-react';
import { useLayoutEffect, useMemo, useState } from 'react';
import { CourseRegistrationBadges } from '@/components/organization/course-offerings/CourseRegistrationBadges';
import { CourseRegistrationRemoveModal } from '@/components/organization/course-offerings/CourseRegistrationRemoveModal';
import { AppLinkListAction } from '@/components/ui/app-link-list-action';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { DetailCardHeader } from '@/components/ui/detail-card-header';
import { IconButton } from '@/components/ui/icon-button';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { ErrorState, NotFoundState } from '@/components/ui/message-cards';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { orgCourseOfferingRegistrationListDefinition } from '@/lib/list/courseOfferingList';
import { useSetBackHref } from '@/lib/navigation/backHrefOverride';
import { usePermissions } from '@/lib/permission/PermissionContext';
import {
  useOfferingRegistrationListState,
  useOfferingRegistrationSummary,
  useOfferingRegistrations,
  useOrganizationCourseOffering,
  usePromoteCourseRegistration,
} from '@/lib/store';
import {
  getOrganizationCourseOfferingDetailPath,
  getOrganizationCourseRegistrationDetailPath,
} from '@/lib/utils/courseOffering';
import { formatDateTime } from '@/lib/utils/date';
import { resolveGatedAppNavigationHref } from '@/lib/utils/sourceLink';
import type { CourseRegistrationDto, CourseRegistrationSummaryDto } from '@/types/courseOffering';
import { PermissionTypeEnum } from '@/types/permission';

interface Props {
  organizationId: number;
  offeringId: number;
}

/** A waitlist registration can only move up while the offering and its audience quota have a free seat. */
function hasFreePlaceFor(
  registration: CourseRegistrationDto,
  summary: CourseRegistrationSummaryDto | undefined,
): boolean {
  if (!summary || summary.freeSeats <= 0) {
    return false;
  }
  if (registration.audienceId == null) {
    return true;
  }
  const quota = summary.quotas.find((entry) => entry.audienceId === registration.audienceId);
  return quota ? quota.freeSeats > 0 : true;
}

export function CourseOfferingRegistrationsDetailView({ organizationId, offeringId }: Props) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
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
  const { data: summary } = useOfferingRegistrationSummary(organizationId, offeringId, canRead);

  const listState = useOfferingRegistrationListState();
  const {
    data: registrations = [],
    isPending,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useOfferingRegistrations({
    organizationId,
    offeringId,
    listQuery: listState.listQuery,
  });
  const promote = usePromoteCourseRegistration(organizationId, offeringId);
  const [removeTarget, setRemoveTarget] = useState<CourseRegistrationDto | null>(null);
  const [promoteTarget, setPromoteTarget] = useState<CourseRegistrationDto | null>(null);

  const promoteRegistration = async () => {
    if (!promoteTarget) {
      return;
    }
    try {
      await promote.mutateAsync(promoteTarget.id);
      showSuccess(t('courseOfferings.registrations.promoteSuccess'));
      setPromoteTarget(null);
    } catch {
      showError(t('courseOfferings.registrations.promoteFailed'));
    }
  };

  if (offeringLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (offeringError) {
    return <ErrorState message={t('courseOfferings.list.loadFailed')} />;
  }

  if (!offering) {
    return <NotFoundState message={t('courseOfferings.detail.notFound')} />;
  }

  return (
    <div className="space-y-3">
      <Card className="p-3 sm:p-4">
        <DetailCardHeader
          className="mb-2"
          eyebrow={offering.trainingCourseTypeName}
          title={t('courseOfferings.detail.participants')}
          subtitle={
            summary && summary.pending > 0 ? (
              <p>{t('courseOfferings.registrations.pendingHint', { count: summary.pending })}</p>
            ) : undefined
          }
        />
      </Card>

      <PaginatedListView
        definition={orgCourseOfferingRegistrationListDefinition}
        state={listState}
        totalItems={registrations.length}
        isPending={isPending}
        emptyState={paginatedListEmptyCard(t('courseOfferings.registrations.empty'))}
        hasNextPage={!!hasNextPage}
        isFetchingNextPage={isFetchingNextPage}
        onLoadMore={() => void fetchNextPage()}
      >
        <ListContainer label={t('courseOfferings.registrations.listTitle')}>
          {registrations.map((registration) => {
            const workflowHref = resolveGatedAppNavigationHref(
              registration,
              { scope: 'org', organizationId, userId: registration.userId },
              hasPermission,
            );
            const canPromoteRow =
              canWrite &&
              registration.status === 'WAITLIST' &&
              hasFreePlaceFor(registration, summary);
            const canRemoveRow =
              canWrite &&
              (registration.status === 'PENDING' ||
                registration.status === 'PARTICIPANT' ||
                registration.status === 'WAITLIST');
            const hasActions = workflowHref != null || canPromoteRow || canRemoveRow;

            return (
              <ListRow
                key={registration.id}
                href={getOrganizationCourseRegistrationDetailPath(
                  organizationId,
                  offeringId,
                  registration.id,
                )}
                tone={registration.status === 'PENDING' ? 'warning' : 'default'}
                title={registration.userDisplayName}
                subtitle={t('courseOfferings.registrations.registeredAt', {
                  date: formatDateTime(registration.registeredAt),
                })}
                badges={<CourseRegistrationBadges registration={registration} />}
                actions={
                  hasActions ? (
                    <>
                      {workflowHref ? (
                        <AppLinkListAction href={workflowHref} link={registration} />
                      ) : null}
                      {canPromoteRow ? (
                        <IconButton
                          variant="action"
                          icon={<ArrowUpToLine />}
                          aria-label={t('courseOfferings.registrations.promote')}
                          title={t('courseOfferings.registrations.promote')}
                          disabled={promote.isPending}
                          onClick={() => setPromoteTarget(registration)}
                        />
                      ) : null}
                      {canRemoveRow ? (
                        <IconButton
                          variant="action"
                          icon={<UserMinus />}
                          aria-label={t('courseOfferings.registrations.remove')}
                          title={t('courseOfferings.registrations.remove')}
                          onClick={() => setRemoveTarget(registration)}
                        />
                      ) : null}
                    </>
                  ) : undefined
                }
              />
            );
          })}
        </ListContainer>
      </PaginatedListView>

      <ConfirmDialog
        isOpen={promoteTarget != null}
        onClose={() => setPromoteTarget(null)}
        onConfirm={() => void promoteRegistration()}
        title={t('courseOfferings.registrations.promoteTitle')}
        message={t('courseOfferings.registrations.promoteConfirm', {
          name: promoteTarget?.userDisplayName ?? '',
        })}
        confirmText={t('courseOfferings.registrations.promote')}
        variant="info"
        isLoading={promote.isPending}
      />

      {removeTarget ? (
        <CourseRegistrationRemoveModal
          organizationId={organizationId}
          offeringId={offeringId}
          registration={removeTarget}
          onClose={() => setRemoveTarget(null)}
        />
      ) : null}
    </div>
  );
}
