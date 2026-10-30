/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { TrainingCourseRecordDetailView } from '@/components/my-area/trainingCourses/TrainingCourseRecordDetailView';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

export default function TrainingCourseRecordDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const recordId = params?.recordId ? parseInt(params.recordId as string, 10) : null;

  if (!recordId || Number.isNaN(recordId)) {
    return <NotFoundState message={t('trainingCourse.record.notFound')} />;
  }

  return <TrainingCourseRecordDetailView scope="self" recordId={recordId} />;
}
