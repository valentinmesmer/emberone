/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

// React Query hooks for management activations (Server State)

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { managementApi } from '@/lib/api/management';
import type { ManagementActivationRequest } from '@/types/management';

export const managementKeys = {
  all: ['management'] as const,
  targets: () => [...managementKeys.all, 'targets'] as const,
  activations: () => [...managementKeys.all, 'activations'] as const,
  targetOrganization: (organizationId: number) =>
    [...managementKeys.all, 'organizations', organizationId] as const,
};

export function useManagementTargets(enabled = true) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: managementKeys.targets(),
    queryFn: () => managementApi.getTargets(),
    enabled: hasEmberOneUserId && enabled,
    staleTime: 60 * 1000,
  });
}

/** Open activations of the current user. Also the client-side view for permissions and the org list. */
export function useMyManagementActivations() {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: managementKeys.activations(),
    queryFn: () => managementApi.getActivations(),
    enabled: hasEmberOneUserId,
    staleTime: 30 * 1000,
    // Expired activations drop out of the list within a minute, even without a realtime event.
    refetchInterval: 60 * 1000,
  });
}

export function useActivateManagement() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: ManagementActivationRequest) => managementApi.activate(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: managementKeys.all }),
  });
}

export function useEndManagementActivation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (activationId: number) => managementApi.endActivation(activationId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: managementKeys.all }),
  });
}
