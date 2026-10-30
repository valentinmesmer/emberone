/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { Suspense } from 'react';
import { OrganizationGroupDetailPanel } from '@/components/organization/OrganizationGroupDetailPanel';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState, NoticeCard } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { useOrganizationById } from '@/lib/store';
import { PermissionTypeEnum } from '@/types/permission';

export default function OrganizationGroupDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const { hasPermission, isLoading: permLoading } = usePermissions();
  const orgId = Number.parseInt(String(params?.id ?? ''), 10);
  const groupId = Number.parseInt(String(params?.groupId ?? ''), 10);
  const orgIdValid = Number.isFinite(orgId) && orgId > 0;
  const groupIdValid = Number.isFinite(groupId) && groupId > 0;

  const canAccessGroups =
    orgIdValid && hasPermission(orgId, PermissionTypeEnum.ORGANIZATION_GROUP_READ);

  const { data: organization, isLoading: orgLoading } = useOrganizationById(
    orgIdValid && !permLoading ? orgId : null,
  );

  const loading = permLoading || orgLoading;

  if (!orgIdValid || !groupIdValid) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!permLoading && !canAccessGroups) {
    return <NoticeCard>{t('organizationManagement.errors.noPermission')}</NoticeCard>;
  }

  if (!organization) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  return (
    <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
      <OrganizationGroupDetailPanel organizationId={organization.id} groupId={groupId} />
    </Suspense>
  );
}
