/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { Suspense, useMemo } from 'react';
import { OrganizationUsersManagement } from '@/components/organization/OrganizationUsersManagement';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState, NoticeCard } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { canAccessUserManagement } from '@/lib/navigation/userManagementSections';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { useOrganizationById } from '@/lib/store';

export default function OrganizationUsersPage() {
  const t = useTranslation();
  const params = useParams();
  const { hasPermission, isLoading: permLoading } = usePermissions();
  const orgId = params?.id as string;

  const orgIdNum = useMemo(() => Number.parseInt(orgId ?? '', 10), [orgId]);
  const orgIdValid = Number.isFinite(orgIdNum) && orgIdNum > 0;

  const canAccess = orgIdValid && canAccessUserManagement(orgIdNum, hasPermission);

  const fetchEnabled = orgIdValid && !permLoading && canAccess;

  const {
    data: organization,
    isLoading: orgLoading,
    isError: orgError,
  } = useOrganizationById(fetchEnabled ? orgIdNum : null);

  const loading = permLoading || (fetchEnabled && orgLoading);

  if (!orgIdValid) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!permLoading && !canAccess) {
    return <NoticeCard>{t('organizationManagement.errors.noPermission')}</NoticeCard>;
  }

  if (orgError || !organization) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  return (
    <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
      <OrganizationUsersManagement
        organizationId={organization.id}
        organizationName={organization.name}
      />
    </Suspense>
  );
}
