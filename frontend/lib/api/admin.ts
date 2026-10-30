/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { buildQuery, fetchAllPageContent } from '@/lib/api/paged';
import type { ListQuery } from '@/lib/list/types';
import type { GroupMembershipType } from '@/types/enums/groupMembershipType';
import type { Salutation } from '@/types/enums/salutation';
import type { SelfOnboardingInvitationDetailDto } from '@/types/onboardingInvitation';
import type {
  CreateChildOrganizationRequest,
  UpdateOrganizationRequest,
} from '@/types/organization';
import type { PageResponse } from '@/types/page';
import type { FederalState, PermissionTypeEnum, SupportedLocale } from '@/types/permission';
import type {
  CreateUserRequest,
  UpdateUserOrganizationGroupMembershipRequest,
  UserDto,
  UserOrganizationGroupMembershipDto,
} from '@/types/user';
import { apiClient } from './client';

export interface TenantDto {
  id: number;
  name: string;
  abbreviation: string;
  locale: SupportedLocale;
  federalState: FederalState;
}

export interface UserSummaryDto {
  id: number;
  salutation?: Salutation | null;
  firstName: string;
  lastName: string;
  username?: string | null;
}

/** Global admin user directory row (all tenants). */
export interface AdminUserListItemDto {
  userId: number;
  tenantId: number;
  tenantName: string;
  username: string;
  firstName: string;
  lastName: string;
  appRole: string;
}

export interface PatchAdminUserAppRoleRequest {
  appRole: string;
}

export interface RootOrganizationSummaryDto {
  id: number;
  name: string;
  organizationCode: string;
  organizationTypeId: number;
  leader?: UserSummaryDto | null;
  hasChildren?: boolean;
  /** True when this org or any descendant has no active leadership group member (from API). */
  leaderMissingInSubtree?: boolean;
}

export interface TenantAdminDetailDto {
  id: number;
  name: string;
  abbreviation: string;
  locale: SupportedLocale;
  federalState: FederalState;
  rootOrganizations: RootOrganizationSummaryDto[];
  /** True when every creatable organization type has a custom standard assigned. */
  standardsConfiguredForOrgCreation: boolean;
}

export interface TenantAdminPatchRequest {
  name?: string;
}

export type RetentionCategory =
  | 'EXIT_IMMEDIATE'
  | 'EXIT_SETTLEMENT'
  | 'DRIVING_LICENSE'
  | 'EVIDENCE'
  | 'ABSENCE'
  | 'TIME_ENTRY'
  | 'EVENT'
  | 'APPOINTMENT'
  | 'WORKFLOW_STALE'
  | 'WORKFLOW'
  | 'TODO'
  | 'NOTIFICATION'
  | 'ONBOARDING'
  | 'AUDIT'
  | 'USER_MASTER';

export type RetentionPeriodUnit = 'DAYS' | 'MONTHS' | 'YEARS';

export interface RetentionCategoryPeriodDto {
  category: RetentionCategory;
  amount: number;
  unit: RetentionPeriodUnit;
  tables: string[];
}

export interface TenantRetentionProfileDto {
  profileKey: string;
  periods: RetentionCategoryPeriodDto[];
}

export interface PurgeTenantRequest {
  confirmationName: string;
}

export interface MoveOrganizationRequest {
  newParentOrganizationId?: number | null;
}

export interface DeactivateOrganizationRequest {
  confirmationName: string;
}

export interface CreateTenantRequest {
  name: string;
  abbreviation: string;
  locale?: SupportedLocale;
  federalState?: FederalState;
}

export interface SetupOrganizationRequest {
  organizationCode: string;
  name: string;
  organizationTypeId: number;
}

export interface AdminOrganizationDetailDto {
  id: number;
  name: string;
  organizationCode: string;
  organizationTypeId: number;
  parentOrganizationId?: number | null;
  leader?: UserSummaryDto | null;
  hasChildren?: boolean;
  /** True when this org or any descendant has no active leadership group member (from API). */
  leaderMissingInSubtree?: boolean;
  /** True when this org may manage other organizations (management module and access). */
  managementCapable?: boolean;
  deactivatedAt?: string | null;
}

/** Minimal org node for admin hierarchy tree (nested children). */
export interface AdminOrganizationTreeNodeDto {
  id: number;
  name: string;
  organizationCode: string;
  organizationTypeId: number;
  /** True when this org has no active leadership group member. */
  leaderMissing: boolean;
  /** True when this org or any descendant has no active leadership group member (from API). */
  leaderMissingInSubtree: boolean;
  deactivatedAt?: string | null;
  children: AdminOrganizationTreeNodeDto[];
}

export interface AdminOrganizationTreeResponseDto {
  tenantId: number;
  tenantName: string;
  roots: AdminOrganizationTreeNodeDto[];
}

