/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { apiClient } from '@/lib/api/client';
import type { EventType } from '@/types/event';

export type CertGrantRecipientMode = 'ALL_ATTENDEES' | 'SPECIFIC';

export interface CertificationEventGrantDto {
  id: number;
  organizationId: number;
  eventType: EventType;
  eventId: number;
  tenantCertDefinitionId: number;
  certDefinitionName: string | null;
  recipientMode: CertGrantRecipientMode;
  completedDate: string;
  notes: string | null;
  createdByUserId: number;
  recipientCount: number;
  recordCount: number;
}

export interface CertificationEventGrantRecipientDto {
  userId: number;
  firstName: string | null;
  lastName: string | null;
  inScope: boolean;
  recordId: number | null;
  edited: boolean;
}

export interface CertificationEventGrantDetailDto {
  grant: CertificationEventGrantDto;
  recipients: CertificationEventGrantRecipientDto[];
}

export interface CreateCertificationEventGrantRequest {
  tenantCertDefinitionId: number;
  recipientMode: CertGrantRecipientMode;
  notes?: string;
  recipientUserIds?: number[];
}

export interface UpdateCertificationEventGrantRequest {
  recipientMode: CertGrantRecipientMode;
  notes?: string;
  recipientUserIds?: number[];
}

function eventCategory(eventType: EventType): string {
  return eventType === 'SERVICE' ? 'service' : 'operation';
}

function grantsBase(organizationId: number, eventType: EventType, eventId: number): string {
  return `/api/v1/auth/organizations/${organizationId}/events/${eventCategory(eventType)}/${eventId}/certification-grants`;
}

export const eventCertificationGrantsApi = {
  list(
    organizationId: number,
    eventType: EventType,
    eventId: number,
  ): Promise<CertificationEventGrantDto[]> {
    return apiClient.get<CertificationEventGrantDto[]>(
      grantsBase(organizationId, eventType, eventId),
    );
  },

  get(
    organizationId: number,
    eventType: EventType,
    eventId: number,
    grantId: number,
  ): Promise<CertificationEventGrantDetailDto> {
    return apiClient.get<CertificationEventGrantDetailDto>(
      `${grantsBase(organizationId, eventType, eventId)}/${grantId}`,
    );
  },

  create(
    organizationId: number,
    eventType: EventType,
    eventId: number,
    data: CreateCertificationEventGrantRequest,
  ): Promise<CertificationEventGrantDetailDto> {
    return apiClient.post<CertificationEventGrantDetailDto>(
      grantsBase(organizationId, eventType, eventId),
      data,
    );
  },

  update(
    organizationId: number,
    eventType: EventType,
    eventId: number,
    grantId: number,
    data: UpdateCertificationEventGrantRequest,
  ): Promise<CertificationEventGrantDetailDto> {
    return apiClient.put<CertificationEventGrantDetailDto>(
      `${grantsBase(organizationId, eventType, eventId)}/${grantId}`,
      data,
    );
  },

  delete(
    organizationId: number,
    eventType: EventType,
    eventId: number,
    grantId: number,
  ): Promise<void> {
    return apiClient.delete(`${grantsBase(organizationId, eventType, eventId)}/${grantId}`);
  },
};
