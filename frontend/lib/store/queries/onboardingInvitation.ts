/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '@/lib/api/admin';
import { ApiRequestError } from '@/lib/api/client';
import { onboardingInvitationApi } from '@/lib/api/onboardingInvitation';
import type { OnboardingStepType } from '@/lib/api/standards';
import type { ListQuery } from '@/lib/list/types';
import { usePagedList } from '@/lib/store/usePagedList';
import type {
  OnboardingInvitationAccess,
  SaveOnboardingStepRequest,
} from '@/types/onboardingInvitation';

export const onboardingInvitationKeys = {
  all: ['onboarding-invitations'] as const,
  byOrganization: (organizationId: number) =>
    [...onboardingInvitationKeys.all, 'organization', organizationId] as const,
  self: () => [...onboardingInvitationKeys.all, 'self'] as const,
  selfOpen: () => [...onboardingInvitationKeys.self(), 'open'] as const,
  selfDetail: (invitationId: number) =>
    [...onboardingInvitationKeys.self(), 'detail', invitationId] as const,
  organizationUserDetail: (organizationId: number, userId: number) =>
    [...onboardingInvitationKeys.all, 'organization-user', organizationId, userId] as const,
  organizationOnboardingAvailable: (organizationId: number) =>
    [...onboardingInvitationKeys.all, 'organization-onboarding-available', organizationId] as const,
  organizationDetail: (organizationId: number, invitationId: number) =>
    [
      ...onboardingInvitationKeys.all,
      'organization',
      organizationId,
      'detail',
      invitationId,
    ] as const,
  adminDetail: (tenantId: number, organizationId: number, invitationId: number) =>
    [
      ...onboardingInvitationKeys.all,
      'admin',
      tenantId,
      organizationId,
      'detail',
      invitationId,
    ] as const,
  adminUserDetail: (tenantId: number, organizationId: number, userId: number) =>
    [...onboardingInvitationKeys.all, 'admin-user', tenantId, organizationId, userId] as const,
  adminRecordingAvailable: (tenantId: number, organizationId: number, userId: number) =>
    [
      ...onboardingInvitationKeys.all,
      'admin-recording-available',
      tenantId,
      organizationId,
      userId,
    ] as const,
};

function detailQueryKey(access: OnboardingInvitationAccess) {
  if (access.kind === 'self') {
    return onboardingInvitationKeys.selfDetail(access.invitationId);
  }
  if (access.kind === 'admin') {
    return onboardingInvitationKeys.adminDetail(
      access.tenantId,
      access.organizationId,
      access.invitationId,
    );
  }
  return onboardingInvitationKeys.organizationDetail(access.organizationId, access.invitationId);
}

export function useOnboardingInvitationList(
  organizationId: number | null | undefined,
  listQuery: ListQuery,
) {
  return usePagedList({
    queryKey: [...onboardingInvitationKeys.byOrganization(organizationId ?? 0), listQuery] as const,
    listQuery,
    queryFn: (query) => onboardingInvitationApi.listForOrganization(organizationId ?? 0, query),
    enabled: !!organizationId,
    staleTime: 5 * 60 * 1000,
  });
}

export function useSelfOnboardingInvitations(options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: onboardingInvitationKeys.self(),
    queryFn: () => onboardingInvitationApi.listForSelf(),
    enabled: options?.enabled ?? true,
    staleTime: 5 * 60 * 1000,
  });
}

/** Open invitations only - for session reminders, not history lists. */
export function useSelfPendingOnboardingInvitations(options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: onboardingInvitationKeys.selfOpen(),
    queryFn: async () => {
      const invitations = await onboardingInvitationApi.listForSelf();
      return invitations.filter((invitation) => invitation.status === 'OPEN');
    },
    enabled: options?.enabled ?? true,
    staleTime: 5 * 60 * 1000,
  });
}

export function useSelfOnboardingInvitationDetail(invitationId: number | null | undefined) {
  return useQuery({
    queryKey: onboardingInvitationKeys.selfDetail(invitationId ?? 0),
    queryFn: () => onboardingInvitationApi.getSelfDetail(invitationId ?? 0),
    enabled: !!invitationId,
    staleTime: 30 * 1000,
  });
}

export function useOrganizationUserOnboardingDetail(
  organizationId: number | null | undefined,
  userId: number | null | undefined,
) {
  return useQuery({
    queryKey: onboardingInvitationKeys.organizationUserDetail(organizationId ?? 0, userId ?? 0),
    queryFn: () =>
      onboardingInvitationApi.getOrganizationUserOnboardingDetail(organizationId ?? 0, userId ?? 0),
    enabled: !!organizationId && !!userId,
    staleTime: 30 * 1000,
    retry: (failureCount, error) => {
      if (error instanceof ApiRequestError && error.status === 404) return false;
      return failureCount < 2;
    },
  });
}

