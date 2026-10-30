/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Suspense } from 'react';
import { LoadingState } from '@/components/ui/loading';
import { WorkflowInboxView } from '@/components/my-area/workflows/WorkflowInboxView';

export default function MyWorkflowInboxPage() {
  return (
    <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
      <WorkflowInboxView />
    </Suspense>
  );
}
