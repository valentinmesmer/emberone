/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { type QueryClient, queryOptions, useQuery } from '@tanstack/react-query';
import { adminApi } from '@/lib/api/admin';
import { adminOrganizationOnboardingDocumentsApi } from '@/lib/api/adminOrganizationOnboardingDocuments';
import { organizationTypesApi } from '@/lib/api/organizationTypes';
import { standardsApi } from '@/lib/api/standards';
import { tenantCertificationsApi } from '@/lib/api/tenantCertifications';
import { tenantDrivingLicenseDefinitionsApi } from '@/lib/api/tenantDrivingLicenseDefinitions';
import { tenantFunctionsApi } from '@/lib/api/tenantFunctions';
import { tenantOnboardingDocumentsApi } from '@/lib/api/tenantOnboardingDocuments';
import { tenantQualificationsApi } from '@/lib/api/tenantQualifications';
import { tenantRanksApi } from '@/lib/api/tenantRanks';
import { tenantTrainingCourseTypesApi } from '@/lib/api/tenantTrainingCourseTypes';
import { workflowsApi } from '@/lib/api/workflows';
import { STALE_TIME } from '@/lib/store/staleTime';
import { invalidateAdminTenant } from './adminLists';

/** Tenant-scoped catalogs managed in the admin area (`/api/v1/auth/admin/tenants/{id}/...`). */
export type AdminTenantCatalog =
  | 'ranks'
  | 'qualifications'
  | 'trainingCourseTypes'
  | 'certifications'
  | 'drivingLicenses'
  | 'functions'
  | 'onboardingDocuments'
  | 'organizationTypes'
  | 'standards'
  | 'workflows';

export const adminTenantCatalogKeys = {
  all: ['admin', 'tenantCatalogs'] as const,
  tree: (tenantId: number) => [...adminTenantCatalogKeys.all, 'tree', tenantId] as const,
  /** One catalog of a tenant: list and all details. */
  catalog: (catalog: AdminTenantCatalog, tenantId: number) =>
    [...adminTenantCatalogKeys.all, catalog, tenantId] as const,
  list: (catalog: AdminTenantCatalog, tenantId: number) =>
    [...adminTenantCatalogKeys.catalog(catalog, tenantId), 'list'] as const,
  detail: (catalog: AdminTenantCatalog, tenantId: number, configKey: string) =>
    [...adminTenantCatalogKeys.catalog(catalog, tenantId), 'detail', configKey] as const,
  /** Organization details and child lists of a tenant. */
  organizations: (tenantId: number) =>
    [...adminTenantCatalogKeys.all, 'organizations', tenantId] as const,
  organization: (tenantId: number, organizationId: number) =>
    [...adminTenantCatalogKeys.organizations(tenantId), 'detail', organizationId] as const,
  childOrganizations: (tenantId: number, organizationId: number) =>
    [...adminTenantCatalogKeys.organizations(tenantId), 'children', organizationId] as const,
  /** Content, enabled modules, onboarding steps and workflows of one standard. */
  standardContent: (tenantId: number, standardId: number) =>
    [...adminTenantCatalogKeys.catalog('standards', tenantId), 'content', standardId] as const,
  standardModules: (tenantId: number, standardId: number) =>
    [...adminTenantCatalogKeys.catalog('standards', tenantId), 'modules', standardId] as const,
  standardOnboardingSteps: (tenantId: number, standardId: number) =>
    [
      ...adminTenantCatalogKeys.catalog('standards', tenantId),
      'onboardingSteps',
      standardId,
    ] as const,
  /** Step types a standard can use (static per deployment). */
  standardOnboardingStepTypes: (tenantId: number) =>
    [...adminTenantCatalogKeys.catalog('standards', tenantId), 'onboardingStepTypes'] as const,
  standardAssignments: (tenantId: number) =>
    [...adminTenantCatalogKeys.catalog('standards', tenantId), 'assignments'] as const,
  standardOrganizationOverrides: (tenantId: number) =>
    [...adminTenantCatalogKeys.catalog('standards', tenantId), 'organizationOverrides'] as const,
  standardWorkflow: (tenantId: number, standardId: number, configKey: string) =>
    [
      ...adminTenantCatalogKeys.catalog('standards', tenantId),
      'workflow',
      standardId,
      configKey,
    ] as const,
  /** Handler catalog of workflow types (static per deployment). */
  workflowTypes: (tenantId: number) =>
    [...adminTenantCatalogKeys.all, 'workflowTypes', tenantId] as const,
  /** Tenant default definitions and their routing rules (catalog `workflows`). */
  workflowDefinitions: (tenantId: number) =>
    [...adminTenantCatalogKeys.list('workflows', tenantId)] as const,
  workflowRoutingRule: (tenantId: number, workflowTypeId: number, ruleId: number) =>
    [
      ...adminTenantCatalogKeys.catalog('workflows', tenantId),
      'routingRule',
      workflowTypeId,
      ruleId,
    ] as const,
  organizationOnboardingAvailable: (tenantId: number, organizationId: number) =>
    [
      ...adminTenantCatalogKeys.organizations(tenantId),
      'onboardingAvailable',
      organizationId,
    ] as const,
  organizationStandard: (tenantId: number, organizationId: number) =>
    [...adminTenantCatalogKeys.organizations(tenantId), 'standard', organizationId] as const,
  organizationOnboardingDocuments: (tenantId: number, organizationId: number) =>
    [
      ...adminTenantCatalogKeys.all,
      'organizationOnboardingDocuments',
      tenantId,
      organizationId,
    ] as const,
};

