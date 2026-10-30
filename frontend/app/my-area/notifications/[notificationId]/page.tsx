/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { NotificationDetailView } from '@/components/notification/NotificationDetailView';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

export default function MyNotificationDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const notificationId = params?.notificationId
    ? parseInt(params.notificationId as string, 10)
    : null;

  if (!notificationId || Number.isNaN(notificationId)) {
    return <NotFoundState message={t('myArea.notifications.detail.notFound')} />;
  }

  return <NotificationDetailView notificationId={notificationId} />;
}
