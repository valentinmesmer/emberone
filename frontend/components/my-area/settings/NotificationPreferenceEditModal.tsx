/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useCallback } from 'react';
import { Button } from '@/components/ui/button';
import { Modal } from '@/components/ui/modal';
import { SwitchField } from '@/components/ui/switch';
import { useTranslation } from '@/hooks/useTranslation';
import { NOTIFICATION_CHANNEL_ORDER } from '@/lib/notification/notificationChannels';
import { cn } from '@/lib/utils/cn';
import { NotificationChannel } from '@/types/notification';
import type { NotificationPreferenceDto } from '@/types/notificationPreference';

function toggleChannel(
  channels: NotificationChannel[],
  channel: NotificationChannel,
  enabled: boolean,
): NotificationChannel[] {
  const next = new Set(channels);
  if (enabled) {
    next.add(channel);
  } else {
    next.delete(channel);
  }
  return Array.from(next);
}

function channelsEqual(a: NotificationChannel[], b: NotificationChannel[]): boolean {
  if (a.length !== b.length) return false;
  const set = new Set(a);
  return b.every((channel) => set.has(channel));
}

export interface NotificationPreferenceEditModalProps {
  preference: NotificationPreferenceDto | null;
  isSaving: boolean;
  onClose: () => void;
  onSave: (channels: NotificationChannel[]) => Promise<void>;
}

export function NotificationPreferenceEditModal({
  preference,
  isSaving,
  onClose,
  onSave,
}: NotificationPreferenceEditModalProps) {
  const t = useTranslation();

  const handleChannelChange = useCallback(
    async (channel: NotificationChannel, enabled: boolean) => {
      if (!preference) return;
      await onSave(toggleChannel(preference.channels, channel, enabled));
    },
    [onSave, preference],
  );

  const handleReset = useCallback(async () => {
    if (!preference) return;
    await onSave([...preference.defaultChannels]);
  }, [onSave, preference]);

  if (!preference) return null;

  const typeKey = preference.type;
  const canReset =
    preference.customized && !channelsEqual(preference.channels, preference.defaultChannels);

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={t(`myArea.settings.notifications.types.${typeKey}.title`)}
      size="sm"
    >
      <p className="text-sm leading-snug text-[var(--text-secondary)]">
        {t(`myArea.settings.notifications.types.${typeKey}.description`)}
      </p>

      <div
        className={cn(
          'mt-4 divide-y divide-[var(--border-color)]/60 overflow-hidden rounded-lg border border-[var(--border-color)]/60',
          isSaving && 'pointer-events-none opacity-70',
        )}
      >
        {NOTIFICATION_CHANNEL_ORDER.map((channel) => (
          <div key={channel} className="px-3">
            <SwitchField
              label={t(`myArea.settings.notifications.channels.${channelKey(channel)}.label`)}
              description={t(
                `myArea.settings.notifications.channels.${channelKey(channel)}.description`,
              )}
              checked={preference.channels.includes(channel)}
              disabled={isSaving}
              onCheckedChange={(checked) => void handleChannelChange(channel, checked)}
            />
          </div>
        ))}
      </div>

      {canReset ? (
        <div className="mt-4 flex justify-end">
          <Button
            type="button"
            variant="ghost"
            size="sm"
            disabled={isSaving}
            onClick={() => void handleReset()}
          >
            {t('myArea.settings.notifications.resetToDefault')}
          </Button>
        </div>
      ) : null}
    </Modal>
  );
}

function channelKey(channel: NotificationChannel): 'inApp' | 'email' {
  return channel === NotificationChannel.IN_APP ? 'inApp' : 'email';
}
