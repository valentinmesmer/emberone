/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { WorkflowDto } from '@/types/workflow';

/** First organization id from workflow participants (approver org context). */
export function resolveWorkflowContextOrganizationId(workflow: WorkflowDto): number | null {
  for (const participant of workflow.participants ?? []) {
    if (participant.organizationId != null) {
      return participant.organizationId;
    }
  }
  return null;
}
