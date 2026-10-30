/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo } from 'react';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { type WorkflowListRequest, workflowsApi } from '@/lib/api/workflows';
import type { ListQuery } from '@/lib/list/types';
import { useUrlSyncedListState } from '@/lib/list/useListState';
import {
  workflowInboxListDefinition,
  workflowInboxOpenQuery,
  workflowListDefinition,
} from '@/lib/list/workflowList';
import { useRealtimePollingFallback } from '@/lib/realtime/useRealtimePollingFallback';
import { certificationKeys } from '@/lib/store/queries/certifications';
import { drivingLicenseKeys } from '@/lib/store/queries/drivingLicenses';
import { onboardingInvitationKeys } from '@/lib/store/queries/onboardingInvitation';
import { organizationKeys } from '@/lib/store/queries/organizations';
import { qualificationKeys } from '@/lib/store/queries/qualifications';
import { rankKeys } from '@/lib/store/queries/ranks';
import { trainingCourseKeys } from '@/lib/store/queries/trainingCourses';
import { userDocumentKeys } from '@/lib/store/queries/userDocuments';
import { userMembershipOverviewKeys } from '@/lib/store/queries/userMembershipOverview';
import { userKeys } from '@/lib/store/queries/users';
import { usePagedList } from '@/lib/store/usePagedList';
import { workflowPromotionKey } from '@/lib/utils/workflowDocumentPromotion';
import {
  type AddressChangeWorkflowPayload,
  type CreateWorkflowCommentRequest,
  type DrivingLicenseRecordWorkflowPayload,
  type MainOrganizationChangeWorkflowPayload,
  type NameChangeWorkflowPayload,
  type OrganizationGroupJoinRequestWorkflowPayload,
  type OrganizationGroupLeaveRequestWorkflowPayload,
  type OrganizationJoinRequestWorkflowPayload,
  type OrganizationLeaveRequestWorkflowPayload,
  type RankRequestWorkflowPayload,
  type TrainingCourseRecordWorkflowPayload,
  type UserDocumentSubmissionWorkflowPayload,
  WORKFLOW_TYPE_IDS,
  type WorkflowDecisionRequestBody,
  type WorkflowDocumentPromotionPreviewDto,
  type WorkflowDocumentPromotionSelection,
  type WorkflowDto,
  type WorkflowTypeCatalogDto,
  type WorkflowTypeId,
} from '@/types/workflow';

export const workflowKeys = {
  all: ['workflows'] as const,
  listRoot: () => [...workflowKeys.all, 'list'] as const,
  opened: (listQuery: ListQuery) => [...workflowKeys.listRoot(), 'opened', listQuery] as const,
  participations: (listQuery: ListQuery) =>
    [...workflowKeys.listRoot(), 'participations', listQuery] as const,
  pendingCount: () => [...workflowKeys.all, 'pending-count'] as const,
  beginnableTypes: () => [...workflowKeys.all, 'beginnable-types'] as const,
  openState: (workflowTypeId: number) =>
    [...workflowKeys.all, 'open-state', workflowTypeId] as const,
  openConcurrencyKeys: (workflowTypeId?: number) =>
    [...workflowKeys.all, 'open-concurrency-keys', workflowTypeId ?? 'all'] as const,
  detail: (workflowId: number) => [...workflowKeys.all, 'detail', workflowId] as const,
  attachments: (workflowId: number) => [...workflowKeys.all, 'attachments', workflowId] as const,
  comments: (workflowId: number) => [...workflowKeys.all, 'comments', workflowId] as const,
  documentPromotionPreview: (workflowId: number) =>
    [...workflowKeys.all, 'document-promotion-preview', workflowId] as const,
};

function toWorkflowListRequest(query: ListQuery & { page: number }): WorkflowListRequest {
  return query;
}

export function useUrlSyncedWorkflowListState() {
  return useUrlSyncedListState(workflowListDefinition);
}

