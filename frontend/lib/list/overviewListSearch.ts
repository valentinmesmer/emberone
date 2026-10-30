/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

export function listSearchTokens(search: string): string[] {
  return search
    .trim()
    .toLowerCase()
    .split(/\s+/)
    .filter((token) => token.length > 0);
}

export function matchesListSearch(
  search: string,
  ...parts: (string | null | undefined)[]
): boolean {
  const tokens = listSearchTokens(search);
  if (tokens.length === 0) {
    return true;
  }
  const haystack = parts
    .filter((part): part is string => typeof part === 'string' && part.trim() !== '')
    .join(' ')
    .toLowerCase();
  return tokens.every((token) => haystack.includes(token));
}
