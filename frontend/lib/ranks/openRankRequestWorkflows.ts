/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { isWorkflowStatusActive } from '@/lib/utils/workflowOpen';
import { parseRankRequestPayload } from '@/lib/workflows/inputDisplay/payloadParsers';
import { WORKFLOW_TYPE_IDS, type WorkflowDto } from '@/types/workflow';

/** Open RANK_REQUEST workflows started by the user. */
export function filterOpenRankRequestWorkflows(workflows: WorkflowDto[]): WorkflowDto[] {
  return workflows.filter(
    (workflow) =>
      isWorkflowStatusActive(workflow.status) &&
      workflow.workflowTypeId === WORKFLOW_TYPE_IDS.RANK_REQUEST,
  );
}

export function resolveRankRequestWorkflowTitle(
  workflow: WorkflowDto,
  ranks: { id: number; name: string; abbreviation: string }[],
): string {
  const payload = parseRankRequestPayload(workflow.payload);
  if (!payload) {
    return '';
  }
  const rank = ranks.find((item) => item.id === payload.tenantRankId);
  if (!rank) {
    return String(payload.tenantRankId);
  }
  return `${rank.name} (${rank.abbreviation})`;
}
