/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ListContainer, ListRow } from '@/components/ui/list';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { useTranslation } from '@/hooks/useTranslation';
import { useUrlSyncedListState } from '@/lib/list';
import { organizationGroupListDefinition } from '@/lib/list/organizationGroupList';
import { useOrganizationGroupList } from '@/lib/store';
import { getGroupDisplayName } from '@/lib/utils';
import { getOrganizationGroupDetailPath } from '@/lib/utils/organization';

interface OrganizationGroupsListProps {
  organizationId: number;
}

export function OrganizationGroupsList({ organizationId }: OrganizationGroupsListProps) {
  const t = useTranslation();
  const listState = useUrlSyncedListState(organizationGroupListDefinition);
  const {
    data: groups = [],
    isPending,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useOrganizationGroupList(organizationId, listState.listQuery);

  return (
    <PaginatedListView
      definition={organizationGroupListDefinition}
      state={listState}
      totalItems={groups.length}
      isPending={isPending}
      emptyState={paginatedListEmptyCard(t('organizationManagement.organizationGroups.noGroups'))}
      hasNextPage={hasNextPage}
      isFetchingNextPage={isFetchingNextPage}
      onLoadMore={() => void fetchNextPage()}
    >
      <ListContainer label={t('organization.groups.title')}>
        {groups.map((group) => (
          <ListRow
            key={group.id}
            compact
            href={getOrganizationGroupDetailPath(organizationId, group.id)}
            title={getGroupDisplayName(group)}
            subtitle={group.description?.trim() || undefined}
          />
        ))}
      </ListContainer>
    </PaginatedListView>
  );
}
