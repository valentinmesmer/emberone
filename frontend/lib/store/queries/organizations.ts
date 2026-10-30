/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

// React Query hooks for Organizations (Server State)

import { useQuery } from '@tanstack/react-query';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { managementApi } from '@/lib/api/management';
import { organizationApi } from '@/lib/api/organization';
import { createInitialListQueryState, toListQuery } from '@/lib/list';
import { organizationGroupMemberListDefinition } from '@/lib/list/organizationGroupMemberList';
import type { ListQuery } from '@/lib/list/types';
import { useUrlSyncedListState } from '@/lib/list/useListState';
import { resolveOrganizationSource } from '@/lib/management/workspaceOrganizations';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { usePagedList } from '@/lib/store/usePagedList';
import { PermissionTypeEnum } from '@/types/permission';
import { useMyManagementActivations } from './management';

export const organizationKeys = {
  all: ['organizations'] as const,
  myOrganizations: () => [...organizationKeys.all, 'my'] as const,
  joinable: () => [...organizationKeys.all, 'joinable'] as const,
  mainChangeTargets: () => [...organizationKeys.all, 'main-change-targets'] as const,
  leaveTargets: () => [...organizationKeys.all, 'leave-targets'] as const,
  groupLeaveTargets: () => [...organizationKeys.all, 'group-leave-targets'] as const,
  groupJoinTargets: () => [...organizationKeys.all, 'group-join-targets'] as const,
  byId: (id: number) => [...organizationKeys.all, id] as const,
  myGroups: (organizationId: number) =>
    [...organizationKeys.all, organizationId, 'my-groups'] as const,
  assignableGroups: (organizationId: number) =>
    [...organizationKeys.all, organizationId, 'groups', 'assignable'] as const,
  audienceGroups: (organizationId: number) =>
    [...organizationKeys.all, organizationId, 'groups', 'audience'] as const,
  groups: (organizationId: number, listQuery: ListQuery) =>
    [...organizationKeys.all, organizationId, 'groups', listQuery] as const,
  groupById: (groupId: number) => [...organizationKeys.all, 'groups', groupId] as const,
  groupMembers: (groupId: number, listQuery: ListQuery) =>
    [...organizationKeys.all, 'groups', groupId, 'members', listQuery] as const,
};

const defaultOrganizationGroupMemberListQuery = toListQuery(
  organizationGroupMemberListDefinition,
  createInitialListQueryState(organizationGroupMemberListDefinition),
);

export function useMyOrganizations() {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: organizationKeys.myOrganizations(),
    queryFn: () => organizationApi.getMyOrganizations(),
    enabled: hasEmberOneUserId,
    staleTime: 5 * 60 * 1000,
  });
}

export function useHasActiveOrganizationMembership() {
  const { data: memberships = [], isLoading, isPending, isFetching } = useMyOrganizations();
  return {
    hasOrganizationMembership: memberships.length > 0,
    isLoading: isLoading || (isPending && isFetching),
  };
}

export function useJoinableOrganizations(enabled = true) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: organizationKeys.joinable(),
    queryFn: () => organizationApi.getJoinableOrganizations(),
    enabled: hasEmberOneUserId && enabled,
    staleTime: 60 * 1000,
  });
}

export function useMainOrganizationChangeTargets(enabled = true) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: organizationKeys.mainChangeTargets(),
    queryFn: () => organizationApi.getMainOrganizationChangeTargets(),
    enabled: hasEmberOneUserId && enabled,
    staleTime: 60 * 1000,
  });
}

export function useOrganizationLeaveTargets(enabled = true) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: organizationKeys.leaveTargets(),
    queryFn: () => organizationApi.getOrganizationLeaveTargets(),
    enabled: hasEmberOneUserId && enabled,
    staleTime: 60 * 1000,
  });
}

export function useOrganizationGroupLeaveTargets(enabled = true) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: organizationKeys.groupLeaveTargets(),
    queryFn: () => organizationApi.getOrganizationGroupLeaveTargets(),
    enabled: hasEmberOneUserId && enabled,
    staleTime: 60 * 1000,
  });
}

export function useOrganizationGroupJoinTargets(enabled = true) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: organizationKeys.groupJoinTargets(),
    queryFn: () => organizationApi.getOrganizationGroupJoinTargets(),
    enabled: hasEmberOneUserId && enabled,
    staleTime: 60 * 1000,
  });
}

/**
 * Master data of an organization. The source decides the endpoint: membership uses the self
 * organization endpoint, management the managed-target endpoint. Waits until both lists settled.
 */
