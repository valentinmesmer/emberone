/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { type StandardContentDto, standardsApi } from '@/lib/api/standards';
import { queryClient, standardContentQueryOptions } from '@/lib/store';

export async function syncStandardGroupMappings(
  tenantId: number,
  standardId: number,
  orgGroupConfigKey: string,
  currentKeys: Set<string>,
  nextKeys: Set<string>,
): Promise<StandardContentDto> {
  let content: StandardContentDto | null = null;

  for (const key of currentKeys) {
    if (!nextKeys.has(key)) {
      content = await standardsApi.deleteGroupMapping(tenantId, standardId, orgGroupConfigKey, key);
    }
  }
  for (const key of nextKeys) {
    if (!currentKeys.has(key)) {
      content = await standardsApi.addGroupMapping(tenantId, standardId, {
        orgGroupConfigKey,
        permissionGroupConfigKey: key,
      });
    }
  }

  if (content) {
    return content;
  }
  return queryClient.fetchQuery(standardContentQueryOptions(tenantId, standardId));
}
