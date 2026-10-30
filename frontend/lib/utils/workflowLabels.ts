/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { BadgeTone, ListRowTone } from '@/components/ui/tones';
import type { WorkflowDto, WorkflowOpenScope, WorkflowStatus } from '@/types/workflow';
import { WORKFLOW_TYPE_IDS } from '@/types/workflow';

export const WORKFLOW_OPEN_SCOPE_ORDER: WorkflowOpenScope[] = ['SELF', 'ORGANIZATION'];

/** Mirrors backend {@link com.emberone.workflow.domain.WorkflowType#openScope}. */
export function workflowOpenScopeForTypeId(workflowTypeId: number): WorkflowOpenScope {
  if (
    workflowTypeId === WORKFLOW_TYPE_IDS.ORGANIZATION_MEMBERSHIP_END_REQUEST ||
    workflowTypeId === WORKFLOW_TYPE_IDS.ORGANIZATION_USER_MAIN_ORGANIZATION_CHANGE
  ) {
    return 'ORGANIZATION';
  }
  return 'SELF';
}

export function workflowOpenScopeTranslationKey(scope: WorkflowOpenScope): string {
  return `admin.standards.workflowOpenScope.${scope}`;
}

export function workflowTypeIdFromEnumKey(enumKey: string): number | null {
  if (enumKey in WORKFLOW_TYPE_IDS) {
    return WORKFLOW_TYPE_IDS[enumKey as keyof typeof WORKFLOW_TYPE_IDS];
  }
  return null;
}

export function groupByWorkflowOpenScope<T>(
  items: T[],
  getWorkflowTypeId: (item: T) => number,
): Map<WorkflowOpenScope, T[]> {
  const groups = new Map<WorkflowOpenScope, T[]>(
    WORKFLOW_OPEN_SCOPE_ORDER.map((scope) => [scope, []]),
  );
  for (const item of items) {
    const scope = workflowOpenScopeForTypeId(getWorkflowTypeId(item));
    groups.get(scope)?.push(item);
  }
  return groups;
}

/** i18n key for the human-readable workflow type label (used in org admin and inbox-style UIs). */
export function workflowTypeTranslationKey(workflowTypeId: number): string {
  const entry = Object.entries(WORKFLOW_TYPE_IDS).find(([, id]) => id === workflowTypeId);
  if (!entry) {
    return 'admin.standards.workflowTypes.unknown';
  }
  return `admin.standards.workflowTypes.${entry[0]}`;
}

/** True while the workflow or its status group may still need user attention in list views. */
export function workflowStatusRequiresAction(status: WorkflowStatus | string): boolean {
  const u = String(status).toUpperCase();
  return u === 'OPEN' || u === 'AWAITING_INPUT_CHANGE';
}

export function workflowStatusBadgeTone(status: WorkflowStatus | string): BadgeTone {
  switch (String(status).toUpperCase()) {
    case 'OPEN':
      return 'warning';
    case 'AWAITING_INPUT_CHANGE':
      return 'accent';
    case 'APPROVED':
      return 'success';
    case 'REJECTED':
      return 'danger';
    case 'CANCELLED':
      return 'neutral';
    default:
      return 'neutral';
  }
}

function hasClaimedPendingApprover(wf: WorkflowDto): boolean {
  const pendingApprovers =
    wf.participants?.filter((p) => p.role === 'APPROVER' && p.slotStatus === 'PENDING') ?? [];
  return pendingApprovers.some((p) => p.claimedByUserId != null);
}

/** Badge + row accent for unclaimed („Offen“) vs claimed („In Bearbeitung“) approver slots. */
export function workflowParticipationSlotDisplayTone(claimed: boolean): {
  badgeTone: BadgeTone;
  rowTone: ListRowTone;
} {
  return claimed
    ? { badgeTone: 'warning', rowTone: 'warning' }
    : { badgeTone: 'accent', rowTone: 'active' };
}

/** List/detail badge: unclaimed approver slots show „Offen“, claimed ones „In Bearbeitung“. */
export function workflowParticipationListStatusLabel(
  wf: WorkflowDto,
  t: (key: string) => string,
): { label: string; tone: BadgeTone; rowTone: ListRowTone } {
  const status = String(wf.status).toUpperCase() as WorkflowStatus;
  if (status === 'OPEN') {
    const pendingApprovers =
      wf.participants?.filter((p) => p.role === 'APPROVER' && p.slotStatus === 'PENDING') ?? [];
    if (pendingApprovers.length > 0) {
      const anyClaimed = hasClaimedPendingApprover(wf);
      const displayTone = workflowParticipationSlotDisplayTone(anyClaimed);
      return {
        label: t(
          anyClaimed
            ? 'myArea.workflowDetail.slotStatusInProgress'
            : 'myArea.workflowDetail.slotStatusPending',
        ),
        tone: displayTone.badgeTone,
        rowTone: displayTone.rowTone,
      };
    }
  }
  return {
    label: t(`myArea.workflows.status.${status}`),
    tone: workflowStatusBadgeTone(status),
    rowTone: workflowStatusRequiresAction(status) ? 'active' : 'default',
  };
}
