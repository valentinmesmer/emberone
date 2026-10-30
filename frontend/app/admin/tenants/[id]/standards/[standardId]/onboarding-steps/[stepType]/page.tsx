/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { Suspense } from 'react';
import { parseOnboardingStepType } from '@/components/admin/standards/onboardingStepUtils';
import { StandardOnboardingStepDetailView } from '@/components/admin/standards/StandardOnboardingStepDetailView';
import { StandardPageGate } from '@/components/admin/standards/StandardPageGate';
import { useStandardContent } from '@/components/admin/standards/useStandardContent';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

function StandardOnboardingStepDetailInner() {
  const params = useParams();
  const t = useTranslation();
  const tenantId = Number.parseInt((params?.id as string) ?? '', 10);
  const standardId = Number.parseInt((params?.standardId as string) ?? '', 10);
  const stepType = parseOnboardingStepType(decodeURIComponent((params?.stepType as string) ?? ''));
  const state = useStandardContent(tenantId, standardId);

  return (
    <StandardPageGate state={state} requireStandard>
      {state.loading ? (
        <LoadingState variant="embedded" size="sm" />
      ) : stepType == null ? (
        <NotFoundState message={t('organizationManagement.errors.notFound')} />
      ) : (
        <StandardOnboardingStepDetailView state={state} stepType={stepType} />
      )}
    </StandardPageGate>
  );
}

export default function StandardOnboardingStepDetailPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[var(--page-bg)] pt-20">
          <LoadingState variant="embedded" />
        </div>
      }
    >
      <StandardOnboardingStepDetailInner />
    </Suspense>
  );
}
