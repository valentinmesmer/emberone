/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { type QueryClient, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
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

function revokeResolvedProfileImageCache(queryClient: QueryClient) {
  for (const [, cachedUrl] of queryClient.getQueriesData<string | null>({
    queryKey: [...profileMediaKeys.all, 'resolved'],
  })) {
    revokeObjectUrlIfBlob(cachedUrl);
  }
}

function revokeSelfProfileImageCache(queryClient: QueryClient) {
  const prev = queryClient.getQueryData<string | null>(profileMediaKeys.selfImage());
  revokeObjectUrlIfBlob(prev);
}

export function invalidateSelfProfileImage(queryClient: QueryClient) {
  revokeSelfProfileImageCache(queryClient);
  revokeResolvedProfileImageCache(queryClient);
  queryClient.removeQueries({ queryKey: profileMediaKeys.selfImage() });
  queryClient.removeQueries({ queryKey: [...profileMediaKeys.all, 'resolved'] });
}

/**
 * Shared resolver: one fetch per download URL (browser HTTP cache + React Query).
 * Returns a blob object URL for image elements; revoked when cache entries are removed.
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
      try {
        const blob = await fetchAuthenticatedBlob(downloadUrl);
        return blob ? blobToObjectUrl(blob) : null;
      } catch {
        return null;
      }
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
      void queryClient.invalidateQueries({ queryKey: userKeys.self() });
    },
  });
}

export function useDeleteProfileImage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => documentApi.deleteProfileImage(),
    onSuccess: () => {
      invalidateSelfProfileImage(queryClient);
      void queryClient.invalidateQueries({ queryKey: userKeys.self() });
    },
  });
}
