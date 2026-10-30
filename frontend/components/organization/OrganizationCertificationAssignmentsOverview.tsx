/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { CertificationExpiryDate } from '@/components/my-area/certifications/CertificationExpiryDate';
import { Badge } from '@/components/ui/badge';
import { ListContainer, ListRow } from '@/components/ui/list';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { useTranslation } from '@/hooks/useTranslation';
import type { CertificationStatus } from '@/lib/api/certifications';
import {
  CERTIFICATION_STATUS_BADGE_TONE,
  CERTIFICATION_STATUS_ROW_TONE,
} from '@/lib/certifications/statusPresentation';
import {
  CertificationOverviewStatusParam,
  certificationOverviewListDefinition,
} from '@/lib/list/certificationOverviewList';
import { getOrganizationUserCertificationDetailPath } from '@/lib/navigation/userManagementSections';
import {
  useOrganizationCertificationStatusOverview,
  useOrganizationCertificationStatusOverviewCounts,
  useUrlSyncedCertificationOverviewListState,
} from '@/lib/store';

type OverviewStatus = Extract<
  CertificationStatus,
  'MISSING' | 'EXPIRED' | 'EXPIRING_SOON' | 'VALID'
>;

/** Main list of the certifications page: uses URL-synced list state (page provides Suspense). */
export function OrganizationCertificationAssignmentsOverview({
  organizationId,
}: {
  organizationId: number;
}) {
  const t = useTranslation();
  const listState = useUrlSyncedCertificationOverviewListState();
  const {
    data: overviewRows = [],
    isPending,
    isError,
    error,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useOrganizationCertificationStatusOverview(organizationId, listState.listQuery);
  const { data: counts } = useOrganizationCertificationStatusOverviewCounts(organizationId);

  const selectCounts = useMemo(
    () =>
      counts
        ? {
            status: {
              [CertificationOverviewStatusParam.MISSING]: counts.missing,
              [CertificationOverviewStatusParam.EXPIRED]: counts.expired,
              [CertificationOverviewStatusParam.EXPIRING_SOON]: counts.expiringSoon,
              [CertificationOverviewStatusParam.VALID]: counts.valid,
            },
          }
        : undefined,
    [counts],
  );

  return (
    <PaginatedListView
      definition={certificationOverviewListDefinition}
      state={listState}
      organizationCertificationFilterOptions={{
        scope: 'organizationCertifications',
        organizationId,
      }}
      selectCounts={selectCounts}
      totalItems={overviewRows.length}
      isPending={isPending}
      error={
        isError
          ? error instanceof Error
            ? error.message
            : t('myArea.error.loadFailed')
          : undefined
      }
      emptyState={paginatedListEmptyCard(t('certifications.overview.empty'))}
      hasNextPage={!!hasNextPage}
      isFetchingNextPage={isFetchingNextPage}
      onLoadMore={() => void fetchNextPage()}
    >
      <ListContainer label={t('certifications.overview.title')}>
        {overviewRows.map((row) => (
          <ListRow
            key={`${row.userId}-${row.tenantCertDefinitionId}`}
            href={getOrganizationUserCertificationDetailPath(
              organizationId,
              row.userId,
              row.tenantCertDefinitionId,
            )}
            compact
            tone={CERTIFICATION_STATUS_ROW_TONE[row.status as OverviewStatus]}
            title={row.certDefinitionName}
            subtitle={row.userDisplayName}
            badges={
              <Badge tone={CERTIFICATION_STATUS_BADGE_TONE[row.status as OverviewStatus]}>
                {t(`certifications.status.${row.status}`)}
              </Badge>
            }
            meta={
              row.expiryDate ? (
                <div className="mt-0.5">
                  <CertificationExpiryDate expiryDate={row.expiryDate} />
                </div>
              ) : undefined
            }
          />
        ))}
      </ListContainer>
    </PaginatedListView>
  );
}
