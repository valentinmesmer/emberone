/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import {
  type CourseRegistrationListRequest,
  courseOfferingsApi,
  type OrgCourseOfferingListRequest,
  type SelfCourseOfferingListRequest,
} from '@/lib/api/courseOfferings';
import { workflowsApi } from '@/lib/api/workflows';
import {
  offeringRegistrationListDefinition,
  orgCourseOfferingListDefinition,
  orgCourseOfferingTemplateListDefinition,
  orgCourseRegistrationListDefinition,
  selfCourseOfferingListDefinition,
  selfCourseOfferingListQuery,
  userCourseRegistrationListDefinition,
} from '@/lib/list/courseOfferingList';
import type { ListQuery, ListQueryState } from '@/lib/list/types';
import { useListState, useUrlSyncedListState } from '@/lib/list/useListState';
import { invalidateWorkflowLists } from '@/lib/store/queries/workflows';
import { STALE_TIME } from '@/lib/store/staleTime';
import { usePagedList } from '@/lib/store/usePagedList';
import type {
  CourseAudienceEntryRequest,
  CourseSessionRequest,
  CreateCourseOfferingRequest,
  ReplaceCoursePrerequisitesRequest,
  UpdateCourseOfferingRequest,
} from '@/types/courseOffering';

export const courseOfferingKeys = {
  all: ['course-offerings'] as const,
  /** Lists, templates and details of one organization's offerings. */
  orgRoot: (organizationId: number) => [...courseOfferingKeys.all, 'org', organizationId] as const,
  orgList: (organizationId: number, listQuery: ListQuery) =>
    [...courseOfferingKeys.orgRoot(organizationId), listQuery] as const,
  orgRegistrations: (organizationId: number, listQuery: ListQuery) =>
    [...courseOfferingKeys.all, 'org-registrations', organizationId, listQuery] as const,
  orgTemplateList: (organizationId: number, listQuery: ListQuery) =>
    [...courseOfferingKeys.orgRoot(organizationId), 'templates', listQuery] as const,
  orgDetail: (organizationId: number, offeringId: number) =>
    [...courseOfferingKeys.orgRoot(organizationId), offeringId] as const,
  offerableTypes: (organizationId: number) =>
    [...courseOfferingKeys.all, 'offerable-types', organizationId] as const,
  audienceOptions: (organizationId: number) =>
    [...courseOfferingKeys.all, 'audience-options', organizationId] as const,
  registrationSummary: (organizationId: number, offeringId: number) =>
    [...courseOfferingKeys.all, 'registrations-summary', organizationId, offeringId] as const,
  /** Registration lists and details of one offering. */
  offeringRegistrationsRoot: (organizationId: number, offeringId: number) =>
    [...courseOfferingKeys.all, 'offering-registrations', organizationId, offeringId] as const,
  offeringRegistrations: (organizationId: number, offeringId: number, listQuery: ListQuery) =>
    [
      ...courseOfferingKeys.offeringRegistrationsRoot(organizationId, offeringId),
      listQuery,
    ] as const,
  offeringRegistration: (organizationId: number, offeringId: number, registrationId: number) =>
    [
      ...courseOfferingKeys.offeringRegistrationsRoot(organizationId, offeringId),
      'detail',
      registrationId,
    ] as const,
  completion: (organizationId: number, offeringId: number) =>
    [...courseOfferingKeys.all, 'completion', organizationId, offeringId] as const,
  selfOfferingsRoot: () => [...courseOfferingKeys.all, 'self-offerings'] as const,
  selfOfferings: (listQuery: ListQuery) =>
    [...courseOfferingKeys.selfOfferingsRoot(), listQuery] as const,
  selfOfferingDetailRoot: () => [...courseOfferingKeys.all, 'self-offering'] as const,
  selfOfferingDetail: (offeringId: number) =>
    [...courseOfferingKeys.selfOfferingDetailRoot(), offeringId] as const,
  userRegistrations: (organizationId: number, userId: number, listQuery: ListQuery) =>
    [...courseOfferingKeys.all, 'user-registrations', organizationId, userId, listQuery] as const,
  userRegistration: (organizationId: number, userId: number, registrationId: number) =>
    [
      ...courseOfferingKeys.all,
      'user-registration',
      organizationId,
      userId,
      registrationId,
    ] as const,
};

