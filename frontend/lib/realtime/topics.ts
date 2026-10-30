/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/** Builds `/topic/tenant.{tenantId}.{domain}` list-level destinations. */
import { EventType } from '@/types/event';

export function buildTenantTopic(tenantId: number, domain: string): string {
  return `/topic/tenant.${tenantId}.${domain}`;
}

/** Builds `/topic/tenant.{tenantId}.{domain}.{entityId}` destinations. */
export function buildTenantEntityTopic(tenantId: number, domain: string, entityId: number): string {
  return `/topic/tenant.${tenantId}.${domain}.${entityId}`;
}

export function buildWorkflowTopic(tenantId: number, workflowId: number): string {
  return buildTenantEntityTopic(tenantId, 'workflow', workflowId);
}

export function buildAppointmentTopic(tenantId: number, appointmentId: number): string {
  return buildTenantEntityTopic(tenantId, 'appointment', appointmentId);
}

export function buildTenantAppointmentsTopic(tenantId: number): string {
  return buildTenantTopic(tenantId, 'appointments');
}

export function eventTypeToTopicDomain(eventType: EventType): string {
  return eventType === EventType.SERVICE ? 'service-event' : 'operation-event';
}

export function buildEventTopic(tenantId: number, eventType: EventType, eventId: number): string {
  return buildTenantEntityTopic(tenantId, eventTypeToTopicDomain(eventType), eventId);
}

export function buildTenantEventsTopic(tenantId: number): string {
  return buildTenantTopic(tenantId, 'events');
}

export function buildTenantOrganizationMembershipsTopic(tenantId: number): string {
  return buildTenantTopic(tenantId, 'organization-memberships');
}