export function useOrganizationOnboardingAvailable(
  organizationId: number | null | undefined,
  enabled = true,
) {
  return useQuery({
    queryKey: onboardingInvitationKeys.organizationOnboardingAvailable(organizationId ?? 0),
    queryFn: () => onboardingInvitationApi.getOrganizationOnboardingAvailable(organizationId ?? 0),
    enabled: enabled && !!organizationId,
    staleTime: 5 * 60 * 1000,
  });
}

export function useOrganizationOnboardingInvitationDetail(
  organizationId: number | null | undefined,
  invitationId: number | null | undefined,
) {
  return useQuery({
    queryKey: onboardingInvitationKeys.organizationDetail(organizationId ?? 0, invitationId ?? 0),
    queryFn: () =>
      onboardingInvitationApi.getOrganizationDetail(organizationId ?? 0, invitationId ?? 0),
    enabled: !!organizationId && !!invitationId,
    staleTime: 30 * 1000,
  });
}

export function useSaveOnboardingStep(access: OnboardingInvitationAccess | null | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      stepType,
      request,
    }: {
      stepType: OnboardingStepType;
      request: SaveOnboardingStepRequest;
    }) => {
      if (!access) throw new Error('Onboarding access is required');
      return onboardingInvitationApi.saveStep(access, stepType, request);
    },
    onSuccess: (detail) => {
      if (!access) return;
      queryClient.setQueryData(detailQueryKey(access), detail);
      if (access.kind === 'self') {
        queryClient.invalidateQueries({ queryKey: onboardingInvitationKeys.self() });
      }
    },
  });
}

export function useUploadOnboardingDocument(access: OnboardingInvitationAccess | null | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      configKey,
      file,
      scope = 'tenant',
    }: {
      configKey: string;
      file: File;
      scope?: 'tenant' | 'organization';
    }) => {
      if (!access) throw new Error('Onboarding access is required');
      return scope === 'organization'
        ? onboardingInvitationApi.uploadOrganizationSignedDocument(access, configKey, file)
        : onboardingInvitationApi.uploadSignedDocument(access, configKey, file);
    },
    onSuccess: (detail) => {
      if (!access) return;
      queryClient.setQueryData(detailQueryKey(access), detail);
    },
  });
}

export function useUploadOnboardingRecordAttachment(
  access: OnboardingInvitationAccess | null | undefined,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      stepType,
      recordKey,
      file,
    }: {
      stepType: OnboardingStepType;
      recordKey: string;
      file: File;
    }) => {
      if (!access) throw new Error('Onboarding access is required');
      return onboardingInvitationApi.uploadRecordAttachment(access, stepType, recordKey, file);
    },
    onSuccess: (detail) => {
      if (!access) return;
      queryClient.setQueryData(detailQueryKey(access), detail);
    },
  });
}

export function useUploadOnboardingDrivingLicenseAttachment(
  access: OnboardingInvitationAccess | null | undefined,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => {
      if (!access) throw new Error('Onboarding access is required');
      return onboardingInvitationApi.uploadDrivingLicenseAttachment(access, file);
    },
    onSuccess: (detail) => {
      if (!access) return;
      queryClient.setQueryData(detailQueryKey(access), detail);
    },
  });
}

export function useDeleteOnboardingRecordAttachment(
  access: OnboardingInvitationAccess | null | undefined,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      stepType,
      recordKey,
      attachmentIndex,
    }: {
      stepType: OnboardingStepType;
      recordKey: string;
      attachmentIndex: number;
    }) => {
      if (!access) throw new Error('Onboarding access is required');
      return onboardingInvitationApi.deleteRecordAttachment(
        access,
        stepType,
        recordKey,
        attachmentIndex,
      );
    },
    onSuccess: (detail) => {
      if (!access) return;
      queryClient.setQueryData(detailQueryKey(access), detail);
    },
  });
}

export function useDeleteOnboardingDrivingLicenseAttachment(
  access: OnboardingInvitationAccess | null | undefined,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (attachmentIndex: number) => {
      if (!access) throw new Error('Onboarding access is required');
      return onboardingInvitationApi.deleteDrivingLicenseAttachment(access, attachmentIndex);
    },
    onSuccess: (detail) => {
      if (!access) return;
      queryClient.setQueryData(detailQueryKey(access), detail);
    },
  });
}

