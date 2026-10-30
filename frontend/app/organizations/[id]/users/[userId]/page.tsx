/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { UserManagementHubView } from '@/components/organization/user-management/UserManagementHubView';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

export default function UserDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const orgId = params?.id ? parseInt(params.id as string, 10) : null;
  const userId = params?.userId ? parseInt(params.userId as string, 10) : null;

  if (!orgId || !userId || Number.isNaN(orgId) || Number.isNaN(userId)) {
    return <NotFoundState message={t('userManagement.errors.notFound')} />;
  }

  return <UserManagementHubView organizationId={orgId} userId={userId} />;
}
