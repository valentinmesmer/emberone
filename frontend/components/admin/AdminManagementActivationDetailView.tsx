/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useEffect } from 'react';
import { standardPermissionLabel } from '@/components/admin/standards/standardContentUtils';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { FieldDisplay } from '@/components/ui/field-display';
import { FormLabel } from '@/components/ui/form-label';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { PageContentSection } from '@/components/ui/page-content-section';
import { useTranslation } from '@/hooks/useTranslation';
import { useSetBackHref } from '@/lib/navigation/backHrefOverride';
import { useAdminManagementActivation } from '@/lib/store/queries/adminManagementActivations';
import { getAdminManagementActivationsPath } from '@/lib/utils/adminPaths';
import { formatDateTime } from '@/lib/utils/date';
import { PermissionTypeEnum } from '@/types/permission';

export interface AdminManagementActivationDetailViewProps {
  activationId: number;
}

/** One entry of the activation log, read-only. */
export function AdminManagementActivationDetailView({
  activationId,
}: AdminManagementActivationDetailViewProps) {
  const t = useTranslation();
  const setBackHref = useSetBackHref();
  const { data: activation, isPending, error } = useAdminManagementActivation(activationId);

  useEffect(() => {
    setBackHref(getAdminManagementActivationsPath());
    return () => setBackHref(null);
  }, [setBackHref]);

  if (isPending) {
    return <LoadingState variant="embedded" />;
  }

  if (error || !activation) {
    return <NotFoundState message={t('admin.managementActivations.detail.notFound')} />;
  }

  const labels = (key: string) => t(`admin.managementActivations.detail.${key}`);

  return (
    <PageContentSection>
      <Card className="p-3 sm:p-4 space-y-3">
        <FieldDisplay label={labels('tenant')} value={activation.tenantName} />
        <FieldDisplay
          label={labels('managingOrganization')}
          value={activation.managingOrganizationName}
        />
        <FieldDisplay
          label={labels('targetOrganization')}
          value={activation.targetOrganizationName}
        />
        <FieldDisplay
          label={labels('status')}
          value={t(`admin.managementActivations.status.${activation.status}`)}
        />
        <FieldDisplay label={labels('validFrom')} value={formatDateTime(activation.activatedAt)} />
        <FieldDisplay label={labels('validUntil')} value={formatDateTime(activation.validUntil)} />
        {activation.endedAt && activation.endedBy ? (
          <>
            <FieldDisplay label={labels('endedAt')} value={formatDateTime(activation.endedAt)} />
            <FieldDisplay
              label={labels('endedBy')}
              value={t(`admin.managementActivations.endedBy.${activation.endedBy}`)}
            />
          </>
        ) : null}
        <FieldDisplay label={labels('reason')} value={activation.reason} />
        <div>
          <FormLabel>{labels('permissions')}</FormLabel>
          <div className="flex flex-wrap gap-1.5">
            {activation.permissionTypes.map((permission) => (
              <Badge key={permission} tone="neutral">
                {standardPermissionLabel(t, PermissionTypeEnum[permission])}
              </Badge>
            ))}
          </div>
        </div>
      </Card>
    </PageContentSection>
  );
}
