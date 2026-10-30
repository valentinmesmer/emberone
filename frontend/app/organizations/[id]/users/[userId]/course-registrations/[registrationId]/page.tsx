/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { OrgUserCourseRegistrationDetailView } from '@/components/organization/user-management/OrgUserCourseRegistrationDetailView';
import { UserManagementSectionGate } from '@/components/organization/user-management/UserManagementSectionGate';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

export default function OrgUserCourseRegistrationDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const orgId = params?.id ? parseInt(params.id as string, 10) : null;
  const userId = params?.userId ? parseInt(params.userId as string, 10) : null;
  const registrationId = params?.registrationId
    ? parseInt(params.registrationId as string, 10)
    : null;

  if (
    !orgId ||
    !userId ||
    !registrationId ||
    Number.isNaN(orgId) ||
    Number.isNaN(userId) ||
    Number.isNaN(registrationId)
  ) {
    return <NotFoundState message={t('courseOfferings.registrations.notFound')} />;
  }

  return (
    <UserManagementSectionGate organizationId={orgId} sectionId="course-registrations">
      {() => (
        <OrgUserCourseRegistrationDetailView
          organizationId={orgId}
          userId={userId}
          registrationId={registrationId}
        />
      )}
    </UserManagementSectionGate>
  );
}
