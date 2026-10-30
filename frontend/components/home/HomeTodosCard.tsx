/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { AppLinkListAction } from '@/components/ui/app-link-list-action';
import { List, ListRow } from '@/components/ui/list';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { useTranslation } from '@/hooks/useTranslation';
import { todoListOpenQuery } from '@/lib/list/todoList';
import { getMyAreaTodoDetailPath, getMyAreaTodosPath } from '@/lib/my-area/myAreaSections';
import { useOpenTodoCount, useTodos } from '@/lib/store';
import { hasAppNavigationLink } from '@/lib/utils/appLink';
import {
  HOME_DASHBOARD_PREVIEW_LIMIT,
  HOME_DASHBOARD_ROW_CLASSNAME,
  HomeDashboardListPanel,
} from './HomeDashboardListPanel';

export function HomeTodosCard() {
  const t = useTranslation();
  const hasEmberOneUserId = useHasEmberOneUserId();

  const {
    data: items,
    isPending,
    isFetching,
  } = useTodos({
    listQuery: todoListOpenQuery,
    enabled: hasEmberOneUserId,
  });
  const { data: openCount } = useOpenTodoCount({ enabled: hasEmberOneUserId });

  const isLoading = isPending && isFetching;
  const todos = items.slice(0, HOME_DASHBOARD_PREVIEW_LIMIT);
  const listPath = hasEmberOneUserId ? getMyAreaTodosPath() : null;
  const count = isLoading ? null : (openCount ?? items.length);

  return (
    <HomeDashboardListPanel
      title={t('home.dashboard.todos.title')}
      count={count}
      href={listPath}
      isLoading={isLoading}
      loadingLabel={t('common.loading')}
      emptyLabel={t('home.dashboard.todos.empty')}
      isEmpty={todos.length === 0}
      listLabel={t('home.dashboard.todos.title')}
    >
      <List
        variant="flush"
        rowSeparator="inset"
        label={t('home.dashboard.todos.title')}
        className="flex-1"
      >
        {todos.map((item) => {
          const hasLink = hasAppNavigationLink(item);
          return (
            <ListRow
              key={item.id}
              href={getMyAreaTodoDetailPath(item.id)}
              title={item.title}
              tone="active"
              accent={item.open}
              compact
              className={HOME_DASHBOARD_ROW_CLASSNAME}
              actions={
                hasLink ? <AppLinkListAction href={item.navigationPath} link={item} /> : undefined
              }
            />
          );
        })}
      </List>
    </HomeDashboardListPanel>
  );
}
