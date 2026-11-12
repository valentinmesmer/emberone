/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { useQuery, useQueryClient } from '@tanstack/react-query';
import { type UserDocumentListRequest, userDocumentApi } from '@/lib/api/userDocument';
import type { ListQuery } from '@/lib/list/types';
import { useUrlSyncedListState } from '@/lib/list/useListState';
import { userDocumentOrgListDefinition } from '@/lib/list/userDocumentList';
import { STALE_TIME } from '@/lib/store/staleTime';
import { useAppMutation } from '@/lib/store/useAppMutation';
import { usePagedList } from '@/lib/store/usePagedList';
import type {
  UpdateUserDocumentRequest,
  UploadUserDocumentParams,
  UserDocumentDto,
} from '@/types/userDocument';

export const organizationUserDocumentKeys = {
  all: ['organization-user-documents'] as const,
  listRoot: (organizationId: number, userId: number) =>
    [...organizationUserDocumentKeys.all, organizationId, userId, 'list'] as const,
  byIds: (organizationId: number, userId: number, ids: readonly number[]) =>
    [...organizationUserDocumentKeys.listRoot(organizationId, userId), 'by-ids', ...ids] as const,
  detailRoot: (organizationId: number, userId: number) =>
    [...organizationUserDocumentKeys.all, organizationId, userId, 'detail'] as const,
  detail: (organizationId: number, userId: number, documentId: number) =>
    [...organizationUserDocumentKeys.detailRoot(organizationId, userId), documentId] as const,
  tags: (organizationId: number, userId: number) =>
    [...organizationUserDocumentKeys.all, organizationId, userId, 'tags'] as const,
};

export function useOrganizationUserDocumentListState() {
  return useUrlSyncedListState(userDocumentOrgListDefinition);
}

type UseOrganizationUserDocumentListOptions = {
  organizationId: number | null | undefined;
  userId: number | null | undefined;
  listQuery: ListQuery;
  enabled?: boolean;
};

export function useOrganizationUserDocumentList({
  organizationId,
  userId,
  listQuery,
  enabled = true,
}: UseOrganizationUserDocumentListOptions) {
  const orgId = organizationId ?? 0;
  const uid = userId ?? 0;
  return usePagedList<UserDocumentDto, UserDocumentListRequest>({
    queryKey: organizationUserDocumentKeys.listRoot(orgId, uid),
    listQuery: listQuery as UserDocumentListRequest,
    queryFn: (query) => userDocumentApi.listForOrganizationUser(orgId, uid, query),
    enabled: !!organizationId && !!userId && enabled,
    staleTime: STALE_TIME.LIST,
  });
}

export function useOrganizationUserDocument(
  organizationId: number | null | undefined,
  userId: number | null | undefined,
  documentId: number,
  enabled = true,
) {
  const orgId = organizationId ?? 0;
  const uid = userId ?? 0;
  return useQuery({
    queryKey: organizationUserDocumentKeys.detail(orgId, uid, documentId),
    queryFn: () => userDocumentApi.getForOrganizationUser(orgId, uid, documentId),
    enabled: !!organizationId && !!userId && enabled && documentId > 0,
    staleTime: STALE_TIME.LIST,
  });
}

/** Metadata of the documents linked to a record (ids come from the record's `userDocumentIds`). */
export function useOrganizationUserDocumentsByIds(
  organizationId: number | null | undefined,
  userId: number | null | undefined,
  ids: readonly number[],
  enabled = true,
) {
  const orgId = organizationId ?? 0;
  const uid = userId ?? 0;
  return useQuery({
    queryKey: organizationUserDocumentKeys.byIds(orgId, uid, ids),
    queryFn: () => userDocumentApi.listByIdsForOrganizationUser(orgId, uid, ids),
    enabled: !!organizationId && !!userId && enabled && ids.length > 0,
    staleTime: STALE_TIME.LIST,
  });
}

export function useOrganizationUserDocumentTags(
  organizationId: number | null | undefined,
  userId: number | null | undefined,
  enabled = true,
) {
  const orgId = organizationId ?? 0;
  const uid = userId ?? 0;
  return useQuery({
    queryKey: organizationUserDocumentKeys.tags(orgId, uid),
    queryFn: () => userDocumentApi.listTagsForOrganizationUser(orgId, uid),
    enabled: !!organizationId && !!userId && enabled,
    staleTime: STALE_TIME.LIST,
  });
}

export function useUploadOrganizationUserDocument(organizationId: number, userId: number) {
  const queryClient = useQueryClient();
  return useAppMutation({
    mutationFn: (params: UploadUserDocumentParams) =>
      userDocumentApi.upload(organizationId, userId, params),
    meta: { errorMessageKey: 'myArea.documents.uploadFailed' },
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: organizationUserDocumentKeys.listRoot(organizationId, userId),
      });
      void queryClient.invalidateQueries({
        queryKey: organizationUserDocumentKeys.tags(organizationId, userId),
      });
    },
  });
}

export function useUpdateOrganizationUserDocument(organizationId: number, userId: number) {
  const queryClient = useQueryClient();
  return useAppMutation({
    mutationFn: ({ documentId, data }: { documentId: number; data: UpdateUserDocumentRequest }) =>
      userDocumentApi.update(organizationId, userId, documentId, data),
    meta: { errorMessageKey: 'myArea.documents.updateFailed' },
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: organizationUserDocumentKeys.listRoot(organizationId, userId),
      });
      void queryClient.invalidateQueries({
        queryKey: organizationUserDocumentKeys.detailRoot(organizationId, userId),
      });
      void queryClient.invalidateQueries({
        queryKey: organizationUserDocumentKeys.tags(organizationId, userId),
      });
    },
  });
}
