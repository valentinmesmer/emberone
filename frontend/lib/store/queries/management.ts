/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

// React Query hooks for management activations (Server State)

import { type QueryClient, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { managementApi } from '@/lib/api/management';
import { STALE_TIME } from '@/lib/store/staleTime';
import type { ManagementActivationRequest } from '@/types/management';

export const managementKeys = {
  all: ['management'] as const,
  targets: () => [...managementKeys.all, 'targets'] as const,
  activations: () => [...managementKeys.all, 'activations'] as const,
};

export function useManagementTargets(enabled = true) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: managementKeys.targets(),
    queryFn: () => managementApi.getTargets(),
    enabled: hasEmberOneUserId && enabled,
    staleTime: STALE_TIME.LIST,
  });
}

/** Open activations of the current user. Also the client-side view for permissions and the org list. */
export function useMyManagementActivations() {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: managementKeys.activations(),
    queryFn: () => managementApi.getActivations(),
    enabled: hasEmberOneUserId,
    staleTime: STALE_TIME.LIVE,
    // Time is the trigger: an open activation expires without any event, so it drops out of the
    // list within a minute. Without open activations there is nothing that can expire.
    refetchInterval: (query) => (query.state.data?.length ? 60 * 1000 : false),
  });
}

/** Activations and the targets that may still be activated; nothing else depends on them. */
function invalidateManagementActivations(queryClient: QueryClient) {
  void queryClient.invalidateQueries({ queryKey: managementKeys.activations() });
  void queryClient.invalidateQueries({ queryKey: managementKeys.targets() });
}

export function useActivateManagement() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: ManagementActivationRequest) => managementApi.activate(request),
    onSuccess: () => invalidateManagementActivations(queryClient),
  });
}

export function useEndManagementActivation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (activationId: number) => managementApi.endActivation(activationId),
    onSuccess: () => invalidateManagementActivations(queryClient),
  });
}
