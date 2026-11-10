/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Badge } from '@/components/ui/badge';
import { ListContainer, ListRow, subtitleLines } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { ErrorState } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { useTranslation } from '@/hooks/useTranslation';
import { orgCourseRegistrationListDefinition } from '@/lib/list/courseOfferingList';
import { getOrganizationUserCourseRegistrationDetailPath } from '@/lib/navigation/userManagementSections';
import {
  useOrganizationById,
  useOrganizationCourseRegistrations,
  useOrgCourseRegistrationListState,
} from '@/lib/store';
import {
  courseRegistrationStatusBadgeTone,
  courseRegistrationStatusLabelKey,
} from '@/lib/utils/courseOffering';
import { formatDateShort } from '@/lib/utils/date';

interface Props {
  organizationId: number;
}

export function OrganizationCourseRegistrationListView({ organizationId }: Props) {
  const t = useTranslation();
  const listState = useOrgCourseRegistrationListState();

  const { data: organization, isLoading: orgLoading } = useOrganizationById(organizationId);

  const { data, isPending, isError, hasNextPage, isFetchingNextPage, fetchNextPage } =
    useOrganizationCourseRegistrations({
      organizationId,
      listQuery: listState.listQuery,
    });

  const registrations = data ?? [];

  if (orgLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!organization) {
    return <ErrorState message={t('organization.errors.notFound')} className="mt-4" />;
  }

  return (
    <>
      <PageHeader
        title={t('courseOfferings.registrations.organizationList.title')}
        description={organization.name}
        size="compact"
      />

      <PaginatedListView
        definition={orgCourseRegistrationListDefinition}
        state={listState}
        totalItems={registrations.length}
        isPending={isPending}
        error={isError ? t('courseOfferings.registrations.organizationList.loadFailed') : undefined}
        emptyState={paginatedListEmptyCard(
          t('courseOfferings.registrations.organizationList.empty'),
        )}
        hasNextPage={hasNextPage}
        isFetchingNextPage={isFetchingNextPage}
        onLoadMore={() => void fetchNextPage()}
      >
        <ListContainer>
          {registrations.map((registration) => {
            const subtitle = subtitleLines([
              registration.trainingCourseTypeName,
              registration.firstSessionStart
                ? formatDateShort(registration.firstSessionStart)
                : null,
            ]);

            return (
              <ListRow
                key={registration.id}
                href={getOrganizationUserCourseRegistrationDetailPath(
                  organizationId,
                  registration.userId,
                  registration.id,
                )}
                title={registration.userDisplayName}
                subtitle={subtitle}
                trailing={
                  <Badge tone={courseRegistrationStatusBadgeTone(registration.status)} pill>
                    {t(courseRegistrationStatusLabelKey(registration.status))}
                  </Badge>
                }
              />
            );
          })}
        </ListContainer>
      </PaginatedListView>
    </>
  );
}
