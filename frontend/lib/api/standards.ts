/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { apiClient } from './client';

export type StandardSource = 'SYSTEM' | 'CUSTOM';

export interface StandardDto {
  id: number;
  kind: string;
  source: StandardSource;
  name: string;
  description?: string | null;
  baseStandardId?: number | null;
  system: boolean;
}

export interface StandardAssignmentDto {
  organizationTypeId: number;
  standardId: number;
}

export interface CreateStandardRequest {
  baseStandardId: number;
  name: string;
  description?: string;
}

export interface UpdateStandardRequest {
  name: string;
  description?: string;
}

export interface AssignStandardRequest {
  standardId: number;
}

/** One organization that overrides its org-type default standard (shown in the standards menu). */
export interface OrganizationStandardOverrideDto {
  organizationId: number;
  organizationName: string;
  organizationCode: string;
  organizationTypeId: number;
  standardId: number;
  standardName?: string | null;
}

/** Standard resolution for a single organization (drives the org-detail "Standard" card). */
export interface OrganizationStandardDto {
  organizationId: number;
  organizationTypeId: number;
  /** True when an explicit per-org override is set; false when inheriting the type default. */
  overridden: boolean;
  overrideStandardId?: number | null;
  typeDefaultStandardName?: string | null;
  effectiveStandardId?: number | null;
  effectiveStandardName?: string | null;
}

/** Set ({@code standardId} number) or clear ({@code standardId} null = inherit) a per-org override. */
export interface SetOrganizationStandardRequest {
  standardId: number | null;
}

export type OrgGroupRemovalImpactLevel = 'NONE' | 'HISTORICAL' | 'ACTIVE';

export interface OrgGroupRemovalImpactDto {
  configKey: string;
  level: OrgGroupRemovalImpactLevel;
  totalOrgsAffected: number;
  totalActiveMemberships: number;
}

export interface StandardSwitchImpactDto {
  removedGroups: OrgGroupRemovalImpactDto[];
}

/** A standard's content section key (matches backend domain keys + the synthetic mappings section). */
export type StandardDomainKey =
  | 'organization-groups'
  | 'permission-groups'
  | 'group-mappings'
  | 'workflow-type-mappings'
  | 'certifications'
  | 'training-courses'
  | 'course-offerings'
  | 'driving-licenses'
  | 'qualifications'
  | 'ranks'
  | 'functions'
  | 'duty-functions';

export type OnboardingStepType =
  | 'ENTRY_DOCUMENTS'
  | 'ORGANIZATION_ENTRY_DOCUMENTS'
  | 'EMERGENCY_CONTACTS'
  | 'TRAINING_COURSES'
  | 'CERTIFICATIONS'
  | 'DRIVING_LICENSE'
  | 'RANK';

export interface OnboardingStepDto {
  stepType: OnboardingStepType;
  sortOrder: number;
  config: Record<string, unknown>;
}

export interface OnboardingStepsResponse {
  steps: OnboardingStepDto[];
}

export interface ReplaceOnboardingStepsRequest {
  steps: OnboardingStepDto[];
}

export interface OnboardingStepTypeDto {
  stepType: OnboardingStepType;
}

export interface OnboardingStepTypesResponse {
  stepTypes: OnboardingStepTypeDto[];
}

export type OrganizationGroupTypeName = 'LEADERSHIP' | 'ALL_MEMBERS' | 'CUSTOM';
export type PermissionGroupTypeName = 'ADMIN' | 'STANDARD' | 'CUSTOM';

export interface StandardContentItemDto {
  domainKey: StandardDomainKey;
  configKey: string;
  title: string;
  subtitle?: string | null;
  /** For permission groups: granted permission type enum names; empty otherwise. */
  permissionTypes: string[];
  /** Raw editable values for prefilling forms (description, autoApprove, enabled, …). */
  attributes: Record<string, string>;
  editable: boolean;
}

export interface StandardContentDto {
  standard: StandardDto;
  items: StandardContentItemDto[];
}

/** Modules enabled for a standard (excludes CORE, which is always implicitly enabled). */
export interface StandardModulesDto {
  modules: string[];
}

export interface CreateOrgGroupRequest {
  name: string;
  description?: string;
  type: OrganizationGroupTypeName;
}

export interface UpdateOrgGroupRequest {
  name: string;
  description?: string;
}

export interface CreatePermissionGroupRequest {
  name: string;
  description?: string;
  type: PermissionGroupTypeName;
  permissionTypes: string[];
}

export interface UpdatePermissionGroupRequest {
  name: string;
  description?: string;
  permissionTypes: string[];
}

export interface UpdateCertificationSelectionsRequest {
  definitionConfigKeys: string[];
}

export interface UpdateTrainingCourseTypeSelectionsRequest {
  definitionConfigKeys: string[];
}

export interface UpdateDrivingLicenseDefinitionSelectionsRequest {
  definitionConfigKeys: string[];
}

export interface UpdateQualificationSelectionsRequest {
  definitionConfigKeys: string[];
}

export interface UpdateRankSelectionsRequest {
  rankConfigKeys: string[];
}