/** The own offerings and their registration state, as seen from the own area. */
function invalidateSelfCourseOfferings(queryClient: ReturnType<typeof useQueryClient>) {
  void queryClient.invalidateQueries({ queryKey: courseOfferingKeys.selfOfferingsRoot() });
  void queryClient.invalidateQueries({ queryKey: courseOfferingKeys.selfOfferingDetailRoot() });
}

function invalidateCourseOfferingOrgDetail(
  queryClient: ReturnType<typeof useQueryClient>,
  organizationId: number,
  offeringId: number,
) {
  // The org root covers the detail of this offering and every list.
  void queryClient.invalidateQueries({ queryKey: courseOfferingKeys.orgRoot(organizationId) });
  void queryClient.invalidateQueries({
    queryKey: courseOfferingKeys.registrationSummary(organizationId, offeringId),
  });
  void queryClient.invalidateQueries({
    queryKey: courseOfferingKeys.offeringRegistrationsRoot(organizationId, offeringId),
  });
  void queryClient.invalidateQueries({
    queryKey: courseOfferingKeys.completion(organizationId, offeringId),
  });
}

export function useUrlSyncedSelfCourseOfferingListState() {
  return useUrlSyncedListState(selfCourseOfferingListDefinition);
}

export function useOfferingRegistrationListState() {
  return useUrlSyncedListState(offeringRegistrationListDefinition);
}

export function useUserCourseRegistrationListState() {
  return useUrlSyncedListState(userCourseRegistrationListDefinition);
}

export function useOrgCourseOfferingListState() {
  return useUrlSyncedListState(orgCourseOfferingListDefinition);
}

export function useOrgCourseRegistrationListState() {
  return useUrlSyncedListState(orgCourseRegistrationListDefinition);
}

export function useOrganizationCourseRegistrations(options: {
  organizationId: number | null | undefined;
  listQuery: ListQuery;
  enabled?: boolean;
}) {
  const orgId = options.organizationId ?? 0;
  return usePagedList({
    queryKey: courseOfferingKeys.orgRegistrations(orgId, options.listQuery),
    listQuery: options.listQuery as CourseRegistrationListRequest,
    queryFn: (query) => courseOfferingsApi.listOrganizationRegistrations(orgId, query),
    enabled: !!options.organizationId && (options.enabled ?? true),
    staleTime: STALE_TIME.LIST,
  });
}

export function useOrganizationCourseOfferings(options: {
  organizationId: number | null | undefined;
  listQuery: ListQuery;
  enabled?: boolean;
}) {
  const orgId = options.organizationId ?? 0;
  return usePagedList({
    queryKey: courseOfferingKeys.orgList(orgId, options.listQuery),
    listQuery: options.listQuery as OrgCourseOfferingListRequest,
    queryFn: (query) => courseOfferingsApi.listForOrganization(orgId, query),
    enabled: !!options.organizationId && (options.enabled ?? true),
    staleTime: STALE_TIME.LIST,
  });
}

export function useOrgCourseOfferingTemplateListState() {
  return useListState(orgCourseOfferingTemplateListDefinition);
}

export function useOrganizationCourseOfferingTemplates(options: {
  organizationId: number | null | undefined;
  listQuery: ListQuery;
  enabled?: boolean;
}) {
  const orgId = options.organizationId ?? 0;
  return usePagedList({
    queryKey: courseOfferingKeys.orgTemplateList(orgId, options.listQuery),
    listQuery: options.listQuery,
    queryFn: (query) => courseOfferingsApi.listTemplates(orgId, query),
    enabled: !!options.organizationId && (options.enabled ?? true),
    staleTime: STALE_TIME.LIST,
  });
}

