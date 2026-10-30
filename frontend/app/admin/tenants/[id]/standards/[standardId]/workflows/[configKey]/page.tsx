/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { Suspense } from 'react';
import { StandardPageGate } from '@/components/admin/standards/StandardPageGate';
import { StandardWorkflowDetailView } from '@/components/admin/standards/StandardWorkflowDetailView';
import { useStandardContent } from '@/components/admin/standards/useStandardContent';
import { LoadingState } from '@/components/ui/loading';

function StandardWorkflowDetailInner() {
  const params = useParams();
  const tenantId = Number.parseInt((params?.id as string) ?? '', 10);
  const standardId = Number.parseInt((params?.standardId as string) ?? '', 10);
  const configKey = decodeURIComponent((params?.configKey as string) ?? '');
  const state = useStandardContent(tenantId, standardId);

  return (
    <StandardPageGate state={state} requireStandard>
      {state.loading ? (
        <LoadingState variant="embedded" size="sm" />
      ) : (
        <StandardWorkflowDetailView state={state} configKey={configKey} />
      )}
    </StandardPageGate>
  );
}

export default function StandardWorkflowDetailPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[var(--page-bg)] pt-20">
          <LoadingState variant="embedded" />
        </div>
      }
    >
      <StandardWorkflowDetailInner />
    </Suspense>
  );
}
