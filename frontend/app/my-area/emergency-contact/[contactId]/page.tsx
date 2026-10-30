/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { MyAreaEmergencyContactDetailView } from '@/components/my-area/emergency-contact/MyAreaEmergencyContactDetailView';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

export default function MyAreaEmergencyContactDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const contactId = params?.contactId ? parseInt(params.contactId as string, 10) : null;

  if (!contactId || Number.isNaN(contactId)) {
    return <NotFoundState message={t('myArea.emergencyContact.error.notFound')} />;
  }

  return <MyAreaEmergencyContactDetailView contactId={contactId} />;
}