export function useOfferableCourseTypes(organizationId: number | null | undefined, enabled = true) {
  const orgId = organizationId ?? 0;
  return useQuery({
    queryKey: courseOfferingKeys.offerableTypes(orgId),
    queryFn: () => courseOfferingsApi.listOfferableTypes(orgId),
    enabled: !!organizationId && enabled,
    staleTime: STALE_TIME.STANDARD,
  });
}

/** Tenant orgs + org types for audience restriction pickers (`COURSE_OFFERING_READ`). */
export function useCourseAudienceOptions(
  organizationId: number | null | undefined,
  enabled = true,
) {
  const orgId = organizationId ?? 0;
  return useQuery({
    queryKey: courseOfferingKeys.audienceOptions(orgId),
    queryFn: () => courseOfferingsApi.listAudienceOptions(orgId),
    enabled: !!organizationId && enabled,
    staleTime: STALE_TIME.STANDARD,
  });
}

export function useSelfCourseOfferings(options: {
  debouncedSearch?: string;
  filters?: ListQueryState['filters'];
  sort?: string;
  listQuery?: ListQuery;
  enabled?: boolean;
}) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  const listQuery =
    options.listQuery ??
    selfCourseOfferingListQuery({
      debouncedSearch: options.debouncedSearch ?? '',
      filters: options.filters ?? {},
      sort: options.sort,
    });
  return usePagedList({
    queryKey: courseOfferingKeys.selfOfferings(listQuery),
    listQuery: listQuery as SelfCourseOfferingListRequest,
    queryFn: (query) => courseOfferingsApi.listSelfOfferings(query),
    enabled: (options.enabled ?? true) && hasEmberOneUserId,
    staleTime: STALE_TIME.LIST,
  });
}

export function useSelfCourseOffering(offeringId: number | null, options?: { enabled?: boolean }) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  const enabled = (options?.enabled ?? true) && hasEmberOneUserId && offeringId != null;
  return useQuery({
    queryKey: courseOfferingKeys.selfOfferingDetail(offeringId ?? 0),
    queryFn: () => {
      if (offeringId == null) {
        throw new Error('offeringId required');
      }
      return courseOfferingsApi.getSelfOffering(offeringId);
    },
    enabled,
    staleTime: STALE_TIME.LIST,
  });
}

export function useOrganizationUserCourseRegistrations(options: {
  organizationId: number;
  userId: number;
  listQuery: ListQuery;
  enabled?: boolean;
}) {
  const { organizationId, userId, listQuery, enabled = true } = options;
  return usePagedList({
    queryKey: courseOfferingKeys.userRegistrations(organizationId, userId, listQuery),
    listQuery: listQuery as CourseRegistrationListRequest,
    queryFn: (query) => courseOfferingsApi.listUserRegistrations(organizationId, userId, query),
    enabled: enabled && organizationId > 0 && userId > 0,
    staleTime: STALE_TIME.LIST,
  });
}

export function useOrganizationUserCourseRegistration(
  organizationId: number,
  userId: number,
  registrationId: number,
  enabled = true,
) {
  return useQuery({
    queryKey: courseOfferingKeys.userRegistration(organizationId, userId, registrationId),
    queryFn: () => courseOfferingsApi.getUserRegistration(organizationId, userId, registrationId),
    enabled: enabled && organizationId > 0 && userId > 0 && registrationId > 0,
    staleTime: STALE_TIME.LIST,
  });
}

export function useOrganizationCourseOffering(
  organizationId: number | null | undefined,
  offeringId: number | null | undefined,
  enabled = true,
) {
  const orgId = organizationId ?? 0;
  const id = offeringId ?? 0;
  return useQuery({
    queryKey: courseOfferingKeys.orgDetail(orgId, id),
    queryFn: () => courseOfferingsApi.getForOrganization(orgId, id),
    enabled: enabled && orgId > 0 && id > 0,
    staleTime: STALE_TIME.LIVE,
  });
}

