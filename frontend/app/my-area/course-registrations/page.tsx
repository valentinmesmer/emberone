/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Suspense } from 'react';
import { MyAreaCourseRegistrationView } from '@/components/my-area/courseOfferings/MyAreaCourseRegistrationView';
import { LoadingState } from '@/components/ui/loading';

export default function CourseRegistrationsPage() {
  return (
    <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
      <MyAreaCourseRegistrationView />
    </Suspense>
  );
}
