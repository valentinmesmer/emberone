/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { SelfCourseOfferingDetailView } from '@/components/my-area/courseOfferings/SelfCourseOfferingDetailView';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

export default function MyAreaCourseOfferingDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const offeringId = params?.offeringId ? parseInt(params.offeringId as string, 10) : null;

  if (!offeringId || Number.isNaN(offeringId)) {
    return <NotFoundState message={t('myArea.courseRegistrations.detail.notFound')} />;
  }

  return <SelfCourseOfferingDetailView offeringId={offeringId} />;
}
