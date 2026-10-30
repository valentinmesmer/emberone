/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { OrganizationManagementView } from '@/components/management/OrganizationManagementView';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { useTranslation } from '@/hooks/useTranslation';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { PermissionTypeEnum } from '@/types/permission';

export default function OrganizationManagementPage() {
  const t = useTranslation();
  const params = useParams();
  const { hasPermission, isLoading: permLoading } = usePermissions();
  const orgIdNum = Number.parseInt(String(params?.id ?? ''), 10);

  if (permLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (
    !Number.isFinite(orgIdNum) ||
    !hasPermission(orgIdNum, PermissionTypeEnum.MANAGEMENT_ACCESS)
  ) {
    return <NoticeCard>{t('organizationManagement.errors.noPermission')}</NoticeCard>;
  }

  return (
    <>
      <PageHeader
        title={t('management.page.title')}
        description={t('management.page.description')}
        size="compact"
      />
      <div className="mt-1 w-full sm:mt-0">
        <OrganizationManagementView managingOrganizationId={orgIdNum} />
      </div>
    </>
  );
}
