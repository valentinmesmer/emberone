/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { QueryClient } from '@tanstack/react-query';
import { emitNotificationCreated } from '@/lib/notification/realtimeToast';
import { emitManagementActivationExpired } from '@/lib/management/activationExpiry';
import { notifyPermissionsChanged } from '@/lib/permission/permissionsRealtime';
import { invalidateServerCacheTopic } from '@/lib/store/cache-invalidation';
import { invalidateAfterEventAttendanceChange } from '@/lib/store/queries/eventAttendance';
import { applyNotificationCreated } from '@/lib/store/queries/notifications';
import { onboardingInvitationKeys } from '@/lib/store/queries/onboardingInvitation';
import { organizationOpenWorkflowKeys } from '@/lib/store/queries/organizationOpenWorkflows';
import { organizationUserWorkflowKeys } from '@/lib/store/queries/organizationUserWorkflows';
import { managementKeys } from '@/lib/store/queries/management';
import { invalidateAfterOrgMembershipChange } from '@/lib/store/queries/userOrganizationMembership';
import { workflowKeys } from '@/lib/store/queries/workflows';
import { EventType } from '@/types/event';
import type { RealtimeEvent } from './types';

function eventTypeFromOrdinal(ordinal: number | undefined): EventType | null {
  if (ordinal === 0) {
    return EventType.SERVICE;
  }
  if (ordinal === 1) {
    return EventType.OPERATION;
  }
  return null;
}

function invalidateWorkflowViews(
  queryClient: QueryClient,
  workflowId: number,
  organizationId?: number,
): void {
  void Promise.all([
    invalidateServerCacheTopic(queryClient, { type: 'workflow', id: workflowId }),
    queryClient.invalidateQueries({ queryKey: workflowKeys.listRoot() }),
    queryClient.invalidateQueries({ queryKey: workflowKeys.pendingCount() }),
    queryClient.invalidateQueries({ queryKey: onboardingInvitationKeys.all }),
    organizationId != null
      ? queryClient.invalidateQueries({ queryKey: organizationOpenWorkflowKeys.all })
      : Promise.resolve(),
    organizationId != null
      ? queryClient.invalidateQueries({ queryKey: organizationUserWorkflowKeys.all })
      : Promise.resolve(),
  ]);
}

/**
 * Maps realtime events to react-query cache invalidations.
 * Notification creates patch the unread list; everything else refetches via invalidation.
 */
export function dispatchRealtimeEvent(queryClient: QueryClient, event: RealtimeEvent): void {
  const workflowId = event.entityIds?.workflowId;

  switch (event.type) {
    case 'permissions.changed':
      notifyPermissionsChanged();
      void invalidateServerCacheTopic(queryClient, { type: 'permissions' });
      return;
    case 'management.activation.expired': {
      void queryClient.invalidateQueries({ queryKey: managementKeys.all });
      void invalidateServerCacheTopic(queryClient, { type: 'permissions' });
      const activationId = event.entityIds?.activationId;
      const managingOrganizationId = event.entityIds?.managingOrganizationId;
      const targetOrganizationId = event.entityIds?.targetOrganizationId;
      if (activationId != null && managingOrganizationId != null && targetOrganizationId != null) {
        emitManagementActivationExpired({
          activationId,
          managingOrganizationId,
          targetOrganizationId,
        });
      }
      return;
    }
    case 'appointment.updated': {
      const appointmentId = event.entityIds?.appointmentId;
      const organizationId = event.entityIds?.organizationId;
      if (appointmentId != null && organizationId != null) {
        void invalidateServerCacheTopic(queryClient, {
          type: 'appointment',
          organizationId,
          id: appointmentId,
        });
      } else {
        void invalidateServerCacheTopic(queryClient, { type: 'appointments' });
      }
      return;
    }
    case 'notification.created': {
      const notification = applyNotificationCreated(queryClient, event.payload);
      if (notification) {
        emitNotificationCreated(notification);
        return;
      }
      void invalidateServerCacheTopic(queryClient, { type: 'notifications' });
      return;
    }
    case 'todo.created':
    case 'todo.updated':
      void invalidateServerCacheTopic(queryClient, { type: 'todos' });
      return;
    case 'workflow.updated':
      if (workflowId != null) {
        invalidateWorkflowViews(queryClient, workflowId, event.entityIds?.organizationId);
        void invalidateServerCacheTopic(queryClient, { type: 'todos' });
      } else {
        void invalidateServerCacheTopic(queryClient, { type: 'workflows' });
        void invalidateServerCacheTopic(queryClient, { type: 'todos' });
      }
      return;
    case 'workflow.comment.created':
      if (workflowId != null) {
        void Promise.all([
          invalidateServerCacheTopic(queryClient, {
            type: 'workflowComments',
            workflowId,
          }),
          invalidateWorkflowViews(queryClient, workflowId, event.entityIds?.organizationId),
        ]);
      }
      return;
    case 'event.updated':
    case 'event.deleted':
      void invalidateServerCacheTopic(queryClient, { type: 'events' });
      return;
    case 'event.attendance.changed': {
      const organizationId = event.entityIds?.organizationId;
      const eventId = event.entityIds?.eventId;
      const eventType = eventTypeFromOrdinal(event.entityIds?.eventTypeOrdinal);
      if (organizationId != null && eventId != null && eventType != null) {
        invalidateAfterEventAttendanceChange(queryClient, organizationId, eventType, eventId);
      }
      return;
    }
    case 'organization-membership.changed': {
      const organizationId = event.entityIds?.organizationId;
      const userId = event.entityIds?.userId;
      if (organizationId != null) {
        invalidateAfterOrgMembershipChange(queryClient, organizationId, userId);
      }
      return;
    }
    default:
      return;
  }
}
