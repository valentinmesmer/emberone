/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

// React Query hooks for Users (Server State)

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo } from 'react';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { userApi } from '@/lib/api/user';
import { createInitialFilterValues, createInitialListQueryState } from '@/lib/list';
import {
  organizationMemberListDefinition,
  toOrganizationMemberApiListQuery,
  toOrganizationMemberListQuery,
} from '@/lib/list/organizationMemberList';
import type { ListQuery } from '@/lib/list/types';
import { usePagedList } from '@/lib/store/usePagedList';
import type { CreateUserRequest, UpdatePersonalDataRequest } from '@/types/user';
import { onboardingInvitationKeys } from './onboardingInvitation';

// Query Keys
export const userKeys = {
  all: ['users'] as const,
  self: () => [...userKeys.all, 'self'] as const,
  organizationTree: () => [...userKeys.all, 'self', 'organization-tree'] as const,
  membershipHistory: () => [...userKeys.all, 'self', 'membership-history'] as const,
  serviceHoursTotal: (year: number) =>
    [...userKeys.all, 'self', 'service-hours-total', year] as const,
  operationAlarmAttendanceStats: () =>
    [...userKeys.all, 'self', 'operation-alarm-attendance-stats'] as const,
  byOrganization: (organizationId: number) =>
    [...userKeys.all, 'organization', organizationId] as const,
  byOrganizationIncludingChildren: (organizationId: number) =>
    [...userKeys.all, 'organization', organizationId, 'including-children'] as const,
};

// Hooks

/**
 * Get current user's data
 */
export function useSelf() {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: userKeys.self(),
    queryFn: () => userApi.getSelf(),
    enabled: hasEmberOneUserId,
    staleTime: 5 * 60 * 1000,
  });
}

/**
 * Get current user's organization tree
 */
export function useOrganizationTree() {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: userKeys.organizationTree(),
    queryFn: () => userApi.getOrganizationTree(),
    enabled: hasEmberOneUserId,
    staleTime: 5 * 60 * 1000,
  });
}

/**
 * Get current user's organization and group memberships, including past ones
 */
export function useMembershipHistory() {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: userKeys.membershipHistory(),
    queryFn: () => userApi.getMembershipHistory(),
    enabled: hasEmberOneUserId,
    staleTime: 5 * 60 * 1000,
  });
}

export function useSelfServiceHoursTotal(year: number) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: userKeys.serviceHoursTotal(year),
    queryFn: () => userApi.getSelfServiceHoursTotal(year),
    enabled: hasEmberOneUserId,
    staleTime: 60 * 1000,
  });
}

export function useSelfOperationAlarmAttendanceStats() {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: userKeys.operationAlarmAttendanceStats(),
    queryFn: () => userApi.getSelfOperationAlarmAttendanceStats(),
    enabled: hasEmberOneUserId,
    staleTime: 60 * 1000,
  });
}

const defaultOrganizationMemberListQuery = toOrganizationMemberListQuery(
  createInitialListQueryState(organizationMemberListDefinition),
);

/** Full member list for lookups (dropdowns, validation). */
export function useUsersForOrganization(organizationId: number | null | undefined) {
  return usePagedList({
    queryKey: userKeys.byOrganization(organizationId ?? 0),
    listQuery: defaultOrganizationMemberListQuery,
    queryFn: (query) =>
      userApi.listUsersForOrganization(
        organizationId ?? 0,
        toOrganizationMemberApiListQuery(query),
      ),
    enabled: !!organizationId,
    fetchAll: true,
    staleTime: 5 * 60 * 1000,
  });
}

export function useOrganizationMemberList(
  organizationId: number | null | undefined,
  listQuery: ListQuery,
) {
  return usePagedList({
    queryKey: [...userKeys.byOrganization(organizationId ?? 0), 'list', listQuery] as const,
    listQuery,
    queryFn: (query) =>
      userApi.listUsersForOrganization(
        organizationId ?? 0,
        toOrganizationMemberApiListQuery(query),
      ),
    enabled: !!organizationId,
    staleTime: 5 * 60 * 1000,
  });
}

/** Organization members for searchable pickers (always loads all result pages). */
export function useOrganizationMembersForPicker(
  organizationId: number | null | undefined,
  debouncedSearch: string,
) {
  const trimmed = debouncedSearch.trim();
  const listQuery = useMemo(
    () =>
      toOrganizationMemberListQuery({
        filters: createInitialFilterValues(organizationMemberListDefinition),
        debouncedSearch: trimmed,
      }),
    [trimmed],
  );
  return usePagedList({
    queryKey: [...userKeys.byOrganization(organizationId ?? 0), 'member-picker', trimmed] as const,
    listQuery,
    queryFn: (query) =>
      userApi.listUsersForOrganization(
        organizationId ?? 0,
        toOrganizationMemberApiListQuery(query),
      ),
    enabled: !!organizationId,
    fetchAll: true,
    staleTime: 5 * 60 * 1000,
  });
}

// Mutations

function invalidateSelfQueries(queryClient: ReturnType<typeof useQueryClient>) {
  queryClient.invalidateQueries({ queryKey: userKeys.self() });
  queryClient.invalidateQueries({ queryKey: userKeys.organizationTree() });
  queryClient.invalidateQueries({ queryKey: userKeys.membershipHistory() });
}

/**
 * Request a notification email change (requires verification)
 */
export function useUpdateNotificationEmail() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: { privateEmail: string }) => userApi.updateNotificationEmail(data),
    onSuccess: () => invalidateSelfQueries(queryClient),
  });
}

/**
 * Confirm a pending notification email change
 */
export function useVerifyNotificationEmail() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: { token: string }) => userApi.verifyNotificationEmail(data),
    onSuccess: (data) => {
      queryClient.setQueryData(userKeys.self(), data);
      invalidateSelfQueries(queryClient);
    },
  });
}

/**
 * Resend verification for a pending notification email change
 */
export function useResendNotificationEmailVerification() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => userApi.resendNotificationEmailVerification(),
    onSuccess: () => invalidateSelfQueries(queryClient),
  });
}

/**
 * Cancel a pending notification email change
 */
export function useCancelPendingNotificationEmail() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => userApi.cancelPendingNotificationEmail(),
    onSuccess: () => invalidateSelfQueries(queryClient),
  });
}
/**
 * Create user for an organization
 */
export function useCreateUserForOrganization() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ organizationId, data }: { organizationId: number; data: CreateUserRequest }) =>
      userApi.createUserForOrganization(organizationId, data),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: userKeys.byOrganization(variables.organizationId),
      });
      queryClient.invalidateQueries({
        queryKey: userKeys.byOrganizationIncludingChildren(variables.organizationId),
      });
      queryClient.invalidateQueries({
        queryKey: onboardingInvitationKeys.byOrganization(variables.organizationId),
      });
    },
  });
}

/**
 * Update personal data for a user (admin only)
 */
export function useUpdatePersonalData() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      organizationId,
      userId,
      data,
    }: {
      organizationId: number;
      userId: number;
      data: UpdatePersonalDataRequest;
    }) => userApi.updatePersonalData(organizationId, userId, data),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: userKeys.byOrganization(variables.organizationId),
      });
      queryClient.invalidateQueries({
        queryKey: userKeys.byOrganizationIncludingChildren(variables.organizationId),
      });
      queryClient.invalidateQueries({ queryKey: userKeys.self() });
    },
  });
}
