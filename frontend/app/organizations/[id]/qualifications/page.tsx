/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { Suspense } from 'react';
import { OrganizationQualificationsOverview } from '@/components/organization/OrganizationQualificationsOverview';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState, NoticeCard } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { useTranslation } from '@/hooks/useTranslation';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { useOrganizationById } from '@/lib/store';
import { PermissionTypeEnum } from '@/types/permission';

export default function OrganizationQualificationsPage() {
  const t = useTranslation();
  const params = useParams();
  const { hasPermission, isLoading: permLoading } = usePermissions();
  const orgId = params?.id as string;
  const orgIdNum = orgId ? parseInt(orgId, 10) : null;

  const canRead = Boolean(
    orgIdNum != null && hasPermission(orgIdNum, PermissionTypeEnum.QUALIFICATION_READ),
  );

  const { data: organization, isLoading: orgLoading } = useOrganizationById(
    orgIdNum && !permLoading && canRead ? orgIdNum : null,
  );

  const loading = permLoading || orgLoading;

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!permLoading && !canRead) {
    return <NoticeCard>{t('organizationManagement.errors.noPermission')}</NoticeCard>;
  }

  if (!organization || !orgIdNum) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  return (
    <>
      <PageHeader
        title={t('organization.hub.tileLabels.qualifications')}
        description={organization.name}
        size="compact"
      />
      <div className="mt-1 w-full sm:mt-0">
        <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
          <OrganizationQualificationsOverview organizationId={organization.id} />
        </Suspense>
      </div>
    </>
  );
}
