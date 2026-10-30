/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Button } from '@/components/ui/button';
import { useTranslation } from '@/hooks/useTranslation';
import { cn } from '@/lib/utils/cn';
import {
  statusDangerSolidButtonClass,
  statusSuccessSolidButtonClass,
} from '@/lib/utils/statusActionClasses';

const approveActionClass = statusSuccessSolidButtonClass;
const rejectActionClass = statusDangerSolidButtonClass;

type WorkflowApprovalSlotOption = {
  id: number;
  label: string;
  kind: 'claim' | 'decide';
};

export interface WorkflowApprovalActionsSectionProps {
  isClaimable: boolean;
  isDecidable: boolean;
  isUnclaimable: boolean;
  isWithdrawable: boolean;
  actionPending: boolean;
  claimLoading?: boolean;
  decideLoading?: boolean;
  unclaimLoading?: boolean;
  withdrawLoading?: boolean;
  onClaim: () => void;
  onApprove: () => void;
  onReject: () => void;
  onUnclaim: () => void;
  onWithdrawApproval: () => void;
  /**
   * When true, the reject action maps to an input-change request - use change-request copy
   * instead of terminal-reject wording.
   */
  mapsRejectToInputChange?: boolean;
  /**
   * `card` - bordered section for desktop / in-page use.
   * `dock` - compact thumb-zone actions for the mobile sticky bar.
   */
  layout?: 'card' | 'dock';
  /** Single-slot context (org · group), shown under the section title. */
  slotLabel?: string | null;
  /** When the user has multiple actionable slots, choose which one actions apply to. */
  slotOptions?: WorkflowApprovalSlotOption[];
  selectedSlotId?: number | null;
  onSelectSlot?: (slotId: number) => void;
}

