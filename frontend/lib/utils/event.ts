/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { eventApi } from '@/lib/api/event';
import type {
  CreateOperationEventRequest,
  CreateServiceEventRequest,
  EventDto,
  UpdateOperationEventRequest,
  UpdateServiceEventRequest,
} from '@/types/event';
import { EventType } from '@/types/event';
import { PermissionTypeEnum } from '@/types/permission';

export function eventTypeToUrlSegment(eventType: EventType): string {
  switch (eventType) {
    case EventType.SERVICE:
      return 'service';
    case EventType.OPERATION:
      return 'operation';
  }
}

export function getReadPermissionForEventType(eventType: EventType): PermissionTypeEnum {
  switch (eventType) {
    case EventType.SERVICE:
      return PermissionTypeEnum.SERVICE_EVENT_READ;
    case EventType.OPERATION:
      return PermissionTypeEnum.OPERATION_EVENT_READ;
  }
}

export function getWritePermissionForEventType(eventType: EventType): PermissionTypeEnum {
  switch (eventType) {
    case EventType.SERVICE:
      return PermissionTypeEnum.SERVICE_EVENT_WRITE;
    case EventType.OPERATION:
      return PermissionTypeEnum.OPERATION_EVENT_WRITE;
  }
}

export async function createEvent(
  eventType: EventType,
  data: CreateServiceEventRequest | CreateOperationEventRequest,
): Promise<EventDto> {
  switch (eventType) {
    case EventType.SERVICE:
      return eventApi.createServiceEvent(data as CreateServiceEventRequest);
    case EventType.OPERATION:
      return eventApi.createOperationEvent(data as CreateOperationEventRequest);
  }
}

export async function updateEvent(
  eventType: EventType,
  organizationId: number,
  eventId: number,
  data: UpdateServiceEventRequest | UpdateOperationEventRequest,
): Promise<EventDto> {
  switch (eventType) {
    case EventType.SERVICE:
      return eventApi.updateServiceEvent(
        organizationId,
        eventId,
        data as UpdateServiceEventRequest,
      );
    case EventType.OPERATION:
      return eventApi.updateOperationEvent(
        organizationId,
        eventId,
        data as UpdateOperationEventRequest,
      );
  }
}

export async function deleteEvent(
  eventType: EventType,
  organizationId: number,
  eventId: number,
): Promise<void> {
  switch (eventType) {
    case EventType.SERVICE:
      return eventApi.deleteServiceEvent(organizationId, eventId);
    case EventType.OPERATION:
      return eventApi.deleteOperationEvent(organizationId, eventId);
  }
}
