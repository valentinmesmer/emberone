/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { BadgeTone, ListRowTone } from '@/components/ui/tones';
import type { QualificationExerciseStatus } from '@/lib/api/qualifications';
import { isPartialOrgDecisionProgress } from '@/lib/qualifications/orgDecisionProgressStyle';

export type QualificationPresentation = {
  rowTone: ListRowTone;
  badgeTone: BadgeTone;
  /** Set when org decisions are partly granted - home pills use a green/orange split. */
  orgDecisionProgressRatio?: number;
};

export const QUALIFICATION_STATUS_BADGE_TONE: Record<QualificationExerciseStatus, BadgeTone> = {
  NOT_AVAILABLE: 'neutral',
  PREREQUISITES_MISSING: 'danger',
  ORG_DECISION_DENIED: 'neutral',
  ORG_DECISION_REVOKED: 'warning',
  CAN_EXERCISE: 'success',
};

export const QUALIFICATION_STATUS_ROW_TONE: Record<QualificationExerciseStatus, ListRowTone> = {
  NOT_AVAILABLE: 'neutral',
  PREREQUISITES_MISSING: 'danger',
  ORG_DECISION_DENIED: 'warning',
  ORG_DECISION_REVOKED: 'warning',
  CAN_EXERCISE: 'success',
};

export type QualificationPresentationInput = {
  aggregateStatus: QualificationExerciseStatus;
  requiresOrgDecision: boolean;
  approvedOrgCount: number;
  totalOrgCount: number;
};

/** List/detail tones for aggregated self-view items (org-decision progress needs finer steps than the API enum). */
export function resolveQualificationPresentation(
  input: QualificationPresentationInput,
): QualificationPresentation {
  const { aggregateStatus, requiresOrgDecision, approvedOrgCount, totalOrgCount } = input;

  if (isPartialOrgDecisionProgress(input)) {
    return {
      rowTone: 'success',
      badgeTone: 'warning',
      orgDecisionProgressRatio: approvedOrgCount / totalOrgCount,
    };
  }

  if (aggregateStatus === 'ORG_DECISION_DENIED' && requiresOrgDecision && approvedOrgCount === 0) {
    return { rowTone: 'warning', badgeTone: 'warning' };
  }

  return {
    rowTone: QUALIFICATION_STATUS_ROW_TONE[aggregateStatus],
    badgeTone: QUALIFICATION_STATUS_BADGE_TONE[aggregateStatus],
  };
}
