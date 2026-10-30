/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { useQuery, useQueryClient } from '@tanstack/react-query';
import { fetchAllPageContent } from '@/lib/api/paged';
import { type UserDocumentListRequest, userDocumentApi } from '@/lib/api/userDocument';
import type { ListQuery } from '@/lib/list/types';
import { useUrlSyncedListState } from '@/lib/list/useListState';
import { userDocumentOrgListDefinition } from '@/lib/list/userDocumentList';
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
    staleTime: 60 * 1000,
  });
}

/** Loads all pages (document pickers, detail lookups). */
export function useOrganizationUserDocuments(
  organizationId: number | null | undefined,
  userId: number | null | undefined,
) {
  return useQuery({
    queryKey: [
      ...organizationUserDocumentKeys.listRoot(organizationId ?? 0, userId ?? 0),
      'all',
    ] as const,
    queryFn: () =>
      fetchAllPageContent((page) =>
        userDocumentApi.listForOrganizationUser(organizationId ?? 0, userId ?? 0, { page }),
      ),
    enabled: !!organizationId && !!userId,
    staleTime: 60 * 1000,
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
    staleTime: 60 * 1000,
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
        queryKey: organizationUserDocumentKeys.tags(organizationId, userId),
      });
    },
  });
}
