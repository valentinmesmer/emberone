/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { isWorkflowStatusActive } from '@/lib/utils/workflowOpen';
import type { WorkflowDto } from '@/types/workflow';

/** Stable key for a certification-definition-scoped workflow target. */
export function certificationDefinitionConcurrencyKey(definitionId: number): string {
  return `cert:${definitionId}`;
}

/** Stable key for a training-course-type-scoped workflow target. */
export function trainingCourseTypeConcurrencyKey(typeId: number): string {
  return `training:${typeId}`;
}

/** Numeric ids encoded as {@code prefix:id} concurrency keys. */
export function idsFromConcurrencyKeys(keys: readonly string[], prefix: string): Set<number> {
  const ids = new Set<number>();
  const needle = `${prefix}:`;
  for (const key of keys) {
    if (!key.startsWith(needle)) {
      continue;
    }
    const id = Number(key.slice(needle.length));
    if (Number.isFinite(id) && id > 0) {
      ids.add(id);
    }
  }
  return ids;
}

/** Keys from other active instances, optionally excluding one workflow's own keys. */
export function blockingConcurrencyKeys(
  openKeys: readonly string[],
  excludeKeys: readonly string[] = [],
): string[] {
  if (excludeKeys.length === 0) {
    return [...openKeys];
  }
  const excluded = new Set(excludeKeys);
  return openKeys.filter((key) => !excluded.has(key));
}

export function findWorkflowByConcurrencyKey(
  workflows: WorkflowDto[],
  workflowTypeId: number,
  key: string,
): WorkflowDto | undefined {
  return workflows.find(
    (workflow) =>
      workflow.workflowTypeId === workflowTypeId &&
      isWorkflowStatusActive(workflow.status) &&
      workflow.openConcurrencyKeys?.includes(key),
  );
}
