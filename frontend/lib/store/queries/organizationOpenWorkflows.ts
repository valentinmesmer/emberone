/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { workflowsApi } from '@/lib/api/workflows';
import type { ListQuery } from '@/lib/list/types';
import { workflowListOpenQuery } from '@/lib/list/workflowList';
import { usePagedList } from '@/lib/store/usePagedList';

export const organizationOpenWorkflowKeys = {
  all: ['organization-open-workflows'] as const,
  list: (organizationId: number, listQuery: ListQuery) =>
    [...organizationOpenWorkflowKeys.all, organizationId, listQuery] as const,
};

type UseOrganizationOpenWorkflowsOptions = {
  listQuery?: ListQuery;
  enabled?: boolean;
  fetchAll?: boolean;
};

export function useOrganizationOpenWorkflows(
  organizationId: number | null,
  options?: UseOrganizationOpenWorkflowsOptions,
) {
  const listQuery = options?.listQuery ?? workflowListOpenQuery;
  const orgId = organizationId ?? 0;
  return usePagedList({
    queryKey: [...organizationOpenWorkflowKeys.all, orgId],
    listQuery,
    queryFn: (query) => workflowsApi.listOpenForOrganization(orgId, query),
    enabled: organizationId != null && (options?.enabled ?? true),
    fetchAll: options?.fetchAll ?? false,
    staleTime: 60 * 1000,
  });
}
