/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { Badge } from '@/components/ui/badge';
import { ListContainer, ListRow } from '@/components/ui/list';
import { ListControls, ListLoadMore, ListNoResults } from '@/components/ui/list-filter';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { useUrlSyncedListState } from '@/lib/list';
import { orgTimeEntryListDefinition } from '@/lib/list/timeEntryList';
import { useOrganizationTimeEntries, useUsersForOrganization } from '@/lib/store';
import { getOrganizationTimeEntryDetailPath } from '@/lib/utils';
import { formatDateTime, formatDurationBetween, formatTime } from '@/lib/utils/date';
import type { UserTimeEntryDto } from '@/types/timeEntry';
import type { UserDto } from '@/types/user';

interface OrganizationTimeEntriesManagementProps {
  organizationId: number;
}

function isSameDay(a: string, b: string): boolean {
  const da = new Date(a);
  const db = new Date(b);
  return (
    da.getFullYear() === db.getFullYear() &&
    da.getMonth() === db.getMonth() &&
    da.getDate() === db.getDate()
  );
}

function formatEndTime(startIso: string, endIso: string): string {
  if (isSameDay(startIso, endIso)) {
    return formatTime(endIso);
  }
  return formatDateTime(endIso);
}

function resolveUserName(
  userById: Map<number, UserDto>,
  entry: UserTimeEntryDto,
  t: (key: string, params?: Record<string, string | number>) => string,
) {
  const u = userById.get(entry.userId);
  if (!u) {
    return t('userManagement.timeEntries.userUnknown', { id: String(entry.userId) });
  }
  return `${u.firstName} ${u.lastName}`.trim();
}

function formatEntrySubtitle(entry: UserTimeEntryDto) {
  const groupName = entry.organizationGroupName?.trim();
  const groupPart = groupName ? ` · ${groupName}` : '';
  const timePart = entry.endTime
    ? `${formatDateTime(entry.startTime)} - ${formatEndTime(entry.startTime, entry.endTime)} · ${formatDurationBetween(entry.startTime, entry.endTime)}`
    : formatDateTime(entry.startTime);

  return (
    <span className="tabular-nums text-[var(--text-secondary)]">
      {timePart}
      {groupPart}
    </span>
  );
}

export function OrganizationTimeEntriesManagement({
  organizationId,
}: OrganizationTimeEntriesManagementProps) {
  const t = useTranslation();
  const listState = useUrlSyncedListState(orgTimeEntryListDefinition);

  const {
    data: entries = [],
    isPending,
    isError,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useOrganizationTimeEntries({ organizationId, listQuery: listState.listQuery });
  const { data: users = [], isLoading: loadingUsers } = useUsersForOrganization(organizationId);

  const userById = useMemo(() => new Map(users.map((u) => [u.id, u] as const)), [users]);

  if (isError) {
    return <EmptyStateCard>{t('myArea.error.loadFailed')}</EmptyStateCard>;
  }

  return (
    <>
      <ListControls definition={orgTimeEntryListDefinition} state={listState} />

      {isPending || loadingUsers ? (
        <LoadingState variant="embedded" size="sm" />
      ) : entries.length === 0 ? (
        listState.hasActiveFilters ? (
          <ListNoResults onReset={listState.reset} />
        ) : (
          <EmptyStateCard>{t('userManagement.timeEntries.empty')}</EmptyStateCard>
        )
      ) : (
        <>
          <ListContainer label={t('userManagement.timeEntries.title')}>
            {entries.map((entry) => (
              <ListRow
                key={entry.id}
                title={resolveUserName(userById, entry, t)}
                href={getOrganizationTimeEntryDetailPath(organizationId, entry.id)}
                tone={!entry.endTime ? 'active' : 'default'}
                subtitle={formatEntrySubtitle(entry)}
                badges={
                  !entry.endTime ? (
                    <Badge tone="accent" pill className="px-2">
                      {t('myArea.timeEntries.running')}
                    </Badge>
                  ) : null
                }
              />
            ))}
          </ListContainer>
          <ListLoadMore
            hasNextPage={hasNextPage}
            isFetchingNextPage={isFetchingNextPage}
            onLoadMore={() => void fetchNextPage()}
          />
        </>
      )}
    </>
  );
}
