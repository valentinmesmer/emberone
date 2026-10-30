/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { Suspense } from 'react';
import { OrganizationCourseRegistrationListView } from '@/components/organization/course-registrations/OrganizationCourseRegistrationListView';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { PermissionTypeEnum } from '@/types/permission';

export default function OrganizationCourseRegistrationsPage() {
  const t = useTranslation();
  const params = useParams();
  const { hasPermission, isLoading: permLoading } = usePermissions();
  const orgId = params?.id as string;
  const orgIdNum = orgId ? parseInt(orgId, 10) : null;

  if (permLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  const canRead = Boolean(
    orgIdNum != null && hasPermission(orgIdNum, PermissionTypeEnum.TRAINING_COURSE_READ),
  );

  if (!canRead || orgIdNum == null) {
    return <NoticeCard>{t('organizationManagement.errors.noPermission')}</NoticeCard>;
  }

  return (
    <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
      <OrganizationCourseRegistrationListView organizationId={orgIdNum} />
    </Suspense>
  );
}
