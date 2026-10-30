/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { TrainingCourseTypeDto } from '@/lib/api/trainingCourses';
import { isWorkflowStatusActive } from '@/lib/utils/workflowOpen';
import {
  findWorkflowByConcurrencyKey,
  idsFromConcurrencyKeys,
  trainingCourseTypeConcurrencyKey,
} from '@/lib/workflows/openConcurrency';
import type { WorkflowDto } from '@/types/workflow';
import { type TrainingCourseRecordWorkflowPayload, WORKFLOW_TYPE_IDS } from '@/types/workflow';

function parseTrainingCoursePayload(payload: string): TrainingCourseRecordWorkflowPayload | null {
  try {
    const parsed = JSON.parse(payload) as TrainingCourseRecordWorkflowPayload;
    if (!Array.isArray(parsed.tenantTrainingCourseTypeIds)) {
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
}

export function trainingCourseTypeIdsFromPayload(
  payload: TrainingCourseRecordWorkflowPayload,
): number[] {
  return [...new Set(payload.tenantTrainingCourseTypeIds.filter((id) => typeof id === 'number'))];
}

/** Open TRAINING_COURSE_RECORD workflow that grants a specific type, if any. */
export function findOpenTrainingCourseWorkflow(
  workflows: WorkflowDto[],
  typeId: number,
): WorkflowDto | undefined {
  return findWorkflowByConcurrencyKey(
    workflows,
    WORKFLOW_TYPE_IDS.TRAINING_COURSE_RECORD,
    trainingCourseTypeConcurrencyKey(typeId),
  );
}

/** Types the user may grant now (excludes types with an open submission workflow). */
export function filterSubmittableTrainingCourseTypes(
  types: TrainingCourseTypeDto[],
  openConcurrencyKeys: readonly string[] = [],
): TrainingCourseTypeDto[] {
  const blockedTypeIds = idsFromConcurrencyKeys(openConcurrencyKeys, 'training');
  return types.filter((type) => type.active && !blockedTypeIds.has(type.id));
}

export interface ParsedOpenTrainingCourseWorkflow {
  workflow: WorkflowDto;
  typeIds: number[];
  payload: TrainingCourseRecordWorkflowPayload;
}

/** Open TRAINING_COURSE_RECORD workflows with resolved type ids. */
export function getOpenTrainingCourseWorkflows(
  workflows: WorkflowDto[],
): ParsedOpenTrainingCourseWorkflow[] {
  const result: ParsedOpenTrainingCourseWorkflow[] = [];
  for (const workflow of workflows) {
    if (
      workflow.workflowTypeId !== WORKFLOW_TYPE_IDS.TRAINING_COURSE_RECORD ||
      !isWorkflowStatusActive(workflow.status)
    ) {
      continue;
    }
    const typeIds = [...idsFromConcurrencyKeys(workflow.openConcurrencyKeys ?? [], 'training')];
    if (typeIds.length === 0 && workflow.payload) {
      const payload = parseTrainingCoursePayload(workflow.payload);
      if (payload != null) {
        typeIds.push(...trainingCourseTypeIdsFromPayload(payload));
      }
    }
    if (typeIds.length === 0) {
      continue;
    }
    const parsedPayload = workflow.payload ? parseTrainingCoursePayload(workflow.payload) : null;
    const payload: TrainingCourseRecordWorkflowPayload = parsedPayload ?? {
      tenantTrainingCourseTypeIds: typeIds,
      startDate: '',
      endDate: '',
      location: '',
    };
    result.push({ workflow, typeIds, payload });
  }
  return result;
}