export interface CreateAdminLeadershipGroupMembershipRequest {
  userId: number;
  startDate: string;
  endDate?: string;
  membershipType: GroupMembershipType;
}

export interface AdminLeadershipGroupMembershipsDto {
  leadershipGroupId: number;
  memberships: UserOrganizationGroupMembershipDto[];
}

export interface CourseOfferingTypeSettingsDto {
  /** False when the course offering module is off for the organization's standard. */
  enabled: boolean;
  definitionConfigKeys: string[];
}

export interface ReplaceCourseOfferingTypesRequest {
  definitionConfigKeys: string[];
}

export type ManagementAudienceKind = 'ORG' | 'ORG_SUBTREE' | 'ORG_TYPE' | 'TENANT';

export interface ManagementAudienceEntryDto {
  id: number;
  kind: ManagementAudienceKind;
  refId: number | null;
  permissionTypes: PermissionTypeEnum[];
  maxDurationMinutes: number;
}

export interface ManagementOptionDto {
  id: number;
  name: string;
}

export interface ManagementAudienceSettingsDto {
  entries: ManagementAudienceEntryDto[];
  organizations: ManagementOptionDto[];
  organizationTypes: ManagementOptionDto[];
}

export interface ManagementAudienceEntryRequest {
  kind: ManagementAudienceKind;
  refId: number | null;
  permissionTypes: PermissionTypeEnum[];
  maxDurationMinutes: number;
}

export interface ReplaceManagementAudiencesRequest {
  entries: ManagementAudienceEntryRequest[];
}

export type AdminListRequest = ListQuery & { page?: number };

export type ManagementActivationStatus = 'ACTIVE' | 'EXPIRED' | 'ENDED';

/** One row of the management activation log. User fields are null for deleted users. */
export interface ManagementActivationLogItemDto {
  id: number;
  tenantId: number;
  tenantName: string;
  userId: number | null;
  userFirstName: string | null;
  userLastName: string | null;
  managingOrganizationId: number;
  managingOrganizationName: string;
  targetOrganizationId: number;
  targetOrganizationName: string;
  activatedAt: string;
  validUntil: string;
  endedAt: string | null;
  endedBy: 'USER' | 'SYSTEM' | null;
  status: ManagementActivationStatus;
  permissionTypes: PermissionTypeEnum[];
  reason: string;
}

