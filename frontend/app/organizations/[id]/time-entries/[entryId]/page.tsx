/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { OrganizationTimeEntryDetailView } from '@/components/organization/OrganizationTimeEntryDetailView';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState, NoticeCard } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { useOrganizationById } from '@/lib/store';
import { PermissionTypeEnum } from '@/types/permission';

export default function OrganizationTimeEntryDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const { hasPermission, isLoading: permLoading } = usePermissions();

  const orgId = params?.id as string;
  const entryIdParam = params?.entryId as string;
  const orgIdNum = orgId ? parseInt(orgId, 10) : null;
  const entryIdNum = entryIdParam ? parseInt(entryIdParam, 10) : null;

  const canAccess = Boolean(
    orgIdNum != null &&
      !Number.isNaN(orgIdNum) &&
      entryIdNum != null &&
      !Number.isNaN(entryIdNum) &&
      hasPermission(orgIdNum, PermissionTypeEnum.TIME_ENTRY_READ),
  );

  const { data: organization, isLoading: orgLoading } = useOrganizationById(
    orgIdNum && !permLoading && canAccess ? orgIdNum : null,
  );

  const loading = permLoading || orgLoading;

  if (!orgIdNum || !entryIdNum || Number.isNaN(orgIdNum) || Number.isNaN(entryIdNum)) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!permLoading && !canAccess) {
    return <NoticeCard>{t('organizationManagement.errors.noPermission')}</NoticeCard>;
  }

  if (!organization) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  return (
    <OrganizationTimeEntryDetailView
      organizationId={organization.id}
      organizationName={organization.name}
      entryId={entryIdNum}
    />
  );
}
