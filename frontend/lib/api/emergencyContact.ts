/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type {
  CreateEmergencyContactRequest,
  EmergencyContactDto,
  ReorderEmergencyContactsRequest,
  UpdateEmergencyContactRequest,
} from '@/types/emergencyContact';
import { apiClient } from './client';

const BASE_PATH = '/api/v1/self/emergency-contacts';

function organizationEmergencyContactsPath(organizationId: number): string {
  return `/api/v1/auth/organizations/${organizationId}/emergency-contacts`;
}

function userEmergencyContactsPath(organizationId: number, userId: number): string {
  return `/api/v1/auth/organizations/${organizationId}/users/${userId}/emergency-contacts`;
}

export const emergencyContactApi = {
  async getAll(): Promise<EmergencyContactDto[]> {
    return apiClient.get<EmergencyContactDto[]>(BASE_PATH);
  },

  async getForOrganization(organizationId: number): Promise<EmergencyContactDto[]> {
    return apiClient.get<EmergencyContactDto[]>(organizationEmergencyContactsPath(organizationId));
  },

  async getForOrganizationUser(
    organizationId: number,
    userId: number,
  ): Promise<EmergencyContactDto[]> {
    return apiClient.get<EmergencyContactDto[]>(userEmergencyContactsPath(organizationId, userId));
  },

  async createForOrganizationUser(
    organizationId: number,
    userId: number,
    data: CreateEmergencyContactRequest,
  ): Promise<EmergencyContactDto> {
    return apiClient.post<EmergencyContactDto>(
      userEmergencyContactsPath(organizationId, userId),
      data,
    );
  },

  async updateForOrganizationUser(
    organizationId: number,
    userId: number,
    contactId: number,
    data: UpdateEmergencyContactRequest,
  ): Promise<EmergencyContactDto> {
    return apiClient.put<EmergencyContactDto>(
      `${userEmergencyContactsPath(organizationId, userId)}/${contactId}`,
      data,
    );
  },

  async deleteForOrganizationUser(
    organizationId: number,
    userId: number,
    contactId: number,
  ): Promise<void> {
    return apiClient.delete(`${userEmergencyContactsPath(organizationId, userId)}/${contactId}`);
  },

  async reorderForOrganizationUser(
    organizationId: number,
    userId: number,
    data: ReorderEmergencyContactsRequest,
  ): Promise<EmergencyContactDto[]> {
    return apiClient.put<EmergencyContactDto[]>(
      `${userEmergencyContactsPath(organizationId, userId)}/reorder`,
      data,
    );
  },

  async create(data: CreateEmergencyContactRequest): Promise<EmergencyContactDto> {
    return apiClient.post<EmergencyContactDto>(BASE_PATH, data);
  },

  async update(
    contactId: number,
    data: UpdateEmergencyContactRequest,
  ): Promise<EmergencyContactDto> {
    return apiClient.put<EmergencyContactDto>(`${BASE_PATH}/${contactId}`, data);
  },

  async delete(contactId: number): Promise<void> {
    return apiClient.delete(`${BASE_PATH}/${contactId}`);
  },

  async reorder(data: ReorderEmergencyContactsRequest): Promise<EmergencyContactDto[]> {
    return apiClient.put<EmergencyContactDto[]>(`${BASE_PATH}/reorder`, data);
  },
};
