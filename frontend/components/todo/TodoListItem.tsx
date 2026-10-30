/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { AppLinkListAction } from '@/components/ui/app-link-list-action';
import { ListRow } from '@/components/ui/list';
import { getMyAreaTodoDetailPath } from '@/lib/my-area/myAreaSections';
import { hasAppNavigationLink } from '@/lib/utils/appLink';
import type { TodoDto } from '@/types/todo';

interface TodoListItemProps {
  todo: TodoDto;
}

export function TodoListItem({ todo }: TodoListItemProps) {
  const hasLink = hasAppNavigationLink(todo);

  return (
    <ListRow
      title={todo.title}
      href={getMyAreaTodoDetailPath(todo.id)}
      tone={todo.open ? 'active' : 'default'}
      accent={todo.open}
      compact
      actions={hasLink ? <AppLinkListAction href={todo.navigationPath} link={todo} /> : undefined}
    />
  );
}
