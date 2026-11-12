/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import {
  matchQuery,
  type QueryCacheNotifyEvent,
  type QueryClient,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import {
  blobToObjectUrl,
  fetchAuthenticatedBlob,
  revokeObjectUrlIfBlob,
} from '@/lib/api/authenticatedBlob';
import { documentApi } from '@/lib/api/document';
import { userKeys } from './users';

const SESSION_IMAGE_CACHE = {
  staleTime: Infinity,
  gcTime: 60 * 60 * 1000,
  refetchOnMount: false,
  refetchOnWindowFocus: false,
  refetchOnReconnect: false,
} as const;

export const profileMediaKeys = {
  all: ['profileMedia'] as const,
  selfImage: () => [...profileMediaKeys.all, 'selfImage'] as const,
  orgUserImage: (organizationId: number, userId: number) =>
    [...profileMediaKeys.all, 'orgUserImage', organizationId, userId] as const,
  resolvedImage: (downloadUrl: string) =>
    [...profileMediaKeys.all, 'resolved', downloadUrl] as const,
};

const RESOLVED_IMAGE_KEY = [...profileMediaKeys.all, 'resolved'] as const;

/** Frees the blob behind a resolved image when React Query garbage-collects or clears the entry. */
export function revokeResolvedImageOnRemoval(event: QueryCacheNotifyEvent): void {
  if (event.type === 'removed' && matchQuery({ queryKey: RESOLVED_IMAGE_KEY }, event.query)) {
    revokeObjectUrlIfBlob(event.query.state.data as string | null | undefined);
  }
}

function revokeResolvedProfileImageCache(queryClient: QueryClient) {
  for (const [, cachedUrl] of queryClient.getQueriesData<string | null>({
    queryKey: RESOLVED_IMAGE_KEY,
  })) {
    revokeObjectUrlIfBlob(cachedUrl);
  }
}

/** The download URLs are stable, so drop the resolved blobs and let mounted images refetch (ETag revalidation). */
export function invalidateSelfProfileImage(queryClient: QueryClient) {
  revokeResolvedProfileImageCache(queryClient);
  void queryClient.resetQueries({ queryKey: RESOLVED_IMAGE_KEY });
}

/**
 * Shared resolver: one fetch per download URL, cached by React Query (the server answers
 * revalidation with 304). Returns a blob object URL for image elements; revoked when the cache
 * entry is removed (`revokeResolvedImageOnRemoval`).
 *
 * Fetch errors are not swallowed: a failed query is retried on the next mount instead of being
 * cached as "no image" for the rest of the session.
 */
export function useAuthenticatedProfileImageObjectUrl(
  downloadUrl: string | null | undefined,
  enabled = true,
) {
  const query = useQuery({
    queryKey: profileMediaKeys.resolvedImage(downloadUrl ?? ''),
    queryFn: async (): Promise<string | null> => {
      if (!downloadUrl) {
        return null;
      }
      const blob = await fetchAuthenticatedBlob(downloadUrl);
      return blob ? blobToObjectUrl(blob) : null;
    },
    enabled: enabled && downloadUrl != null,
    ...SESSION_IMAGE_CACHE,
  });

  return {
    objectUrl: query.data ?? null,
    loading: query.isLoading,
    error: query.isError,
  };
}

export function useSelfProfileImage(enabled = true) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: profileMediaKeys.selfImage(),
    queryFn: async (): Promise<string | null> => documentApi.getProfileImageUrl(),
    enabled: hasEmberOneUserId && enabled,
    ...SESSION_IMAGE_CACHE,
  });
}

export function useOrgUserProfileImage(
  organizationId: number | null | undefined,
  userId: number | null | undefined,
  enabled = true,
) {
  const resolvedOrganizationId = organizationId ?? null;
  const resolvedUserId = userId ?? null;
  const canLoad = resolvedOrganizationId != null && resolvedUserId != null && enabled;
  return useQuery({
    queryKey: profileMediaKeys.orgUserImage(resolvedOrganizationId ?? 0, resolvedUserId ?? 0),
    queryFn: async (): Promise<string | null> => {
      if (resolvedOrganizationId == null || resolvedUserId == null) {
        return null;
      }
      return documentApi.getOrgUserProfileImageUrl(resolvedOrganizationId, resolvedUserId);
    },
    enabled: canLoad,
    ...SESSION_IMAGE_CACHE,
  });
}

export function useUploadProfileImage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => documentApi.uploadProfileImage(file),
    onSuccess: () => {
      invalidateSelfProfileImage(queryClient);
      void queryClient.invalidateQueries({ queryKey: userKeys.self(), exact: true });
    },
  });
}

export function useDeleteProfileImage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => documentApi.deleteProfileImage(),
    onSuccess: () => {
      invalidateSelfProfileImage(queryClient);
      void queryClient.invalidateQueries({ queryKey: userKeys.self(), exact: true });
    },
  });
}
