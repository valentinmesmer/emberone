/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { CourseOfferingRegistrationDetailView } from '@/components/organization/course-offerings/CourseOfferingRegistrationDetailView';
import { LoadingState } from '@/components/ui/loading';
import { ErrorState, NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { getOrganizationUserSectionPath } from '@/lib/navigation/userManagementSections';
import { useOrganizationUserCourseRegistrations } from '@/lib/store';

export interface OrgUserCourseRegistrationDetailViewProps {
  organizationId: number;
  userId: number;
  registrationId: number;
}

/** Resolves the offering of a user's registration, then shows it within the user's context. */
export function OrgUserCourseRegistrationDetailView({
  organizationId,
  userId,
  registrationId,
}: OrgUserCourseRegistrationDetailViewProps) {
  const t = useTranslation();
  const {
    data: registrations = [],
    isPending,
    isError,
  } = useOrganizationUserCourseRegistrations({
    organizationId,
    userId,
    listQuery: {},
    fetchAll: true,
  });

  if (isPending) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (isError) {
    return <ErrorState message={t('courseOfferings.list.loadFailed')} />;
  }

  const registration = registrations.find((item) => item.id === registrationId);
  if (!registration) {
    return <NotFoundState message={t('courseOfferings.registrations.notFound')} />;
  }

  return (
    <CourseOfferingRegistrationDetailView
      organizationId={registration.organizationId}
      offeringId={registration.offeringId}
      registrationId={registration.id}
      backHref={getOrganizationUserSectionPath(organizationId, userId, 'course-registrations')}
      allowActions={false}
    />
  );
}
