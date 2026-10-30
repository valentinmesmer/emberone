/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { authorizedFetch } from '@/lib/api/authorizedFetch';

/** Fetches a protected download URL; returns `null` when no image is set (204). */
export async function fetchAuthenticatedBlob(downloadUrl: string): Promise<Blob | null> {
  const response = await authorizedFetch(downloadUrl, { method: 'GET' });

  if (response.status === 204) {
    return null;
  }
  if (!response.ok) {
    throw new Error(`Failed to fetch image (${response.status})`);
  }
  return response.blob();
}

export function blobToObjectUrl(blob: Blob): string {
  return URL.createObjectURL(blob);
}

export function revokeObjectUrlIfBlob(url: string | null | undefined): void {
  if (url?.startsWith('blob:')) {
    URL.revokeObjectURL(url);
  }
}
