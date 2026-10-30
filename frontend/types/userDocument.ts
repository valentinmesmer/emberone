/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { AppLinkFields, AppLinkType } from '@/types/appLink';

export type UserDocumentLinkType = AppLinkType;

export interface UserDocumentLinkDto extends AppLinkFields {
  id: number;
  linkType: UserDocumentLinkType;
  linkId: number;
}

export interface UserDocumentDto {
  id: number;
  title: string;
  description?: string | null;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  tags: string[];
  links: UserDocumentLinkDto[];
  viewedAt?: string | null;
}

export interface UnreadDocumentCountDto {
  count: number;
}

export interface UpdateUserDocumentRequest {
  title?: string | null;
  description?: string | null;
  tags?: string[];
}

export interface UploadUserDocumentParams {
  file?: File;
  title?: string;
  description?: string;
  tags?: string[];
}
