/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useEffect } from 'react';
import { ListContainer, ListRow } from '@/components/ui/list';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useUrlSyncedListState } from '@/lib/list';
import { getAdminUserListDefinition } from '@/lib/list/adminUserList';
import { useAdminUsers } from '@/lib/store';
import { getAdminUserDetailPath } from '@/lib/utils/adminPaths';

export interface AdminGlobalUsersPanelProps {
  /**
   * Global admin: tenant column, tenant filter, organizations of the chosen tenant. Otherwise the
   * list is limited to the own tenant and the organization filter offers its organizations.
   */
  isGlobalAdmin?: boolean;
  emptyMessageKey?: string;
}

/** Main list of `/admin/users` (search + filter menubar, URL-synced). */
export function AdminGlobalUsersPanel({
  isGlobalAdmin = false,
  emptyMessageKey = 'admin.globalUsersEmpty',
}: AdminGlobalUsersPanelProps) {
  const t = useTranslation();
  const { showError } = useToast();
  const definition = getAdminUserListDefinition(isGlobalAdmin);
  const listState = useUrlSyncedListState(definition);
  const {
    data: users,
    isPending,
    error,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useAdminUsers({ listQuery: listState.listQuery });

  useEffect(() => {
    if (error) {
      showError(error instanceof Error ? error.message : t('admin.usersLoadError'));
    }
  }, [error, showError, t]);

  return (
    <PaginatedListView
      definition={definition}
      state={listState}
      totalItems={users.length}
      isPending={isPending}
      emptyState={paginatedListEmptyCard(t(emptyMessageKey))}
      hasNextPage={!!hasNextPage}
      isFetchingNextPage={isFetchingNextPage}
      onLoadMore={() => void fetchNextPage()}
    >
      <ListContainer label={t('admin.globalUsers')} className="mt-1 sm:mt-0">
        {users.map((u) => {
          const title = `${u.firstName} ${u.lastName}`.trim() || u.username;
          const subtitle = isGlobalAdmin ? u.tenantName : undefined;
          const roleLabel = t(`admin.roles.${u.appRole}`);

          return (
            <ListRow
              key={`${u.tenantId}-${u.userId}`}
              href={getAdminUserDetailPath(u.userId)}
              title={title}
              subtitle={subtitle}
              trailing={
                <span className="max-w-[9rem] truncate text-right text-sm text-[var(--text-secondary)] sm:max-w-[11rem]">
                  {roleLabel}
                </span>
              }
              chevron
            />
          );
        })}
      </ListContainer>
    </PaginatedListView>
  );
}