/** Refreshes list and details of one tenant catalog after a mutation. */
export function invalidateAdminTenantCatalog(
  queryClient: QueryClient,
  catalog: AdminTenantCatalog,
  tenantId: number,
): Promise<void> {
  // Standard content embeds names from the catalogs, so catalog changes refresh it too.
  if (catalog !== 'standards') {
    void queryClient.invalidateQueries({
      queryKey: adminTenantCatalogKeys.catalog('standards', tenantId),
    });
  }
  return queryClient.invalidateQueries({
    queryKey: adminTenantCatalogKeys.catalog(catalog, tenantId),
  });
}

/** Refreshes only the list of a tenant catalog (details are updated by the caller). */
export function invalidateAdminTenantCatalogList(
  queryClient: QueryClient,
  catalog: AdminTenantCatalog,
  tenantId: number,
): Promise<void> {
  return queryClient.invalidateQueries({
    queryKey: adminTenantCatalogKeys.list(catalog, tenantId),
  });
}

/**
 * Marks a tenant catalog stale without refetching mounted queries. For mutations that leave the
 * detail page (delete, archive): refetching the removed detail would fail before navigation.
 */
export function markAdminTenantCatalogStale(
  queryClient: QueryClient,
  catalog: AdminTenantCatalog,
  tenantId: number,
): Promise<void> {
  return queryClient.invalidateQueries({
    queryKey: adminTenantCatalogKeys.catalog(catalog, tenantId),
    refetchType: 'none',
  });
}

export function invalidateAdminOrganizationTree(
  queryClient: QueryClient,
  tenantId: number,
): Promise<void> {
  return queryClient.invalidateQueries({ queryKey: adminTenantCatalogKeys.tree(tenantId) });
}

/** After a structure change: organization details, child lists, tree and tenant summary. */
export function invalidateAdminOrganizations(
  queryClient: QueryClient,
  tenantId: number,
): Promise<void> {
  void queryClient.invalidateQueries({ queryKey: adminTenantCatalogKeys.tree(tenantId) });
  void invalidateAdminTenant(queryClient, tenantId);
  return queryClient.invalidateQueries({
    queryKey: adminTenantCatalogKeys.organizations(tenantId),
  });
}

export function invalidateAdminOrganizationOnboardingDocuments(
  queryClient: QueryClient,
  tenantId: number,
  organizationId: number,
): Promise<void> {
  return queryClient.invalidateQueries({
    queryKey: adminTenantCatalogKeys.organizationOnboardingDocuments(tenantId, organizationId),
  });
}

