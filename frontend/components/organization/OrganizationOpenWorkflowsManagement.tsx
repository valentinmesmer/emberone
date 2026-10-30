/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Badge } from '@/components/ui/badge';
import { ListContainer, ListRow } from '@/components/ui/list';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { useTranslation } from '@/hooks/useTranslation';
import { workflowListDefinition } from '@/lib/list/workflowList';
import { getOrganizationUserWorkflowDetailPath } from '@/lib/navigation/userManagementSections';
import { useOrganizationOpenWorkflows, useUrlSyncedWorkflowListState } from '@/lib/store';
import { workflowStatusBadgeTone, workflowTypeTranslationKey } from '@/lib/utils/workflowLabels';

interface OrganizationOpenWorkflowsManagementProps {
  organizationId: number;
}

export function OrganizationOpenWorkflowsManagement({
  organizationId,
}: OrganizationOpenWorkflowsManagementProps) {
  const t = useTranslation();
  const listState = useUrlSyncedWorkflowListState();
  const {
    data: workflows = [],
    isPending: isLoading,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useOrganizationOpenWorkflows(organizationId, { listQuery: listState.listQuery });

  return (
    <PaginatedListView
      definition={workflowListDefinition}
      state={listState}
      organizationWorkflowFilterOptions={{
        scope: 'organizationWorkflows',
        organizationId,
      }}
      totalItems={workflows.length}
      isPending={isLoading}
      emptyState={paginatedListEmptyCard(t('organization.workflows.empty'))}
      hasNextPage={!!hasNextPage}
      isFetchingNextPage={isFetchingNextPage}
      onLoadMore={() => void fetchNextPage()}
    >
      <ListContainer label={t('organization.workflows.title')}>
        {workflows.map((workflow) => {
          const typeLabel = t(workflowTypeTranslationKey(workflow.workflowTypeId));
          const statusLabel = t(`myArea.workflows.status.${workflow.status}`);
          const openerName =
            typeof workflow.openedByDisplayName === 'string' &&
            workflow.openedByDisplayName.trim() !== ''
              ? workflow.openedByDisplayName.trim()
              : t('common.notAvailable');

          return (
            <ListRow
              key={workflow.id}
              title={typeLabel}
              href={getOrganizationUserWorkflowDetailPath(
                organizationId,
                workflow.openedByUserId,
                workflow.id,
              )}
              subtitle={<span>{t('myArea.workflows.fields.openedBy', { name: openerName })}</span>}
              badges={
                <Badge tone={workflowStatusBadgeTone(workflow.status)} pill>
                  {statusLabel}
                </Badge>
              }
            />
          );
        })}
      </ListContainer>
    </PaginatedListView>
  );
}
