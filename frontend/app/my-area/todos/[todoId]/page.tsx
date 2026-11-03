/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useParams } from 'next/navigation';
import { TodoDetailView } from '@/components/todo/TodoDetailView';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

export default function MyTodoDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const todoId = params?.todoId ? parseInt(params.todoId as string, 10) : null;

  if (!todoId || Number.isNaN(todoId)) {
    return <NotFoundState message={t('myArea.todos.detail.notFound')} />;
  }

  return <TodoDetailView todoId={todoId} />;
}
