/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { ListContainer, ListRow } from '@/components/ui/list';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { useTranslation } from '@/hooks/useTranslation';
import { orgAbsenceEmptyMessageKey, orgAbsenceListDefinition } from '@/lib/list/absenceList';
import { getAbsenceReasonLabelKey } from '@/lib/my-area/absenceReasonOptions';
import { getOrganizationUserAbsenceDetailPath } from '@/lib/navigation/userManagementSections';
import {
  useOrganizationActiveAbsences,
  useUrlSyncedOrgAbsenceListState,
  useUsersForOrganization,
} from '@/lib/store';
import { formatDateShort } from '@/lib/utils';
import type { UserAbsenceDto } from '@/types/absence';
import type { UserDto } from '@/types/user';

interface OrganizationActiveAbsencesManagementProps {
  organizationId: number;
}

function resolveUserName(
  userById: Map<number, UserDto>,
  absence: UserAbsenceDto,
  t: (key: string, params?: Record<string, string | number>) => string,
) {
  const u = userById.get(absence.userId);
  if (!u) {
    return t('userManagement.absences.userUnknown', { id: String(absence.userId) });
  }
  return `${u.firstName} ${u.lastName}`.trim();
}

export function OrganizationActiveAbsencesManagement({
  organizationId,
}: OrganizationActiveAbsencesManagementProps) {
  const t = useTranslation();
  const listState = useUrlSyncedOrgAbsenceListState();
  const {
    data: absences = [],
    isPending: loadingAbsences,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useOrganizationActiveAbsences({
    organizationId,
    listQuery: listState.listQuery,
  });
  const { data: users = [], isLoading: loadingUsers } = useUsersForOrganization(organizationId);

  const userById = useMemo(() => new Map(users.map((u) => [u.id, u] as const)), [users]);

  const emptyMessageKey = orgAbsenceEmptyMessageKey(listState.filters.period);

  return (
    <PaginatedListView
      definition={orgAbsenceListDefinition}
      state={listState}
      organizationAbsenceFilterOptions={{
        scope: 'organizationAbsences',
        organizationId,
      }}
      totalItems={absences.length}
      isPending={loadingAbsences || loadingUsers}
      emptyState={paginatedListEmptyCard(t(emptyMessageKey))}
      hasNextPage={!!hasNextPage}
      isFetchingNextPage={isFetchingNextPage}
      onLoadMore={() => void fetchNextPage()}
    >
      <ListContainer label={t('userManagement.absences.title')}>
        {absences.map((absence) => (
          <ListRow
            key={absence.id}
            title={resolveUserName(userById, absence, t)}
            href={getOrganizationUserAbsenceDetailPath(organizationId, absence.userId, absence.id)}
            subtitle={
              <>
                <span className="font-medium text-[var(--text-primary)] tabular-nums">
                  {formatDateShort(absence.startDate)} - {formatDateShort(absence.endDate)}
                </span>
                <span aria-hidden> · </span>
                <span>{t(getAbsenceReasonLabelKey(absence.reason))}</span>
              </>
            }
          />
        ))}
      </ListContainer>
    </PaginatedListView>
  );
}
