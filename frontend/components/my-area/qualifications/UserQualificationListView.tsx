/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { Badge } from '@/components/ui/badge';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { useTranslation } from '@/hooks/useTranslation';
import { getMyAreaQualificationDetailPath } from '@/lib/my-area/myAreaSections';
import {
  type AggregatedQualificationItem,
  aggregateSelfQualificationStatuses,
  groupSelfQualificationListSections,
  QUALIFICATION_LIST_SECTION_TITLE_KEY,
  qualificationListBadgeLabel,
  type SelfQualificationListSectionId,
} from '@/lib/qualifications/aggregateQualifications';
import { useMyOrganizations, useSelfQualificationStatus } from '@/lib/store';

function shouldShowStatusBadge(
  sectionId: SelfQualificationListSectionId,
  item: AggregatedQualificationItem,
): boolean {
  if (sectionId === 'prerequisitesMissing') {
    return false;
  }
  if (item.orgDecisionProgressRatio != null) {
    return true;
  }
  if (sectionId === 'valid') {
    return false;
  }
  if (item.aggregateStatus === 'ORG_DECISION_REVOKED') {
    return true;
  }
  return false;
}

function QualificationListSection({
  title,
  items,
  sectionId,
  t,
}: {
  title: string;
  items: AggregatedQualificationItem[];
  sectionId: SelfQualificationListSectionId;
  t: (key: string, params?: Record<string, string | number>) => string;
}) {
  return (
    <section className="w-full min-w-0">
      <h2 className="mb-2 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
        {title}
      </h2>
      <ListContainer label={title}>
        {items.map((item) => {
          const showBadge = shouldShowStatusBadge(sectionId, item);
          return (
            <ListRow
              key={item.tenantQualificationDefinitionId}
              href={getMyAreaQualificationDetailPath(item.tenantQualificationDefinitionId)}
              tone={item.rowTone}
              compact
              title={item.name}
              badges={
                showBadge ? (
                  <Badge tone={item.badgeTone}>{qualificationListBadgeLabel(item, t)}</Badge>
                ) : undefined
              }
            />
          );
        })}
      </ListContainer>
    </section>
  );
}

export function UserQualificationListView() {
  const t = useTranslation();
  const { data: statuses = [], isLoading: statusLoading } = useSelfQualificationStatus();
  const { data: memberships = [], isLoading: orgsLoading } = useMyOrganizations();

  const organizationNameById = useMemo(
    () =>
      new Map(
        memberships
          .map((membership) => membership.organization)
          .filter(
            (organization): organization is NonNullable<typeof organization> =>
              organization != null,
          )
          .map((organization) => [organization.id, organization.name] as const),
      ),
    [memberships],
  );

  const sections = useMemo(() => {
    const items = aggregateSelfQualificationStatuses(statuses, organizationNameById);
    return groupSelfQualificationListSections(items);
  }, [organizationNameById, statuses]);

  const isLoading = statusLoading || orgsLoading;

  return (
    <>
      <PageHeader
        title={t('qualifications.title')}
        description={t('qualifications.subtitle')}
        size="compact"
      />

      {isLoading ? (
        <LoadingState variant="embedded" />
      ) : sections.length === 0 ? (
        <EmptyStateCard>{t('qualifications.empty')}</EmptyStateCard>
      ) : (
        <div className="flex w-full min-w-0 flex-col gap-5">
          {sections.map((section) => (
            <QualificationListSection
              key={section.id}
              sectionId={section.id}
              title={t(QUALIFICATION_LIST_SECTION_TITLE_KEY[section.id])}
              items={section.items}
              t={t}
            />
          ))}
        </div>
      )}
    </>
  );
}
