/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { AdminOrganizationTreeNodeDto, RootOrganizationSummaryDto } from '@/lib/api/admin';

export interface LeaderIssueSummary {
  /** Orgs without a direct leader assignment. */
  directCount: number;
  /** Orgs that have a leader but a descendant is missing one. */
  subtreeOnlyCount: number;
  /** First org to open for fixing (direct issue preferred). */
  firstOrgId: number | null;
}

export function summarizeLeaderIssuesFromTree(
  nodes: AdminOrganizationTreeNodeDto[],
): LeaderIssueSummary {
  let directCount = 0;
  let subtreeOnlyCount = 0;
  let firstOrgId: number | null = null;

  function walk(node: AdminOrganizationTreeNodeDto) {
    if (node.leaderMissing) {
      directCount++;
      firstOrgId ??= node.id;
    } else if (node.leaderMissingInSubtree) {
      subtreeOnlyCount++;
      if (firstOrgId == null) {
        firstOrgId = findFirstDirectIssue(node)?.id ?? null;
      }
    }
    for (const child of node.children ?? []) {
      walk(child);
    }
  }

  for (const root of nodes) {
    walk(root);
  }

  return { directCount, subtreeOnlyCount, firstOrgId };
}

function findFirstDirectIssue(
  node: AdminOrganizationTreeNodeDto,
): AdminOrganizationTreeNodeDto | null {
  if (node.leaderMissing) {
    return node;
  }
  for (const child of node.children ?? []) {
    const found = findFirstDirectIssue(child);
    if (found) {
      return found;
    }
  }
  return null;
}

export function hasLeaderIssueInRoots(roots: RootOrganizationSummaryDto[]): boolean {
  return roots.some((root) => root.leaderMissingInSubtree);
}

export function totalLeaderIssues(summary: LeaderIssueSummary): number {
  return summary.directCount + summary.subtreeOnlyCount;
}
