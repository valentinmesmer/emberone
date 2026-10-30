/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

export {
  REALTIME_TOKEN_EXPIRED_CLOSE_CODE,
  REALTIME_USER_NOTIFICATIONS_DESTINATION,
  REALTIME_USER_TODOS_DESTINATION,
  REALTIME_USER_WORKFLOWS_DESTINATION,
} from './constants';
export { dispatchRealtimeEvent } from './eventDispatcher';
export { isRealtimeEnabled } from './isRealtimeEnabled';
export { RealtimeProvider, useRealtime } from './RealtimeContext';
export {
  buildAppointmentTopic,
  buildEventTopic,
  buildTenantAppointmentsTopic,
  buildTenantEntityTopic,
  buildTenantEventsTopic,
  buildTenantOrganizationMembershipsTopic,
  buildTenantTopic,
  buildWorkflowTopic,
  eventTypeToTopicDomain,
} from './topics';
export type { RealtimeEvent, RealtimeTopicHandler } from './types';
export { useRealtimePollingFallback } from './useRealtimePollingFallback';
export { useRealtimeTopic } from './useRealtimeTopic';
export { buildRealtimeWebSocketUrl } from './wsUrl';
