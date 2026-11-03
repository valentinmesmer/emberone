/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
