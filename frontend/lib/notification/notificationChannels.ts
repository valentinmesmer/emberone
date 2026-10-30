/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { NotificationChannel } from '@/types/notification';

/** Stable display order when rendering channel lists and summaries. */
export const NOTIFICATION_CHANNEL_ORDER: NotificationChannel[] = [
  NotificationChannel.IN_APP,
  NotificationChannel.EMAIL,
];
