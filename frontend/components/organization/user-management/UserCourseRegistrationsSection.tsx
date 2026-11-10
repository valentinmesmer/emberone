/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Badge } from '@/components/ui/badge';
import { ListContainer, ListRow, subtitleLines } from '@/components/ui/list';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { useTranslation } from '@/hooks/useTranslation';
import { userCourseRegistrationListDefinition } from '@/lib/list/courseOfferingList';
import { getOrganizationUserCourseRegistrationDetailPath } from '@/lib/navigation/userManagementSections';
import {
  useOrganizationUserCourseRegistrations,
  useUserCourseRegistrationListState,
} from '@/lib/store';
import {
  courseRegistrationStatusBadgeTone,
  courseRegistrationStatusLabelKey,
} from '@/lib/utils/courseOffering';
import { formatDateShort } from '@/lib/utils/date';

interface UserCourseRegistrationsSectionProps {
  organizationId: number;
  userId: number;
}

export function UserCourseRegistrationsSection({
  organizationId,
  userId,
}: UserCourseRegistrationsSectionProps) {
  const t = useTranslation();
  const listState = useUserCourseRegistrationListState();
  const {
    data: registrations = [],
    isPending,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useOrganizationUserCourseRegistrations({
    organizationId,
    userId,
    listQuery: listState.listQuery,
  });

  return (
    <PaginatedListView
      definition={userCourseRegistrationListDefinition}
      state={listState}
      totalItems={registrations.length}
      isPending={isPending}
      emptyState={paginatedListEmptyCard(t('userManagement.courseRegistrations.empty'))}
      hasNextPage={!!hasNextPage}
      isFetchingNextPage={isFetchingNextPage}
      onLoadMore={() => void fetchNextPage()}
    >
      <ListContainer label={t('userManagement.sections.courseRegistrations')}>
        {registrations.map((registration) => (
          <ListRow
            key={registration.id}
            href={getOrganizationUserCourseRegistrationDetailPath(
              organizationId,
              userId,
              registration.id,
            )}
            title={registration.trainingCourseTypeName}
            subtitle={subtitleLines([
              registration.organizationName,
              registration.firstSessionStart
                ? formatDateShort(registration.firstSessionStart)
                : null,
            ])}
            badges={
              <Badge tone={courseRegistrationStatusBadgeTone(registration.status)} pill>
                {t(courseRegistrationStatusLabelKey(registration.status))}
              </Badge>
            }
          />
        ))}
      </ListContainer>
    </PaginatedListView>
  );
}
