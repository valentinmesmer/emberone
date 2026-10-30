/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { ManagementTargetActions } from '@/components/management/ManagementTargetActions';
import { Card } from '@/components/ui/card';
import { DetailCardHeader } from '@/components/ui/detail-card-header';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { formatManagementUntil } from '@/lib/management/workspaceOrganizations';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { useManagementTargets, useMyManagementActivations } from '@/lib/store';
import { PermissionTypeEnum } from '@/types/permission';

export default function OrganizationManagementTargetPage() {
  const t = useTranslation();
  const params = useParams();
  const { hasPermission, isLoading: permLoading } = usePermissions();
  const orgIdNum = Number.parseInt(String(params?.id ?? ''), 10);
  const targetIdNum = Number.parseInt(String(params?.targetId ?? ''), 10);
  const targets = useManagementTargets();
  const activations = useMyManagementActivations();

  if (permLoading || targets.isPending || activations.isPending) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (
    !Number.isFinite(orgIdNum) ||
    !hasPermission(orgIdNum, PermissionTypeEnum.MANAGEMENT_ACCESS)
  ) {
    return <NoticeCard>{t('organizationManagement.errors.noPermission')}</NoticeCard>;
  }

  const target = (targets.data ?? []).find(
    (entry) => entry.managingOrganizationId === orgIdNum && entry.organizationId === targetIdNum,
  );
  if (!target) {
    return <NoticeCard>{t('management.detail.notFound')}</NoticeCard>;
  }

  const nowMs = Date.now();
  const open = (activations.data ?? []).find(
    (activation) =>
      activation.targetOrganizationId === target.organizationId &&
      Date.parse(activation.validUntil) > nowMs,
  );

  return (
    <div className="space-y-3">
      <Card className="p-3 sm:p-4">
        <DetailCardHeader
          title={target.organizationName}
          subtitle={
            open ? (
              <span className="text-[var(--status-success-accent)]">
                {t('management.detail.activeStatus')}
              </span>
            ) : (
              t('management.detail.inactive')
            )
          }
        />
        <dl className="grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
          <div className="col-span-2">
            <dt className="text-xs text-[var(--text-secondary)]">
              {t('management.detail.managingLabel')}
            </dt>
            <dd>{target.managingOrganizationName}</dd>
          </div>
          {open ? (
            <>
              <div>
                <dt className="text-xs text-[var(--text-secondary)]">
                  {t('management.detail.startedLabel')}
                </dt>
                <dd>
                  {t('management.detail.startedAt', {
                    time: formatManagementUntil(open.activatedAt),
                  })}
                </dd>
              </div>
              <div>
                <dt className="text-xs text-[var(--text-secondary)]">
                  {t('management.detail.endsLabel')}
                </dt>
                <dd>
                  {t('management.detail.endsAt', {
                    time: formatManagementUntil(open.validUntil),
                  })}
                </dd>
              </div>

              <div className="col-span-2">
                <dt className="text-xs text-[var(--text-secondary)]">
                  {t('management.detail.reasonLabel')}
                </dt>
                <dd className="break-words">{open.reason}</dd>
              </div>
            </>
          ) : null}
        </dl>
      </Card>
      <ManagementTargetActions target={target} activation={open} />
    </div>
  );
}