export function useUrlSyncedWorkflowInboxListState() {
  return useUrlSyncedListState(workflowInboxListDefinition);
}

export function useMyWorkflowOpenedPage(options?: {
  listQuery?: ListQuery;
  fetchAll?: boolean;
  enabled?: boolean;
}) {
  const listQuery = options?.listQuery ?? workflowInboxOpenQuery;
  return usePagedList({
    queryKey: workflowKeys.opened(listQuery),
    listQuery,
    queryFn: (query) => workflowsApi.listMyOpenedWorkflows(toWorkflowListRequest(query)),
    fetchAll: options?.fetchAll,
    enabled: options?.enabled,
    staleTime: 30 * 1000,
  });
}

export function useMyWorkflowParticipationsPage(options?: {
  listQuery?: ListQuery;
  fetchAll?: boolean;
  enabled?: boolean;
}) {
  const listQuery = options?.listQuery ?? workflowInboxOpenQuery;
  return usePagedList({
    queryKey: workflowKeys.participations(listQuery),
    listQuery,
    queryFn: (query) => workflowsApi.listMyWorkflowParticipations(toWorkflowListRequest(query)),
    fetchAll: options?.fetchAll,
    enabled: options?.enabled,
    staleTime: 30 * 1000,
  });
}

export function useMyWorkflowPendingCount(options?: { enabled?: boolean }) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  const refetchInterval = useRealtimePollingFallback(60 * 1000);
  return useQuery({
    queryKey: workflowKeys.pendingCount(),
    queryFn: () => workflowsApi.getMyWorkflowPendingCount(),
    enabled: hasEmberOneUserId && (options?.enabled ?? true),
    staleTime: 30 * 1000,
    refetchInterval,
    select: (data) => data.count,
  });
}

export function useMyBeginnableWorkflowTypes() {
  return useQuery({
    queryKey: workflowKeys.beginnableTypes(),
    queryFn: () => workflowsApi.getMyBeginnableWorkflowTypes(),
    staleTime: 60 * 1000,
  });
}

export function useWorkflowOpenState(workflowTypeId: WorkflowTypeId, enabled = true) {
  return useQuery({
    queryKey: workflowKeys.openState(workflowTypeId),
    queryFn: () => workflowsApi.getWorkflowOpenState(workflowTypeId),
    enabled,
    staleTime: 30 * 1000,
  });
}

export function useWorkflowOpenConcurrencyKeys(workflowTypeId: WorkflowTypeId, enabled = true) {
  return useQuery({
    queryKey: workflowKeys.openConcurrencyKeys(workflowTypeId),
    queryFn: async () => {
      const items = await workflowsApi.getWorkflowOpenConcurrencyKeys(workflowTypeId);
      return items[0]?.keys ?? [];
    },
    enabled,
    staleTime: 30 * 1000,
  });
}

/** Workflow types blocked because the user already has an open instance (type-wide). */
export function useBlockedBeginnableWorkflowTypeIds(types: WorkflowTypeCatalogDto[]) {
  const singletonTypes = useMemo(
    () => types.filter((type) => !type.allowsMultipleConcurrentOpen),
    [types],
  );
  const queries = useQueries({
    queries: singletonTypes.map((type) => ({
      queryKey: workflowKeys.openState(type.workflowTypeId),
      queryFn: () => workflowsApi.getWorkflowOpenState(type.workflowTypeId as WorkflowTypeId),
      staleTime: 30 * 1000,
    })),
  });
  return useMemo(() => {
    const blocked = new Set<number>();
    singletonTypes.forEach((type, index) => {
      if (queries[index]?.data?.typeBlocked) {
        blocked.add(type.workflowTypeId);
      }
    });
    return blocked;
  }, [queries, singletonTypes]);
}

export function useWorkflow(workflowId: number | null | undefined) {
  return useQuery({
    queryKey: workflowKeys.detail(workflowId ?? -1),
    queryFn: () => workflowsApi.getWorkflow(workflowId ?? 0),
    enabled: workflowId != null && workflowId > 0,
    staleTime: 15 * 1000,
  });
}