export function useOfferingRegistrationSummary(
  organizationId: number,
  offeringId: number,
  enabled = true,
) {
  return useQuery({
    queryKey: courseOfferingKeys.registrationSummary(organizationId, offeringId),
    queryFn: () => courseOfferingsApi.registrationSummary(organizationId, offeringId),
    enabled: enabled && organizationId > 0 && offeringId > 0,
    staleTime: STALE_TIME.LIVE,
  });
}

export function useOfferingRegistrations(options: {
  organizationId: number;
  offeringId: number;
  listQuery: ListQuery;
  enabled?: boolean;
}) {
  const { organizationId, offeringId, listQuery, enabled = true } = options;
  return usePagedList({
    queryKey: courseOfferingKeys.offeringRegistrations(organizationId, offeringId, listQuery),
    listQuery: listQuery as CourseRegistrationListRequest,
    queryFn: (query) => courseOfferingsApi.listRegistrations(organizationId, offeringId, query),
    enabled: enabled && organizationId > 0 && offeringId > 0,
    staleTime: STALE_TIME.LIVE,
  });
}

export function useOfferingRegistration(
  organizationId: number,
  offeringId: number,
  registrationId: number,
  enabled = true,
) {
  return useQuery({
    queryKey: courseOfferingKeys.offeringRegistration(organizationId, offeringId, registrationId),
    queryFn: () => courseOfferingsApi.getRegistration(organizationId, offeringId, registrationId),
    enabled: enabled && organizationId > 0 && offeringId > 0 && registrationId > 0,
    staleTime: STALE_TIME.LIVE,
  });
}

export function useCourseOfferingCompletion(
  organizationId: number,
  offeringId: number,
  enabled = true,
) {
  return useQuery({
    queryKey: courseOfferingKeys.completion(organizationId, offeringId),
    queryFn: () => courseOfferingsApi.getCompletion(organizationId, offeringId),
    enabled: enabled && organizationId > 0 && offeringId > 0,
    staleTime: STALE_TIME.LIVE,
  });
}

export function useWithdrawFromCourseWaitlist() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (registrationId: number) => courseOfferingsApi.withdrawFromWaitlist(registrationId),
    onSuccess: () => {
      invalidateSelfCourseOfferings(queryClient);
    },
  });
}

export function usePromoteCourseRegistration(organizationId: number, offeringId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (registrationId: number) =>
      courseOfferingsApi.promoteRegistration(organizationId, offeringId, registrationId),
    onSuccess: () => {
      invalidateCourseOfferingOrgDetail(queryClient, organizationId, offeringId);
    },
  });
}

export function useRemoveCourseRegistration(organizationId: number, offeringId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ registrationId, reason }: { registrationId: number; reason: string }) =>
      courseOfferingsApi.removeRegistration(organizationId, offeringId, registrationId, reason),
    onSuccess: () => {
      invalidateCourseOfferingOrgDetail(queryClient, organizationId, offeringId);
    },
  });
}

export function useCreateCourseOffering(organizationId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateCourseOfferingRequest) =>
      courseOfferingsApi.create(organizationId, data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: courseOfferingKeys.orgRoot(organizationId) });
    },
  });
}

export function useSaveCourseOfferingAsTemplate(organizationId: number, offeringId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (templateName: string) =>
      courseOfferingsApi.saveAsTemplate(organizationId, offeringId, templateName),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: courseOfferingKeys.orgRoot(organizationId) });
    },
  });
}

export function useInstantiateCourseOfferingTemplate(organizationId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (vars: { templateId: number; firstSessionDate: string }) =>
      courseOfferingsApi.instantiateFromTemplate(
        organizationId,
        vars.templateId,
        vars.firstSessionDate,
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: courseOfferingKeys.orgRoot(organizationId) });
    },
  });
}

