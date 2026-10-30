/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { ReactNode } from 'react';
import type { ListRowTone } from '@/components/ui/tones';
import { compareText } from '@/lib/i18n/locale';
import { formatDateTime } from '@/lib/utils/date';
import {
  isActiveMemberOfOrganizationGroup,
  type TranslationFunction,
} from '@/lib/utils/membership';
import { workflowParticipationSlotDisplayTone } from '@/lib/utils/workflowLabels';
import type { UserOrganizationGroupMembershipDto } from '@/types/user';
import type {
  WorkflowDecisionDto,
  WorkflowDto,
  WorkflowParticipantDto,
  WorkflowStatus,
} from '@/types/workflow';

export function normalizeWorkflowStatus(status: string | undefined): WorkflowStatus | string {
  if (!status) return 'OPEN';
  const u = String(status).toUpperCase();
  if (
    u === 'OPEN' ||
    u === 'AWAITING_INPUT_CHANGE' ||
    u === 'APPROVED' ||
    u === 'REJECTED' ||
    u === 'CANCELLED'
  ) {
    return u;
  }
  return status;
}

export function blocksSelfApprove(wf: WorkflowDto, userId: number): boolean {
  return wf.openedByUserId === userId && wf.selfApproveAllowed !== true;
}

export function isClaimableApproverSlot(
  p: WorkflowParticipantDto,
  wf: WorkflowDto,
  userId: number | undefined,
  groupMemberships: UserOrganizationGroupMembershipDto[] | undefined,
): boolean {
  const st = normalizeWorkflowStatus(wf.status);
  if (st !== 'OPEN' || userId == null) return false;
  if (blocksSelfApprove(wf, userId)) return false;
  return (
    p.role === 'APPROVER' &&
    p.slotStatus === 'PENDING' &&
    p.principalType === 'ORGANIZATION_GROUP' &&
    (p.claimedByUserId == null || p.claimedByUserId === undefined) &&
    isActiveMemberOfOrganizationGroup(groupMemberships, p.principalId)
  );
}

export function isDecidableApproverSlot(
  p: WorkflowParticipantDto,
  wf: WorkflowDto,
  userId: number,
): boolean {
  const st = normalizeWorkflowStatus(wf.status);
  if (st !== 'OPEN') return false;
  if (blocksSelfApprove(wf, userId)) return false;
  return (
    p.role === 'APPROVER' &&
    p.slotStatus === 'PENDING' &&
    p.claimedByUserId != null &&
    p.claimedByUserId === userId
  );
}

export function isUnclaimableApproverSlot(
  p: WorkflowParticipantDto,
  wf: WorkflowDto,
  userId: number,
): boolean {
  const st = normalizeWorkflowStatus(wf.status);
  if (st !== 'OPEN' && st !== 'AWAITING_INPUT_CHANGE') return false;
  if (blocksSelfApprove(wf, userId)) return false;
  return (
    p.role === 'APPROVER' &&
    p.principalType === 'ORGANIZATION_GROUP' &&
    p.slotStatus === 'PENDING' &&
    p.claimedByUserId != null &&
    p.claimedByUserId === userId
  );
}

export function isWithdrawableApprovedSlot(
  slot: WorkflowParticipantDto,
  wf: WorkflowDto,
  userId: number,
  decision: WorkflowDecisionDto | undefined,
): boolean {
  const st = normalizeWorkflowStatus(wf.status);
  if (st !== 'OPEN' || userId == null) return false;
  return (
    slot.role === 'APPROVER' &&
    slot.slotStatus === 'APPROVED' &&
    slot.claimedByUserId === userId &&
    decision?.decisionType === 'APPROVED' &&
    decision.userId === userId
  );
}

/** Mobile inbox / detail: prefer a slot the user can decide, else one they can claim. */
export type ActionableApproverSlotKind = 'claim' | 'decide';

export type ActionableApproverSlot = {
  slot: WorkflowParticipantDto;
  kind: ActionableApproverSlotKind;
};

/** All slots the current user can claim or decide, decidable first then claimable. */
export function listActionableApproverSlots(
  wf: WorkflowDto,
  userId: number | undefined,
  groupMemberships: UserOrganizationGroupMembershipDto[] | undefined,
): ActionableApproverSlot[] {
  if (userId == null) return [];
  const participants = wf.participants ?? [];
  const decidable = participants
    .filter((p) => isDecidableApproverSlot(p, wf, userId))
    .map((slot) => ({ slot, kind: 'decide' as const }));
  const claimable = participants
    .filter((p) => isClaimableApproverSlot(p, wf, userId, groupMemberships))
    .map((slot) => ({ slot, kind: 'claim' as const }));
  return [...decidable, ...claimable];
}

function formatPersonName(firstName?: string | null, lastName?: string | null): string {
  return `${firstName ?? ''} ${lastName ?? ''}`.trim();
}

