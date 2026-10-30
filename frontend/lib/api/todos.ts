/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { ListQuery } from '@/lib/list/types';
import type { TodoDto, TodoPageDto } from '@/types/todo';
import { apiClient } from './client';
import { buildQuery } from './paged';

const BASE_PATH = '/api/v1/self/todos';

export type TodoListRequest = ListQuery & {
  page?: number;
};

export const todosApi = {
  async list(query: TodoListRequest): Promise<TodoPageDto> {
    const qs = buildQuery({
      page: query.page ?? 0,
      size: query.size,
      search: query.search,
      sort: query.sort,
      status: query.status,
      type: query.type,
    });
    return apiClient.get<TodoPageDto>(qs ? `${BASE_PATH}?${qs}` : BASE_PATH);
  },

  async get(id: number): Promise<TodoDto> {
    return apiClient.get<TodoDto>(`${BASE_PATH}/${id}`);
  },

  async getOpenCount(): Promise<{ count: number }> {
    return apiClient.get<{ count: number }>(`${BASE_PATH}/open-count`);
  },

  async complete(id: number): Promise<TodoDto> {
    return apiClient.post<TodoDto>(`${BASE_PATH}/${id}/complete`, {});
  },
};