export const adminApi = {
  listTenantsPage(query: AdminListRequest = {}): Promise<PageResponse<TenantDto>> {
    const qs = buildQuery({
      page: query.page ?? 0,
      size: query.size,
      search: query.search,
      sort: query.sort,
      federalState: query.federalState,
    });
    return apiClient.get<PageResponse<TenantDto>>(`/api/v1/auth/admin/tenants?${qs}`);
  },

  listConfiguredFederalStates(): Promise<FederalState[]> {
    return apiClient.get<FederalState[]>('/api/v1/auth/admin/tenants/federal-states');
  },

  getTenant(id: number): Promise<TenantAdminDetailDto> {
    return apiClient.get<TenantAdminDetailDto>(`/api/v1/auth/admin/tenants/${id}`);
  },

  getTenantRetentionProfile(tenantId: number): Promise<TenantRetentionProfileDto> {
    return apiClient.get<TenantRetentionProfileDto>(
      `/api/v1/auth/admin/tenants/${tenantId}/retention`,
    );
  },

  patchTenant(id: number, data: TenantAdminPatchRequest): Promise<TenantAdminDetailDto> {
    return apiClient.patch<TenantAdminDetailDto>(`/api/v1/auth/admin/tenants/${id}`, data);
  },

  purgeTenant(id: number, data: PurgeTenantRequest): Promise<void> {
    return apiClient.post<void>(`/api/v1/auth/admin/tenants/${id}/purge`, data);
  },

  createRootOrganization(
    tenantId: number,
    data: SetupOrganizationRequest,
  ): Promise<TenantAdminDetailDto> {
    return apiClient.post<TenantAdminDetailDto>(
      `/api/v1/auth/admin/tenants/${tenantId}/root-organization`,
      data,
    );
  },

  /** Tenant row only (no organization or user). */
  createTenant(data: CreateTenantRequest): Promise<TenantDto> {
    return apiClient.post<TenantDto>('/api/v1/auth/admin/tenants', data);
  },

  listAdminUsersPage(query: AdminListRequest = {}): Promise<PageResponse<AdminUserListItemDto>> {
    const qs = buildQuery({
      page: query.page ?? 0,
      size: query.size,
      search: query.search,
      sort: query.sort,
      tenantId: query.tenantId,
      organizationId: query.organizationId,
      appRole: query.appRole,
    });
    return apiClient.get<PageResponse<AdminUserListItemDto>>(`/api/v1/auth/admin/users?${qs}`);
  },

  getAdminUser(userId: number): Promise<AdminUserListItemDto> {
    return apiClient.get<AdminUserListItemDto>(`/api/v1/auth/admin/users/${userId}`);
  },

  patchUserAppRole(
    tenantId: number,
    userId: number,
    data: PatchAdminUserAppRoleRequest,
  ): Promise<void> {
    return apiClient.patch<void>(
      `/api/v1/auth/admin/tenants/${tenantId}/users/${userId}/app-role`,
      data,
    );
  },

  deactivateUser(tenantId: number, userId: number): Promise<void> {
    return apiClient.post<void>(
      `/api/v1/auth/admin/tenants/${tenantId}/users/${userId}/deactivate`,
      {},
    );
  },

  getAdminOrganization(tenantId: number, orgId: number): Promise<AdminOrganizationDetailDto> {
    return apiClient.get<AdminOrganizationDetailDto>(
      `/api/v1/auth/admin/tenants/${tenantId}/organizations/${orgId}`,
    );
  },

  getAdminOrganizationUsersPage(
    tenantId: number,
    orgId: number,
    page = 0,
    size?: number,
  ): Promise<PageResponse<UserDto>> {
    const query = buildQuery({ page, size });
    return apiClient.get<PageResponse<UserDto>>(
      `/api/v1/auth/admin/tenants/${tenantId}/organizations/${orgId}/users?${query}`,
    );
  },

  getAdminOrganizationUsers(tenantId: number, orgId: number): Promise<UserDto[]> {
    return fetchAllPageContent((page) =>
      adminApi.getAdminOrganizationUsersPage(tenantId, orgId, page),
    );
  },

  getAdminOrganizationTree(tenantId: number): Promise<AdminOrganizationTreeResponseDto> {
    return apiClient.get<AdminOrganizationTreeResponseDto>(
      `/api/v1/auth/admin/tenants/${tenantId}/organizations/tree`,
    );
  },

  getAdminChildOrganizations(
    tenantId: number,
    orgId: number,
  ): Promise<AdminOrganizationDetailDto[]> {
    return apiClient.get<AdminOrganizationDetailDto[]>(
      `/api/v1/auth/admin/tenants/${tenantId}/organizations/${orgId}/children`,
    );
  },

  createAdminChildOrganization(
    tenantId: number,
    orgId: number,
    data: CreateChildOrganizationRequest,
  ): Promise<AdminOrganizationDetailDto> {
    return apiClient.post<AdminOrganizationDetailDto>(
      `/api/v1/auth/admin/tenants/${tenantId}/organizations/${orgId}/children`,
      data,
    );
  },

  updateAdminOrganization(
    tenantId: number,
    orgId: number,
    data: UpdateOrganizationRequest,
  ): Promise<AdminOrganizationDetailDto> {
    return apiClient.put<AdminOrganizationDetailDto>(
      `/api/v1/auth/admin/tenants/${tenantId}/organizations/${orgId}`,
      data,
    );
  },

  moveAdminOrganization(
    tenantId: number,
    orgId: number,
    data: MoveOrganizationRequest,
  ): Promise<AdminOrganizationDetailDto> {
    return apiClient.post<AdminOrganizationDetailDto>(
      `/api/v1/auth/admin/tenants/${tenantId}/organizations/${orgId}/move`,
      data,
    );
  },

  deactivateAdminOrganization(
    tenantId: number,
    orgId: number,
    data: DeactivateOrganizationRequest,
  ): Promise<void> {
    return apiClient.post<void>(
      `/api/v1/auth/admin/tenants/${tenantId}/organizations/${orgId}/deactivate`,
      data,
    );
  },

  createAdminOrgLeaderUser(
    tenantId: number,
    orgId: number,
    data: CreateUserRequest,
  ): Promise<AdminOrganizationDetailDto> {
    return apiClient.post<AdminOrganizationDetailDto>(
      `/api/v1/auth/admin/tenants/${tenantId}/organizations/${orgId}/leader-user`,
      data,
    );
  },

  getOnboardingAvailable(tenantId: number, orgId: number): Promise<{ available: boolean }> {
    return apiClient.get<{ available: boolean }>(
      `/api/v1/auth/admin/tenants/${tenantId}/organizations/${orgId}/onboarding-available`,
    );
  },

  getOnboardingRecordingAvailable(
    tenantId: number,
    orgId: number,
    userId: number,
  ): Promise<{ available: boolean }> {
    return apiClient.get<{ available: boolean }>(
      `/api/v1/auth/admin/tenants/${tenantId}/organizations/${orgId}/users/${userId}/onboarding-recording-available`,
    );
  },

  getAdminUserOnboardingInvitation(
    tenantId: number,
    orgId: number,
    userId: number,
  ): Promise<SelfOnboardingInvitationDetailDto> {
    return apiClient.get<SelfOnboardingInvitationDetailDto>(
      `/api/v1/auth/admin/tenants/${tenantId}/organizations/${orgId}/users/${userId}/onboarding-invitation`,
    );
  },

  startAdminOnboardingRecording(
    tenantId: number,
    orgId: number,
    userId: number,
  ): Promise<SelfOnboardingInvitationDetailDto> {
    return apiClient.post(
      `/api/v1/auth/admin/tenants/${tenantId}/organizations/${orgId}/users/${userId}/onboarding-invitations`,
      {},
    );
  },

  getAdminOnboardingInvitationDetail(
    tenantId: number,
    orgId: number,
    invitationId: number,
  ): Promise<SelfOnboardingInvitationDetailDto> {
    return apiClient.get(
      `/api/v1/auth/admin/tenants/${tenantId}/organizations/${orgId}/onboarding-invitations/${invitationId}`,
    );
  },

  completeAdminOnboardingInvitation(
    tenantId: number,
    orgId: number,
    invitationId: number,
    adminRecordReason: string,
  ): Promise<void> {
    return apiClient.post(
      `/api/v1/auth/admin/tenants/${tenantId}/organizations/${orgId}/onboarding-invitations/${invitationId}/complete`,
      { adminRecordReason },
    );
  },

  getLeadershipGroupMemberships(
    tenantId: number,
    orgId: number,
  ): Promise<AdminLeadershipGroupMembershipsDto> {
    return apiClient.get<AdminLeadershipGroupMembershipsDto>(
      `/api/v1/auth/admin/tenants/${tenantId}/organizations/${orgId}/leadership-group-memberships`,
    );
  },

  listManagementActivationsPage(
    query: AdminListRequest = {},
  ): Promise<PageResponse<ManagementActivationLogItemDto>> {
    const qs = buildQuery({
      page: query.page ?? 0,
      size: query.size,
      search: query.search,
      sort: query.sort,
      tenantId: query.tenantId,
      status: query.status,
      organizationId: query.organizationId,
    });
    return apiClient.get<PageResponse<ManagementActivationLogItemDto>>(
      `/api/v1/auth/admin/management-activations?${qs}`,
    );
  },

  getManagementActivation(activationId: number): Promise<ManagementActivationLogItemDto> {
    return apiClient.get<ManagementActivationLogItemDto>(
      `/api/v1/auth/admin/management-activations/${activationId}`,
    );
  },

  getManagementAudiences(tenantId: number, orgId: number): Promise<ManagementAudienceSettingsDto> {
    return apiClient.get<ManagementAudienceSettingsDto>(
      `/api/v1/auth/admin/tenants/${tenantId}/organizations/${orgId}/management-audiences`,
    );
  },

  replaceManagementAudiences(
    tenantId: number,
    orgId: number,
    data: ReplaceManagementAudiencesRequest,
  ): Promise<ManagementAudienceSettingsDto> {
    return apiClient.put<ManagementAudienceSettingsDto>(
      `/api/v1/auth/admin/tenants/${tenantId}/organizations/${orgId}/management-audiences`,
      data,
    );
  },

  getCourseOfferingTypes(tenantId: number, orgId: number): Promise<CourseOfferingTypeSettingsDto> {
    return apiClient.get<CourseOfferingTypeSettingsDto>(
      `/api/v1/auth/admin/tenants/${tenantId}/organizations/${orgId}/course-offering-types`,
    );
  },

  replaceCourseOfferingTypes(
    tenantId: number,
    orgId: number,
    data: ReplaceCourseOfferingTypesRequest,
  ): Promise<CourseOfferingTypeSettingsDto> {
    return apiClient.put<CourseOfferingTypeSettingsDto>(
      `/api/v1/auth/admin/tenants/${tenantId}/organizations/${orgId}/course-offering-types`,
      data,
    );
  },

  createLeadershipGroupMembership(
    tenantId: number,
    orgId: number,
    data: CreateAdminLeadershipGroupMembershipRequest,
  ): Promise<UserOrganizationGroupMembershipDto> {
    return apiClient.post<UserOrganizationGroupMembershipDto>(
      `/api/v1/auth/admin/tenants/${tenantId}/organizations/${orgId}/leadership-group-memberships`,
      data,
    );
  },

  updateLeadershipGroupMembership(
    tenantId: number,
    orgId: number,
    membershipId: number,
    data: UpdateUserOrganizationGroupMembershipRequest,
  ): Promise<UserOrganizationGroupMembershipDto> {
    return apiClient.put<UserOrganizationGroupMembershipDto>(
      `/api/v1/auth/admin/tenants/${tenantId}/organizations/${orgId}/leadership-group-memberships/${membershipId}`,
      data,
    );
  },
};
