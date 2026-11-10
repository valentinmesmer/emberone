/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useParams } from 'next/navigation';
import { OrganizationTimeEntryDetailView } from '@/components/organization/OrganizationTimeEntryDetailView';
import { UserManagementSectionGate } from '@/components/organization/user-management/UserManagementSectionGate';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { useOrganizationById } from '@/lib/store';

function OrgUserTimeEntryDetailPageContent({
  orgId,
  userId,
  entryId,
}: {
  orgId: number;
  userId: number;
  entryId: number;
}) {
  const t = useTranslation();
  const { data: organization, isLoading } = useOrganizationById(orgId);

  if (isLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!organization) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  return (
    <OrganizationTimeEntryDetailView
      organizationId={organization.id}
      organizationName={organization.name}
      entryId={entryId}
      userId={userId}
    />
  );
}

export default function OrgUserTimeEntryDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const orgId = params?.id ? parseInt(params.id as string, 10) : null;
  const userId = params?.userId ? parseInt(params.userId as string, 10) : null;
  const entryId = params?.entryId ? parseInt(params.entryId as string, 10) : null;

  if (
    !orgId ||
    !userId ||
    !entryId ||
    Number.isNaN(orgId) ||
    Number.isNaN(userId) ||
    Number.isNaN(entryId)
  ) {
    return <NotFoundState message={t('myArea.timeEntries.error.notFound')} />;
  }

  return (
    <UserManagementSectionGate organizationId={orgId} sectionId="time-entries">
      {() => <OrgUserTimeEntryDetailPageContent orgId={orgId} userId={userId} entryId={entryId} />}
    </UserManagementSectionGate>
  );
}
