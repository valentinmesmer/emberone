/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { AppLinkFields } from '@/types/appLink';
import type { PageResponse } from '@/types/page';

export const TodoType = {
  WORKFLOW_START_REQUESTED: 'WORKFLOW_START_REQUESTED',
  WORKFLOW_APPROVAL_REQUIRED: 'WORKFLOW_APPROVAL_REQUIRED',
  WORKFLOW_INPUT_CHANGE_REQUIRED: 'WORKFLOW_INPUT_CHANGE_REQUIRED',
  CERTIFICATION_EXPIRING: 'CERTIFICATION_EXPIRING',
} as const;

export type TodoType = (typeof TodoType)[keyof typeof TodoType];

export const TodoStatus = {
  OPEN: 'OPEN',
  COMPLETED: 'COMPLETED',
  CANCELLED: 'CANCELLED',
} as const;

export type TodoStatus = (typeof TodoStatus)[keyof typeof TodoStatus];

export interface TodoAssigneeGroupDto {
  organizationGroupId: number;
  organizationGroupName?: string | null;
}

export interface TodoDto extends AppLinkFields {
  id: number;
  type: TodoType;
  title: string;
  body: string;
  params?: Record<string, unknown>;
  status: TodoStatus;
  assigneeUserId?: number | null;
  assigneeGroups?: TodoAssigneeGroupDto[];
  completedAt?: string | null;
  archivedAt?: string | null;
  open: boolean;
}

/** Flat paginated todo list (page fields + {@code openCount}). */
export type TodoPageDto = PageResponse<TodoDto> & {
  openCount: number;
};
