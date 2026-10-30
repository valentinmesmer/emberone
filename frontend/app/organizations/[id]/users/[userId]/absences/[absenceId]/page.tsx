/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { OrgUserAbsenceDetailView } from '@/components/organization/user-management/OrgUserAbsenceDetailView';
import { UserManagementSectionGate } from '@/components/organization/user-management/UserManagementSectionGate';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

export default function OrgUserAbsenceDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const orgId = params?.id ? parseInt(params.id as string, 10) : null;
  const userId = params?.userId ? parseInt(params.userId as string, 10) : null;
  const absenceId = params?.absenceId ? parseInt(params.absenceId as string, 10) : null;

  if (
    !orgId ||
    !userId ||
    !absenceId ||
    Number.isNaN(orgId) ||
    Number.isNaN(userId) ||
    Number.isNaN(absenceId)
  ) {
    return <NotFoundState message={t('myArea.absence.error.notFound')} />;
  }

  return (
    <UserManagementSectionGate organizationId={orgId} sectionId="absences">
      {() => (
        <OrgUserAbsenceDetailView organizationId={orgId} userId={userId} absenceId={absenceId} />
      )}
    </UserManagementSectionGate>
  );
}
