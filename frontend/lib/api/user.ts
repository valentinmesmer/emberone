/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

// User API endpoints

import type { ListQuery } from '@/lib/list/types';
import type { OperationAlarmAttendanceStatsDto, ServiceHoursTotalDto } from '@/types/event';
import type { OrganizationTreeDto, SelfMembershipHistoryDto } from '@/types/organization';
import type {
  CreateUserRequest,
  UpdatePersonalDataRequest,
  UserDto,
  UserPageDto,
} from '@/types/user';
import { apiClient } from './client';
import { buildQuery } from './paged';

export const userApi = {
  /**
   * Get current user's data
   */
  async getSelf(): Promise<UserDto> {
    return apiClient.get<UserDto>('/api/v1/self');
  },

  /**
   * Get current user's organization tree
   */
  async getOrganizationTree(): Promise<OrganizationTreeDto[]> {
    return apiClient.get<OrganizationTreeDto[]>('/api/v1/self/organizations/tree');
  },

  /**
   * Get current user's organization and group memberships, including past ones
   */
  async getMembershipHistory(): Promise<SelfMembershipHistoryDto> {
    return apiClient.get<SelfMembershipHistoryDto>('/api/v1/self/organizations/memberships');
  },

  async getSelfServiceHoursTotal(year: number): Promise<ServiceHoursTotalDto> {
    return apiClient.get<ServiceHoursTotalDto>(`/api/v1/self/service-hours-total?year=${year}`);
  },

  async getSelfOperationAlarmAttendanceStats(): Promise<OperationAlarmAttendanceStatsDto> {
    return apiClient.get<OperationAlarmAttendanceStatsDto>(
      '/api/v1/self/operation-alarm-attendance-stats',
    );
  },

  /**
   * Request a notification email change (requires verification of the new address)
   */
  async updateNotificationEmail(data: { privateEmail: string }): Promise<UserDto> {
    return apiClient.put<UserDto>('/api/v1/self/notification-email', data);
  },

  /**
   * Confirm a pending notification email change with the verification token
   */
  async verifyNotificationEmail(data: { token: string }): Promise<UserDto> {
    return apiClient.post<UserDto>('/api/v1/self/notification-email/verify', data);
  },

  /**
   * Confirm a pending notification email change without being signed in
   */
  async verifyNotificationEmailPublic(data: { token: string; tenantId: number }): Promise<void> {
    await apiClient.post<void>('/api/v1/public/notification-email/verify', data, true);
  },

  /**
   * Resend the verification notification for a pending email change
   */
  async resendNotificationEmailVerification(): Promise<UserDto> {
    return apiClient.post<UserDto>('/api/v1/self/notification-email/resend', {});
  },

  /**
   * Cancel a pending notification email change
   */
  async cancelPendingNotificationEmail(): Promise<UserDto> {
    return apiClient.delete<UserDto>('/api/v1/self/notification-email/pending');
  },

  async listUsersForOrganization(organizationId: number, query: ListQuery): Promise<UserPageDto> {
    const qs = buildQuery({
      page: query.page ?? 0,
      size: query.size,
      search: query.search,
      sort: query.sort,
    });
    const path = `/api/v1/auth/users/organizations/${organizationId}`;
    return apiClient.get<UserPageDto>(qs ? `${path}?${qs}` : path);
  },

  /**
   * Create a user for an organization
   */
  async createUserForOrganization(
    organizationId: number,
    data: CreateUserRequest,
  ): Promise<UserDto> {
    return apiClient.post<UserDto>(`/api/v1/auth/users/organizations/${organizationId}`, data);
  },

  /**
   * Update personal data for a user (admin only)
   */
  async updatePersonalData(
    organizationId: number,
    userId: number,
    data: UpdatePersonalDataRequest,
  ): Promise<UserDto> {
    return apiClient.put<UserDto>(
      `/api/v1/auth/users/organizations/${organizationId}/users/${userId}/personal-data`,
      data,
    );
  },
};
