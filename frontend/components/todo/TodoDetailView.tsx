/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Check } from 'lucide-react';
import { AppLinkNavigateButton } from '@/components/ui/app-link-navigate-button';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { FieldDisplay } from '@/components/ui/field-display';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useCompleteTodo, useTodo } from '@/lib/store';

export interface TodoDetailViewProps {
  todoId: number;
}

export function TodoDetailView({ todoId }: TodoDetailViewProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const { data: todo, isPending, isError } = useTodo(todoId);
  const completeMutation = useCompleteTodo();

  const handleComplete = async () => {
    if (!todo?.open) return;
    try {
      await completeMutation.mutateAsync(todo.id);
      showSuccess(t('myArea.todos.detail.completeSuccess'));
    } catch (e) {
      showError(e instanceof Error ? e.message : t('myArea.error.loadFailed'));
    }
  };

  if (isPending) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (isError || !todo) {
    return <NotFoundState message={t('myArea.todos.detail.notFound')} />;
  }

  const title = todo.title;
  const body = todo.body;
  const href = todo.navigationPath ?? null;

  return (
    <div className="w-full space-y-4">
      <Card className="p-3 sm:p-4">
        <div className="mb-4">
          <p className="text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
            {t('myArea.todos.title')}
          </p>
          <h1 className="mt-1 break-words text-lg font-semibold text-[var(--text-primary)]">
            {title}
          </h1>
        </div>

        <div className="space-y-4">
          {body ? (
            <FieldDisplay label={t('myArea.todos.detail.message')} value={body} multiline />
          ) : null}
        </div>

        {todo.open ? (
          <div className="mt-4 flex flex-wrap gap-2 border-t border-[var(--border-color)] pt-4">
            {href ? <AppLinkNavigateButton href={href} link={todo} /> : null}
            <Button
              type="button"
              variant="secondary"
              size="sm"
              icon={<Check className="h-4 w-4" aria-hidden />}
              onClick={() => void handleComplete()}
              isLoading={completeMutation.isPending}
            >
              {completeMutation.isPending
                ? t('myArea.todos.detail.completeLoading')
                : t('myArea.todos.detail.complete')}
            </Button>
          </div>
        ) : href ? (
          <div className="mt-4 border-t border-[var(--border-color)] pt-4">
            <AppLinkNavigateButton href={href} link={todo} />
          </div>
        ) : null}
      </Card>
    </div>
  );
}