export function useUpdateCourseOffering(organizationId: number, offeringId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateCourseOfferingRequest) =>
      courseOfferingsApi.update(organizationId, offeringId, data),
    onSuccess: () => {
      invalidateCourseOfferingOrgDetail(queryClient, organizationId, offeringId);
    },
  });
}

export function useDeleteCourseOffering(organizationId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (offeringId: number) => courseOfferingsApi.delete(organizationId, offeringId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: courseOfferingKeys.orgRoot(organizationId) });
    },
  });
}

export function usePublishCourseOffering(organizationId: number, offeringId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => courseOfferingsApi.publish(organizationId, offeringId),
    onSuccess: () => {
      invalidateCourseOfferingOrgDetail(queryClient, organizationId, offeringId);
    },
  });
}

export function useReplaceCourseOfferingAudiences(organizationId: number, offeringId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (entries: CourseAudienceEntryRequest[]) =>
      courseOfferingsApi.replaceAudiences(organizationId, offeringId, entries),
    onSuccess: () => {
      invalidateCourseOfferingOrgDetail(queryClient, organizationId, offeringId);
    },
  });
}

export function useReplaceCourseOfferingPrerequisites(organizationId: number, offeringId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: ReplaceCoursePrerequisitesRequest) =>
      courseOfferingsApi.replacePrerequisites(organizationId, offeringId, data),
    onSuccess: () => {
      invalidateCourseOfferingOrgDetail(queryClient, organizationId, offeringId);
    },
  });
}

export function useAddCourseSession(organizationId: number, offeringId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CourseSessionRequest) =>
      courseOfferingsApi.addSession(organizationId, offeringId, data),
    onSuccess: () => {
      invalidateCourseOfferingOrgDetail(queryClient, organizationId, offeringId);
    },
  });
}

export function useUpdateCourseSession(organizationId: number, offeringId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ sessionId, data }: { sessionId: number; data: CourseSessionRequest }) =>
      courseOfferingsApi.updateSession(organizationId, offeringId, sessionId, data),
    onSuccess: () => {
      invalidateCourseOfferingOrgDetail(queryClient, organizationId, offeringId);
    },
  });
}

export function useDeleteCourseSession(organizationId: number, offeringId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (sessionId: number) =>
      courseOfferingsApi.deleteSession(organizationId, offeringId, sessionId),
    onSuccess: () => {
      invalidateCourseOfferingOrgDetail(queryClient, organizationId, offeringId);
    },
  });
}

export function useCancelCourseOffering(organizationId: number, offeringId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (reason: string) => courseOfferingsApi.cancel(organizationId, offeringId, reason),
    onSuccess: () => {
      invalidateCourseOfferingOrgDetail(queryClient, organizationId, offeringId);
    },
  });
}

export function useSetCourseFinalResult(organizationId: number, offeringId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ registrationId, passed }: { registrationId: number; passed: boolean | null }) =>
      courseOfferingsApi.setFinalResult(organizationId, offeringId, registrationId, passed),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: courseOfferingKeys.completion(organizationId, offeringId),
      });
    },
  });
}

export function useCompleteCourseOffering(organizationId: number, offeringId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => courseOfferingsApi.completeOffering(organizationId, offeringId),
    onSuccess: () => {
      invalidateCourseOfferingOrgDetail(queryClient, organizationId, offeringId);
    },
  });
}

export function useOpenCourseOfferingRegistrationWorkflow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (offeringId: number) => workflowsApi.openCourseOfferingRegistration({ offeringId }),
    onSuccess: () => {
      invalidateSelfCourseOfferings(queryClient);
      invalidateWorkflowLists(queryClient);
    },
  });
}

export function useOpenCourseRegistrationCancellationWorkflow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (registrationId: number) =>
      workflowsApi.openCourseRegistrationCancellation({ registrationId }),
    onSuccess: () => {
      invalidateSelfCourseOfferings(queryClient);
      invalidateWorkflowLists(queryClient);
    },
  });
}
