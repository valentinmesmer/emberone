/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useParams } from 'next/navigation';
import { UserCertificationDetailView } from '@/components/my-area/certifications/UserCertificationDetailView';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

export default function CertificationDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const definitionId = params?.definitionId ? parseInt(params.definitionId as string, 10) : null;

  if (!definitionId || Number.isNaN(definitionId)) {
    return <NotFoundState message={t('certifications.detail.notFound')} />;
  }

  return <UserCertificationDetailView definitionId={definitionId} />;
}
