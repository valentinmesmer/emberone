/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { MyAreaOnboardingWizard } from '@/components/my-area/onboarding';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

export default function MyAreaOnboardingDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const invitationId = params?.invitationId ? parseInt(params.invitationId as string, 10) : null;

  if (!invitationId || Number.isNaN(invitationId)) {
    return <NotFoundState message={t('myArea.onboarding.wizard.notFound')} />;
  }

  return <MyAreaOnboardingWizard key={invitationId} invitationId={invitationId} />;
}
