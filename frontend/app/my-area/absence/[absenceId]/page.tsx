/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useParams } from 'next/navigation';
import { MyAreaAbsenceDetailView } from '@/components/my-area/absence/MyAreaAbsenceDetailView';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

export default function MyAreaAbsenceDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const absenceId = params?.absenceId ? parseInt(params.absenceId as string, 10) : null;

  if (!absenceId || Number.isNaN(absenceId)) {
    return <NotFoundState message={t('myArea.absence.error.notFound')} />;
  }

  return <MyAreaAbsenceDetailView absenceId={absenceId} />;
}