export function useUploadOnboardingRankAttachment(
  access: OnboardingInvitationAccess | null | undefined,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => {
      if (!access) throw new Error('Onboarding access is required');
      return onboardingInvitationApi.uploadRankAttachment(access, file);
    },
    onSuccess: (detail) => {
      if (!access) return;
      queryClient.setQueryData(detailQueryKey(access), detail);
    },
  });
}

export function useDeleteOnboardingRankAttachment(
  access: OnboardingInvitationAccess | null | undefined,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (attachmentIndex: number) => {
      if (!access) throw new Error('Onboarding access is required');
      return onboardingInvitationApi.deleteRankAttachment(access, attachmentIndex);
    },
    onSuccess: (detail) => {
      if (!access) return;
      queryClient.setQueryData(detailQueryKey(access), detail);
    },
  });
}

export function useAdminOnboardingInvitationDetail(
  tenantId: number | null | undefined,
  organizationId: number | null | undefined,
  invitationId: number | null | undefined,
) {
  return useQuery({
    queryKey: onboardingInvitationKeys.adminDetail(
      tenantId ?? 0,
      organizationId ?? 0,
      invitationId ?? 0,
    ),
    queryFn: () =>
      adminApi.getAdminOnboardingInvitationDetail(
        tenantId ?? 0,
        organizationId ?? 0,
        invitationId ?? 0,
      ),
    enabled: !!tenantId && !!organizationId && !!invitationId,
    staleTime: 30 * 1000,
  });
}

export function useAdminUserOnboardingInvitation(
  tenantId: number | null | undefined,
  organizationId: number | null | undefined,
  userId: number | null | undefined,
  enabled = true,
) {
  return useQuery({
    queryKey: onboardingInvitationKeys.adminUserDetail(
      tenantId ?? 0,
      organizationId ?? 0,
      userId ?? 0,
    ),
    queryFn: () =>
      adminApi.getAdminUserOnboardingInvitation(tenantId ?? 0, organizationId ?? 0, userId ?? 0),
    enabled: enabled && !!tenantId && !!organizationId && !!userId,
    staleTime: 30 * 1000,
    retry: (failureCount, error) => {
      if (error instanceof ApiRequestError && error.status === 404) return false;
      return failureCount < 2;
    },
  });
}

export function useAdminOnboardingRecordingAvailable(
  tenantId: number | null | undefined,
  organizationId: number | null | undefined,
  userId: number | null | undefined,
  enabled = true,
) {
  return useQuery({
    queryKey: onboardingInvitationKeys.adminRecordingAvailable(
      tenantId ?? 0,
      organizationId ?? 0,
      userId ?? 0,
    ),
    queryFn: () =>
      adminApi.getOnboardingRecordingAvailable(tenantId ?? 0, organizationId ?? 0, userId ?? 0),
    enabled: enabled && !!tenantId && !!organizationId && !!userId,
    staleTime: 30 * 1000,
  });
}

export function useStartAdminOnboardingRecording(
  tenantId: number,
  organizationId: number,
  userId: number,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => adminApi.startAdminOnboardingRecording(tenantId, organizationId, userId),
    onSuccess: (detail) => {
      queryClient.setQueryData(
        onboardingInvitationKeys.adminUserDetail(tenantId, organizationId, userId),
        detail,
      );
      queryClient.setQueryData(
        onboardingInvitationKeys.adminDetail(tenantId, organizationId, detail.id),
        detail,
      );
      queryClient.setQueryData(
        onboardingInvitationKeys.adminRecordingAvailable(tenantId, organizationId, userId),
        { available: false },
      );
    },
  });
}

export function useCompleteAdminOnboardingInvitation(
  tenantId: number,
  organizationId: number,
  userId?: number,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      invitationId,
      adminRecordReason,
    }: {
      invitationId: number;
      adminRecordReason: string;
    }) =>
      adminApi.completeAdminOnboardingInvitation(
        tenantId,
        organizationId,
        invitationId,
        adminRecordReason,
      ),
    onSuccess: (_void, variables) => {
      queryClient.invalidateQueries({
        queryKey: onboardingInvitationKeys.adminDetail(
          tenantId,
          organizationId,
          variables.invitationId,
        ),
      });
      if (userId != null) {
        queryClient.invalidateQueries({
          queryKey: onboardingInvitationKeys.adminUserDetail(tenantId, organizationId, userId),
        });
      } else {
        queryClient.invalidateQueries({
          queryKey: [...onboardingInvitationKeys.all, 'admin-user', tenantId, organizationId],
        });
      }
    },
  });
}
