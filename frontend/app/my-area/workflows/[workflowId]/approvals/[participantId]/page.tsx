/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Suspense } from 'react';
import { useParams } from 'next/navigation';
import { WorkflowApprovalSlotDetailView } from '@/components/my-area/workflows/WorkflowApprovalSlotDetailView';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

function ApprovalSlotPageBody({
  workflowId,
  participantId,
}: {
  workflowId: number;
  participantId: number;
}) {
  return <WorkflowApprovalSlotDetailView workflowId={workflowId} participantId={participantId} />;
}

export default function MyAreaWorkflowApprovalSlotPage() {
  const t = useTranslation();
  const params = useParams();
  const workflowId = params?.workflowId ? parseInt(params.workflowId as string, 10) : null;
  const participantId = params?.participantId ? parseInt(params.participantId as string, 10) : null;

  if (!workflowId || Number.isNaN(workflowId) || !participantId || Number.isNaN(participantId)) {
    return <NotFoundState message={t('myArea.workflowDetail.approvalSlotNotFound')} hint={false} />;
  }

  return (
    <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
      <ApprovalSlotPageBody workflowId={workflowId} participantId={participantId} />
    </Suspense>
  );
}
