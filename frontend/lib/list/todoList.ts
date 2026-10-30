/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { createInitialListQueryState, defineList, toListQuery } from './defineList';
import type { ListQuery } from './types';

export const TodoListStatusParam = {
  OPEN: 'OPEN',
  DONE: 'DONE',
} as const;

export type TodoListStatusParam = (typeof TodoListStatusParam)[keyof typeof TodoListStatusParam];

const statusFilter = {
  kind: 'multiSelect',
  key: 'status',
  labelKey: 'todo.list.status.label',
  defaultValue: [TodoListStatusParam.OPEN],
  countsInMenubarWhenSelected: true,
  options: [
    { value: TodoListStatusParam.OPEN, labelKey: 'todo.list.status.open' },
    { value: TodoListStatusParam.DONE, labelKey: 'todo.list.status.done' },
  ],
} as const;

const typeFilter = {
  kind: 'multiSelect',
  key: 'type',
  labelKey: 'todo.list.type.label',
  options: [],
  runtimeOptions: true,
} as const;

/**
 * Self `/my-area/todos` - Status (default nur Offen; keine Auswahl = alle) + Aufgabentyp in menubar.
 */
export const todoListDefinition = defineList({
  id: 'todo',
  filterOptionsSource: 'self/todos',
  sheetLayout: 'menubar',
  filters: [statusFilter, typeFilter],
});

const initialTodoListState = createInitialListQueryState(todoListDefinition);

/** Default open-todos list query (status OPEN). */
export const todoListOpenQuery: ListQuery = toListQuery(todoListDefinition, initialTodoListState);
