/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { CourseOfferingAudienceGroupDetailView } from '@/components/organization/course-offerings/CourseOfferingAudienceGroupDetailView';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState, NoticeCard } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { useOrganizationById } from '@/lib/store';
import { PermissionTypeEnum } from '@/types/permission';

export default function OrganizationCourseAudienceGroupDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const { hasPermission, isLoading: permLoading } = usePermissions();

  const orgId = params?.id as string;
  const offeringIdParam = params?.offeringId as string;
  const audienceIdParam = params?.audienceId as string;
  const orgIdNum = orgId ? parseInt(orgId, 10) : null;
  const offeringIdNum = offeringIdParam ? parseInt(offeringIdParam, 10) : null;
  const audienceIdNum = audienceIdParam ? parseInt(audienceIdParam, 10) : null;

  const canRead = Boolean(
    orgIdNum != null &&
      offeringIdNum != null &&
      !Number.isNaN(offeringIdNum) &&
      audienceIdNum != null &&
      !Number.isNaN(audienceIdNum) &&
      hasPermission(orgIdNum, PermissionTypeEnum.COURSE_OFFERING_READ),
  );

  const { data: organization, isLoading: orgLoading } = useOrganizationById(
    orgIdNum && !permLoading && canRead ? orgIdNum : null,
  );

  const loading = permLoading || orgLoading;

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!permLoading && !canRead) {
    return <NoticeCard>{t('organizationManagement.errors.noPermission')}</NoticeCard>;
  }

  if (
    !organization ||
    !orgIdNum ||
    offeringIdNum == null ||
    Number.isNaN(offeringIdNum) ||
    audienceIdNum == null ||
    Number.isNaN(audienceIdNum)
  ) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  return (
    <CourseOfferingAudienceGroupDetailView
      organizationId={orgIdNum}
      offeringId={offeringIdNum}
      audienceId={audienceIdNum}
    />
  );
}
