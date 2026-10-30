/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { OrgUserQualificationDetailView } from '@/components/organization/user-management/OrgUserQualificationDetailView';
import { UserManagementSectionGate } from '@/components/organization/user-management/UserManagementSectionGate';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

export default function OrgUserQualificationDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const orgId = params?.id ? parseInt(params.id as string, 10) : null;
  const userId = params?.userId ? parseInt(params.userId as string, 10) : null;
  const definitionId = params?.definitionId ? parseInt(params.definitionId as string, 10) : null;

  if (
    !orgId ||
    !userId ||
    !definitionId ||
    Number.isNaN(orgId) ||
    Number.isNaN(userId) ||
    Number.isNaN(definitionId)
  ) {
    return <NotFoundState message={t('qualifications.detail.notFound')} />;
  }

  return (
    <UserManagementSectionGate organizationId={orgId} sectionId="qualifications">
      {({ canWrite }) => (
        <OrgUserQualificationDetailView
          organizationId={orgId}
          userId={userId}
          definitionId={definitionId}
          canWrite={canWrite}
        />
      )}
    </UserManagementSectionGate>
  );
}