export function useWorkflowAttachments(workflowId: number | null | undefined) {
  return useQuery({
    queryKey: workflowKeys.attachments(workflowId ?? -1),
    queryFn: () => workflowsApi.listWorkflowAttachments(workflowId ?? 0),
    enabled: workflowId != null && workflowId > 0,
    staleTime: 15 * 1000,
  });
}

export function useWorkflowDocumentPromotionPreview(
  workflowId: number | null | undefined,
  enabled = true,
) {
  return useQuery({
    queryKey: workflowKeys.documentPromotionPreview(workflowId ?? -1),
    queryFn: () => workflowsApi.getDocumentPromotionPreview(workflowId ?? 0),
    enabled: enabled && workflowId != null && workflowId > 0,
    staleTime: 10 * 1000,
  });
}

export function useUpdateWorkflowDocumentPromotions() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      workflowId,
      selections,
    }: {
      workflowId: number;
      selections: WorkflowDocumentPromotionSelection[];
    }) => workflowsApi.updateDocumentPromotions(workflowId, selections),
    onMutate: async ({ workflowId, selections }) => {
      const queryKey = workflowKeys.documentPromotionPreview(workflowId);
      await queryClient.cancelQueries({ queryKey });

      const previousPreview =
        queryClient.getQueryData<WorkflowDocumentPromotionPreviewDto>(queryKey);
      const selectedKeys = new Set(
        selections.map((selection) =>
          workflowPromotionKey(selection.source, selection.attachmentId),
        ),
      );

      queryClient.setQueryData<WorkflowDocumentPromotionPreviewDto>(queryKey, (current) => {
        if (!current) return current;
        return {
          ...current,
          items: current.items.map((item) => ({
            ...item,
            selected: selectedKeys.has(workflowPromotionKey(item.source, item.attachmentId)),
          })),
        };
      });

      return { previousPreview };
    },
    onError: (_error, variables, context) => {
      if (context?.previousPreview) {
        queryClient.setQueryData(
          workflowKeys.documentPromotionPreview(variables.workflowId),
          context.previousPreview,
        );
      }
    },
    onSettled: (_data, _error, variables) => {
      void queryClient.invalidateQueries({
        queryKey: workflowKeys.documentPromotionPreview(variables.workflowId),
      });
    },
  });
}

export function useWorkflowComments(workflowId: number | null | undefined) {
  return useQuery({
    queryKey: workflowKeys.comments(workflowId ?? -1),
    queryFn: () => workflowsApi.listWorkflowComments(workflowId ?? 0),
    enabled: workflowId != null && workflowId > 0,
    staleTime: 10 * 1000,
  });
}

export function useCreateWorkflowComment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      workflowId,
      body,
    }: {
      workflowId: number;
      body: CreateWorkflowCommentRequest;
    }) => workflowsApi.createWorkflowComment(workflowId, body),
    onSuccess: (_data, variables) => {
      void queryClient.invalidateQueries({ queryKey: workflowKeys.comments(variables.workflowId) });
    },
  });
}

export function useUploadWorkflowCommentAttachment() {
  return useMutation({
    mutationFn: ({ workflowId, file }: { workflowId: number; file: File }) =>
      workflowsApi.uploadWorkflowCommentAttachment(workflowId, file),
  });
}

function invalidateWorkflowLists(queryClient: ReturnType<typeof useQueryClient>) {
  void queryClient.invalidateQueries({ queryKey: workflowKeys.listRoot() });
  void queryClient.invalidateQueries({ queryKey: workflowKeys.pendingCount() });
  void queryClient.invalidateQueries({
    predicate: (query) =>
      Array.isArray(query.queryKey) &&
      query.queryKey[0] === workflowKeys.all[0] &&
      (query.queryKey[1] === 'open-state' || query.queryKey[1] === 'open-concurrency-keys'),
  });
}

