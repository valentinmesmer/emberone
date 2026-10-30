/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { useMembershipHistory } from '@/lib/store';
import { formatDateShort } from '@/lib/utils/date';
import type { GroupMembershipPeriodDto } from '@/types/organization';

function formatPeriod(startDate: string, endDate: string | null): string {
  return `${formatDateShort(startDate)} - ${formatDateShort(endDate)}`;
}

/** Active and past organization and group memberships of the signed-in user. */
export function OrganizationMembershipsCard() {
  const t = useTranslation();
  const { data, isLoading } = useMembershipHistory();

  const organizations = useMemo(() => data?.organizations ?? [], [data]);
  const groups = useMemo(() => data?.groups ?? [], [data]);
  const organizationNameById = useMemo(
    () => new Map(organizations.map((item) => [item.organizationId, item.organizationName])),
    [organizations],
  );
  const multipleOrganizations = new Set(organizations.map((item) => item.organizationId)).size > 1;

  const groupSubtitle = (group: GroupMembershipPeriodDto) => (
    <>
      <span className="block">
        {[
          multipleOrganizations ? organizationNameById.get(group.organizationId) : null,
          t(`userManagement.groupMembership.types.${group.membershipType}`),
        ]
          .filter(Boolean)
          .join(' · ')}
      </span>
      <span className="block">
        {group.active
          ? t('ranks.myArea.since', { date: formatDateShort(group.startDate) })
          : formatPeriod(group.startDate, group.endDate)}
      </span>
    </>
  );

  return (
    <Card className="p-3 sm:p-4">
      <h2 className="text-lg font-semibold text-[var(--text-primary)]">
        {t('myArea.masterData.membershipsOverview')}
      </h2>

      <div className="mt-4 space-y-4 border-t border-[var(--border-color)] pt-4">
        {isLoading ? (
          <LoadingState variant="embedded" size="sm" />
        ) : organizations.length === 0 ? (
          <EmptyStateCard>{t('myArea.sections.organizationsEmpty')}</EmptyStateCard>
        ) : (
          <>
            <section aria-label={t('myArea.masterData.membershipsOrganizations')}>
              <h3 className="mb-1.5 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
                {t('myArea.masterData.membershipsOrganizations')}
              </h3>
              <ListContainer label={t('myArea.masterData.membershipsOrganizations')}>
                {organizations.map((item) => (
                  <ListRow
                    key={`${item.organizationId}-${item.startDate}`}
                    compact={!item.active}
                    accent={item.active}
                    tone={item.active ? 'active' : undefined}
                    selected={item.active}
                    title={item.organizationName}
                    subtitle={
                      item.active
                        ? t('ranks.myArea.since', { date: formatDateShort(item.startDate) })
                        : formatPeriod(item.startDate, item.endDate)
                    }
                    badges={
                      item.active ? (
                        item.main ? (
                          <Badge tone="accent">{t('myArea.fields.mainOrganization')}</Badge>
                        ) : null
                      ) : (
                        <Badge tone="neutral">{t('myArea.masterData.membershipEnded')}</Badge>
                      )
                    }
                  />
                ))}
              </ListContainer>
            </section>

            {groups.length > 0 ? (
              <section aria-label={t('myArea.masterData.membershipsGroups')}>
                <h3 className="mb-1.5 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
                  {t('myArea.masterData.membershipsGroups')}
                </h3>
                <ListContainer label={t('myArea.masterData.membershipsGroups')}>
                  {groups.map((item) => (
                    <ListRow
                      key={`${item.organizationGroupId}-${item.startDate}`}
                      compact={!item.active}
                      accent={item.active}
                      tone={item.active ? 'active' : undefined}
                      selected={item.active}
                      title={item.organizationGroupName}
                      subtitle={groupSubtitle(item)}
                      badges={
                        item.active ? null : (
                          <Badge tone="neutral">{t('myArea.masterData.membershipEnded')}</Badge>
                        )
                      }
                    />
                  ))}
                </ListContainer>
              </section>
            ) : null}
          </>
        )}
      </div>
    </Card>
  );
}
