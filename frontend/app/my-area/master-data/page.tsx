/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