/** Refetch domain data that any workflow type may mutate when the instance closes. */
function invalidateAfterWorkflowClose(queryClient: ReturnType<typeof useQueryClient>) {
  void queryClient.invalidateQueries({ queryKey: certificationKeys.all });
  void queryClient.invalidateQueries({ queryKey: trainingCourseKeys.all });
  void queryClient.invalidateQueries({ queryKey: drivingLicenseKeys.all });
  void queryClient.invalidateQueries({ queryKey: qualificationKeys.all });
  void queryClient.invalidateQueries({ queryKey: userKeys.self() });
  void queryClient.invalidateQueries({ queryKey: organizationKeys.myOrganizations() });
  void queryClient.invalidateQueries({ queryKey: organizationKeys.joinable() });
  void queryClient.invalidateQueries({ queryKey: organizationKeys.leaveTargets() });
  void queryClient.invalidateQueries({ queryKey: organizationKeys.groupLeaveTargets() });
  void queryClient.invalidateQueries({ queryKey: organizationKeys.groupJoinTargets() });
  void queryClient.invalidateQueries({ queryKey: userMembershipOverviewKeys.all });
  void queryClient.invalidateQueries({ queryKey: userDocumentKeys.all });
  void queryClient.invalidateQueries({ queryKey: workflowKeys.beginnableTypes() });
  void queryClient.invalidateQueries({ queryKey: onboardingInvitationKeys.all });
}

function isWorkflowClosed(workflow: WorkflowDto): boolean {
  const status = String(workflow.status).toUpperCase();
  return status !== 'OPEN' && status !== 'AWAITING_INPUT_CHANGE';
}

export function useClaimWorkflowParticipant() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ workflowId, participantId }: { workflowId: number; participantId: number }) =>
      workflowsApi.claimWorkflowParticipant(workflowId, participantId),
    onSuccess: (_data, variables) => {
      void queryClient.invalidateQueries({ queryKey: workflowKeys.detail(variables.workflowId) });
      invalidateWorkflowLists(queryClient);
    },
  });
}

export function useUnclaimWorkflowParticipant() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ workflowId, participantId }: { workflowId: number; participantId: number }) =>
      workflowsApi.unclaimWorkflowParticipant(workflowId, participantId),
    onSuccess: (_data, variables) => {
      void queryClient.invalidateQueries({ queryKey: workflowKeys.detail(variables.workflowId) });
      invalidateWorkflowLists(queryClient);
    },
  });
}

export function useWithdrawWorkflowApproval() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ workflowId, participantId }: { workflowId: number; participantId: number }) =>
      workflowsApi.withdrawWorkflowApproval(workflowId, participantId),
    onSuccess: (_data, variables) => {
      void queryClient.invalidateQueries({ queryKey: workflowKeys.detail(variables.workflowId) });
      void queryClient.invalidateQueries({
        queryKey: workflowKeys.documentPromotionPreview(variables.workflowId),
      });
      invalidateWorkflowLists(queryClient);
    },
  });
}

export function useSubmitWorkflowDecision() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ workflowId, body }: { workflowId: number; body: WorkflowDecisionRequestBody }) =>
      workflowsApi.submitWorkflowDecision(workflowId, body),
    onSuccess: (data, variables) => {
      void queryClient.invalidateQueries({ queryKey: workflowKeys.detail(variables.workflowId) });
      void queryClient.invalidateQueries({
        queryKey: workflowKeys.documentPromotionPreview(variables.workflowId),
      });
      invalidateWorkflowLists(queryClient);
      if (isWorkflowClosed(data)) {
        invalidateAfterWorkflowClose(queryClient);
      }
    },
  });
}

export function useCancelWorkflow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (workflowId: number) => workflowsApi.cancelWorkflow(workflowId),
    onSuccess: (data, workflowId) => {
      void queryClient.invalidateQueries({ queryKey: workflowKeys.detail(workflowId) });
      invalidateWorkflowLists(queryClient);
      if (isWorkflowClosed(data)) {
        invalidateAfterWorkflowClose(queryClient);
      }
    },
  });
}

