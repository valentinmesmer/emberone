/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { BadgeTone, ListRowTone } from '@/components/ui/tones';
import type {
  QualificationExerciseStatus,
  QualificationStatusItemDto,
  SelfQualificationStatusItemDto,
  UserQualificationStatusDto,
} from '@/lib/api/qualifications';
import { resolveQualificationPresentation } from '@/lib/qualifications/statusPresentation';

export type QualificationOrgStatusEntry = {
  organizationId: number;
  organizationName: string;
  status: QualificationExerciseStatus;
  orgDecisionGranted: boolean;
};

export type AggregatedQualificationItem = {
  tenantQualificationDefinitionId: number;
  abbreviation: string;
  name: string;
  requiresOrgDecision: boolean;
  prerequisitesMet: boolean;
  orgEntries: QualificationOrgStatusEntry[];
  approvedOrgCount: number;
  totalOrgCount: number;
  aggregateStatus: QualificationExerciseStatus;
  rowTone: ListRowTone;
  badgeTone: BadgeTone;
  orgDecisionProgressRatio?: number;
};

export function userQualificationStatusesFromSelfRows(
  rows: SelfQualificationStatusItemDto[],
  userId = 0,
): UserQualificationStatusDto[] {
  const byOrg = new Map<number, QualificationStatusItemDto[]>();
  for (const row of rows) {
    const items = byOrg.get(row.organizationId) ?? [];
    items.push(row.item);
    byOrg.set(row.organizationId, items);
  }
  return Array.from(byOrg.entries()).map(([organizationId, items]) => ({
    userId,
    organizationId,
    items,
  }));
}

export function aggregateSelfQualificationStatuses(
  statuses: UserQualificationStatusDto[],
  organizationNameById: Map<number, string>,
): AggregatedQualificationItem[] {
  const byDefinition = new Map<number, AggregatedQualificationItem>();

  for (const orgStatus of statuses) {
    const organizationName =
      organizationNameById.get(orgStatus.organizationId) ?? `#${orgStatus.organizationId}`;

    for (const item of orgStatus.items) {
      let aggregated = byDefinition.get(item.tenantQualificationDefinitionId);
      if (!aggregated) {
        aggregated = {
          tenantQualificationDefinitionId: item.tenantQualificationDefinitionId,
          abbreviation: item.abbreviation,
          name: item.name,
          requiresOrgDecision: item.requiresOrgDecision,
          prerequisitesMet: item.prerequisitesMet,
          orgEntries: [],
          approvedOrgCount: 0,
          totalOrgCount: 0,
          aggregateStatus: item.status,
          rowTone: 'neutral',
          badgeTone: 'neutral',
        };
        byDefinition.set(item.tenantQualificationDefinitionId, aggregated);
      }

      aggregated.orgEntries.push({
        organizationId: orgStatus.organizationId,
        organizationName,
        status: item.status,
        orgDecisionGranted: item.orgDecisionGranted,
      });
      aggregated.prerequisitesMet = aggregated.prerequisitesMet || item.prerequisitesMet;
      if (item.orgDecisionGranted || item.status === 'CAN_EXERCISE') {
        aggregated.approvedOrgCount += 1;
      }
      aggregated.totalOrgCount += 1;
    }
  }

  return Array.from(byDefinition.values())
    .map((item) => finalizeAggregateItem(item))
    .sort((left, right) => {
      const rankDiff = qualificationListSortRank(left) - qualificationListSortRank(right);
      if (rankDiff !== 0) {
        return rankDiff;
      }
      return left.name.localeCompare(right.name, undefined, { sensitivity: 'base' });
    });
}

export type SelfQualificationListSectionId = 'valid' | 'notAppointed' | 'prerequisitesMissing';

export const QUALIFICATION_LIST_SECTION_TITLE_KEY: Record<SelfQualificationListSectionId, string> =
  {
    valid: 'qualifications.status.CAN_EXERCISE',
    notAppointed: 'qualifications.status.ORG_DECISION_DENIED',
    prerequisitesMissing: 'qualifications.status.PREREQUISITES_MISSING',
  };

const SELF_QUALIFICATION_LIST_SECTION_ORDER: SelfQualificationListSectionId[] = [
  'valid',
  'notAppointed',
  'prerequisitesMissing',
];

function selfQualificationListSectionId(
  item: AggregatedQualificationItem,
): SelfQualificationListSectionId {
  if (item.aggregateStatus === 'CAN_EXERCISE') {
    return 'valid';
  }
  if (item.aggregateStatus === 'PREREQUISITES_MISSING') {
    return 'prerequisitesMissing';
  }
  // At least one org appointed → treat as usable under Gültig (badge shows X/Y).
  if (
    item.requiresOrgDecision &&
    item.approvedOrgCount > 0 &&
    item.approvedOrgCount < item.totalOrgCount
  ) {
    return 'valid';
  }
  return 'notAppointed';
}

export function groupSelfQualificationListSections(items: AggregatedQualificationItem[]) {
  const buckets: Record<SelfQualificationListSectionId, AggregatedQualificationItem[]> = {
    valid: [],
    notAppointed: [],
    prerequisitesMissing: [],
  };

  for (const item of items) {
    buckets[selfQualificationListSectionId(item)].push(item);
  }

  return SELF_QUALIFICATION_LIST_SECTION_ORDER.map((id) => ({
    id,
    items: buckets[id],
  })).filter((section) => section.items.length > 0);
}

