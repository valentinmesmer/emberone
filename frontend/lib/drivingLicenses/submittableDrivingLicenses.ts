/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { isWorkflowStatusActive } from '@/lib/utils/workflowOpen';
import { parseDrivingLicenseRecordPayload } from '@/lib/workflows/inputDisplay/payloadParsers';
import type { DrivingLicenseRecordWorkflowPayload, WorkflowDto } from '@/types/workflow';
import { WORKFLOW_TYPE_IDS } from '@/types/workflow';

export function getOpenDrivingLicenseWorkflow(
  workflows: WorkflowDto[],
): { workflow: WorkflowDto; payload: DrivingLicenseRecordWorkflowPayload } | null {
  for (const workflow of workflows) {
    if (
      workflow.workflowTypeId !== WORKFLOW_TYPE_IDS.DRIVING_LICENSE_RECORD ||
      !isWorkflowStatusActive(workflow.status)
    ) {
      continue;
    }
    const payload = parseDrivingLicenseRecordPayload(workflow.payload);
    if (payload != null) {
      return { workflow, payload };
    }
  }
  return null;
}
