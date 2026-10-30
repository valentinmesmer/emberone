/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { fetchAllPageContent } from '@/lib/api/paged';
import type { TrainingCourseUserRecordDto } from '@/lib/api/trainingCourses';
import {
  type CreateTrainingCourseUserRecordRequest,
  type TrainingCourseRecordListRequest,
  trainingCoursesApi,
  type UpdateTrainingCourseUserRecordRequest,
} from '@/lib/api/trainingCourses';
import { trainingCourseRecordListDefinition } from '@/lib/list/trainingCourseRecordList';
import { useListState, useUrlSyncedListState } from '@/lib/list/useListState';
import { invalidateQualificationStatusForOrganization } from '@/lib/store/queries/qualifications';
import { usePagedList } from '@/lib/store/usePagedList';

export const trainingCourseKeys = {
  all: ['trainingCourses'] as const,
  myRecords: () => [...trainingCourseKeys.all, 'my-records'] as const,
  myAvailableTypes: () => [...trainingCourseKeys.all, 'my-available-types'] as const,
  userRecords: (orgId: number, userId: number) =>
    [...trainingCourseKeys.all, 'user-records', orgId, userId] as const,
  pendingSubmissions: (orgId: number, userId: number) =>
    [...trainingCourseKeys.all, 'pending-submissions', orgId, userId] as const,
  catalog: (orgId: number, userId: number) =>
    [...trainingCourseKeys.all, 'catalog', orgId, userId] as const,
};

export function useTrainingCourseRecordListState() {
  return useListState(trainingCourseRecordListDefinition);
}

export function useUrlSyncedTrainingCourseRecordListState() {
  return useUrlSyncedListState(trainingCourseRecordListDefinition);
}

type UseTrainingCourseRecordListOptions = {
  listQuery: TrainingCourseRecordListRequest;
  enabled?: boolean;
  fetchAll?: boolean;
};

export function useMyTrainingCourseRecords(enabled = true) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: [...trainingCourseKeys.myRecords(), 'all'] as const,
    queryFn: () => fetchAllPageContent((page) => trainingCoursesApi.getMyRecords({ page })),
    enabled: hasEmberOneUserId && enabled,
    staleTime: 60 * 1000,
  });
}

export function useMyTrainingCourseRecordList({
  listQuery,
  enabled = true,
}: UseTrainingCourseRecordListOptions) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return usePagedList<TrainingCourseUserRecordDto, TrainingCourseRecordListRequest>({
    queryKey: trainingCourseKeys.myRecords(),
    listQuery,
    queryFn: (query) => trainingCoursesApi.getMyRecords(query),
    enabled: hasEmberOneUserId && enabled,
    staleTime: 60 * 1000,
  });
}

export function useUserTrainingCourseRecordList({
  organizationId,
  userId,
  listQuery,
  enabled = true,
}: UseTrainingCourseRecordListOptions & {
  organizationId: number;
  userId: number;
}) {
  return usePagedList<TrainingCourseUserRecordDto, TrainingCourseRecordListRequest>({
    queryKey: trainingCourseKeys.userRecords(organizationId, userId),
    listQuery,
    queryFn: (query) => trainingCoursesApi.getUserRecords(organizationId, userId, query),
    enabled: enabled && organizationId > 0 && userId > 0,
    staleTime: 60 * 1000,
  });
}

export function useMyAvailableTrainingCourseTypes(enabled = true) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: trainingCourseKeys.myAvailableTypes(),
    queryFn: () => trainingCoursesApi.getMyAvailableTypes(),
    enabled: hasEmberOneUserId && enabled,
    staleTime: 5 * 60 * 1000,
  });
}

export function useUserTrainingCourseRecords(
  organizationId: number,
  userId: number,
  enabled = true,
) {
  return useQuery({
    queryKey: trainingCourseKeys.userRecords(organizationId, userId),
    queryFn: () =>
      fetchAllPageContent((page) =>
        trainingCoursesApi.getUserRecords(organizationId, userId, { page }),
      ),
    enabled: enabled && organizationId > 0 && userId > 0,
    staleTime: 60 * 1000,
  });
}

export function useUserTrainingCoursePendingSubmissions(
  organizationId: number,
  userId: number,
  enabled = true,
) {
  return useQuery({
    queryKey: trainingCourseKeys.pendingSubmissions(organizationId, userId),
    queryFn: () => trainingCoursesApi.getPendingSubmissions(organizationId, userId),
    enabled: enabled && organizationId > 0 && userId > 0,
    staleTime: 60 * 1000,
  });
}

export function useOrgTrainingCourseCatalog(
  organizationId: number,
  userId: number,
  enabled = true,
) {
  return useQuery({
    queryKey: trainingCourseKeys.catalog(organizationId, userId),
    queryFn: () => trainingCoursesApi.getAvailableTypes(organizationId, userId),
    enabled: enabled && organizationId > 0 && userId > 0,
    staleTime: 5 * 60 * 1000,
  });
}

function useInvalidateUserTrainingCourses(organizationId: number, userId: number) {
  const queryClient = useQueryClient();
  return () => {
    invalidateQualificationStatusForOrganization(queryClient, organizationId);
    return Promise.all([
      queryClient.invalidateQueries({
        queryKey: trainingCourseKeys.userRecords(organizationId, userId),
      }),
      queryClient.invalidateQueries({
        queryKey: trainingCourseKeys.pendingSubmissions(organizationId, userId),
      }),
      queryClient.invalidateQueries({
        queryKey: trainingCourseKeys.catalog(organizationId, userId),
      }),
    ]);
  };
}

export function useCreateTrainingCourseRecord(organizationId: number, userId: number) {
  const invalidate = useInvalidateUserTrainingCourses(organizationId, userId);
  return useMutation({
    mutationFn: (data: CreateTrainingCourseUserRecordRequest) =>
      trainingCoursesApi.createRecord(organizationId, userId, data),
    onSuccess: () => void invalidate(),
  });
}

export function useUpdateTrainingCourseRecord(organizationId: number, userId: number) {
  const invalidate = useInvalidateUserTrainingCourses(organizationId, userId);
  return useMutation({
    mutationFn: ({
      recordId,
      data,
    }: {
      recordId: number;
      data: UpdateTrainingCourseUserRecordRequest;
    }) => trainingCoursesApi.updateRecord(organizationId, userId, recordId, data),
    onSuccess: () => void invalidate(),
  });
}

export function useDeleteTrainingCourseRecord(organizationId: number, userId: number) {
  const invalidate = useInvalidateUserTrainingCourses(organizationId, userId);
  return useMutation({
    mutationFn: (recordId: number) =>
      trainingCoursesApi.deleteRecord(organizationId, userId, recordId),
    onSuccess: () => void invalidate(),
  });
}
