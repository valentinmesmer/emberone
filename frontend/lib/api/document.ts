/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

// Document API endpoints

import { authorizedFetch, parseApiError } from '@/lib/api/authorizedFetch';
import { getRuntimeConfig } from '@/lib/runtimeConfig';

async function apiBase(): Promise<string> {
  const cfg = await getRuntimeConfig();
  return cfg.NEXT_PUBLIC_API_BASE_URL || '';
}

export const documentApi = {
  /** Upload profile image */
  async uploadProfileImage(file: File): Promise<string> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await authorizedFetch(`${await apiBase()}/api/v1/self/profile-image/upload`, {
      method: 'POST',
      body: formData,
    });

    if (!response.ok) {
      throw await parseApiError(response);
    }

    return response.text();
  },

  /** Absolute download URL for the current user's profile image. */
  async getProfileImageUrl(): Promise<string> {
    return `${await apiBase()}/api/v1/self/profile-image/download`;
  },

  /** Absolute download URL for another org member's profile image. */
  async getOrgUserProfileImageUrl(organizationId: number, userId: number): Promise<string> {
    return `${await apiBase()}/api/v1/auth/organizations/${organizationId}/users/${userId}/profile-image/download`;
  },

  /** Delete profile image */
  async deleteProfileImage(): Promise<void> {
    const response = await authorizedFetch(`${await apiBase()}/api/v1/self/profile-image/delete`, {
      method: 'DELETE',
    });

    if (!response.ok) {
      throw await parseApiError(response);
    }
  },
};
