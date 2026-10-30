/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { PageResponse } from '@/types/page';

export enum EventType {
  SERVICE = 'SERVICE',
  OPERATION = 'OPERATION',
}

export interface EventAttendanceDto {
  id: number;
  eventType: EventType;
  eventId: number;
  userId: number;
  firstName: string;
  lastName: string;
}

export interface CreateEventAttendanceRequest {
  userId: number;
}

export interface ServiceHoursTotalDto {
  eventHours: number;
  timeEntryHours: number;
  totalHours: number;
}

export interface EventAttendanceSelfItemDto {
  eventId: number;
  attending: boolean;
}

export interface OperationAlarmAttendanceStatsDto {
  totalAlarms: number;
  attendedAlarms: number;
}

export interface ServiceEventDto {
  id: number;
  start: string;
  end?: string;
  organizationId: number;
  location?: string;
  description?: string;
  leaderId?: number;
  eventType: EventType;
  topic?: string;
}

export interface OperationEventDto {
  id: number;
  start: string;
  end?: string;
  organizationId: number;
  location?: string;
  description?: string;
  leaderId?: number;
  eventType: EventType;
  number?: string;
  keyword?: string;
  tag?: string;
  specialSignal?: boolean;
}

export interface CreateServiceEventRequest {
  start: string;
  end?: string;
  organizationId: number;
  location?: string;
  description?: string;
  leaderId?: number;
  topic?: string;
  promoteFromAppointmentId?: number;
}

export interface CreateOperationEventRequest {
  start: string;
  end?: string;
  organizationId: number;
  location?: string;
  description?: string;
  leaderId?: number;
  number?: string;
  keyword?: string;
  tag?: string;
  specialSignal?: boolean;
}

export interface UpdateServiceEventRequest {
  start: string;
  end?: string;
  location?: string;
  description?: string;
  leaderId?: number;
  topic?: string;
}

export interface UpdateOperationEventRequest {
  start: string;
  end?: string;
  location?: string;
  description?: string;
  leaderId?: number;
  number?: string;
  keyword?: string;
  tag?: string;
  specialSignal?: boolean;
}

export type EventFormState = {
  start: string;
  end?: string;
  organizationId?: number;
  location: string;
  description?: string;
  leaderId?: number | string;
  topic?: string;
  number?: string;
  keyword?: string;
  tag?: string;
  specialSignal?: boolean | string;
};

export type EventModalInitialValues = Partial<EventFormState>;

export type CreateEventPayload = CreateServiceEventRequest | CreateOperationEventRequest;

export type UpdateEventPayload = UpdateServiceEventRequest | UpdateOperationEventRequest;

export type EventDto = ServiceEventDto | OperationEventDto;

export type ServiceEventPageDto = PageResponse<ServiceEventDto>;
export type OperationEventPageDto = PageResponse<OperationEventDto>;
export type EventAttendancePageDto = PageResponse<EventAttendanceDto>;
