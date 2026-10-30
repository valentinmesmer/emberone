/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

export function parseContentDispositionFileName(
  contentDisposition: string | null | undefined,
): string | null {
  if (!contentDisposition) {
    return null;
  }

  const rfc5987Match = contentDisposition.match(/filename\*=(?:UTF-8''|utf-8'')([^;]+)/i);
  if (rfc5987Match) {
    try {
      return decodeURIComponent(rfc5987Match[1].trim());
    } catch {
      // Fall through to standard filename parsing.
    }
  }

  const quotedMatch = contentDisposition.match(/filename="([^"]+)"/i);
  if (quotedMatch) {
    return quotedMatch[1];
  }

  const unquotedMatch = contentDisposition.match(/filename=([^;]+)/i);
  if (unquotedMatch) {
    return unquotedMatch[1].trim();
  }

  return null;
}
