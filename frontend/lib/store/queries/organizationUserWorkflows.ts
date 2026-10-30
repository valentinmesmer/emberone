/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { workflowsApi } from '@/lib/api/workflows';
import type { ListQuery } from '@/lib/list/types';
import { useUrlSyncedListState } from '@/lib/list/useListState';
import {
  workflowUserOrgDefaultQuery,
  workflowUserOrgListDefinition,
} from '@/lib/list/workflowList';
import { usePagedList } from '@/lib/store/usePagedList';

export const organizationUserWorkflowKeys = {
  all: ['organization-user-workflows'] as const,
  list: (organizationId: number, userId: number, listQuery: ListQuery) =>
    [...organizationUserWorkflowKeys.all, organizationId, userId, listQuery] as const,
};

export function useOrganizationUserWorkflowListState() {
  return useUrlSyncedListState(workflowUserOrgListDefinition);
}

type UseOrganizationUserWorkflowsOptions = {
  listQuery?: ListQuery;
  enabled?: boolean;
  fetchAll?: boolean;
};

export function useOrganizationUserWorkflows(
  organizationId: number | null | undefined,
  userId: number | null | undefined,
  options?: UseOrganizationUserWorkflowsOptions,
) {
  const listQuery = options?.listQuery ?? workflowUserOrgDefaultQuery;
  const orgId = organizationId ?? 0;
  const uid = userId ?? 0;
  return usePagedList({
    queryKey: [...organizationUserWorkflowKeys.all, orgId, uid],
    listQuery,
    queryFn: (query) => workflowsApi.listForOrganizationUser(orgId, uid, query),
    enabled: !!organizationId && !!userId && (options?.enabled ?? true),
    fetchAll: options?.fetchAll ?? false,
    staleTime: 60 * 1000,
  });
}