/**
 * Whether onboarding is available for an organization. Only needed once, right after creating a
 * leader, so callers use `queryClient.fetchQuery` instead of a mounted hook.
 */
export function adminOrganizationOnboardingAvailableQueryOptions(
  tenantId: number,
  organizationId: number,
) {
  return queryOptions({
    queryKey: adminTenantCatalogKeys.organizationOnboardingAvailable(tenantId, organizationId),
    queryFn: () => adminApi.getOnboardingAvailable(tenantId, organizationId),
    staleTime: STALE_TIME.STANDARD,
  });
}

/** Organization tree of a tenant (`GET .../organizations/tree`). */
export function useAdminOrganizationTree(tenantId: number, enabled = true) {
  return useQuery({
    queryKey: adminTenantCatalogKeys.tree(tenantId),
    queryFn: () => adminApi.getAdminOrganizationTree(tenantId),
    enabled: enabled && tenantId > 0,
    // Includes leader issues derived from memberships, so it is a list, not a catalog.
    staleTime: STALE_TIME.LIST,
  });
}

export function useAdminOrganization(tenantId: number, organizationId: number, enabled = true) {
  return useQuery({
    queryKey: adminTenantCatalogKeys.organization(tenantId, organizationId),
    queryFn: () => adminApi.getAdminOrganization(tenantId, organizationId),
    enabled: enabled && tenantId > 0 && organizationId > 0,
    staleTime: STALE_TIME.LIST,
  });
}

export function useAdminChildOrganizations(
  tenantId: number,
  organizationId: number,
  enabled = true,
) {
  return useQuery({
    queryKey: adminTenantCatalogKeys.childOrganizations(tenantId, organizationId),
    queryFn: () => adminApi.getAdminChildOrganizations(tenantId, organizationId),
    enabled: enabled && tenantId > 0 && organizationId > 0,
    staleTime: STALE_TIME.LIST,
  });
}

function listOptions(catalog: AdminTenantCatalog, tenantId: number, enabled: boolean) {
  return {
    queryKey: adminTenantCatalogKeys.list(catalog, tenantId),
    enabled: enabled && tenantId > 0,
    staleTime: STALE_TIME.STANDARD,
  };
}

function detailOptions(
  catalog: AdminTenantCatalog,
  tenantId: number,
  configKey: string,
  enabled: boolean,
) {
  return {
    queryKey: adminTenantCatalogKeys.detail(catalog, tenantId, configKey),
    enabled: enabled && tenantId > 0 && configKey !== '',
    staleTime: STALE_TIME.STANDARD,
  };
}

export function useTenantOrganizationTypes(tenantId: number, enabled = true) {
  return useQuery({
    ...listOptions('organizationTypes', tenantId, enabled),
    queryFn: () => organizationTypesApi.list(tenantId),
  });
}

export function useTenantRanks(tenantId: number, enabled = true) {
  return useQuery({
    ...listOptions('ranks', tenantId, enabled),
    queryFn: () => tenantRanksApi.list(tenantId),
  });
}

export function useTenantRank(tenantId: number, configKey: string, enabled = true) {
  return useQuery({
    ...detailOptions('ranks', tenantId, configKey, enabled),
    queryFn: () => tenantRanksApi.get(tenantId, configKey),
  });
}

export function useTenantQualifications(tenantId: number, enabled = true) {
  return useQuery({
    ...listOptions('qualifications', tenantId, enabled),
    queryFn: () => tenantQualificationsApi.list(tenantId),
  });
}

export function useTenantQualification(tenantId: number, configKey: string, enabled = true) {
  return useQuery({
    ...detailOptions('qualifications', tenantId, configKey, enabled),
    queryFn: () => tenantQualificationsApi.get(tenantId, configKey),
  });
}

export function useTenantTrainingCourseTypes(tenantId: number, enabled = true) {
  return useQuery({
    ...listOptions('trainingCourseTypes', tenantId, enabled),
    queryFn: () => tenantTrainingCourseTypesApi.list(tenantId),
  });
}

