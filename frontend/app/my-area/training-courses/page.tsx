/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Suspense } from 'react';
import { LoadingState } from '@/components/ui/loading';
import { UserTrainingCourseListView } from '@/components/my-area/trainingCourses/UserTrainingCourseListView';

export default function TrainingCoursesPage() {
  return (
    <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
      <UserTrainingCourseListView />
    </Suspense>
  );
}
