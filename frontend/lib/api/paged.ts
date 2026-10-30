/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { PageResponse } from '@/types/page';

export type QueryParamValue =
  | string
  | number
  | boolean
  | readonly string[]
  | readonly number[]
  | undefined
  | null;

export type PagedListQuery = {
  page?: number;
  size?: number;
  search?: string;
};

/**
 * Serializes list/pagination query params for API requests.
 * Multi-select values use repeated keys; omit empty search.
 */
export function buildQuery(params: PagedListQuery & Record<string, QueryParamValue>): string {
  const searchParams = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null) {
      continue;
    }
    if (key === 'search' && typeof value === 'string' && !value.trim()) {
      continue;
    }
    if (Array.isArray(value)) {
      for (const item of value) {
        const serialized = String(item);
        if (serialized !== '') {
          searchParams.append(key, serialized);
        }
      }
      continue;
    }
    searchParams.set(key, String(value));
  }
  return searchParams.toString();
}

/** Safety cap: 200 rows/page × 50 pages = 10k items max per aggregate fetch. */
export const FETCH_ALL_MAX_PAGES = 50;

/** Fetches every page from a paginated list endpoint (up to {@link FETCH_ALL_MAX_PAGES}). */
export async function fetchAllPageContent<T>(
  fetchPage: (page: number) => Promise<PageResponse<T>>,
): Promise<T[]> {
  const items: T[] = [];
  let page = 0;
  let hasNext = true;
  while (hasNext && page < FETCH_ALL_MAX_PAGES) {
    const result = await fetchPage(page);
    items.push(...result.content);
    hasNext = result.hasNext;
    page += 1;
  }
  return items;
}