export function useTenantTrainingCourseType(tenantId: number, configKey: string, enabled = true) {
  return useQuery({
    ...detailOptions('trainingCourseTypes', tenantId, configKey, enabled),
    queryFn: () => tenantTrainingCourseTypesApi.get(tenantId, configKey),
  });
}

export function useTenantCertifications(tenantId: number, enabled = true) {
  return useQuery({
    ...listOptions('certifications', tenantId, enabled),
    queryFn: () => tenantCertificationsApi.list(tenantId),
  });
}

export function useTenantCertification(tenantId: number, configKey: string, enabled = true) {
  return useQuery({
    ...detailOptions('certifications', tenantId, configKey, enabled),
    queryFn: () => tenantCertificationsApi.get(tenantId, configKey),
  });
}

export function useTenantDrivingLicenseDefinitions(tenantId: number, enabled = true) {
  return useQuery({
    ...listOptions('drivingLicenses', tenantId, enabled),
    queryFn: () => tenantDrivingLicenseDefinitionsApi.list(tenantId),
  });
}

export function useTenantDrivingLicenseDefinition(
  tenantId: number,
  configKey: string,
  enabled = true,
) {
  return useQuery({
    ...detailOptions('drivingLicenses', tenantId, configKey, enabled),
    queryFn: () => tenantDrivingLicenseDefinitionsApi.get(tenantId, configKey),
  });
}

export function useTenantFunctions(tenantId: number, enabled = true) {
  return useQuery({
    ...listOptions('functions', tenantId, enabled),
    queryFn: () => tenantFunctionsApi.list(tenantId),
  });
}

export function useTenantFunction(tenantId: number, configKey: string, enabled = true) {
  return useQuery({
    ...detailOptions('functions', tenantId, configKey, enabled),
    queryFn: () => tenantFunctionsApi.get(tenantId, configKey),
  });
}

export function useTenantOnboardingDocuments(tenantId: number, enabled = true) {
  return useQuery({
    ...listOptions('onboardingDocuments', tenantId, enabled),
    queryFn: () => tenantOnboardingDocumentsApi.list(tenantId),
  });
}

export function useTenantOnboardingDocument(tenantId: number, configKey: string, enabled = true) {
  return useQuery({
    ...detailOptions('onboardingDocuments', tenantId, configKey, enabled),
    queryFn: () => tenantOnboardingDocumentsApi.get(tenantId, configKey),
  });
}

export function useAdminOrganizationOnboardingDocuments(
  tenantId: number,
  organizationId: number,
  enabled = true,
) {
  return useQuery({
    queryKey: adminTenantCatalogKeys.organizationOnboardingDocuments(tenantId, organizationId),
    queryFn: () => adminOrganizationOnboardingDocumentsApi.list(tenantId, organizationId),
    enabled: enabled && tenantId > 0 && organizationId > 0,
    staleTime: STALE_TIME.STANDARD,
  });
}

/** Shared by the hook and one-off `queryClient.fetchQuery` calls so both use the same cache entry. */
export function standardContentQueryOptions(tenantId: number, standardId: number) {
  return queryOptions({
    queryKey: adminTenantCatalogKeys.standardContent(tenantId, standardId),
    queryFn: () => standardsApi.getContent(tenantId, standardId),
    staleTime: STALE_TIME.STANDARD,
  });
}

export function useStandardContentQuery(tenantId: number, standardId: number, enabled = true) {
  return useQuery({
    ...standardContentQueryOptions(tenantId, standardId),
    enabled: enabled && tenantId > 0 && standardId > 0,
  });
}

export function useStandardEnabledModules(tenantId: number, standardId: number, enabled = true) {
  return useQuery({
    queryKey: adminTenantCatalogKeys.standardModules(tenantId, standardId),
    queryFn: () => standardsApi.getEnabledModules(tenantId, standardId),
    enabled: enabled && tenantId > 0 && standardId > 0,
    staleTime: STALE_TIME.STANDARD,
  });
}

