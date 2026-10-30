/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { UserDocumentDto } from '@/types/userDocument';

export function collectUserDocumentTags(documents: UserDocumentDto[]): string[] {
  const tags = new Set<string>();
  for (const doc of documents) {
    for (const tag of doc.tags) {
      tags.add(tag);
    }
  }
  return [...tags].sort((a, b) => a.localeCompare(b, undefined, { sensitivity: 'base' }));
}
