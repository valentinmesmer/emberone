/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Suspense } from 'react';
import { UserDocumentListView } from '@/components/my-area/documents/UserDocumentListView';
import { LoadingState } from '@/components/ui/loading';

export default function DocumentsPage() {
  return (
    <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
      <UserDocumentListView />
    </Suspense>
  );
}
