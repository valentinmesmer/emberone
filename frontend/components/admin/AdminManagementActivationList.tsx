/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useEffect } from 'react';
import { Badge } from '@/components/ui/badge';
import { ListContainer, ListRow } from '@/components/ui/list';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useUrlSyncedListState } from '@/lib/list';
import { getAdminManagementActivationListDefinition } from '@/lib/list/adminManagementActivationList';
import { useAdminManagementActivations } from '@/lib/store/queries/adminManagementActivations';
import { getAdminManagementActivationDetailPath } from '@/lib/utils/adminPaths';
import { ACTIVATION_STATUS_TONE, getActivationPersonName } from '@/lib/utils/managementActivation';

export interface AdminManagementActivationListProps {
  /** Global admin: tenant filter in the menubar; otherwise the own tenant only. */
  isGlobalAdmin?: boolean;
}

/** Read-only log of `/admin/management` (search + filter menubar, URL-synced). */
export function AdminManagementActivationList({
  isGlobalAdmin = false,
}: AdminManagementActivationListProps) {
  const t = useTranslation();
  const { showError } = useToast();
  const definition = getAdminManagementActivationListDefinition(isGlobalAdmin);
  const listState = useUrlSyncedListState(definition);
  const {
    data: activations,
    isPending,
    error,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useAdminManagementActivations({ listQuery: listState.listQuery });

  useEffect(() => {
    if (error) {
      showError(
        error instanceof Error ? error.message : t('admin.managementActivations.loadFailed'),
      );
    }
  }, [error, showError, t]);

  return (
    <PaginatedListView
      definition={definition}
      state={listState}
      totalItems={activations.length}
      isPending={isPending}
      emptyState={paginatedListEmptyCard(t('admin.managementActivations.empty'))}
      hasNextPage={!!hasNextPage}
      isFetchingNextPage={isFetchingNextPage}
      onLoadMore={() => void fetchNextPage()}
    >
      <ListContainer label={t('admin.managementActivations.listLabel')} className="mt-1 sm:mt-0">
        {activations.map((a) => (
          <ListRow
            key={a.id}
            href={getAdminManagementActivationDetailPath(a.id)}
            title={getActivationPersonName(a, t('admin.managementActivations.formerMember'))}
            subtitle={a.tenantName}
            badges={
              <Badge tone={ACTIVATION_STATUS_TONE[a.status]}>
                {t(`admin.managementActivations.status.${a.status}`)}
              </Badge>
            }
          />
        ))}
      </ListContainer>
    </PaginatedListView>
  );
}