export function slotStatusBadge(
  p: WorkflowParticipantDto,
  t: (key: string) => string,
): { label: string; tone: 'neutral' | 'accent' | 'success' | 'danger' | 'warning' } {
  if (p.slotStatus === 'APPROVED') {
    return { label: t('myArea.workflowDetail.slotStatusApproved'), tone: 'success' };
  }
  if (p.slotStatus === 'REJECTED') {
    return { label: t('myArea.workflowDetail.slotStatusRejected'), tone: 'danger' };
  }
  const claimed = p.claimedByUserId != null;
  const displayTone = workflowParticipationSlotDisplayTone(claimed);
  return {
    label: t(
      claimed
        ? 'myArea.workflowDetail.slotStatusInProgress'
        : 'myArea.workflowDetail.slotStatusPending',
    ),
    tone: displayTone.badgeTone,
  };
}

export function sortApproverSlots(slots: WorkflowParticipantDto[]): WorkflowParticipantDto[] {
  return [...slots].sort((a, b) => {
    const orgA = (a.organizationDisplayName ?? '').toLowerCase();
    const orgB = (b.organizationDisplayName ?? '').toLowerCase();
    if (orgA !== orgB) return compareText(orgA, orgB);
    return compareText(a.principalDisplayName ?? '', b.principalDisplayName ?? '');
  });
}

export function slotListRowTone(slot: WorkflowParticipantDto): ListRowTone {
  if (slot.slotStatus === 'APPROVED') return 'success';
  if (slot.slotStatus === 'REJECTED') return 'danger';
  return workflowParticipationSlotDisplayTone(slot.claimedByUserId != null).rowTone;
}

export function slotOrgName(slot: WorkflowParticipantDto, t: TranslationFunction): string {
  const org = slot.organizationDisplayName?.trim();
  if (org) return org;
  if (slot.principalType === 'USER' && slot.principalDisplayName?.trim()) {
    return slot.principalDisplayName.trim();
  }
  return t('common.notAvailable');
}

export function slotGroupName(slot: WorkflowParticipantDto): string | null {
  if (slot.principalType !== 'ORGANIZATION_GROUP') return null;
  const group = slot.principalDisplayName?.trim();
  return group || null;
}

/** Compact one-line handler status for list rows. */
export function slotHandlerStatusLine(
  slot: WorkflowParticipantDto,
  t: TranslationFunction,
): string {
  return slotHandlerLabel(slot, t);
}

function slotHandlerLabel(slot: WorkflowParticipantDto, t: TranslationFunction): string {
  if (slot.claimedByDisplayName?.trim()) {
    return t('myArea.workflowDetail.approvalStatusHandler', {
      name: slot.claimedByDisplayName.trim(),
    });
  }
  if (slot.slotStatus === 'APPROVED' && slot.claimedByUserId == null) {
    return t('myArea.workflowDetail.approvalStatusAutoApproved');
  }
  return t('myArea.workflowDetail.approvalStatusHandlerNone');
}

export function findDecisionForSlot(
  slot: WorkflowParticipantDto,
  decisions: WorkflowDecisionDto[] | undefined,
): WorkflowDecisionDto | undefined {
  return decisions?.find((d) => d.participantId === slot.id);
}

function decisionActorLabel(
  decision: WorkflowDecisionDto,
  t: (key: string, params?: Record<string, string | number>) => string,
  self?: { id: number; firstName?: string | null; lastName?: string | null },
): string {
  if (decision.userId == null) return t('myArea.workflowDetail.actorSystem');
  const name = decision.userDisplayName?.trim();
  if (name) return name;
  if (self != null && decision.userId === self.id) {
    const selfName = formatPersonName(self.firstName, self.lastName);
    if (selfName) return selfName;
  }
  return t('myArea.workflowDetail.actorUser', { id: decision.userId });
}

export function buildSlotSubtitle(
  slot: WorkflowParticipantDto,
  decision: WorkflowDecisionDto | undefined,
  t: TranslationFunction,
  self?: { id: number; firstName?: string | null; lastName?: string | null },
): ReactNode {
  const isDecided = slot.slotStatus === 'APPROVED' || slot.slotStatus === 'REJECTED';
  const isAutoApproved = isDecided && decision != null && decision.userId == null;
  const handlerLine = isAutoApproved ? (
    <span>
      {t('myArea.workflowDetail.approvalStatusAutoApproved')}
      {decision?.decidedAt ? (
        <>
          <span aria-hidden> · </span>
          {formatDateTime(decision.decidedAt)}
        </>
      ) : null}
    </span>
  ) : !isDecided ? (
    <span>{slotHandlerLabel(slot, t)}</span>
  ) : slot.claimedByDisplayName?.trim() ? (
    <span>
      {t('myArea.workflowDetail.approvalStatusHandler', {
        name: slot.claimedByDisplayName.trim(),
      })}
    </span>
  ) : null;

  return (
    <div className="flex flex-col gap-0.5">
      {handlerLine ? <div>{handlerLine}</div> : null}
      {decision && isDecided && !isAutoApproved && decision.decidedAt ? (
        <span>
          {t('myArea.workflowDetail.approvalStatusDecided', {
            name: decisionActorLabel(decision, t, self),
            when: formatDateTime(decision.decidedAt),
          })}
        </span>
      ) : null}
      {decision && isDecided && decision.comment?.trim() ? (
        <span className="text-[var(--text-primary)]">{decision.comment.trim()}</span>
      ) : null}
    </div>
  );
}
