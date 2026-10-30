/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { FileText } from 'lucide-react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useLayoutEffect, useMemo, useState } from 'react';
import { WorkflowApprovalActionsSection } from '@/components/my-area/workflows/WorkflowApprovalActionsSection';
import { WorkflowApproveConfirmDialog } from '@/components/my-area/workflows/WorkflowApproveConfirmDialog';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { FieldDisplay } from '@/components/ui/field-display';
import { FormLabel } from '@/components/ui/form-label';
import { List, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { ErrorState, NotFoundState } from '@/components/ui/message-cards';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { PageHeader } from '@/components/ui/page-header';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import {
  getMyAreaWorkflowDetailPath,
  getMyAreaWorkflowInboxPath,
} from '@/lib/my-area/myAreaSections';
import { useSetBackHref } from '@/lib/navigation/backHrefOverride';
import {
  useClaimWorkflowParticipant,
  useSelf,
  useSubmitWorkflowDecision,
  useUnclaimWorkflowParticipant,
  useUpdateWorkflowDocumentPromotions,
  useWithdrawWorkflowApproval,
  useWorkflow,
  useWorkflowDocumentPromotionPreview,
} from '@/lib/store';
import {
  buildSlotSubtitle,
  findDecisionForSlot,
  isClaimableApproverSlot,
  isDecidableApproverSlot,
  isUnclaimableApproverSlot,
  isWithdrawableApprovedSlot,
  normalizeWorkflowStatus,
  slotGroupName,
  slotOrgName,
  slotStatusBadge,
} from '@/lib/utils/workflowApprovalSlot';
import {
  buildWorkflowDocumentPromotionSelections,
  selectedKeysFromPreviewItems,
} from '@/lib/utils/workflowDocumentPromotion';
import { workflowTypeTranslationKey } from '@/lib/utils/workflowLabels';

export interface WorkflowApprovalSlotDetailViewProps {
  workflowId: number;
  participantId: number;
}

export function WorkflowApprovalSlotDetailView({
  workflowId,
  participantId,
}: WorkflowApprovalSlotDetailViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const searchParams = useSearchParams();
  const { showError, showSuccess } = useToast();
  const setBackHref = useSetBackHref();
  const { data: self } = useSelf();
  const { data: wf, isLoading, error } = useWorkflow(workflowId);
  const statusForQueries = wf ? normalizeWorkflowStatus(wf.status) : 'OPEN';
  const { data: promotionPreview } = useWorkflowDocumentPromotionPreview(
    workflowId,
    statusForQueries === 'OPEN',
  );
  const promotionItems = useMemo(() => promotionPreview?.items ?? [], [promotionPreview?.items]);
  const promotionSelectedKeys = useMemo(
    () => selectedKeysFromPreviewItems(promotionItems),
    [promotionItems],
  );

  const claimMutation = useClaimWorkflowParticipant();
  const unclaimMutation = useUnclaimWorkflowParticipant();
  const withdrawApprovalMutation = useWithdrawWorkflowApproval();
  const decideMutation = useSubmitWorkflowDecision();
  const updateDocumentPromotionsMutation = useUpdateWorkflowDocumentPromotions();

  const [rejectOpen, setRejectOpen] = useState(false);
  const [approveOpen, setApproveOpen] = useState(false);
  const [rejectComment, setRejectComment] = useState('');

  const fromInbox = searchParams.get('from') === 'inbox';
  const workflowDetailPath = getMyAreaWorkflowDetailPath(workflowId);
  const inboxPath = getMyAreaWorkflowInboxPath();
  const currentUserId = self?.id;
  const groupMemberships = self?.organizationGroupMemberships;

  useLayoutEffect(() => {
    setBackHref(fromInbox ? inboxPath : workflowDetailPath);
    return () => setBackHref(null);
  }, [fromInbox, inboxPath, workflowDetailPath, setBackHref]);

  const slot = useMemo(
    () => wf?.participants?.find((p) => p.id === participantId && p.role === 'APPROVER'),
    [wf?.participants, participantId],
  );

  const decision = useMemo(
    () => (slot ? findDecisionForSlot(slot, wf?.decisions) : undefined),
    [slot, wf?.decisions],
  );

  const isClaimable = useMemo(
    () =>
      wf && slot && currentUserId != null
        ? isClaimableApproverSlot(slot, wf, currentUserId, groupMemberships)
        : false,
    [wf, slot, currentUserId, groupMemberships],
  );

  const isDecidable = useMemo(
    () =>
      wf && slot && currentUserId != null
        ? isDecidableApproverSlot(slot, wf, currentUserId)
        : false,
    [wf, slot, currentUserId],
  );

  const isUnclaimable = useMemo(
    () =>
      wf && slot && currentUserId != null
        ? isUnclaimableApproverSlot(slot, wf, currentUserId)
        : false,
    [wf, slot, currentUserId],
  );

  const isWithdrawable = useMemo(
    () =>
      wf && slot && currentUserId != null
        ? isWithdrawableApprovedSlot(slot, wf, currentUserId, decision)
        : false,
    [wf, slot, currentUserId, decision],
  );

  const hasActions = isClaimable || isDecidable || isUnclaimable || isWithdrawable;
  const actionPending =
    claimMutation.isPending ||
    unclaimMutation.isPending ||
    withdrawApprovalMutation.isPending ||
    decideMutation.isPending ||
    updateDocumentPromotionsMutation.isPending;

  const handleClaim = async () => {
    if (!slot) return;
    try {
      await claimMutation.mutateAsync({ workflowId, participantId: slot.id });
      showSuccess(t('myArea.workflowDetail.claimSuccess'));
    } catch (e) {
      showError(e instanceof Error ? e.message : t('myArea.error.loadFailed'));
    }
  };

  const handleUnclaim = async () => {
    if (!slot) return;
    try {
      await unclaimMutation.mutateAsync({ workflowId, participantId: slot.id });
      showSuccess(t('myArea.workflowDetail.unclaimSuccess'));
      router.push(fromInbox ? inboxPath : workflowDetailPath);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('myArea.error.loadFailed'));
    }
  };

  const handleWithdrawApproval = async () => {
    if (!slot) return;
    try {
      await withdrawApprovalMutation.mutateAsync({ workflowId, participantId: slot.id });
      showSuccess(t('myArea.workflowDetail.withdrawApprovalSuccess'));
    } catch (e) {
      showError(e instanceof Error ? e.message : t('myArea.error.loadFailed'));
    }
  };

  const handleApproveConfirm = async () => {
    if (!slot) return;
    try {
      if (promotionItems.length > 0) {
        const selections = buildWorkflowDocumentPromotionSelections(
          promotionItems,
          promotionSelectedKeys,
        );
        await updateDocumentPromotionsMutation.mutateAsync({ workflowId, selections });
      }
      await decideMutation.mutateAsync({
        workflowId,
        body: {
          participantId: slot.id,
          decision: 'APPROVED',
        },
      });
      showSuccess(t('myArea.workflowDetail.approveSuccess'));
      setApproveOpen(false);
      router.push(fromInbox ? inboxPath : workflowDetailPath);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('myArea.error.loadFailed'));
    }
  };

  const handleRejectSubmit = async () => {
    if (!slot) return;
    const mapsRejectToInputChange = wf?.mapsRejectToInputChange === true;
    const comment = rejectComment.trim();
    if (mapsRejectToInputChange && !comment) return;
    try {
      await decideMutation.mutateAsync({
        workflowId,
        body: {
          participantId: slot.id,
          decision: 'REJECTED',
          comment: comment || undefined,
        },
      });
      showSuccess(
        t(
          mapsRejectToInputChange
            ? 'myArea.workflowDetail.requestInputChangeSuccess'
            : 'myArea.workflowDetail.rejectSuccess',
        ),
      );
      setRejectOpen(false);
      router.push(fromInbox ? inboxPath : workflowDetailPath);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('myArea.error.loadFailed'));
    }
  };

  if (isLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (error) {
    return <ErrorState message={t('myArea.workflowDetail.loadFailed')} />;
  }

  if (!wf || !slot) {
    return <NotFoundState message={t('myArea.workflowDetail.approvalSlotNotFound')} hint={false} />;
  }

  const badge = slotStatusBadge(slot, t);
  const groupName = slotGroupName(slot);
  const status = normalizeWorkflowStatus(wf.status);
  const showActionSurface = hasActions && (status === 'OPEN' || status === 'AWAITING_INPUT_CHANGE');
  const typeLabel = t(workflowTypeTranslationKey(wf.workflowTypeId));
  const openerName =
    typeof wf.openedByDisplayName === 'string' && wf.openedByDisplayName.trim() !== ''
      ? wf.openedByDisplayName.trim()
      : t('common.notAvailable');

  const actionProps = {
    isClaimable,
    isDecidable,
    isUnclaimable,
    isWithdrawable,
    actionPending,
    claimLoading: claimMutation.isPending,
    decideLoading: decideMutation.isPending,
    unclaimLoading: unclaimMutation.isPending,
    withdrawLoading: withdrawApprovalMutation.isPending,
    onClaim: () => void handleClaim(),
    onApprove: () => setApproveOpen(true),
    onReject: () => {
      setRejectComment('');
      setRejectOpen(true);
    },
    onUnclaim: () => void handleUnclaim(),
    onWithdrawApproval: () => void handleWithdrawApproval(),
    mapsRejectToInputChange: wf.mapsRejectToInputChange === true,
  };

  const taskTitle = isClaimable
    ? t('myArea.workflowDetail.approvalTaskClaimTitle')
    : isDecidable
      ? t('myArea.workflowDetail.approvalTaskDecideTitle')
      : t('myArea.workflowDetail.approvalSlotDetailSection');

  const taskHint = isClaimable
    ? t('myArea.workflowDetail.claimSlotHint')
    : isDecidable
      ? t('myArea.workflowDetail.claimedByYouHint')
      : null;

  return (
    <div className="w-full space-y-3 sm:space-y-4">
      <PageHeader
        title={typeLabel}
        size="compact"
        action={
          <Badge tone={badge.tone} pill className="py-px">
            {badge.label}
          </Badge>
        }
      />

      {showActionSurface ? (
        <Card className="space-y-2 border-[var(--status-warning-accent)]/35 bg-[var(--status-warning-surface)] p-3 sm:p-4">
          <p className="text-sm font-semibold text-[var(--text-primary)]">{taskTitle}</p>
          {taskHint ? <p className="text-sm text-[var(--text-secondary)]">{taskHint}</p> : null}
          <p className="text-sm text-[var(--text-secondary)]">
            {t('myArea.workflows.fields.openedBy', { name: openerName })}
          </p>
        </Card>
      ) : null}

      <Card className="space-y-3 p-3 sm:p-4">
        <h2 className="text-sm font-semibold text-[var(--text-primary)]">
          {t('myArea.workflowDetail.approvalSlotDetailSection')}
        </h2>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <FieldDisplay
            label={t('myArea.workflowDetail.approvalSlotOrganization')}
            value={slotOrgName(slot, t)}
          />
          {groupName ? (
            <FieldDisplay label={t('myArea.workflowDetail.approvalSlotGroup')} value={groupName} />
          ) : null}
          <div>
            <FormLabel>{t('myArea.workflowDetail.approvalSlotStatus')}</FormLabel>
            <Badge tone={badge.tone} pill className="py-px">
              {badge.label}
            </Badge>
          </div>
        </div>
        <div className="text-sm text-[var(--text-secondary)]">
          {buildSlotSubtitle(slot, decision, t, self)}
        </div>
      </Card>

      <List label={t('myArea.workflowDetail.reviewWorkflowLinkTitle')}>
        <ListRow
          href={workflowDetailPath}
          leading={
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[var(--surface-soft)] text-[var(--text-secondary)]">
              <FileText className="h-4 w-4" aria-hidden />
            </span>
          }
          title={t('myArea.workflowDetail.reviewWorkflowLinkTitle')}
          subtitle={t('myArea.workflowDetail.reviewWorkflowLinkHint')}
        />
      </List>

      {showActionSurface ? (
        <>
          <div className="hidden sm:block">
            <WorkflowApprovalActionsSection {...actionProps} layout="card" />
          </div>
          {/* Spacer so content clears the sticky dock + bottom nav on phones. */}
          <div className="h-28 sm:hidden" aria-hidden />
          <div
            className="fixed inset-x-0 z-30 border-t border-[var(--border-color)] bg-[var(--card-bg)]/95 px-4 py-3 backdrop-blur-md sm:hidden"
            style={{
              bottom: 'calc(4rem + 1px + env(safe-area-inset-bottom, 0px))',
            }}
          >
            <WorkflowApprovalActionsSection {...actionProps} layout="dock" />
          </div>
        </>
      ) : null}

      <WorkflowApproveConfirmDialog
        isOpen={approveOpen}
        isLoading={decideMutation.isPending || updateDocumentPromotionsMutation.isPending}
        items={promotionItems}
        selectedKeys={promotionSelectedKeys}
        onClose={() => setApproveOpen(false)}
        onConfirm={() => void handleApproveConfirm()}
      />

      <Modal
        isOpen={rejectOpen}
        onClose={() => setRejectOpen(false)}
        title={t(
          wf.mapsRejectToInputChange
            ? 'myArea.workflowDetail.requestInputChangeTitle'
            : 'myArea.workflowDetail.rejectModalTitle',
        )}
        size="sm"
        footer={
          <ModalFooter
            onCancel={() => setRejectOpen(false)}
            onSave={() => void handleRejectSubmit()}
            isValid={!wf.mapsRejectToInputChange || rejectComment.trim().length > 0}
            isLoading={decideMutation.isPending}
            saveText={t(
              wf.mapsRejectToInputChange
                ? 'myArea.workflowDetail.requestInputChangeSubmit'
                : 'myArea.workflowDetail.rejectConfirm',
            )}
          />
        }
      >
        <div className="space-y-4">
          <p className="text-sm text-[var(--text-secondary)]">
            {t(
              wf.mapsRejectToInputChange
                ? 'myArea.workflowDetail.requestInputChangeIntro'
                : 'myArea.workflowDetail.rejectModalIntro',
            )}
          </p>
          <Textarea
            label={
              wf.mapsRejectToInputChange
                ? t('myArea.workflowDetail.requestInputChangeMessageLabel')
                : undefined
            }
            value={rejectComment}
            onChange={(e) => setRejectComment(e.target.value)}
            placeholder={t(
              wf.mapsRejectToInputChange
                ? 'myArea.workflowDetail.requestInputChangePlaceholder'
                : 'myArea.workflowDetail.rejectCommentPlaceholder',
            )}
            rows={4}
            maxLength={2000}
            className="w-full"
            required={wf.mapsRejectToInputChange === true}
          />
        </div>
      </Modal>
    </div>
  );
}
