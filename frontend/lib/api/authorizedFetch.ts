/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { getTokenGetter } from '@/lib/api/tokenGetter';
import { forceSilentRenew, redirectToLogin } from '@/lib/auth/sessionRecovery';

type ErrorBody = { error?: string; message?: string; code?: string };

export class ApiRequestError extends Error {
  readonly status: number;
  readonly code?: string;

  constructor(message: string, status: number, code?: string) {
    super(message);
    this.name = 'ApiRequestError';
    this.status = status;
    this.code = code;
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

export async function parseApiError(response: Response): Promise<ApiRequestError> {
  const errorData = (await response.json().catch(() => ({}))) as ErrorBody;
  const errorMessage =
    errorData.error || errorData.message || `HTTP error! status: ${response.status}`;
  const code = typeof errorData.code === 'string' && errorData.code ? errorData.code : undefined;
  return new ApiRequestError(errorMessage, response.status, code);
}

async function requireAccessToken(): Promise<string> {
  const tokenGetter = getTokenGetter();
  if (!tokenGetter) {
    await redirectToLogin();
    throw new ApiRequestError(
      'No access token available. Your session may have expired - sign in again.',
      401,
    );
  }

  let token: string | null = null;
  try {
    token = await tokenGetter();
  } catch {
    await redirectToLogin();
    throw new ApiRequestError(
      'Could not obtain an access token. Try signing in again or check the browser console.',
      401,
    );
  }

  if (!token?.trim()) {
    await redirectToLogin();
    throw new ApiRequestError(
      'No access token available. Your session may have expired - sign in again.',
      401,
    );
  }

  return token.trim();
}

export type AuthorizedFetchOptions = RequestInit & {
  /** When true, do not attach a Bearer token or run 401 recovery. */
  skipAuth?: boolean;
};

/**
 * Single entry point for authenticated browser fetches:
 * attach Bearer token → on 401 silent-renew once → retry → else login redirect.
 */
export async function authorizedFetch(
  url: string,
  init: AuthorizedFetchOptions = {},
): Promise<Response> {
  const { skipAuth = false, headers: initHeaders, ...rest } = init;
  const headers: Record<string, string> = {
    ...((initHeaders as Record<string, string> | undefined) ?? {}),
  };

  if (!skipAuth) {
    headers.Authorization = `Bearer ${await requireAccessToken()}`;
  }

  let response = await fetch(url, { ...rest, headers });

  if (skipAuth || response.status !== 401) {
    return response;
  }

  const renewedToken = await forceSilentRenew();
  if (!renewedToken) {
    await redirectToLogin();
    throw await parseApiError(response);
  }

  headers.Authorization = `Bearer ${renewedToken}`;
  response = await fetch(url, { ...rest, headers });

  if (response.status === 401) {
    await redirectToLogin();
  }

  return response;
}
