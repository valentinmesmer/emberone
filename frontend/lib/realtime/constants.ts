/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/** Mirrors backend {@link com.emberone.platform.realtime.auth.RealtimeCloseCodes#TOKEN_EXPIRED}. */
export const REALTIME_TOKEN_EXPIRED_CLOSE_CODE = 4001;

export const REALTIME_USER_NOTIFICATIONS_DESTINATION = '/user/queue/notifications';
export const REALTIME_USER_TODOS_DESTINATION = '/user/queue/todos';
export const REALTIME_USER_PERMISSIONS_DESTINATION = '/user/queue/permissions';
export const REALTIME_USER_WORKFLOWS_DESTINATION = '/user/queue/workflows';

export const REALTIME_INITIAL_RECONNECT_DELAY_MS = 1_000;
export const REALTIME_MAX_RECONNECT_DELAY_MS = 30_000;
