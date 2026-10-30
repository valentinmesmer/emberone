/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo } from 'react';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import {
  type CertificationOverviewRequest,
  type CertificationOverviewRowDto,
  type CreateUserCertificationAssignmentRequest,
  type CreateUserCertificationRecordRequest,
  certificationsApi,
  type UserCertificationStatusDto,
} from '@/lib/api/certifications';
import { hasAssignableCertifications } from '@/lib/certifications/assignmentEligibility';
import { certificationOverviewListDefinition } from '@/lib/list/certificationOverviewList';
import type { ListQuery } from '@/lib/list/types';
import { useUrlSyncedListState } from '@/lib/list/useListState';
import { invalidateQualificationStatusForOrganization } from '@/lib/store/queries/qualifications';
import { usePagedList } from '@/lib/store/usePagedList';

export const certificationKeys = {
  all: ['certifications'] as const,
  myStatus: () => [...certificationKeys.all, 'my-status'] as const,
  myRecords: () => [...certificationKeys.all, 'my-records'] as const,
  myRequestableDefinitions: () => [...certificationKeys.all, 'my-requestable-definitions'] as const,
  userStatus: (orgId: number, userId: number) =>
    [...certificationKeys.all, 'user-status', orgId, userId] as const,
  statusOverview: (orgId: number) => [...certificationKeys.all, 'status-overview', orgId] as const,
  statusOverviewCounts: (orgId: number) =>
    [...certificationKeys.all, 'status-overview-counts', orgId] as const,
  userRecords: (orgId: number, userId: number) =>
    [...certificationKeys.all, 'user-records', orgId, userId] as const,
  assignments: (orgId: number, userId: number) =>
    [...certificationKeys.all, 'assignments', orgId, userId] as const,
  catalog: (orgId: number, userId: number) =>
    [...certificationKeys.all, 'catalog', orgId, userId] as const,
};

export function useMyCertificationStatus() {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: certificationKeys.myStatus(),
    queryFn: () => certificationsApi.getMyStatus(),
    enabled: hasEmberOneUserId,
    staleTime: 60 * 1000,
  });
}

export function useMyCertificationRecords() {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: certificationKeys.myRecords(),
    queryFn: () => certificationsApi.getMyRecords(),
    enabled: hasEmberOneUserId,
    staleTime: 60 * 1000,
  });
}

export function useMyRequestableCertificationDefinitions(enabled = true) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: certificationKeys.myRequestableDefinitions(),
    queryFn: () => certificationsApi.getMyRequestableDefinitions(),
    enabled: hasEmberOneUserId && enabled,
    staleTime: 60 * 1000,
  });
}

export function useUserCertificationStatus(organizationId: number, userId: number, enabled = true) {
  return useQuery({
    queryKey: certificationKeys.userStatus(organizationId, userId),
    queryFn: () => certificationsApi.getUserStatus(organizationId, userId),
    enabled: enabled && organizationId > 0 && userId > 0,
    staleTime: 60 * 1000,
  });
}

export function useUrlSyncedCertificationOverviewListState() {
  return useUrlSyncedListState(certificationOverviewListDefinition);
}

export function useOrganizationCertificationStatusOverview(
  organizationId: number,
  listQuery: ListQuery,
  enabled = true,
) {
  return usePagedList<CertificationOverviewRowDto, CertificationOverviewRequest>({
    queryKey: certificationKeys.statusOverview(organizationId),
    listQuery: listQuery as CertificationOverviewRequest,
    queryFn: (query) => certificationsApi.getStatusOverview(organizationId, query),
    enabled: enabled && organizationId > 0,
    staleTime: 60 * 1000,
  });
}

export function useOrganizationCertificationStatusOverviewCounts(
  organizationId: number,
  enabled = true,
) {
  return useQuery({
    queryKey: certificationKeys.statusOverviewCounts(organizationId),
    queryFn: () => certificationsApi.getStatusOverviewCounts(organizationId),
    enabled: enabled && organizationId > 0,
    staleTime: 60 * 1000,
  });
}

