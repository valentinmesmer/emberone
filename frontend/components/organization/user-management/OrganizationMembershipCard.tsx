/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Pencil, Plus } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { DetailCardAction, DetailCardHeader } from '@/components/ui/detail-card-header';
import { FieldDisplay } from '@/components/ui/field-display';
import { ListContainer, ListRow } from '@/components/ui/list';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { formatDateTime, getGroupDisplayName, getMembershipTypeLabel } from '@/lib/utils';
import {
  formatMembershipPeriod,
  partitionGroupMemberships,
  partitionOrganizationMemberships,
  sortMembershipsByRecency,
} from '@/lib/utils/membershipPartition';
import { getOrganizationGroupMembershipDetailPath } from '@/lib/utils/organization';
import type { OrganizationMembershipSectionDto } from '@/types/user';

interface OrganizationMembershipCardProps {
  section: OrganizationMembershipSectionDto;
  userId: number;
  canWrite: boolean;
  showAddGroup: boolean;
  onAddGroupMembership: () => void;
  onEditOrganizationMembership: () => void;
}

export function OrganizationMembershipCard({
  section,
  userId,
  canWrite,
  showAddGroup,
  onAddGroupMembership,
  onEditOrganizationMembership,
}: OrganizationMembershipCardProps) {
  const t = useTranslation();
  const orgId = section.organization.id;
  const orgName = section.organization.name ?? t('common.notAvailable');

  const { active: activeOrgMemberships, history: historyOrgMemberships } =
    partitionOrganizationMemberships(section.organizationMemberships);
  const primaryOrgMembership =
    activeOrgMemberships[0] ?? sortMembershipsByRecency(historyOrgMemberships)[0];
  const isActive = activeOrgMemberships.length > 0;
  const showOrgActions = canWrite && isActive && !!primaryOrgMembership;
  const inLeadershipGroup = section.inLeadershipGroup;
  const canEditOrganizationMembership = showOrgActions && !inLeadershipGroup;
  const groupRows = (section.groups ?? []).flatMap((groupSection) => {
    const { active, history } = partitionGroupMemberships(groupSection.memberships);
    const groupName = getGroupDisplayName(groupSection.group);
    const primary = active[0] ?? sortMembershipsByRecency(history)[0];

    if (!primary) {
      return [];
    }

    const historical = active.length === 0;

    return [
      <ListRow
        key={primary.id}
        href={getOrganizationGroupMembershipDetailPath(orgId, primary.id, {
          kind: 'user',
          userId,
        })}
        title={groupName}
        badges={
          <>
            <Badge tone={historical ? 'neutral' : 'accent'}>
              {getMembershipTypeLabel(primary.membershipType, t)}
            </Badge>
            {historical ? (
              <Badge tone="neutral">{t('userManagement.memberships.historicalTag')}</Badge>
            ) : null}
            {groupSection.group.archived ? (
              <Badge tone="neutral">{t('userManagement.groupMembership.archivedBadge')}</Badge>
            ) : null}
          </>
        }
        meta={
          <span className="text-xs text-[var(--text-secondary)]">
            {formatMembershipPeriod(primary.startDate, primary.endDate)}
          </span>
        }
      />,
    ];
  });

  return (
    <div className="space-y-4">
      {primaryOrgMembership ? (
        <Card className="p-3 sm:p-4">
          <DetailCardHeader
            className="mb-0"
            eyebrow={
              <span className="font-semibold uppercase tracking-wide">
                {t('userManagement.organizationMembership.title')}
              </span>
            }
            title={orgName}
            badges={
              primaryOrgMembership.main || !isActive ? (
                <>
                  {primaryOrgMembership.main ? (
                    <Badge tone="accent">{t('myArea.fields.mainOrganization')}</Badge>
                  ) : null}
                  {!isActive ? (
                    <Badge tone="neutral">{t('userManagement.memberships.historicalTag')}</Badge>
                  ) : null}
                </>
              ) : undefined
            }
            actions={
              canEditOrganizationMembership ? (
                <DetailCardAction
                  label={t('common.edit')}
                  icon={<Pencil className="h-4 w-4" aria-hidden />}
                  onClick={onEditOrganizationMembership}
                />
              ) : undefined
            }
          />

          <div className="mt-4 grid grid-cols-1 gap-3 border-t border-[var(--border-color)] pt-4 text-sm lg:grid-cols-2">
            <FieldDisplay
              label={t('userManagement.organizationMembership.startDate')}
              value={formatDateTime(primaryOrgMembership.startDate)}
            />
            <FieldDisplay
              label={t('userManagement.organizationMembership.endDate')}
              value={
                primaryOrgMembership.endDate
                  ? formatDateTime(primaryOrgMembership.endDate)
                  : undefined
              }
              fallback={t('userManagement.memberships.ongoing')}
            />
          </div>
          {isActive && inLeadershipGroup ? (
            <p className="mt-3 text-sm text-[var(--text-secondary)]">
              {t('userManagement.organizationMembership.blockedByLeadershipHint')}
            </p>
          ) : null}
        </Card>
      ) : null}

      <section className="space-y-2">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <h3 className="text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
            {t('userManagement.memberships.groupsTitle')}
          </h3>
          {showAddGroup ? (
            <Button
              variant="secondary"
              size="xs"
              onClick={onAddGroupMembership}
              fullWidth="responsive"
              className="sm:w-auto"
              icon={<Plus className="h-3.5 w-3.5" aria-hidden />}
            >
              {t('common.add')}
            </Button>
          ) : null}
        </div>
        {groupRows.length > 0 ? (
          <ListContainer label={t('userManagement.memberships.groupsTitle')}>
            {groupRows}
          </ListContainer>
        ) : (
          <EmptyStateCard>{t('userManagement.memberships.noGroups')}</EmptyStateCard>
        )}
      </section>
    </div>
  );
}
