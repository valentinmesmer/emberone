/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Suspense } from 'react';
import { NotificationListView } from '@/components/notification/NotificationListView';
import { LoadingState } from '@/components/ui/loading';

export default function MyNotificationsPage() {
  return (
    <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
      <NotificationListView />
    </Suspense>
  );
}
