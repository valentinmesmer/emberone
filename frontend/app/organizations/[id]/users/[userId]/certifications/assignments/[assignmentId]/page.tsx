/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { OrgUserCertificationAssignmentDetailView } from '@/components/organization/user-management/OrgUserCertificationAssignmentDetailView';
import { UserManagementSectionGate } from '@/components/organization/user-management/UserManagementSectionGate';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

export default function OrgUserCertificationAssignmentDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const orgId = params?.id ? parseInt(params.id as string, 10) : null;
  const userId = params?.userId ? parseInt(params.userId as string, 10) : null;
  const assignmentId = params?.assignmentId ? parseInt(params.assignmentId as string, 10) : null;

  if (
    !orgId ||
    !userId ||
    !assignmentId ||
    Number.isNaN(orgId) ||
    Number.isNaN(userId) ||
    Number.isNaN(assignmentId)
  ) {
    return <NotFoundState message={t('certifications.assign.detail.notFound')} />;
  }

  return (
    <UserManagementSectionGate organizationId={orgId} sectionId="certifications">
      {({ canWrite }) => (
        <OrgUserCertificationAssignmentDetailView
          organizationId={orgId}
          userId={userId}
          assignmentId={assignmentId}
          canWrite={canWrite}
        />
      )}
    </UserManagementSectionGate>
  );
}
