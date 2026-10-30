/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Suspense } from 'react';
import { LoadingState } from '@/components/ui/loading';
import { TimeEntryList } from '@/components/my-area/time-entries';

export default function TimeEntriesPage() {
  return (
    <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
      <TimeEntryList />
    </Suspense>
  );
}
