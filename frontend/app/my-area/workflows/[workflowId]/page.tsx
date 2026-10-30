/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { WorkflowDetailView } from '@/components/my-area/workflows/WorkflowDetailView';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

export default function MyAreaWorkflowDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const workflowId = params?.workflowId ? parseInt(params.workflowId as string, 10) : null;

  if (!workflowId || Number.isNaN(workflowId)) {
    return <NotFoundState message={t('myArea.workflowDetail.notFound')} hint={false} />;
  }

  return <WorkflowDetailView workflowId={workflowId} />;
}