export function useUpdateWorkflowPayload() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ workflowId, payload }: { workflowId: number; payload: unknown }) =>
      workflowsApi.updateWorkflowPayload(workflowId, payload),
    onSuccess: (_data, variables) => {
      void queryClient.invalidateQueries({ queryKey: workflowKeys.detail(variables.workflowId) });
      void queryClient.invalidateQueries({
        queryKey: workflowKeys.attachments(variables.workflowId),
      });
      void queryClient.invalidateQueries({
        queryKey: workflowKeys.documentPromotionPreview(variables.workflowId),
      });
      invalidateWorkflowLists(queryClient);
      void queryClient.invalidateQueries({ queryKey: onboardingInvitationKeys.all });
      void queryClient.invalidateQueries({ queryKey: certificationKeys.all });
      void queryClient.invalidateQueries({ queryKey: trainingCourseKeys.all });
      void queryClient.invalidateQueries({ queryKey: drivingLicenseKeys.all });
    },
  });
}

export function useRequestWorkflowInputChange() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ workflowId, message }: { workflowId: number; message: string }) =>
      workflowsApi.requestWorkflowInputChange(workflowId, { message }),
    onSuccess: (_data, variables) => {
      void queryClient.invalidateQueries({ queryKey: workflowKeys.detail(variables.workflowId) });
      invalidateWorkflowLists(queryClient);
      void queryClient.invalidateQueries({ queryKey: onboardingInvitationKeys.all });
    },
  });
}

export function useWithdrawWorkflowInputChange() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (workflowId: number) => workflowsApi.withdrawWorkflowInputChange(workflowId),
    onSuccess: (_data, workflowId) => {
      void queryClient.invalidateQueries({ queryKey: workflowKeys.detail(workflowId) });
      invalidateWorkflowLists(queryClient);
      void queryClient.invalidateQueries({ queryKey: onboardingInvitationKeys.all });
    },
  });
}

export function useSubmitNameChangeWorkflow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      files,
      payload,
    }: {
      files: File[];
      payload: Omit<NameChangeWorkflowPayload, 'attachmentIds'>;
    }) => {
      const uploaded = await Promise.all(
        files.map((file) =>
          workflowsApi.uploadWorkflowDraftAttachment(WORKFLOW_TYPE_IDS.NAME_CHANGE, file),
        ),
      );
      return workflowsApi.openNameChangeWorkflow({
        ...payload,
        attachmentIds: uploaded.map((item) => item.id),
      });
    },
    onSuccess: (data) => {
      invalidateWorkflowLists(queryClient);
      void queryClient.invalidateQueries({ queryKey: workflowKeys.all });
      if (isWorkflowClosed(data)) {
        invalidateAfterWorkflowClose(queryClient);
      }
    },
  });
}

export function useSubmitCertificationRecordWorkflow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      files,
      payload,
    }: {
      files: File[];
      payload: { tenantCertDefinitionId: number; completedDate: string; notes?: string | null };
    }) => {
      const attachmentIds = (
        await Promise.all(
          files.map((file) =>
            workflowsApi.uploadWorkflowDraftAttachment(
              WORKFLOW_TYPE_IDS.CERTIFICATION_RECORD,
              file,
            ),
          ),
        )
      ).map((item) => item.id);
      return workflowsApi.openCertificationRecordWorkflow({ ...payload, attachmentIds });
    },
    onSuccess: (data) => {
      invalidateWorkflowLists(queryClient);
      void queryClient.invalidateQueries({ queryKey: workflowKeys.all });
      void queryClient.invalidateQueries({ queryKey: certificationKeys.all });
      if (isWorkflowClosed(data)) {
        invalidateAfterWorkflowClose(queryClient);
      }
    },
  });
}

