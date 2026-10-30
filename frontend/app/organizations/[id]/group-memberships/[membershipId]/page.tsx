/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams, useSearchParams } from 'next/navigation';
import { Suspense } from 'react';
import { GroupMembershipDetailView } from '@/components/organization/GroupMembershipDetailView';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { parseOrganizationGroupMembershipDetailReferrer } from '@/lib/utils/organization';

function GroupMembershipDetailPageContent() {
  const t = useTranslation();
  const params = useParams();
  const searchParams = useSearchParams();

  const orgId = params?.id ? parseInt(params.id as string, 10) : null;
  const membershipId = params?.membershipId ? parseInt(params.membershipId as string, 10) : null;

  if (!orgId || !membershipId || Number.isNaN(orgId) || Number.isNaN(membershipId)) {
    return <NotFoundState message={t('userManagement.errors.notFound')} />;
  }

  const referrer = parseOrganizationGroupMembershipDetailReferrer(searchParams);

  return (
    <GroupMembershipDetailView
      organizationId={orgId}
      membershipId={membershipId}
      referrer={referrer}
    />
  );
}

export default function OrganizationGroupMembershipDetailPage() {
  return (
    <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
      <GroupMembershipDetailPageContent />
    </Suspense>
  );
}
