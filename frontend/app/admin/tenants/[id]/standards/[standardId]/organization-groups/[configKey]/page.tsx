/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { Suspense } from 'react';
import { StandardOrgGroupDetailView } from '@/components/admin/standards/StandardOrgGroupDetailView';
import { StandardPageGate } from '@/components/admin/standards/StandardPageGate';
import { useStandardContent } from '@/components/admin/standards/useStandardContent';
import { LoadingState } from '@/components/ui/loading';

function StandardOrgGroupDetailInner() {
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
        <StandardOrgGroupDetailView state={state} configKey={configKey} />
      )}
    </StandardPageGate>
  );
}

export default function StandardOrgGroupDetailPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[var(--page-bg)] pt-20">
          <LoadingState variant="embedded" />
        </div>
      }
    >
      <StandardOrgGroupDetailInner />
    </Suspense>
  );
}