export function useSubmitTrainingCourseRecordWorkflow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      files,
      payload,
    }: {
      files: File[];
      payload: Omit<TrainingCourseRecordWorkflowPayload, 'attachmentIds'>;
    }) => {
      const attachmentIds = (
        await Promise.all(
          files.map((file) =>
            workflowsApi.uploadWorkflowDraftAttachment(
              WORKFLOW_TYPE_IDS.TRAINING_COURSE_RECORD,
              file,
            ),
          ),
        )
      ).map((item) => item.id);
      return workflowsApi.openTrainingCourseRecordWorkflow({ ...payload, attachmentIds });
    },
    onSuccess: (data) => {
      invalidateWorkflowLists(queryClient);
      void queryClient.invalidateQueries({ queryKey: workflowKeys.all });
      void queryClient.invalidateQueries({ queryKey: trainingCourseKeys.all });
      if (isWorkflowClosed(data)) {
        invalidateAfterWorkflowClose(queryClient);
      }
    },
  });
}

export function useSubmitRankRequestWorkflow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      files,
      payload,
    }: {
      files: File[];
      payload: Omit<RankRequestWorkflowPayload, 'attachmentIds'>;
    }) => {
      const attachmentIds =
        files.length === 0
          ? undefined
          : (
              await Promise.all(
                files.map((file) =>
                  workflowsApi.uploadWorkflowDraftAttachment(WORKFLOW_TYPE_IDS.RANK_REQUEST, file),
                ),
              )
            ).map((item) => item.id);
      return workflowsApi.openRankRequestWorkflow({
        ...payload,
        attachmentIds,
      });
    },
    onSuccess: (data) => {
      invalidateWorkflowLists(queryClient);
      void queryClient.invalidateQueries({ queryKey: workflowKeys.all });
      void queryClient.invalidateQueries({ queryKey: rankKeys.myRank() });
      if (isWorkflowClosed(data)) {
        invalidateAfterWorkflowClose(queryClient);
      }
    },
  });
}

export function useSubmitDrivingLicenseRecordWorkflow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      files,
      payload,
    }: {
      files: File[];
      payload: Omit<DrivingLicenseRecordWorkflowPayload, 'attachmentIds'>;
    }) => {
      const attachmentIds = (
        await Promise.all(
          files.map((file) =>
            workflowsApi.uploadWorkflowDraftAttachment(
              WORKFLOW_TYPE_IDS.DRIVING_LICENSE_RECORD,
              file,
            ),
          ),
        )
      ).map((item) => item.id);
      return workflowsApi.openDrivingLicenseRecordWorkflow({ ...payload, attachmentIds });
    },
    onSuccess: (data) => {
      invalidateWorkflowLists(queryClient);
      void queryClient.invalidateQueries({ queryKey: workflowKeys.all });
      void queryClient.invalidateQueries({ queryKey: drivingLicenseKeys.all });
      if (isWorkflowClosed(data)) {
        invalidateAfterWorkflowClose(queryClient);
      }
    },
  });
}

export function useSubmitUserDocumentSubmissionWorkflow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      files,
      payload,
    }: {
      files: File[];
      payload: Omit<UserDocumentSubmissionWorkflowPayload, 'attachmentIds'>;
    }) => {
      const uploaded = await Promise.all(
        files.map((file) =>
          workflowsApi.uploadWorkflowDraftAttachment(
            WORKFLOW_TYPE_IDS.USER_DOCUMENT_SUBMISSION,
            file,
          ),
        ),
      );
      return workflowsApi.openUserDocumentSubmissionWorkflow({
        ...payload,
        attachmentIds: uploaded.map((item) => item.id),
      });
    },
    onSuccess: (data) => {
      invalidateWorkflowLists(queryClient);
      void queryClient.invalidateQueries({ queryKey: workflowKeys.all });
      void queryClient.invalidateQueries({ queryKey: userDocumentKeys.all });
      if (isWorkflowClosed(data)) {
        invalidateAfterWorkflowClose(queryClient);
      }
    },
  });
}

