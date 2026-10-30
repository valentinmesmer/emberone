/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { notificationPreferencesApi } from '@/lib/api/notificationPreferences';
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
    staleTime: 5 * 60 * 1000,
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
