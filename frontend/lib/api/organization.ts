/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

// Organization API endpoints

import type { ListQuery } from '@/lib/list/types';
import type { OrganizationDto } from '@/types/organization';
import {
  isJoinRequestableOrganizationGroup,
  isLeaveRequestableOrganizationGroup,
  type OrganizationGroupDto,
  type OrganizationGroupJoinTargetDto,
  type OrganizationGroupLeaveTargetDto,
  type OrganizationGroupMemberPageDto,
  type OrganizationGroupPageDto,
} from '@/types/organizationGroup';
import type { UserOrganizationMembershipDto } from '@/types/user';
import { apiClient } from './client';
import { buildQuery } from './paged';

export const organizationApi = {
  async getMyOrganizations(): Promise<UserOrganizationMembershipDto[]> {
    return apiClient.get<UserOrganizationMembershipDto[]>('/api/v1/self/organizations');
  },

  async getOrganizationById(id: number): Promise<OrganizationDto> {
    return apiClient.get<OrganizationDto>(`/api/v1/self/organizations/${id}`);
  },

  /** Current user's groups in one organization. */
  async getMyOrganizationGroups(organizationId: number): Promise<OrganizationGroupDto[]> {
    return apiClient.get<OrganizationGroupDto[]>(
      `/api/v1/self/organizations/${organizationId}/groups`,
    );
  },

  async getJoinableOrganizations(): Promise<OrganizationDto[]> {
    return apiClient.get<OrganizationDto[]>('/api/v1/self/organizations/joinable');
  },

  async getMainOrganizationChangeTargets(): Promise<OrganizationDto[]> {
    return apiClient.get<OrganizationDto[]>('/api/v1/self/organizations/main-change-targets');
  },

  /** Organizations the current user may request to leave. Leadership members are omitted. */
  async getOrganizationLeaveTargets(): Promise<OrganizationDto[]> {
    return apiClient.get<OrganizationDto[]>('/api/v1/self/organizations/leave-targets');
  },

  /** Custom groups the current user may request to leave. Leadership is omitted. */
  async getOrganizationGroupLeaveTargets(): Promise<OrganizationGroupLeaveTargetDto[]> {
    const targets = await apiClient.get<OrganizationGroupLeaveTargetDto[]>(
      '/api/v1/self/organizations/group-leave-targets',
    );
    return targets.filter((target) =>
      isLeaveRequestableOrganizationGroup(target.organizationGroupType),
    );
  },

  /** Custom groups the current user may request to join. Leadership and existing memberships are omitted. */
  async getOrganizationGroupJoinTargets(): Promise<OrganizationGroupJoinTargetDto[]> {
    const targets = await apiClient.get<OrganizationGroupJoinTargetDto[]>(
      '/api/v1/self/organizations/group-join-targets',
    );
    return targets.filter((target) =>
      isJoinRequestableOrganizationGroup(target.organizationGroupType),
    );
  },

  /** All organization groups (requires ORGANIZATION_GROUP_READ). */
  async listOrganizationGroups(
    organizationId: number,
    query: ListQuery & { status?: string },
  ): Promise<OrganizationGroupPageDto> {
    const qs = buildQuery({
      organizationId,
      page: query.page ?? 0,
      size: query.size,
      search: query.search,
      sort: query.sort,
      status: query.status,
    });
    return apiClient.get<OrganizationGroupPageDto>(`/api/v1/auth/organizations/groups?${qs}`);
  },

  async getOrganizationGroupById(groupId: number): Promise<OrganizationGroupDto> {
    return apiClient.get<OrganizationGroupDto>(`/api/v1/auth/organizations/groups/${groupId}`);
  },

  /** Groups available for new membership assignment (requires USER_WRITE). */
  async getAssignableOrganizationGroups(organizationId: number): Promise<OrganizationGroupDto[]> {
    return apiClient.get<OrganizationGroupDto[]>(
      `/api/v1/auth/organizations/${organizationId}/groups/assignable`,
    );
  },

  /** Groups available as appointment audience (requires APPOINTMENT_WRITE). */
  async getAppointmentAudienceGroups(organizationId: number): Promise<OrganizationGroupDto[]> {
    return apiClient.get<OrganizationGroupDto[]>(
      `/api/v1/auth/organizations/${organizationId}/groups/audience`,
    );
  },

  async listOrganizationGroupMembers(
    groupId: number,
    query: ListQuery,
  ): Promise<OrganizationGroupMemberPageDto> {
    const qs = buildQuery({
      page: query.page ?? 0,
      size: query.size,
      search: query.search,
      sort: query.sort,
    });
    const path = `/api/v1/auth/organizations/groups/${groupId}/members`;
    return apiClient.get<OrganizationGroupMemberPageDto>(qs ? `${path}?${qs}` : path);
  },
};