export function useSubmitAddressChangeWorkflow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      files,
      payload,
    }: {
      files: File[];
      payload: Omit<AddressChangeWorkflowPayload, 'attachmentIds'>;
    }) => {
      const attachmentIds =
        files.length > 0
          ? (
              await Promise.all(
                files.map((file) =>
                  workflowsApi.uploadWorkflowDraftAttachment(
                    WORKFLOW_TYPE_IDS.ADDRESS_CHANGE,
                    file,
                  ),
                ),
              )
            ).map((item) => item.id)
          : undefined;
      return workflowsApi.openAddressChangeWorkflow({
        ...payload,
        ...(attachmentIds != null && attachmentIds.length > 0 ? { attachmentIds } : {}),
      });
    },
    onSuccess: (data) => {
      invalidateWorkflowLists(queryClient);
      void queryClient.invalidateQueries({ queryKey: userKeys.self() });
      void queryClient.invalidateQueries({ queryKey: workflowKeys.all });
      if (isWorkflowClosed(data)) {
        invalidateAfterWorkflowClose(queryClient);
      }
    },
  });
}

export function useSubmitOrganizationJoinRequestWorkflow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      files,
      payload,
    }: {
      files: File[];
      payload: Omit<OrganizationJoinRequestWorkflowPayload, 'attachmentIds'>;
    }) => {
      const attachmentIds =
        files.length > 0
          ? (
              await Promise.all(
                files.map((file) =>
                  workflowsApi.uploadWorkflowDraftAttachment(
                    WORKFLOW_TYPE_IDS.ORGANIZATION_JOIN_REQUEST,
                    file,
                  ),
                ),
              )
            ).map((item) => item.id)
          : undefined;
      return workflowsApi.openOrganizationJoinRequestWorkflow({
        ...payload,
        ...(attachmentIds != null && attachmentIds.length > 0 ? { attachmentIds } : {}),
      });
    },
    onSuccess: (data) => {
      invalidateWorkflowLists(queryClient);
      void queryClient.invalidateQueries({ queryKey: organizationKeys.joinable() });
      void queryClient.invalidateQueries({ queryKey: workflowKeys.all });
      if (isWorkflowClosed(data)) {
        invalidateAfterWorkflowClose(queryClient);
      }
    },
  });
}

export function useSubmitMainOrganizationChangeWorkflow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      files,
      payload,
    }: {
      files: File[];
      payload: Omit<MainOrganizationChangeWorkflowPayload, 'attachmentIds'>;
    }) => {
      const attachmentIds =
        files.length > 0
          ? (
              await Promise.all(
                files.map((file) =>
                  workflowsApi.uploadWorkflowDraftAttachment(
                    WORKFLOW_TYPE_IDS.MAIN_ORGANIZATION_CHANGE,
                    file,
                  ),
                ),
              )
            ).map((item) => item.id)
          : undefined;
      return workflowsApi.openMainOrganizationChangeWorkflow({
        ...payload,
        ...(attachmentIds != null && attachmentIds.length > 0 ? { attachmentIds } : {}),
      });
    },
    onSuccess: (data) => {
      invalidateWorkflowLists(queryClient);
      void queryClient.invalidateQueries({ queryKey: organizationKeys.mainChangeTargets() });
      void queryClient.invalidateQueries({ queryKey: organizationKeys.myOrganizations() });
      void queryClient.invalidateQueries({ queryKey: workflowKeys.all });
      if (isWorkflowClosed(data)) {
        invalidateAfterWorkflowClose(queryClient);
      }
    },
  });
}

