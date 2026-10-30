/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { Suspense } from 'react';
import { OrganizationRanksOverview } from '@/components/organization/OrganizationRanksOverview';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState, NoticeCard } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { useTranslation } from '@/hooks/useTranslation';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { useOrganizationById } from '@/lib/store';
import { PermissionTypeEnum } from '@/types/permission';

export default function OrganizationRanksPage() {
  const t = useTranslation();
  const params = useParams();
  const { hasPermission, isLoading: permLoading } = usePermissions();
  const orgIdNum = params?.id ? Number.parseInt(params.id as string, 10) : null;

  const canRead = Boolean(
    orgIdNum != null && hasPermission(orgIdNum, PermissionTypeEnum.RANK_READ),
  );

  const { data: organization, isLoading: orgLoading } = useOrganizationById(
    orgIdNum && !permLoading && canRead ? orgIdNum : null,
  );

  if (permLoading || orgLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!canRead) {
    return <NoticeCard>{t('organizationManagement.errors.noPermission')}</NoticeCard>;
  }

  if (!organization || !orgIdNum) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  return (
    <>
      <PageHeader
        title={t('organization.hub.tileLabels.ranks')}
        description={organization.name}
        size="compact"
      />
      <div className="mt-1 w-full sm:mt-0">
        <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
          <OrganizationRanksOverview organizationId={organization.id} />
        </Suspense>
      </div>
    </>
  );
}
