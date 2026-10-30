/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Badge } from '@/components/ui/badge';
import { useTranslation } from '@/hooks/useTranslation';
import { NOTIFICATION_CHANNEL_ORDER } from '@/lib/notification/notificationChannels';
import { cn } from '@/lib/utils/cn';
import { NotificationChannel } from '@/types/notification';

function channelKey(channel: NotificationChannel): 'inApp' | 'email' {
  return channel === NotificationChannel.IN_APP ? 'inApp' : 'email';
}

export interface NotificationPreferenceChannelSummaryProps {
  channels: NotificationChannel[];
  className?: string;
}

export function NotificationPreferenceChannelSummary({
  channels,
  className,
}: NotificationPreferenceChannelSummaryProps) {
  const t = useTranslation();
  const enabled = NOTIFICATION_CHANNEL_ORDER.filter((channel) => channels.includes(channel));

  if (enabled.length === 0) {
    return (
      <span className={cn('shrink-0 text-xs text-[var(--text-secondary)]', className)}>
        {t('myArea.settings.notifications.channelsSummary.none')}
      </span>
    );
  }

  return (
    <span className={cn('flex shrink-0 items-center gap-1', className)}>
      {enabled.map((channel) => {
        const label = t(`myArea.settings.notifications.channels.${channelKey(channel)}.label`);
        return (
          <Badge key={channel} tone="accent" pill className="px-1.5 py-px">
            {label}
          </Badge>
        );
      })}
    </span>
  );
}
