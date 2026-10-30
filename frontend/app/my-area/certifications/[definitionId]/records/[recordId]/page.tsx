/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { CertificationRecordDetailView } from '@/components/certifications/CertificationRecordDetailView';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

export default function CertificationRecordDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const definitionId = params?.definitionId ? parseInt(params.definitionId as string, 10) : null;
  const recordId = params?.recordId ? parseInt(params.recordId as string, 10) : null;

  if (!definitionId || !recordId || Number.isNaN(definitionId) || Number.isNaN(recordId)) {
    return <NotFoundState message={t('certifications.record.notFound')} />;
  }

  return (
    <CertificationRecordDetailView scope="self" definitionId={definitionId} recordId={recordId} />
  );
}
