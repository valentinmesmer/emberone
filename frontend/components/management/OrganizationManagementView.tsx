/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { formatManagementUntil } from '@/lib/management/workspaceOrganizations';
import { useManagementTargets, useMyManagementActivations } from '@/lib/store';

interface OrganizationManagementViewProps {
  managingOrganizationId: number;
}

/** Organizations this managing organization may open, with the open activations marked. */
export function OrganizationManagementView({
  managingOrganizationId,
}: OrganizationManagementViewProps) {
  const t = useTranslation();
  const targets = useManagementTargets();
  const activations = useMyManagementActivations();

  if (targets.isPending || activations.isPending) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  const rows = (targets.data ?? []).filter(
    (target) => target.managingOrganizationId === managingOrganizationId,
  );
  const nowMs = Date.now();
  const openByTarget = new Map(
    (activations.data ?? [])
      .filter((activation) => Date.parse(activation.validUntil) > nowMs)
      .map((activation) => [activation.targetOrganizationId, activation]),
  );

  if (rows.length === 0) return <EmptyStateCard>{t('management.empty')}</EmptyStateCard>;

  return (
    <ListContainer>
      {rows.map((target) => {
        const open = openByTarget.get(target.organizationId);
        return (
          <ListRow
            key={target.organizationId}
            title={target.organizationName}
            href={`/organizations/${managingOrganizationId}/management/${target.organizationId}`}
            tone={open ? 'success' : 'default'}
            subtitle={
              open ? (
                <>
                  <span className="text-[var(--status-success-accent)]">
                    {t('management.row.active')}
                  </span>{' '}
                  {t('management.row.activeUntil', {
                    time: formatManagementUntil(open.validUntil),
                  })}
                </>
              ) : (
                t('management.row.inactive')
              )
            }
          />
        );
      })}
    </ListContainer>
  );
}
