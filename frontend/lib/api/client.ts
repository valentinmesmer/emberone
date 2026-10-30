/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

// API Client Configuration

import { ApiRequestError, authorizedFetch, parseApiError } from '@/lib/api/authorizedFetch';
import { getRuntimeConfig } from '@/lib/runtimeConfig';

export { ApiRequestError };

export class ApiClient {
  constructor(private readonly baseUrlOverride: string | null = null) {}

  private async resolveBaseUrl(): Promise<string> {
    if (this.baseUrlOverride !== null) {
      return this.baseUrlOverride;
    }
    const cfg = await getRuntimeConfig();
    return cfg.NEXT_PUBLIC_API_BASE_URL || '';
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit & { skipAuth?: boolean } = {},
  ): Promise<T> {
    const baseUrl = await this.resolveBaseUrl();
    const url = `${baseUrl}${endpoint}`;

    const { skipAuth = false, headers: optionHeaders, ...fetchOptions } = options;

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...((optionHeaders as Record<string, string>) || {}),
    };

    try {
      const response = await authorizedFetch(url, { ...fetchOptions, headers, skipAuth });

      if (!response.ok) {
        throw await parseApiError(response);
      }

      // Handle 204 No Content responses (empty body)
      if (response.status === 204 || response.status === 201) {
        const contentType = response.headers.get('content-type');
        const contentLength = response.headers.get('content-length');

        if (!contentType?.includes('application/json') || contentLength === '0') {
          return null as T;
        }
      }

      const text = await response.text();
      if (!text || text.trim() === '') {
        return null as T;
      }

      return JSON.parse(text);
    } catch (error) {
      if (error instanceof Error) {
        throw error;
      }
      throw new Error('An unexpected error occurred');
    }
  }

  async post<T>(endpoint: string, data: unknown, skipAuth = false): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'POST',
      body: JSON.stringify(data),
      skipAuth,
    });
  }

  /**
   * POST multipart (e.g. file upload). Do not set Content-Type - the browser sets the boundary.
   */
  async postFormData<T>(endpoint: string, formData: FormData, skipAuth = false): Promise<T> {
    const baseUrl = await this.resolveBaseUrl();
    const url = `${baseUrl}${endpoint}`;

    try {
      const response = await authorizedFetch(url, {
        method: 'POST',
        body: formData,
        skipAuth,
      });

      if (!response.ok) {
        throw await parseApiError(response);
      }

      if (response.status === 204) {
        return null as T;
      }

      const text = await response.text();
      if (!text || text.trim() === '') {
        return null as T;
      }

      return JSON.parse(text);
    } catch (error) {
      if (error instanceof Error) {
        throw error;
      }
      throw new Error('An unexpected error occurred');
    }
  }

  async get<T>(endpoint: string, skipAuth = false): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'GET',
      skipAuth,
    });
  }

  async put<T>(endpoint: string, data: unknown, skipAuth = false): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PUT',
      body: JSON.stringify(data),
      skipAuth,
    });
  }

  async patch<T>(endpoint: string, data: unknown, skipAuth = false): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PATCH',
      body: JSON.stringify(data),
      skipAuth,
    });
  }

  async delete<T>(endpoint: string, skipAuth = false): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'DELETE',
      skipAuth,
    });
  }

  /**
   * Authenticated GET returning a binary body (e.g. file download). Does not parse JSON.
   */
  async getBlob(
    endpoint: string,
    skipAuth = false,
  ): Promise<{ blob: Blob; contentDisposition: string | null }> {
    const baseUrl = await this.resolveBaseUrl();
    const url = `${baseUrl}${endpoint}`;

    const response = await authorizedFetch(url, { method: 'GET', skipAuth });
    if (!response.ok) {
      throw await parseApiError(response);
    }

    const blob = await response.blob();
    return {
      blob,
      contentDisposition: response.headers.get('content-disposition'),
    };
  }
}

export const apiClient = new ApiClient();
