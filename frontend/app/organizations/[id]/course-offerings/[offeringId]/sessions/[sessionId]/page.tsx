/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { CourseOfferingSessionDetailView } from '@/components/organization/course-offerings/CourseOfferingSessionDetailView';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState, NoticeCard } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { useOrganizationById } from '@/lib/store';
import { PermissionTypeEnum } from '@/types/permission';

export default function OrganizationCourseSessionDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const { hasPermission, isLoading: permLoading } = usePermissions();

  const orgId = params?.id as string;
  const offeringIdParam = params?.offeringId as string;
  const sessionIdParam = params?.sessionId as string;
  const orgIdNum = orgId ? parseInt(orgId, 10) : null;
  const offeringIdNum = offeringIdParam ? parseInt(offeringIdParam, 10) : null;
  const sessionIdNum = sessionIdParam ? parseInt(sessionIdParam, 10) : null;

  const canRead = Boolean(
    orgIdNum != null &&
      offeringIdNum != null &&
      !Number.isNaN(offeringIdNum) &&
      sessionIdNum != null &&
      !Number.isNaN(sessionIdNum) &&
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
    sessionIdNum == null ||
    Number.isNaN(sessionIdNum)
  ) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  return (
    <CourseOfferingSessionDetailView
      organizationId={orgIdNum}
      offeringId={offeringIdNum}
      sessionId={sessionIdNum}
    />
  );
}
