/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/**
 * Maps domain topics to React Query invalidations (mutations and realtime dispatcher).
 */
import type { QueryClient } from '@tanstack/react-query';
import { appointmentKeys } from './queries/appointments';
import { emergencyContactKeys } from './queries/emergencyContacts';
import { eventKeys } from './queries/events';
import { notificationKeys } from './queries/notifications';
import { onboardingInvitationKeys } from './queries/onboardingInvitation';
import { organizationOpenWorkflowKeys } from './queries/organizationOpenWorkflows';
import { organizationKeys } from './queries/organizations';
import { organizationUserWorkflowKeys } from './queries/organizationUserWorkflows';
import { permissionKeys } from './queries/permissions';
import { todoKeys } from './queries/todos';
import { userKeys } from './queries/users';
import { workflowKeys } from './queries/workflows';

/** Narrow topics so a future transport can map events → invalidations. */
export type ServerCacheTopic =
  | { type: 'organizations' }
  | { type: 'organization'; id: number }
  | { type: 'organizationGroups'; organizationId: number }
  | { type: 'users' }
  | { type: 'usersForOrganization'; organizationId: number }
  | { type: 'events' }
  | { type: 'permissions' }
  | { type: 'appointments' }
  | { type: 'appointment'; organizationId: number; id: number }
  | { type: 'emergencyContacts' }
  | { type: 'notifications' }
  | { type: 'todos' }
  | { type: 'workflows' }
  | { type: 'workflow'; id: number }
  | { type: 'workflowComments'; workflowId: number };

export function invalidateServerCacheTopic(
  queryClient: QueryClient,
  topic: ServerCacheTopic,
): Promise<void> {
  switch (topic.type) {
    case 'organizations':
      return queryClient.invalidateQueries({ queryKey: organizationKeys.all });
    case 'organization':
      return queryClient.invalidateQueries({ queryKey: organizationKeys.byId(topic.id) });
    case 'organizationGroups':
      return queryClient.invalidateQueries({
        queryKey: [...organizationKeys.all, topic.organizationId, 'groups'],
      });
    case 'users':
      return queryClient.invalidateQueries({ queryKey: userKeys.all });
    case 'usersForOrganization':
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: userKeys.byOrganization(topic.organizationId),
        }),
        queryClient.invalidateQueries({
          queryKey: userKeys.byOrganizationIncludingChildren(topic.organizationId),
        }),
      ]).then(() => {});
    case 'events':
      return queryClient.invalidateQueries({ queryKey: eventKeys.all });
    case 'permissions':
      return queryClient.invalidateQueries({ queryKey: permissionKeys.all });
    case 'appointments':
      return queryClient.invalidateQueries({ queryKey: appointmentKeys.all });
    case 'appointment':
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: appointmentKeys.detail(topic.organizationId, topic.id),
        }),
        queryClient.invalidateQueries({
          queryKey: appointmentKeys.participationStats(topic.organizationId, topic.id),
        }),
        queryClient.invalidateQueries({ queryKey: appointmentKeys.all }),
      ]).then(() => {});
    case 'emergencyContacts':
      return queryClient.invalidateQueries({ queryKey: emergencyContactKeys.all });
    case 'notifications':
      return queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    case 'todos':
      return queryClient.invalidateQueries({ queryKey: todoKeys.all });
    case 'workflows':
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: workflowKeys.all }),
        queryClient.invalidateQueries({ queryKey: organizationOpenWorkflowKeys.all }),
        queryClient.invalidateQueries({ queryKey: organizationUserWorkflowKeys.all }),
        queryClient.invalidateQueries({ queryKey: onboardingInvitationKeys.all }),
      ]).then(() => {});
    case 'workflow':
      return queryClient.invalidateQueries({ queryKey: workflowKeys.detail(topic.id) });
    case 'workflowComments':
      return queryClient.invalidateQueries({ queryKey: workflowKeys.comments(topic.workflowId) });
  }
}
