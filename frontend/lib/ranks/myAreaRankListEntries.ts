/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type {
  ResolvedDutyFunctionDto,
  UserRankCurrentDto,
  UserRankHistoryItemDto,
} from '@/lib/api/ranks';
import { compareText } from '@/lib/i18n/locale';
import type { WorkflowDto } from '@/types/workflow';

export type MyAreaRankListEntry =
  | {
      kind: 'function';
      id: string;
      function: ResolvedDutyFunctionDto;
    }
  | {
      kind: 'function_history';
      id: string;
      function: ResolvedDutyFunctionDto;
    }
  | {
      kind: 'rank_pending';
      id: string;
      workflow: WorkflowDto;
    }
  | {
      kind: 'rank_current';
      id: string;
      rank: UserRankCurrentDto;
    }
  | {
      kind: 'rank_history';
      id: string;
      item: UserRankHistoryItemDto;
    };

function compareFunctions(a: ResolvedDutyFunctionDto, b: ResolvedDutyFunctionDto): number {
  return (
    compareText(a.organizationName, b.organizationName) ||
    compareText(a.functionName, b.functionName) ||
    compareText(a.organizationGroupName, b.organizationGroupName)
  );
}

function compareFunctionHistory(a: ResolvedDutyFunctionDto, b: ResolvedDutyFunctionDto): number {
  return (
    (b.membershipEndDate ?? '').localeCompare(a.membershipEndDate ?? '') ||
    b.membershipStartDate.localeCompare(a.membershipStartDate) ||
    compareFunctions(a, b)
  );
}

export function isFunctionEntry(
  entry: MyAreaRankListEntry,
): entry is Extract<MyAreaRankListEntry, { kind: 'function' | 'function_history' }> {
  return entry.kind === 'function' || entry.kind === 'function_history';
}

export function buildMyAreaRankListEntries(params: {
  functions: ResolvedDutyFunctionDto[] | undefined;
  openRankWorkflows: WorkflowDto[];
  current: UserRankCurrentDto | null;
  history: UserRankHistoryItemDto[];
}): MyAreaRankListEntry[] {
  const entries: MyAreaRankListEntry[] = [];

  const allFunctions = params.functions ?? [];
  const functionId = (prefix: string, fn: ResolvedDutyFunctionDto) =>
    `${prefix}-${fn.organizationId}-${fn.functionConfigKey}-${fn.organizationGroupConfigKey}-${fn.membershipStartDate}`;

  for (const fn of allFunctions.filter((item) => item.active).sort(compareFunctions)) {
    entries.push({ kind: 'function', id: functionId('fn', fn), function: fn });
  }
  for (const fn of allFunctions.filter((item) => !item.active).sort(compareFunctionHistory)) {
    entries.push({ kind: 'function_history', id: functionId('fn-hist', fn), function: fn });
  }

  for (const workflow of params.openRankWorkflows) {
    entries.push({
      kind: 'rank_pending',
      id: `pending-${workflow.id}`,
      workflow,
    });
  }

  if (params.current) {
    entries.push({
      kind: 'rank_current',
      id: 'rank-current',
      rank: params.current,
    });
  }

  const historyWithoutOpenCurrent =
    params.current == null
      ? params.history
      : params.history.filter(
          (item) =>
            item.endDate != null ||
            item.tenantRankId !== params.current?.tenantRankId ||
            item.startDate !== params.current?.startDate,
        );

  for (const item of historyWithoutOpenCurrent) {
    entries.push({
      kind: 'rank_history',
      id: `hist-${item.startDate}-${item.tenantRankId}-${item.endDate ?? 'open'}`,
      item,
    });
  }

  return entries;
}
