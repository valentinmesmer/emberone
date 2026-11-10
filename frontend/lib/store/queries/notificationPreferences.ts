/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { notificationPreferencesApi } from '@/lib/api/notificationPreferences';
import { STALE_TIME } from '@/lib/store/staleTime';
import type { NotificationType } from '@/types/notification';
import type { UpdateNotificationPreferenceRequest } from '@/types/notificationPreference';

export const notificationPreferenceKeys = {
  all: ['notification-preferences'] as const,
  list: () => [...notificationPreferenceKeys.all, 'list'] as const,
};

export function useNotificationPreferences() {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: notificationPreferenceKeys.list(),
    queryFn: () => notificationPreferencesApi.getAll(),
    enabled: hasEmberOneUserId,
    staleTime: STALE_TIME.STANDARD,
  });
}

export function useUpdateNotificationPreference() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      type,
      data,
    }: {
      type: NotificationType;
      data: UpdateNotificationPreferenceRequest;
    }) => notificationPreferencesApi.update(type, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationPreferenceKeys.list() });
    },
  });
}
