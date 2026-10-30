/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { createInitialListQueryState, defineList, toListQuery } from './defineList';
import type { ListQuery } from './types';

/** Workflow instance status (API `status` filter on self inbox). */
export const WorkflowInboxStatusParam = {
  OPEN: 'OPEN',
  AWAITING_INPUT_CHANGE: 'AWAITING_INPUT_CHANGE',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  CANCELLED: 'CANCELLED',
} as const;

export type WorkflowInboxStatusParam =
  (typeof WorkflowInboxStatusParam)[keyof typeof WorkflowInboxStatusParam];

const orgDeptStatusFilter = {
  kind: 'multiSelect',
  key: 'status',
  labelKey: 'workflows.list.status.label',
  defaultValue: [WorkflowInboxStatusParam.OPEN, WorkflowInboxStatusParam.AWAITING_INPUT_CHANGE],
  countsInMenubarWhenSelected: true,
  options: [
    { value: WorkflowInboxStatusParam.OPEN, labelKey: 'myArea.workflows.status.OPEN' },
    {
      value: WorkflowInboxStatusParam.AWAITING_INPUT_CHANGE,
      labelKey: 'myArea.workflows.status.AWAITING_INPUT_CHANGE',
    },
    { value: WorkflowInboxStatusParam.APPROVED, labelKey: 'myArea.workflows.status.APPROVED' },
    { value: WorkflowInboxStatusParam.REJECTED, labelKey: 'myArea.workflows.status.REJECTED' },
    { value: WorkflowInboxStatusParam.CANCELLED, labelKey: 'myArea.workflows.status.CANCELLED' },
  ],
} as const;

const inboxStatusFilter = {
  kind: 'multiSelect',
  key: 'status',
  labelKey: 'workflows.list.status.label',
  defaultValue: [
    WorkflowInboxStatusParam.OPEN,
    WorkflowInboxStatusParam.AWAITING_INPUT_CHANGE,
    WorkflowInboxStatusParam.APPROVED,
  ],
  countsInMenubarWhenSelected: true,
  options: [
    { value: WorkflowInboxStatusParam.OPEN, labelKey: 'myArea.workflows.status.OPEN' },
    {
      value: WorkflowInboxStatusParam.AWAITING_INPUT_CHANGE,
      labelKey: 'myArea.workflows.status.AWAITING_INPUT_CHANGE',
    },
    { value: WorkflowInboxStatusParam.APPROVED, labelKey: 'myArea.workflows.status.APPROVED' },
    { value: WorkflowInboxStatusParam.REJECTED, labelKey: 'myArea.workflows.status.REJECTED' },
    { value: WorkflowInboxStatusParam.CANCELLED, labelKey: 'myArea.workflows.status.CANCELLED' },
  ],
} as const;

const inboxTypeFilter = {
  kind: 'multiSelect',
  key: 'type',
  labelKey: 'workflows.list.type.label',
  options: [],
  runtimeOptions: true,
} as const;

/** Org `/organizations/.../workflows` - Status (default offen + Anpassung) + Antragstyp, Suche. */
export const workflowListDefinition = defineList({
  id: 'workflow',
  filterOptionsSource: 'org/workflows',
  search: {},
  sort: {
    options: [
      { value: 'openedAt,desc', labelKey: 'workflows.list.sort.openedAtDesc' },
      { value: 'openedAt,asc', labelKey: 'workflows.list.sort.openedAtAsc' },
    ],
  },
  sheetLayout: 'menubar',
  filters: [orgDeptStatusFilter, inboxTypeFilter],
});

const userOrgStatusFilter = {
  kind: 'multiSelect',
  key: 'status',
  labelKey: 'workflows.list.status.label',
  defaultValue: [
    WorkflowInboxStatusParam.OPEN,
    WorkflowInboxStatusParam.AWAITING_INPUT_CHANGE,
    WorkflowInboxStatusParam.APPROVED,
    WorkflowInboxStatusParam.REJECTED,
    WorkflowInboxStatusParam.CANCELLED,
  ],
  countsInMenubarWhenSelected: true,
  options: [
    { value: WorkflowInboxStatusParam.OPEN, labelKey: 'myArea.workflows.status.OPEN' },
    {
      value: WorkflowInboxStatusParam.AWAITING_INPUT_CHANGE,
      labelKey: 'myArea.workflows.status.AWAITING_INPUT_CHANGE',
    },
    { value: WorkflowInboxStatusParam.APPROVED, labelKey: 'myArea.workflows.status.APPROVED' },
    { value: WorkflowInboxStatusParam.REJECTED, labelKey: 'myArea.workflows.status.REJECTED' },
    { value: WorkflowInboxStatusParam.CANCELLED, labelKey: 'myArea.workflows.status.CANCELLED' },
  ],
} as const;

/**
 * Self `/my-area/workflows` - Status (default: aktiv + genehmigt; leer = alle) + Antragstyp, Suche.
 */
export const workflowInboxListDefinition = defineList({
  id: 'workflow-inbox',
  filterOptionsSource: 'self/workflows',
  search: {},
  sort: {
    options: [
      { value: 'openedAt,desc', labelKey: 'workflows.list.sort.openedAtDesc' },
      { value: 'openedAt,asc', labelKey: 'workflows.list.sort.openedAtAsc' },
    ],
  },
  sheetLayout: 'menubar',
  filters: [inboxStatusFilter, inboxTypeFilter],
});

/** Org user-management `…/users/{userId}/workflows` - same menubar pattern as self inbox. */
export const workflowUserOrgListDefinition = defineList({
  id: 'workflow-user-org',
  filterOptionsSource: 'org/user-workflows',
  search: {},
  sort: {
    options: [
      { value: 'openedAt,desc', labelKey: 'workflows.list.sort.openedAtDesc' },
      { value: 'openedAt,asc', labelKey: 'workflows.list.sort.openedAtAsc' },
    ],
  },
  sheetLayout: 'menubar',
  filters: [userOrgStatusFilter, inboxTypeFilter],
});

const initialWorkflowListState = createInitialListQueryState(workflowListDefinition);
const initialWorkflowInboxListState = createInitialListQueryState(workflowInboxListDefinition);
const initialWorkflowUserOrgListState = createInitialListQueryState(workflowUserOrgListDefinition);

/** Default open workflows (org lists and pending counts). */
export const workflowListOpenQuery: ListQuery = toListQuery(
  workflowListDefinition,
  initialWorkflowListState,
);

/** Default self inbox (active statuses). */
export const workflowInboxOpenQuery: ListQuery = toListQuery(
  workflowInboxListDefinition,
  initialWorkflowInboxListState,
);

/** Default org user-management workflow list (all statuses). */
export const workflowUserOrgDefaultQuery: ListQuery = toListQuery(
  workflowUserOrgListDefinition,
  initialWorkflowUserOrgListState,
);