export function useSubmitOrganizationLeaveRequestWorkflow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      files,
      payload,
    }: {
      files: File[];
      payload: Omit<OrganizationLeaveRequestWorkflowPayload, 'attachmentIds'>;
    }) => {
      const attachmentIds =
        files.length > 0
          ? (
              await Promise.all(
                files.map((file) =>
                  workflowsApi.uploadWorkflowDraftAttachment(
                    WORKFLOW_TYPE_IDS.ORGANIZATION_LEAVE_REQUEST,
                    file,
                  ),
                ),
              )
            ).map((item) => item.id)
          : undefined;
      return workflowsApi.openOrganizationLeaveRequestWorkflow({
        ...payload,
        ...(attachmentIds != null && attachmentIds.length > 0 ? { attachmentIds } : {}),
      });
    },
    onSuccess: (data) => {
      invalidateWorkflowLists(queryClient);
      void queryClient.invalidateQueries({ queryKey: organizationKeys.leaveTargets() });
      void queryClient.invalidateQueries({ queryKey: organizationKeys.myOrganizations() });
      void queryClient.invalidateQueries({ queryKey: workflowKeys.all });
      if (isWorkflowClosed(data)) {
        invalidateAfterWorkflowClose(queryClient);
      }
    },
  });
}

export function useSubmitOrganizationGroupLeaveRequestWorkflow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      files,
      payload,
    }: {
      files: File[];
      payload: Omit<OrganizationGroupLeaveRequestWorkflowPayload, 'attachmentIds'>;
    }) => {
      const attachmentIds =
        files.length > 0
          ? (
              await Promise.all(
                files.map((file) =>
                  workflowsApi.uploadWorkflowDraftAttachment(
                    WORKFLOW_TYPE_IDS.ORGANIZATION_GROUP_LEAVE_REQUEST,
                    file,
                  ),
                ),
              )
            ).map((item) => item.id)
          : undefined;
      return workflowsApi.openOrganizationGroupLeaveRequestWorkflow({
        ...payload,
        ...(attachmentIds != null && attachmentIds.length > 0 ? { attachmentIds } : {}),
      });
    },
    onSuccess: (data) => {
      invalidateWorkflowLists(queryClient);
      void queryClient.invalidateQueries({ queryKey: organizationKeys.groupLeaveTargets() });
      void queryClient.invalidateQueries({ queryKey: organizationKeys.myOrganizations() });
      void queryClient.invalidateQueries({ queryKey: workflowKeys.all });
      if (isWorkflowClosed(data)) {
        invalidateAfterWorkflowClose(queryClient);
      }
    },
  });
}

export function useSubmitOrganizationGroupJoinRequestWorkflow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      files,
      payload,
    }: {
      files: File[];
      payload: Omit<OrganizationGroupJoinRequestWorkflowPayload, 'attachmentIds'>;
    }) => {
      const attachmentIds =
        files.length > 0
          ? (
              await Promise.all(
                files.map((file) =>
                  workflowsApi.uploadWorkflowDraftAttachment(
                    WORKFLOW_TYPE_IDS.ORGANIZATION_GROUP_JOIN_REQUEST,
                    file,
                  ),
                ),
              )
            ).map((item) => item.id)
          : undefined;
      return workflowsApi.openOrganizationGroupJoinRequestWorkflow({
        ...payload,
        ...(attachmentIds != null && attachmentIds.length > 0 ? { attachmentIds } : {}),
      });
    },
    onSuccess: (data) => {
      invalidateWorkflowLists(queryClient);
      void queryClient.invalidateQueries({ queryKey: organizationKeys.groupJoinTargets() });
      void queryClient.invalidateQueries({ queryKey: organizationKeys.myOrganizations() });
      void queryClient.invalidateQueries({ queryKey: workflowKeys.all });
      if (isWorkflowClosed(data)) {
        invalidateAfterWorkflowClose(queryClient);
      }
    },
  });
}

export function useSubmitOnboardingSubmissionWorkflow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (invitationId: number) =>
      workflowsApi.openOnboardingSubmissionWorkflow(invitationId),
    onSuccess: (data) => {
      invalidateWorkflowLists(queryClient);
      void queryClient.invalidateQueries({ queryKey: workflowKeys.all });
      void queryClient.invalidateQueries({ queryKey: onboardingInvitationKeys.all });
      if (isWorkflowClosed(data)) {
        invalidateAfterWorkflowClose(queryClient);
      }
    },
  });
}
