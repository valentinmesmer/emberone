/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Suspense } from 'react';
import { MasterDataView } from '@/components/my-area/master-data';
import { LoadingState } from '@/components/ui/loading';

export default function MasterDataPage() {
  return (
    <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
      <MasterDataView />
    </Suspense>
  );
}