export function WorkflowApprovalActionsSection({
  isClaimable,
  isDecidable,
  isUnclaimable,
  isWithdrawable,
  actionPending,
  claimLoading = false,
  decideLoading = false,
  unclaimLoading = false,
  withdrawLoading = false,
  onClaim,
  onApprove,
  onReject,
  onUnclaim,
  onWithdrawApproval,
  mapsRejectToInputChange = false,
  layout = 'card',
  slotLabel = null,
  slotOptions,
  selectedSlotId = null,
  onSelectSlot,
}: WorkflowApprovalActionsSectionProps) {
  const t = useTranslation();
  const isDock = layout === 'dock';
  const multiSlot = (slotOptions?.length ?? 0) > 1;
  const actionsDisabled = actionPending || (multiSlot && selectedSlotId == null);

  if (!isClaimable && !isDecidable && !isUnclaimable && !isWithdrawable && !multiSlot) {
    return null;
  }

  const primarySize = isDock ? 'lg' : 'md';

  return (
    <section
      className={cn(
        isDock ? 'space-y-2' : 'space-y-2 rounded-lg border border-[var(--border-color)] p-3',
      )}
    >
      {!isDock ? (
        <p className="text-sm font-semibold text-[var(--text-primary)]">
          {t('myArea.workflowDetail.approvalSlotActionsSection')}
        </p>
      ) : null}

      {multiSlot && slotOptions && onSelectSlot ? (
        <div className="space-y-2">
          <p className="text-sm text-[var(--text-secondary)]">
            {t('myArea.workflowDetail.chooseApprovalSlotHint')}
          </p>
          <div
            className="space-y-1.5"
            role="radiogroup"
            aria-label={t('myArea.workflowDetail.chooseApprovalSlotHint')}
          >
            {slotOptions.map((option) => {
              const selected = option.id === selectedSlotId;
              return (
                <button
                  key={option.id}
                  type="button"
                  aria-pressed={selected}
                  disabled={actionPending}
                  onClick={() => onSelectSlot(option.id)}
                  className={cn(
                    'flex w-full items-center gap-3 rounded-lg border px-3 py-2.5 text-left transition-colors',
                    selected
                      ? 'border-[var(--fire-red)] bg-[var(--fire-red)]/10'
                      : 'border-[var(--border-color)] bg-[var(--surface-soft)] hover:bg-[var(--hover-bg)]',
                    actionPending && 'opacity-50',
                  )}
                >
                  <span
                    className={cn(
                      'flex h-4 w-4 shrink-0 items-center justify-center rounded-full border-2',
                      selected ? 'border-[var(--fire-red)]' : 'border-[var(--text-secondary)]',
                    )}
                    aria-hidden
                  >
                    {selected ? (
                      <span className="h-2 w-2 rounded-full bg-[var(--fire-red)]" />
                    ) : null}
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block text-sm font-semibold text-[var(--text-primary)]">
                      {option.label}
                    </span>
                    <span className="block text-xs text-[var(--text-secondary)]">
                      {option.kind === 'decide'
                        ? t('myArea.workflowDetail.slotOptionDecide')
                        : t('myArea.workflowDetail.slotOptionClaim')}
                    </span>
                  </span>
                </button>
              );
            })}
          </div>
        </div>
      ) : slotLabel ? (
        <p className="text-sm text-[var(--text-secondary)]">
          {t('myArea.workflowDetail.approvalSlotContext', { slot: slotLabel })}
        </p>
      ) : null}

      {isClaimable ? (
        <>
          {!isDock && !multiSlot ? (
            <p className="text-sm text-[var(--text-secondary)]">
              {t('myArea.workflowDetail.claimSlotHint')}
            </p>
          ) : isDock ? (
            <p className="text-center text-xs text-[var(--text-secondary)]">
              {t('myArea.workflowDetail.claimSlotDockHint')}
            </p>
          ) : null}
          <Button
            type="button"
            variant="primary"
            size={primarySize}
            fullWidth
            isLoading={claimLoading}
            disabled={actionsDisabled}
            onClick={onClaim}
          >
            {t('myArea.workflowDetail.claim')}
          </Button>
        </>
      ) : null}

      {isDecidable ? (
        <>
          {!isDock && !multiSlot ? (
            <p className="text-sm text-[var(--text-secondary)]">
              {t('myArea.workflowDetail.claimedByYouHint')}
            </p>
          ) : null}
          <div className="grid grid-cols-2 gap-2">
            <Button
              type="button"
              variant="secondary"
              size={primarySize}
              className={approveActionClass}
              fullWidth
              isLoading={decideLoading}
              disabled={actionsDisabled}
              onClick={onApprove}
            >
              {t('myArea.workflowDetail.approve')}
            </Button>
            <Button
              type="button"
              variant={mapsRejectToInputChange ? 'outline' : 'secondary'}
              size={primarySize}
              className={mapsRejectToInputChange ? undefined : rejectActionClass}
              fullWidth
              isLoading={decideLoading}
              disabled={actionsDisabled}
              onClick={onReject}
            >
              {t(
                mapsRejectToInputChange
                  ? 'myArea.workflowDetail.requestInputChange'
                  : 'myArea.workflowDetail.reject',
              )}
            </Button>
          </div>
        </>
      ) : null}

      {isUnclaimable ? (
        isDock ? (
          <button
            type="button"
            className="w-full py-1 text-center text-sm font-medium text-[var(--text-secondary)] underline-offset-2 hover:underline disabled:opacity-50"
            disabled={actionsDisabled || unclaimLoading}
            onClick={onUnclaim}
          >
            {t('myArea.workflowDetail.unclaim')}
          </button>
        ) : (
          <Button
            type="button"
            variant="secondary"
            fullWidth
            isLoading={unclaimLoading}
            disabled={actionsDisabled}
            onClick={onUnclaim}
          >
            {t('myArea.workflowDetail.unclaim')}
          </Button>
        )
      ) : null}

      {isWithdrawable ? (
        <Button
          type="button"
          variant="secondary"
          fullWidth
          isLoading={withdrawLoading}
          disabled={actionsDisabled}
          onClick={onWithdrawApproval}
        >
          {t('myArea.workflowDetail.withdrawApproval')}
        </Button>
      ) : null}
    </section>
  );
}
