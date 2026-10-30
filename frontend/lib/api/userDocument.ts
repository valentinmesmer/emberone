/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { buildQuery, type QueryParamValue } from '@/lib/api/paged';
import type { ListQuery } from '@/lib/list/types';
import { parseContentDispositionFileName } from '@/lib/utils/contentDisposition';
import { openAttachmentBlob } from '@/lib/utils/openAttachmentBlob';
import type { PageResponse } from '@/types/page';
import type {
  UnreadDocumentCountDto,
  UpdateUserDocumentRequest,
  UploadUserDocumentParams,
  UserDocumentDto,
} from '@/types/userDocument';
import { apiClient } from './client';

export type UserDocumentPageDto = PageResponse<UserDocumentDto>;

export type UserDocumentListApiParams = {
  tag?: string;
  viewed?: 'UNREAD' | 'READ';
  contentType?: string;
};

export type UserDocumentListRequest = ListQuery & UserDocumentListApiParams;

const SELF_BASE_PATH = '/api/v1/self/documents';

/** Maps menubar multiSelect state to single-value list GET params (backend contract). */
export function userDocumentListApiParams(query: ListQuery): UserDocumentListApiParams {
  const tag = singleMultiSelectValue(query.tag);
  const contentType = singleMultiSelectValue(query.contentType);
  const viewed = resolveViewedParam(query.viewed);
  return { tag, contentType, viewed };
}

function singleMultiSelectValue(raw: unknown): string | undefined {
  if (!Array.isArray(raw) || raw.length === 0) {
    return undefined;
  }
  const value = raw[0];
  return typeof value === 'string' && value !== '' ? value : undefined;
}

function resolveViewedParam(raw: unknown): UserDocumentListApiParams['viewed'] {
  if (!Array.isArray(raw) || raw.length === 0) {
    return undefined;
  }
  const unread = raw.includes('UNREAD');
  const read = raw.includes('READ');
  if (unread && read) {
    return undefined;
  }
  if (unread) {
    return 'UNREAD';
  }
  if (read) {
    return 'READ';
  }
  return undefined;
}

function listQueryToHttpParams(query: UserDocumentListRequest): Record<string, QueryParamValue> {
  const { tag, viewed, contentType } = userDocumentListApiParams(query);
  return {
    page: query.page ?? 0,
    size: query.size,
    search: query.search,
    sort: query.sort,
    tag,
    viewed,
    contentType,
  };
}

function organizationDocumentsPath(organizationId: number, userId: number): string {
  return `/api/v1/auth/organizations/${organizationId}/users/${userId}/documents`;
}

export const userDocumentApi = {
  async getUnreadCount(): Promise<UnreadDocumentCountDto> {
    return apiClient.get<UnreadDocumentCountDto>(`${SELF_BASE_PATH}/unread-count`);
  },

  async markViewed(documentId: number): Promise<void> {
    return apiClient.post<void>(`${SELF_BASE_PATH}/${documentId}/mark-viewed`, undefined);
  },

  async markAllViewed(): Promise<void> {
    return apiClient.post<void>(`${SELF_BASE_PATH}/mark-all-viewed`, undefined);
  },

  async list(query: UserDocumentListRequest = {}): Promise<UserDocumentPageDto> {
    const qs = buildQuery(listQueryToHttpParams(query));
    return apiClient.get<UserDocumentPageDto>(qs ? `${SELF_BASE_PATH}?${qs}` : SELF_BASE_PATH);
  },

  async listTags(): Promise<string[]> {
    return apiClient.get<string[]>(`${SELF_BASE_PATH}/tags`);
  },

  async listForOrganizationUser(
    organizationId: number,
    userId: number,
    query: UserDocumentListRequest = {},
  ): Promise<UserDocumentPageDto> {
    const qs = buildQuery(listQueryToHttpParams(query));
    const base = organizationDocumentsPath(organizationId, userId);
    return apiClient.get<UserDocumentPageDto>(qs ? `${base}?${qs}` : base);
  },

  async listTagsForOrganizationUser(organizationId: number, userId: number): Promise<string[]> {
    return apiClient.get<string[]>(`${organizationDocumentsPath(organizationId, userId)}/tags`);
  },

  async downloadForOrganizationUser(
    organizationId: number,
    userId: number,
    documentId: number,
    fallbackFileName: string,
  ): Promise<{ blob: Blob; fileName: string }> {
    const { blob, contentDisposition } = await apiClient.getBlob(
      `${organizationDocumentsPath(organizationId, userId)}/${documentId}/download`,
    );
    return {
      blob,
      fileName: parseContentDispositionFileName(contentDisposition) ?? fallbackFileName,
    };
  },

  async viewForOrganizationUser(
    organizationId: number,
    userId: number,
    documentId: number,
    targetWindow: Window,
    fallbackFileName = 'document',
  ): Promise<void> {
    const { blob, fileName } = await userDocumentApi.downloadForOrganizationUser(
      organizationId,
      userId,
      documentId,
      fallbackFileName,
    );
    openAttachmentBlob(blob, fileName, targetWindow);
  },

  async download(
    documentId: number,
    fallbackFileName: string,
  ): Promise<{ blob: Blob; fileName: string }> {
    const { blob, contentDisposition } = await apiClient.getBlob(
      `${SELF_BASE_PATH}/${documentId}/download`,
    );
    return {
      blob,
      fileName: parseContentDispositionFileName(contentDisposition) ?? fallbackFileName,
    };
  },

  /**
   * Opens a document in a new browser tab for inline viewing when the media type is allowlisted.
   * Otherwise triggers a download. Pass a window opened synchronously from the click handler
   * so popup blockers allow navigation after the async download.
   */
  async view(
    documentId: number,
    targetWindow: Window,
    fallbackFileName = 'document',
  ): Promise<void> {
    const { blob, fileName } = await userDocumentApi.download(documentId, fallbackFileName);
    openAttachmentBlob(blob, fileName, targetWindow);
  },

  async upload(
    organizationId: number,
    userId: number,
    params: UploadUserDocumentParams,
  ): Promise<UserDocumentDto> {
    const formData = new FormData();
    if (!params.file) {
      throw new Error('File is required');
    }
    formData.append('file', params.file);
    if (params.title) {
      formData.append('title', params.title);
    }
    if (params.description) {
      formData.append('description', params.description);
    }
    params.tags?.forEach((tag) => {
      formData.append('tags', tag);
    });
    return apiClient.postFormData<UserDocumentDto>(
      organizationDocumentsPath(organizationId, userId),
      formData,
    );
  },

  async update(
    organizationId: number,
    userId: number,
    documentId: number,
    data: UpdateUserDocumentRequest,
  ): Promise<UserDocumentDto> {
    return apiClient.put<UserDocumentDto>(
      `${organizationDocumentsPath(organizationId, userId)}/${documentId}`,
      data,
    );
  },
};
