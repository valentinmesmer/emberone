/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { NotificationDto } from '@/types/notification';

type NotificationCreatedListener = (notification: NotificationDto) => void;

const listeners = new Set<NotificationCreatedListener>();

export function onNotificationCreated(listener: NotificationCreatedListener): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function emitNotificationCreated(notification: NotificationDto): void {
  listeners.forEach((listener) => {
    listener(notification);
  });
}
