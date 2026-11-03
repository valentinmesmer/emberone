/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

export const VIEWABLE_ATTACHMENT_MEDIA_TYPES = new Set([
  'application/pdf',
  'image/jpeg',
  'image/png',
  'image/gif',
  'image/webp',
]);

export function isViewableAttachmentMediaType(contentType: string | null | undefined): boolean {
  if (!contentType) {
    return false;
  }
  return VIEWABLE_ATTACHMENT_MEDIA_TYPES.has(contentType.toLowerCase());
}
