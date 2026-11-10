/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Suspense } from 'react';
import { WorkflowInboxView } from '@/components/my-area/workflows/WorkflowInboxView';
import { LoadingState } from '@/components/ui/loading';

export default function MyWorkflowInboxPage() {
  return (
    <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
      <WorkflowInboxView />
    </Suspense>
  );
}
