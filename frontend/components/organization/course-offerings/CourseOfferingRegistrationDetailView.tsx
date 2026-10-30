/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ArrowUpCircle, UserMinus } from 'lucide-react';
import { useLayoutEffect, useMemo, useState } from 'react';
import { CourseRegistrationBadges } from '@/components/organization/course-offerings/CourseRegistrationBadges';
import { CourseRegistrationRemoveModal } from '@/components/organization/course-offerings/CourseRegistrationRemoveModal';
import { ActionRowIcon, ActionsSection } from '@/components/ui/actions-section';
import { AppLinkNavigateButton } from '@/components/ui/app-link-navigate-button';
import { Card } from '@/components/ui/card';
import { DetailCardHeader } from '@/components/ui/detail-card-header';
import { FieldDisplay } from '@/components/ui/field-display';
import { ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { ErrorState, NotFoundState } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useSetBackHref } from '@/lib/navigation/backHrefOverride';
import { usePermissions } from '@/lib/permission/PermissionContext';
import {
  useOfferingRegistration,
  useOrganizationCourseOffering,
  usePromoteCourseRegistration,
} from '@/lib/store';
import { getOrganizationCourseRegistrationsPath } from '@/lib/utils/courseOffering';
import { formatDateTime } from '@/lib/utils/date';
import { resolveGatedAppNavigationHref } from '@/lib/utils/sourceLink';
import { PermissionTypeEnum } from '@/types/permission';

interface Props {
  organizationId: number;
  offeringId: number;
  registrationId: number;
  /** Where "back" leads; defaults to the offering's registration list. */
  backHref?: string;
  /** Promote and remove are offered only where the offering is managed. */
  allowActions?: boolean;
}

/** One registration: facts, the registration request, and the organization's decisions. */
export function CourseOfferingRegistrationDetailView({
  organizationId,
  offeringId,
  registrationId,
  backHref,
  allowActions = true,
}: Props) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const { hasPermission } = usePermissions();
  const setBackHref = useSetBackHref();

  const canRead = hasPermission(organizationId, PermissionTypeEnum.COURSE_OFFERING_READ);
  const canWrite = hasPermission(organizationId, PermissionTypeEnum.COURSE_OFFERING_WRITE);

  const listPath = useMemo(
    () => backHref ?? getOrganizationCourseRegistrationsPath(organizationId, offeringId),
    [backHref, organizationId, offeringId],
  );

  useLayoutEffect(() => {
    setBackHref(listPath);
    return () => setBackHref(null);
  }, [listPath, setBackHref]);

  const { data: offering } = useOrganizationCourseOffering(
    canRead ? organizationId : null,
    offeringId,
    canRead,
  );
  const {
    data: registration,
    isLoading,
    isError,
  } = useOfferingRegistration(organizationId, offeringId, registrationId, canRead);
  const promote = usePromoteCourseRegistration(organizationId, offeringId);
  const [removeOpen, setRemoveOpen] = useState(false);

  if (isLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (isError) {
    return <ErrorState message={t('courseOfferings.list.loadFailed')} />;
  }

  if (!registration) {
    return <NotFoundState message={t('courseOfferings.registrations.notFound')} />;
  }

  const canPromote = allowActions && canWrite && registration.status === 'WAITLIST';
  const canRemove =
    allowActions &&
    canWrite &&
    (registration.status === 'PENDING' ||
      registration.status === 'PARTICIPANT' ||
      registration.status === 'WAITLIST');

  const workflowHref = resolveGatedAppNavigationHref(
    registration,
    { scope: 'org', organizationId, userId: registration.userId },
    hasPermission,
  );

  const promoteRegistration = async () => {
    try {
      await promote.mutateAsync(registration.id);
      showSuccess(t('courseOfferings.registrations.promoteSuccess'));
    } catch {
      showError(t('courseOfferings.registrations.promoteFailed'));
    }
  };

  return (
    <div className="space-y-3">
      <Card className="p-3 sm:p-4">
        <DetailCardHeader
          className="mb-2"
          eyebrow={offering?.trainingCourseTypeName}
          title={registration.userDisplayName}
          badges={<CourseRegistrationBadges registration={registration} />}
        />

        <div className="grid grid-cols-1 gap-3 border-t border-[var(--border-color)] pt-4 text-sm sm:grid-cols-2">
          <FieldDisplay
            label={t('courseOfferings.registrations.fields.registeredAt')}
            value={formatDateTime(registration.registeredAt)}
          />
          {registration.approvedAt ? (
            <FieldDisplay
              label={t('courseOfferings.registrations.fields.approvedAt')}
              value={formatDateTime(registration.approvedAt)}
            />
          ) : null}
          {registration.endedAt ? (
            <FieldDisplay
              label={t('courseOfferings.registrations.fields.endedAt')}
              value={formatDateTime(registration.endedAt)}
            />
          ) : null}
          {registration.endReason ? (
            <FieldDisplay
              label={t('courseOfferings.registrations.fields.endReason')}
              value={registration.endReason}
              multiline
              className="sm:col-span-2"
            />
          ) : null}
        </div>

        {workflowHref ? (
          <div className="mt-4 border-t border-[var(--border-color)] pt-4">
            <AppLinkNavigateButton href={workflowHref} link={registration} />
          </div>
        ) : null}
      </Card>

      {canPromote || canRemove ? (
        <ActionsSection title={t('courseOfferings.actions.title')}>
          {canPromote ? (
            <ListRow
              leading={<ActionRowIcon icon={ArrowUpCircle} />}
              title={t('courseOfferings.registrations.promote')}
              subtitle={t('courseOfferings.registrations.promoteHint')}
              onClick={() => void promoteRegistration()}
            />
          ) : null}
          {canRemove ? (
            <ListRow
              leading={<ActionRowIcon icon={UserMinus} />}
              title={t('courseOfferings.registrations.remove')}
              subtitle={t('courseOfferings.registrations.removeHint')}
              onClick={() => setRemoveOpen(true)}
            />
          ) : null}
        </ActionsSection>
      ) : null}

      {removeOpen ? (
        <CourseRegistrationRemoveModal
          organizationId={organizationId}
          offeringId={offeringId}
          registration={registration}
          onClose={() => setRemoveOpen(false)}
        />
      ) : null}
    </div>
  );
}
