/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
