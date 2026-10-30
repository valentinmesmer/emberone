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
import { workflowUserOrgListDefinition } from '@/lib/list/workflowList';
import { getOrganizationUserWorkflowDetailPath } from '@/lib/navigation/userManagementSections';
import { useOrganizationUserWorkflowListState, useOrganizationUserWorkflows } from '@/lib/store';
import { workflowStatusBadgeTone, workflowTypeTranslationKey } from '@/lib/utils/workflowLabels';

interface UserWorkflowsSectionProps {
  organizationId: number;
  userId: number;
}

export function UserWorkflowsSection({ organizationId, userId }: UserWorkflowsSectionProps) {
  const t = useTranslation();
  const listState = useOrganizationUserWorkflowListState();
  const {
    data: workflows = [],
    isPending,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useOrganizationUserWorkflows(organizationId, userId, { listQuery: listState.listQuery });

  return (
    <PaginatedListView
      definition={workflowUserOrgListDefinition}
      state={listState}
      organizationUserWorkflowFilterOptions={{
        scope: 'organizationUserWorkflows',
        organizationId,
        userId,
      }}
      totalItems={workflows.length}
      isPending={isPending}
      emptyState={paginatedListEmptyCard(t('userManagement.workflows.empty'))}
      hasNextPage={!!hasNextPage}
      isFetchingNextPage={isFetchingNextPage}
      onLoadMore={() => void fetchNextPage()}
    >
      <ListContainer label={t('userManagement.sections.workflows')}>
        {workflows.map((workflow) => {
          const typeLabel = t(workflowTypeTranslationKey(workflow.workflowTypeId));
          const statusLabel = t(`myArea.workflows.status.${workflow.status}`);
          return (
            <ListRow
              key={workflow.id}
              title={typeLabel}
              href={getOrganizationUserWorkflowDetailPath(organizationId, userId, workflow.id)}
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
