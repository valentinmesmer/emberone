/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { Suspense } from 'react';
import { GroupMembershipDetailView } from '@/components/organization/GroupMembershipDetailView';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

function OrganizationGroupMembershipDetailPageContent() {
  const t = useTranslation();
  const params = useParams();

  const orgId = params?.id ? Number.parseInt(params.id as string, 10) : null;
  const groupId = params?.groupId ? Number.parseInt(params.groupId as string, 10) : null;
  const membershipId = params?.membershipId
    ? Number.parseInt(params.membershipId as string, 10)
    : null;

  if (
    !orgId ||
    !groupId ||
    !membershipId ||
    Number.isNaN(orgId) ||
    Number.isNaN(groupId) ||
    Number.isNaN(membershipId)
  ) {
    return <NotFoundState message={t('userManagement.errors.notFound')} />;
  }

  return (
    <GroupMembershipDetailView
      organizationId={orgId}
      membershipId={membershipId}
      referrer={{ kind: 'group', groupId }}
    />
  );
}

export default function OrganizationGroupMembershipDetailPage() {
  return (
    <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
      <OrganizationGroupMembershipDetailPageContent />
    </Suspense>
  );
}
