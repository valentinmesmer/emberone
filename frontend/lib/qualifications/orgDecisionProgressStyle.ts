/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { QualificationExerciseStatus } from '@/lib/api/qualifications';

export function isPartialOrgDecisionProgress(input: {
  aggregateStatus: QualificationExerciseStatus;
  requiresOrgDecision: boolean;
  approvedOrgCount: number;
  totalOrgCount: number;
}): boolean {
  const { aggregateStatus, requiresOrgDecision, approvedOrgCount, totalOrgCount } = input;
  return (
    aggregateStatus === 'ORG_DECISION_DENIED' &&
    requiresOrgDecision &&
    approvedOrgCount > 0 &&
    approvedOrgCount < totalOrgCount &&
    totalOrgCount > 0
  );
}