export function useOrganizationById(id: number | null | undefined) {
  const memberships = useMyOrganizations();
  const activations = useMyManagementActivations();
  const settled =
    (!memberships.isPending || memberships.fetchStatus === 'idle') &&
    (!activations.isPending || activations.fetchStatus === 'idle');
  const source =
    id && settled
      ? resolveOrganizationSource(id, memberships.data ?? [], activations.data ?? [], Date.now())
      : null;

  return useQuery({
    queryKey: [...organizationKeys.byId(id ?? 0), source],
    queryFn: () =>
      source === 'management'
        ? managementApi.getTargetOrganization(id ?? 0)
        : organizationApi.getOrganizationById(id ?? 0),
    enabled: !!id && source !== null,
    staleTime: 5 * 60 * 1000,
  });
}

/** Current user's groups in one organization (self endpoint). */
export function useMyOrganizationGroups(organizationId: number | null | undefined) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: organizationKeys.myGroups(organizationId ?? 0),
    queryFn: () => organizationApi.getMyOrganizationGroups(organizationId ?? 0),
    enabled: hasEmberOneUserId && !!organizationId,
    staleTime: 5 * 60 * 1000,
  });
}

/** Groups available for membership assignment (USER_WRITE). */
export function useAssignableOrganizationGroups(organizationId: number | null | undefined) {
  const { hasPermission, isLoading: permLoading } = usePermissions();
  const canWrite =
    organizationId != null && hasPermission(organizationId, PermissionTypeEnum.USER_WRITE);

  return useQuery({
    queryKey: organizationKeys.assignableGroups(organizationId ?? 0),
    queryFn: () => organizationApi.getAssignableOrganizationGroups(organizationId ?? 0),
    enabled: !!organizationId && !permLoading && canWrite,
    staleTime: 5 * 60 * 1000,
  });
}

/** Groups available as appointment audience (APPOINTMENT_WRITE). */
export function useAppointmentAudienceGroups(organizationId: number | null | undefined) {
  const { hasPermission, isLoading: permLoading } = usePermissions();
  const canWrite =
    organizationId != null && hasPermission(organizationId, PermissionTypeEnum.APPOINTMENT_WRITE);

  return useQuery({
    queryKey: organizationKeys.audienceGroups(organizationId ?? 0),
    queryFn: () => organizationApi.getAppointmentAudienceGroups(organizationId ?? 0),
    enabled: !!organizationId && !permLoading && canWrite,
    staleTime: 5 * 60 * 1000,
  });
}

export function useOrganizationGroupList(
  organizationId: number | null | undefined,
  listQuery: ListQuery,
) {
  const { hasPermission, isLoading: permLoading } = usePermissions();
  const canRead =
    organizationId != null &&
    hasPermission(organizationId, PermissionTypeEnum.ORGANIZATION_GROUP_READ);

  return usePagedList({
    queryKey: organizationKeys.groups(organizationId ?? 0, listQuery),
    listQuery,
    queryFn: (query) => organizationApi.listOrganizationGroups(organizationId ?? 0, query),
    enabled: !!organizationId && !permLoading && canRead,
    staleTime: 5 * 60 * 1000,
  });
}

export function useOrganizationGroupById(
  groupId: number | null | undefined,
  organizationId: number | null | undefined,
) {
  const { hasPermission, isLoading: permLoading } = usePermissions();
  const canRead =
    organizationId != null &&
    hasPermission(organizationId, PermissionTypeEnum.ORGANIZATION_GROUP_READ);

  return useQuery({
    queryKey: organizationKeys.groupById(groupId ?? 0),
    queryFn: () => organizationApi.getOrganizationGroupById(groupId ?? 0),
    enabled: !!groupId && !!organizationId && !permLoading && canRead,
    staleTime: 5 * 60 * 1000,
  });
}

export function useOrganizationGroupMembers(
  groupId: number | null | undefined,
  organizationId: number | null | undefined,
  options?: { listQuery?: ListQuery; fetchAll?: boolean },
) {
  const listQuery = options?.listQuery ?? defaultOrganizationGroupMemberListQuery;
  const { hasPermission, isLoading: permLoading } = usePermissions();
  const canRead =
    organizationId != null &&
    hasPermission(organizationId, PermissionTypeEnum.ORGANIZATION_GROUP_READ);

  return usePagedList({
    queryKey: organizationKeys.groupMembers(groupId ?? 0, listQuery),
    listQuery,
    queryFn: (query) => organizationApi.listOrganizationGroupMembers(groupId ?? 0, query),
    enabled: !!groupId && !!organizationId && !permLoading && canRead,
    fetchAll: options?.fetchAll ?? false,
    staleTime: 5 * 60 * 1000,
  });
}

export function useOrganizationGroupMemberList(
  groupId: number | null | undefined,
  organizationId: number | null | undefined,
  listQuery: ListQuery,
) {
  return useOrganizationGroupMembers(groupId, organizationId, { listQuery, fetchAll: false });
}

export function useUrlSyncedOrganizationGroupMemberListState() {
  return useUrlSyncedListState(organizationGroupMemberListDefinition);
}