export function useUserCertificationAssignments(
  organizationId: number,
  userId: number,
  enabled = true,
) {
  return useQuery({
    queryKey: certificationKeys.assignments(organizationId, userId),
    queryFn: () => certificationsApi.listAssignments(organizationId, userId),
    enabled: enabled && organizationId > 0 && userId > 0,
    staleTime: 60 * 1000,
  });
}

export function useUserCertificationRecords(
  organizationId: number,
  userId: number,
  enabled = true,
) {
  return useQuery({
    queryKey: certificationKeys.userRecords(organizationId, userId),
    queryFn: () => certificationsApi.getUserRecords(organizationId, userId),
    enabled: enabled && organizationId > 0 && userId > 0,
    staleTime: 60 * 1000,
  });
}

export function useOrgCertificationCatalog(organizationId: number, userId: number, enabled = true) {
  return useQuery({
    queryKey: certificationKeys.catalog(organizationId, userId),
    queryFn: () => certificationsApi.getCatalog(organizationId, userId),
    enabled: enabled && organizationId > 0 && userId > 0,
    staleTime: 5 * 60 * 1000,
  });
}

export function useCanAssignCertifications(organizationId: number, userId: number, enabled = true) {
  const assignmentsQuery = useUserCertificationAssignments(organizationId, userId, enabled);
  const statusQuery = useUserCertificationStatus(organizationId, userId, enabled);
  const catalogQuery = useOrgCertificationCatalog(organizationId, userId, enabled);

  const canAssign = useMemo(() => {
    if (!enabled) return true;
    return hasAssignableCertifications(
      catalogQuery.data,
      assignmentsQuery.data ?? [],
      statusQuery.data?.items ?? [],
    );
  }, [enabled, catalogQuery.data, assignmentsQuery.data, statusQuery.data?.items]);

  const isLoading =
    enabled &&
    (assignmentsQuery.isLoading ||
      statusQuery.isLoading ||
      catalogQuery.isLoading ||
      canAssign === undefined);

  return { canAssign: enabled ? canAssign : true, isLoading };
}

function useInvalidateUserCertifications(organizationId: number, userId: number) {
  const queryClient = useQueryClient();
  return () => {
    invalidateQualificationStatusForOrganization(queryClient, organizationId);
    return Promise.all([
      queryClient.invalidateQueries({
        queryKey: certificationKeys.userStatus(organizationId, userId),
      }),
      queryClient.invalidateQueries({ queryKey: certificationKeys.statusOverview(organizationId) }),
      queryClient.invalidateQueries({
        queryKey: certificationKeys.statusOverviewCounts(organizationId),
      }),
      queryClient.invalidateQueries({
        queryKey: certificationKeys.userRecords(organizationId, userId),
      }),
      queryClient.invalidateQueries({
        queryKey: certificationKeys.assignments(organizationId, userId),
      }),
    ]);
  };
}

export function useCreateCertificationAssignment(organizationId: number, userId: number) {
  const invalidate = useInvalidateUserCertifications(organizationId, userId);
  return useMutation({
    mutationFn: (data: CreateUserCertificationAssignmentRequest) =>
      certificationsApi.createAssignment(organizationId, userId, data),
    onSuccess: () => void invalidate(),
  });
}

export function useDeleteCertificationAssignment(organizationId: number, userId: number) {
  const invalidate = useInvalidateUserCertifications(organizationId, userId);
  return useMutation({
    mutationFn: (assignmentId: number) =>
      certificationsApi.deleteAssignment(organizationId, userId, assignmentId),
    onSuccess: () => void invalidate(),
  });
}

export function useCreateCertificationRecord(organizationId: number, userId: number) {
  const invalidate = useInvalidateUserCertifications(organizationId, userId);
  return useMutation({
    mutationFn: (data: CreateUserCertificationRecordRequest) =>
      certificationsApi.createRecord(organizationId, userId, data),
    onSuccess: () => void invalidate(),
  });
}

export type { UserCertificationStatusDto };
