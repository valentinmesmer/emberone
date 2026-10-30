/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Suspense } from 'react';
import { LoadingState } from '@/components/ui/loading';
import { UserDocumentListView } from '@/components/my-area/documents/UserDocumentListView';

export default function DocumentsPage() {
  return (
    <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
      <UserDocumentListView />
    </Suspense>
  );
}
