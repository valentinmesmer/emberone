/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { TrainingCourseRecordDetailView } from '@/components/my-area/trainingCourses/TrainingCourseRecordDetailView';
import { UserManagementSectionGate } from '@/components/organization/user-management/UserManagementSectionGate';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

export default function OrgUserTrainingCourseRecordDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const orgId = params?.id ? parseInt(params.id as string, 10) : null;
  const userId = params?.userId ? parseInt(params.userId as string, 10) : null;
  const recordId = params?.recordId ? parseInt(params.recordId as string, 10) : null;

  if (
    !orgId ||
    !userId ||
    !recordId ||
    Number.isNaN(orgId) ||
    Number.isNaN(userId) ||
    Number.isNaN(recordId)
  ) {
    return <NotFoundState message={t('trainingCourse.record.notFound')} />;
  }

  return (
    <UserManagementSectionGate organizationId={orgId} sectionId="training-courses">
      {({ canWrite }) => (
        <TrainingCourseRecordDetailView
          scope="org"
          organizationId={orgId}
          userId={userId}
          recordId={recordId}
          canWrite={canWrite}
        />
      )}
    </UserManagementSectionGate>
  );
}
