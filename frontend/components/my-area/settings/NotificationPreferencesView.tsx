/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useCallback, useMemo, useState } from 'react';
import { NotificationPreferenceEditModal } from '@/components/my-area/settings/NotificationPreferenceEditModal';
import { NotificationPreferenceRow } from '@/components/my-area/settings/NotificationPreferenceRow';
import { ListContainer } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { ErrorState } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useNotificationPreferences, useUpdateNotificationPreference } from '@/lib/store';
import type {
  NotificationChannel,
  NotificationType as NotificationTypeValue,
} from '@/types/notification';

export function NotificationPreferencesView() {
  const t = useTranslation();
  const { showError } = useToast();
  const { data, isLoading, error } = useNotificationPreferences();
  const updatePreference = useUpdateNotificationPreference();
  const [editingType, setEditingType] = useState<NotificationTypeValue | null>(null);

  const preferences = useMemo(() => data?.preferences ?? [], [data?.preferences]);

  const editingPreference = useMemo(
    () => preferences.find((preference) => preference.type === editingType) ?? null,
    [editingType, preferences],
  );

  const handleSaveChannels = useCallback(
    async (type: NotificationTypeValue, channels: NotificationChannel[]) => {
      try {
        await updatePreference.mutateAsync({
          type,
          data: { channels },
        });
      } catch (err) {
        showError(
          err instanceof Error ? err.message : t('myArea.settings.notifications.error.saveFailed'),
        );
        throw err;
      }
    },
    [showError, t, updatePreference],
  );

  const header = (
    <PageHeader
      title={t('myArea.settings.notifications.title')}
      description={t('myArea.settings.notifications.subtitle')}
      size="compact"
    />
  );

  if (isLoading) {
    return (
      <>
        {header}
        <div className="mt-1 w-full sm:mt-0">
          <LoadingState variant="embedded" size="sm" />
        </div>
      </>
    );
  }

  if (error) {
    return (
      <>
        {header}
        <ErrorState
          message={t('myArea.settings.notifications.error.loadFailed')}
          className="mt-1 sm:mt-0"
        />
      </>
    );
  }

  return (
    <>
      {header}

      <div className="mt-1 w-full sm:mt-0">
        <ListContainer label={t('myArea.settings.notifications.title')}>
          {preferences.map((preference) => (
            <NotificationPreferenceRow
              key={preference.type}
              title={t(`myArea.settings.notifications.types.${preference.type}.title`)}
              channels={preference.channels}
              ariaLabel={t('myArea.settings.notifications.editChannelsFor', {
                type: t(`myArea.settings.notifications.types.${preference.type}.title`),
              })}
              onClick={() => setEditingType(preference.type)}
            />
          ))}
        </ListContainer>
      </div>

      <NotificationPreferenceEditModal
        preference={editingPreference}
        isSaving={updatePreference.isPending}
        onClose={() => setEditingType(null)}
        onSave={async (channels) => {
          if (!editingPreference) return;
          await handleSaveChannels(editingPreference.type, channels);
        }}
      />
    </>
  );
}
