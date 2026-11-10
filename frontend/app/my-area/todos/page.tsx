/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Suspense } from 'react';
import { TodoListView } from '@/components/todo/TodoListView';
import { LoadingState } from '@/components/ui/loading';

export default function MyTodosPage() {
  return (
    <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
      <TodoListView />
    </Suspense>
  );
}