export interface UpdateFunctionSelectionsRequest {
  functionConfigKeys: string[];
}

export type FunctionGrantMembershipType = 'LEADER' | 'DEPUTY_LEADER' | 'MEMBER';

export interface FunctionGrantRequest {
  organizationGroupConfigKey: string;
  membershipType: FunctionGrantMembershipType;
  functionConfigKey: string;
}

export interface UpdateFunctionGrantsRequest {
  grants: FunctionGrantRequest[];
}

export interface CreateGroupMappingRequest {
  orgGroupConfigKey: string;
  permissionGroupConfigKey: string;
}

export interface UpdateWorkflowRequest {
  approverOrgGroupConfigKey: string;
  autoApprove: boolean;
  enabled: boolean;
  selfApproveAllowed: boolean;
}

const base = (tenantId: number) => `/api/v1/auth/admin/tenants/${tenantId}/standards`;

export const standardsApi = {
  listStandards(tenantId: number): Promise<StandardDto[]> {
    return apiClient.get<StandardDto[]>(base(tenantId));
  },

  cloneStandard(tenantId: number, data: CreateStandardRequest): Promise<StandardDto> {
    return apiClient.post<StandardDto>(base(tenantId), data);
  },

  updateStandard(tenantId: number, id: number, data: UpdateStandardRequest): Promise<StandardDto> {
    return apiClient.put<StandardDto>(`${base(tenantId)}/${id}`, data);
  },

  getContent(tenantId: number, id: number): Promise<StandardContentDto> {
    return apiClient.get<StandardContentDto>(`${base(tenantId)}/${id}/content`);
  },

  getEnabledModules(tenantId: number, id: number): Promise<StandardModulesDto> {
    return apiClient.get<StandardModulesDto>(`${base(tenantId)}/${id}/modules`);
  },

  updateEnabledModules(
    tenantId: number,
    id: number,
    data: StandardModulesDto,
  ): Promise<StandardModulesDto> {
    return apiClient.put<StandardModulesDto>(`${base(tenantId)}/${id}/modules`, data);
  },

  addOrgGroup(
    tenantId: number,
    id: number,
    data: CreateOrgGroupRequest,
  ): Promise<StandardContentDto> {
    return apiClient.post<StandardContentDto>(`${base(tenantId)}/${id}/organization-groups`, data);
  },

  updateOrgGroup(
    tenantId: number,
    id: number,
    configKey: string,
    data: UpdateOrgGroupRequest,
  ): Promise<StandardContentDto> {
    return apiClient.put<StandardContentDto>(
      `${base(tenantId)}/${id}/organization-groups/${encodeURIComponent(configKey)}`,
      data,
    );
  },

  deleteOrgGroup(tenantId: number, id: number, configKey: string): Promise<StandardContentDto> {
    return apiClient.delete<StandardContentDto>(
      `${base(tenantId)}/${id}/organization-groups/${encodeURIComponent(configKey)}`,
    );
  },

  getOrgGroupRemovalImpact(
    tenantId: number,
    id: number,
    configKey: string,
  ): Promise<OrgGroupRemovalImpactDto> {
    return apiClient.get<OrgGroupRemovalImpactDto>(
      `${base(tenantId)}/${id}/organization-groups/${encodeURIComponent(configKey)}/removal-impact`,
    );
  },

  getAssignmentImpact(
    tenantId: number,
    organizationTypeId: number,
    standardId: number,
  ): Promise<StandardSwitchImpactDto> {
    return apiClient.get<StandardSwitchImpactDto>(
      `${base(tenantId)}/org-types/${organizationTypeId}/assignment-impact?standardId=${standardId}`,
    );
  },

  getOrganizationStandardImpact(
    tenantId: number,
    organizationId: number,
    standardId: number | null,
  ): Promise<StandardSwitchImpactDto> {
    const query = standardId != null ? `?standardId=${standardId}` : '';
    return apiClient.get<StandardSwitchImpactDto>(
      `${base(tenantId)}/organizations/${organizationId}/standard-impact${query}`,
    );
  },

  addPermissionGroup(
    tenantId: number,
    id: number,
    data: CreatePermissionGroupRequest,
  ): Promise<StandardContentDto> {
    return apiClient.post<StandardContentDto>(`${base(tenantId)}/${id}/permission-groups`, data);
  },

  updatePermissionGroup(
    tenantId: number,
    id: number,
    configKey: string,
    data: UpdatePermissionGroupRequest,
  ): Promise<StandardContentDto> {
    return apiClient.put<StandardContentDto>(
      `${base(tenantId)}/${id}/permission-groups/${encodeURIComponent(configKey)}`,
      data,
    );
  },

  deletePermissionGroup(
    tenantId: number,
    id: number,
    configKey: string,
  ): Promise<StandardContentDto> {
    return apiClient.delete<StandardContentDto>(
      `${base(tenantId)}/${id}/permission-groups/${encodeURIComponent(configKey)}`,
    );
  },

  addGroupMapping(
    tenantId: number,
    id: number,
    data: CreateGroupMappingRequest,
  ): Promise<StandardContentDto> {
    return apiClient.post<StandardContentDto>(`${base(tenantId)}/${id}/group-mappings`, data);
  },

  deleteGroupMapping(
    tenantId: number,
    id: number,
    orgGroupConfigKey: string,
    permissionGroupConfigKey: string,
  ): Promise<StandardContentDto> {
    return apiClient.delete<StandardContentDto>(
      `${base(tenantId)}/${id}/group-mappings/${encodeURIComponent(orgGroupConfigKey)}/${encodeURIComponent(permissionGroupConfigKey)}`,
    );
  },

  updateWorkflow(
    tenantId: number,
    id: number,
    configKey: string,
    data: UpdateWorkflowRequest,
  ): Promise<StandardContentDto> {
    return apiClient.put<StandardContentDto>(
      `${base(tenantId)}/${id}/workflows/${encodeURIComponent(configKey)}`,
      data,
    );
  },

  updateCertificationSelections(
    tenantId: number,
    id: number,
    data: UpdateCertificationSelectionsRequest,
  ): Promise<StandardContentDto> {
    return apiClient.put<StandardContentDto>(
      `${base(tenantId)}/${id}/certification-selections`,
      data,
    );
  },

  updateTrainingCourseTypeSelections(
    tenantId: number,
    id: number,
    data: UpdateTrainingCourseTypeSelectionsRequest,
  ): Promise<StandardContentDto> {
    return apiClient.put<StandardContentDto>(
      `${base(tenantId)}/${id}/training-course-type-selections`,
      data,
    );
  },

  updateDrivingLicenseDefinitionSelections(
    tenantId: number,
    id: number,
    data: UpdateDrivingLicenseDefinitionSelectionsRequest,
  ): Promise<StandardContentDto> {
    return apiClient.put<StandardContentDto>(
      `${base(tenantId)}/${id}/driving-license-selections`,
      data,
    );
  },

  updateQualificationSelections(
    tenantId: number,
    id: number,
    data: UpdateQualificationSelectionsRequest,
  ): Promise<StandardContentDto> {
    return apiClient.put<StandardContentDto>(
      `${base(tenantId)}/${id}/qualification-selections`,
      data,
    );
  },

  updateRankSelections(
    tenantId: number,
    id: number,
    data: UpdateRankSelectionsRequest,
  ): Promise<StandardContentDto> {
    return apiClient.put<StandardContentDto>(`${base(tenantId)}/${id}/rank-selections`, data);
  },

  updateFunctionSelections(
    tenantId: number,
    id: number,
    data: UpdateFunctionSelectionsRequest,
  ): Promise<StandardContentDto> {
    return apiClient.put<StandardContentDto>(`${base(tenantId)}/${id}/function-selections`, data);
  },

  updateFunctionGrants(
    tenantId: number,
    id: number,
    data: UpdateFunctionGrantsRequest,
  ): Promise<StandardContentDto> {
    return apiClient.put<StandardContentDto>(`${base(tenantId)}/${id}/function-grants`, data);
  },

  getOnboardingSteps(tenantId: number, id: number): Promise<OnboardingStepsResponse> {
    return apiClient.get<OnboardingStepsResponse>(`${base(tenantId)}/${id}/onboarding-steps`);
  },

  replaceOnboardingSteps(
    tenantId: number,
    id: number,
    data: ReplaceOnboardingStepsRequest,
  ): Promise<OnboardingStepsResponse> {
    return apiClient.put<OnboardingStepsResponse>(`${base(tenantId)}/${id}/onboarding-steps`, data);
  },

  listOnboardingStepTypes(tenantId: number): Promise<OnboardingStepTypesResponse> {
    return apiClient.get<OnboardingStepTypesResponse>(`${base(tenantId)}/onboarding-step-types`);
  },

  deleteStandard(tenantId: number, id: number): Promise<void> {
    return apiClient.delete<void>(`${base(tenantId)}/${id}`);
  },

  listAssignments(tenantId: number): Promise<StandardAssignmentDto[]> {
    return apiClient.get<StandardAssignmentDto[]>(`${base(tenantId)}/assignments`);
  },

  assignStandard(
    tenantId: number,
    organizationTypeId: number,
    data: AssignStandardRequest,
  ): Promise<StandardAssignmentDto> {
    return apiClient.put<StandardAssignmentDto>(
      `${base(tenantId)}/org-types/${organizationTypeId}`,
      data,
    );
  },

  listOrganizationOverrides(tenantId: number): Promise<OrganizationStandardOverrideDto[]> {
    return apiClient.get<OrganizationStandardOverrideDto[]>(
      `${base(tenantId)}/organization-overrides`,
    );
  },

  getOrganizationStandard(
    tenantId: number,
    organizationId: number,
  ): Promise<OrganizationStandardDto> {
    return apiClient.get<OrganizationStandardDto>(
      `${base(tenantId)}/organizations/${organizationId}/standard`,
    );
  },

  setOrganizationStandard(
    tenantId: number,
    organizationId: number,
    data: SetOrganizationStandardRequest,
  ): Promise<OrganizationStandardDto> {
    return apiClient.put<OrganizationStandardDto>(
      `${base(tenantId)}/organizations/${organizationId}/standard`,
      data,
    );
  },
};