export function useStandardOnboardingSteps(
  tenantId: number,
  standardId: number | null | undefined,
  enabled = true,
) {
  const id = standardId ?? 0;
  return useQuery({
    queryKey: adminTenantCatalogKeys.standardOnboardingSteps(tenantId, id),
    queryFn: () => standardsApi.getOnboardingSteps(tenantId, id),
    enabled: enabled && tenantId > 0 && id > 0,
    staleTime: STALE_TIME.STANDARD,
  });
}

export function useStandardOnboardingStepTypes(tenantId: number, enabled = true) {
  return useQuery({
    queryKey: adminTenantCatalogKeys.standardOnboardingStepTypes(tenantId),
    queryFn: () => standardsApi.listOnboardingStepTypes(tenantId),
    enabled: enabled && tenantId > 0,
    staleTime: STALE_TIME.STANDARD,
  });
}

export function useStandardAssignments(tenantId: number, enabled = true) {
  return useQuery({
    queryKey: adminTenantCatalogKeys.standardAssignments(tenantId),
    queryFn: () => standardsApi.listAssignments(tenantId),
    enabled: enabled && tenantId > 0,
    staleTime: STALE_TIME.STANDARD,
  });
}

export function useStandardOrganizationOverrides(tenantId: number, enabled = true) {
  return useQuery({
    queryKey: adminTenantCatalogKeys.standardOrganizationOverrides(tenantId),
    queryFn: () => standardsApi.listOrganizationOverrides(tenantId),
    enabled: enabled && tenantId > 0,
    staleTime: STALE_TIME.STANDARD,
  });
}

export function useStandardWorkflow(
  tenantId: number,
  standardId: number,
  configKey: string,
  enabled = true,
) {
  return useQuery({
    queryKey: adminTenantCatalogKeys.standardWorkflow(tenantId, standardId, configKey),
    queryFn: () => standardsApi.getWorkflow(tenantId, standardId, configKey),
    enabled: enabled && tenantId > 0 && standardId > 0 && configKey !== '',
    staleTime: STALE_TIME.STANDARD,
  });
}

export function useAdminOrganizationStandard(
  tenantId: number,
  organizationId: number,
  enabled = true,
) {
  return useQuery({
    queryKey: adminTenantCatalogKeys.organizationStandard(tenantId, organizationId),
    queryFn: () => standardsApi.getOrganizationStandard(tenantId, organizationId),
    enabled: enabled && tenantId > 0 && organizationId > 0,
    staleTime: STALE_TIME.STANDARD,
  });
}

export function useTenantStandards(tenantId: number, enabled = true) {
  return useQuery({
    ...listOptions('standards', tenantId, enabled),
    queryFn: () => standardsApi.listStandards(tenantId),
  });
}

export function useAdminWorkflowTypeCatalog(tenantId: number, enabled = true) {
  return useQuery({
    queryKey: adminTenantCatalogKeys.workflowTypes(tenantId),
    queryFn: () => workflowsApi.getWorkflowTypeCatalog(tenantId),
    enabled: enabled && tenantId > 0,
    staleTime: STALE_TIME.STANDARD,
  });
}

/** All tenant default workflow definitions in one request (list and detail views share it). */
export function useAdminTenantWorkflowDefinitions(tenantId: number, enabled = true) {
  return useQuery({
    queryKey: adminTenantCatalogKeys.workflowDefinitions(tenantId),
    queryFn: () => workflowsApi.listTenantWorkflowDefinitions(tenantId),
    enabled: enabled && tenantId > 0,
    staleTime: STALE_TIME.STANDARD,
  });
}

export function useAdminTenantWorkflowRoutingRule(
  tenantId: number,
  workflowTypeId: number,
  ruleId: number,
  enabled = true,
) {
  return useQuery({
    queryKey: adminTenantCatalogKeys.workflowRoutingRule(tenantId, workflowTypeId, ruleId),
    queryFn: () => workflowsApi.getTenantWorkflowRoutingRule(tenantId, workflowTypeId, ruleId),
    enabled: enabled && tenantId > 0 && workflowTypeId > 0 && ruleId > 0,
    staleTime: STALE_TIME.STANDARD,
  });
}
