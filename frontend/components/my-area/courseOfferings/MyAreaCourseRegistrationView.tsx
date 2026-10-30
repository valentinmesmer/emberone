/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Suspense } from 'react';
import { LoadingState } from '@/components/ui/loading';
import { PageHeader } from '@/components/ui/page-header';
import { useTranslation } from '@/hooks/useTranslation';
import { SelfCourseOfferingList } from './SelfCourseOfferingList';

function MyAreaCourseRegistrationViewBody() {
  const t = useTranslation();

  return (
    <>
      <PageHeader
        title={t('myArea.menu.courseRegistrations')}
        description={t('myArea.courseRegistrations.subtitle')}
        size="compact"
      />
      <div className="mt-3">
        <SelfCourseOfferingList />
      </div>
    </>
  );
}

export function MyAreaCourseRegistrationView() {
  return (
    <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
      <MyAreaCourseRegistrationViewBody />
    </Suspense>
  );
}
