/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
