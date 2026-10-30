/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { NotificationPreferenceChannelSummary } from '@/components/my-area/settings/NotificationPreferenceChannelSummary';
import { ListRow } from '@/components/ui/list';
import type { NotificationChannel } from '@/types/notification';

export interface NotificationPreferenceRowProps {
  title: string;
  channels: NotificationChannel[];
  ariaLabel: string;
  onClick: () => void;
}

export function NotificationPreferenceRow({
  title,
  channels,
  ariaLabel,
  onClick,
}: NotificationPreferenceRowProps) {
  return (
    <ListRow
      title={title}
      ariaLabel={ariaLabel}
      onClick={onClick}
      accent={false}
      badges={<NotificationPreferenceChannelSummary channels={channels} />}
    />
  );
}
