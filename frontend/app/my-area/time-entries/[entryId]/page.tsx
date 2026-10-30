/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { MyAreaTimeEntryDetailView } from '@/components/my-area/time-entries/MyAreaTimeEntryDetailView';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

export default function MyAreaTimeEntryDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const entryId = params?.entryId ? parseInt(params.entryId as string, 10) : null;

  if (!entryId || Number.isNaN(entryId)) {
    return <NotFoundState message={t('myArea.timeEntries.error.notFound')} />;
  }

  return <MyAreaTimeEntryDetailView entryId={entryId} />;
}
