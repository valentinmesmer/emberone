/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

// React Query keys and hooks for the signed-in user's permissions (server state)

import { useQuery } from '@tanstack/react-query';
import { permissionApi } from '@/lib/api/permission';
import { STALE_TIME } from '@/lib/store/staleTime';

export const permissionKeys = {
  all: ['permissions'] as const,
  self: () => [...permissionKeys.all, 'self'] as const,
};

/**
 * Permissions per organization. Realtime `permissions.changed` invalidates this query; the
 * refetch on reconnect covers events missed while the socket was down.
 */
export function useSelfPermissions(enabled: boolean) {
  return useQuery({
    queryKey: permissionKeys.self(),
    queryFn: () => permissionApi.getSelfPermissions(),
    enabled,
    staleTime: STALE_TIME.STANDARD,
    refetchOnReconnect: true,
  });
}