function orgQualificationListSectionId(
  status: QualificationExerciseStatus,
): SelfQualificationListSectionId {
  if (status === 'CAN_EXERCISE') {
    return 'valid';
  }
  if (status === 'PREREQUISITES_MISSING') {
    return 'prerequisitesMissing';
  }
  return 'notAppointed';
}

export function groupOrgQualificationStatusItems(items: QualificationStatusItemDto[]) {
  const buckets: Record<SelfQualificationListSectionId, QualificationStatusItemDto[]> = {
    valid: [],
    notAppointed: [],
    prerequisitesMissing: [],
  };

  for (const item of items) {
    buckets[orgQualificationListSectionId(item.status)].push(item);
  }

  for (const sectionId of SELF_QUALIFICATION_LIST_SECTION_ORDER) {
    buckets[sectionId].sort((left, right) =>
      left.name.localeCompare(right.name, undefined, { sensitivity: 'base' }),
    );
  }

  return SELF_QUALIFICATION_LIST_SECTION_ORDER.map((id) => ({
    id,
    items: buckets[id],
  })).filter((section) => section.items.length > 0);
}

/** Lower rank appears first: fully valid, partial appointment, org gaps, missing prerequisites. */
function qualificationListSortRank(item: AggregatedQualificationItem): number {
  if (item.aggregateStatus === 'CAN_EXERCISE') {
    return 0;
  }
  if (
    item.requiresOrgDecision &&
    item.approvedOrgCount > 0 &&
    item.approvedOrgCount < item.totalOrgCount
  ) {
    return 1;
  }
  if (
    item.aggregateStatus === 'ORG_DECISION_DENIED' ||
    item.aggregateStatus === 'ORG_DECISION_REVOKED' ||
    item.aggregateStatus === 'NOT_AVAILABLE'
  ) {
    return 2;
  }
  if (item.aggregateStatus === 'PREREQUISITES_MISSING') {
    return 3;
  }
  return 4;
}

function finalizeAggregateItem(item: AggregatedQualificationItem): AggregatedQualificationItem {
  const aggregateStatus = resolveAggregateStatus(item);
  const presentation = resolveQualificationPresentation({
    aggregateStatus,
    requiresOrgDecision: item.requiresOrgDecision,
    approvedOrgCount: item.approvedOrgCount,
    totalOrgCount: item.totalOrgCount,
  });
  return {
    ...item,
    aggregateStatus,
    rowTone: presentation.rowTone,
    badgeTone: presentation.badgeTone,
    orgDecisionProgressRatio: presentation.orgDecisionProgressRatio,
  };
}

function resolveAggregateStatus(item: AggregatedQualificationItem): QualificationExerciseStatus {
  if (!item.prerequisitesMet) {
    return 'PREREQUISITES_MISSING';
  }
  if (!item.requiresOrgDecision) {
    return 'CAN_EXERCISE';
  }
  if (item.approvedOrgCount === 0) {
    const hasRevoked = item.orgEntries.some((entry) => entry.status === 'ORG_DECISION_REVOKED');
    return hasRevoked ? 'ORG_DECISION_REVOKED' : 'ORG_DECISION_DENIED';
  }
  if (item.approvedOrgCount === item.totalOrgCount) {
    return 'CAN_EXERCISE';
  }
  return 'ORG_DECISION_DENIED';
}

export function qualificationListBadgeLabel(
  item: AggregatedQualificationItem,
  t: (key: string, params?: Record<string, string | number>) => string,
): string {
  if (item.aggregateStatus === 'CAN_EXERCISE') {
    return t('qualifications.status.CAN_EXERCISE');
  }
  if (item.aggregateStatus === 'PREREQUISITES_MISSING') {
    return t('qualifications.status.PREREQUISITES_MISSING');
  }
  if (
    item.requiresOrgDecision &&
    item.approvedOrgCount > 0 &&
    item.approvedOrgCount < item.totalOrgCount
  ) {
    return t('qualifications.list.orgDecisionProgress', {
      approved: item.approvedOrgCount,
      total: item.totalOrgCount,
    });
  }
  return t(`qualifications.status.${item.aggregateStatus}`);
}

export function qualificationDetailItemFromStatuses(
  statuses: UserQualificationStatusDto[],
  definitionId: number,
): QualificationStatusItemDto | null {
  for (const orgStatus of statuses) {
    const item = orgStatus.items.find(
      (entry) => entry.tenantQualificationDefinitionId === definitionId,
    );
    if (item) {
      return item;
    }
  }
  return null;
}

export function qualificationOrgEntriesFromStatuses(
  statuses: UserQualificationStatusDto[],
  definitionId: number,
  organizationNameById: Map<number, string>,
): QualificationOrgStatusEntry[] {
  const entries: QualificationOrgStatusEntry[] = [];

  for (const orgStatus of statuses) {
    const item = orgStatus.items.find(
      (entry) => entry.tenantQualificationDefinitionId === definitionId,
    );
    if (!item) {
      continue;
    }
    entries.push({
      organizationId: orgStatus.organizationId,
      organizationName:
        organizationNameById.get(orgStatus.organizationId) ?? `#${orgStatus.organizationId}`,
      status: item.status,
      orgDecisionGranted: item.orgDecisionGranted,
    });
  }

  return entries.sort((left, right) =>
    left.organizationName.localeCompare(right.organizationName, undefined, { sensitivity: 'base' }),
  );
}
