/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Suspense } from 'react';
import { LoadingState } from '@/components/ui/loading';
import { NotificationListView } from '@/components/notification/NotificationListView';

export default function MyNotificationsPage() {
  return (
    <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
      <NotificationListView />
    </Suspense>
  );
}
