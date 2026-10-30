/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { PencilLine } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useCallback, useMemo, useState } from 'react';
import { WorkflowApprovalActionsSection } from '@/components/my-area/workflows/WorkflowApprovalActionsSection';
import { WorkflowApproveConfirmDialog } from '@/components/my-area/workflows/WorkflowApproveConfirmDialog';
import { WorkflowCommentsSection } from '@/components/my-area/workflows/WorkflowCommentsSection';
import { WorkflowEditInputModalHost } from '@/components/my-area/workflows/WorkflowEditInputModalHost';
import { WorkflowInputChangeRequestNote } from '@/components/my-area/workflows/WorkflowInputChangeRequestNote';
import { WorkflowMediaSection } from '@/components/my-area/workflows/WorkflowMediaSection';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { FieldDisplay } from '@/components/ui/field-display';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { ErrorState, NotFoundState } from '@/components/ui/message-cards';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { PageHeader } from '@/components/ui/page-header';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useAuth } from '@/lib/auth/AuthContext';
import {
  getMyAreaOnboardingDetailPath,
  getMyAreaWorkflowInboxPath,
} from '@/lib/my-area/myAreaSections';
import { buildWorkflowTopic, useRealtimeTopic } from '@/lib/realtime';
import {
  useCancelWorkflow,
  useClaimWorkflowParticipant,
  useRequestWorkflowInputChange,
  useSelf,
  useSubmitWorkflowDecision,
  useUnclaimWorkflowParticipant,
  useUpdateWorkflowDocumentPromotions,
  useWithdrawWorkflowInputChange,
  useWorkflow,
  useWorkflowAttachments,
  useWorkflowDocumentPromotionPreview,
} from '@/lib/store';
import { formatDateTime } from '@/lib/utils/date';
import { isActiveMemberOfOrganizationGroup } from '@/lib/utils/membership';
import {
  blocksSelfApprove,
  isUnclaimableApproverSlot,
  listActionableApproverSlots,
  normalizeWorkflowStatus,
  slotGroupName,
  slotHandlerStatusLine,
  slotListRowTone,
  slotOrgName,
  slotStatusBadge,
  sortApproverSlots,
} from '@/lib/utils/workflowApprovalSlot';
import {
  buildWorkflowDocumentPromotionSelections,
  selectedKeysFromPreviewItems,
  workflowPromotionKey,
} from '@/lib/utils/workflowDocumentPromotion';
import {
  workflowParticipationListStatusLabel,
  workflowTypeTranslationKey,
} from '@/lib/utils/workflowLabels';
import { isWorkflowStatusActive } from '@/lib/utils/workflowOpen';
import {
  WorkflowInputDisplay,
  workflowInputPayloadCountsAsProof,
} from '@/lib/workflows/inputDisplay';
import { parseOnboardingSubmissionPayload } from '@/lib/workflows/inputDisplay/payloadParsers';
import { resolveWorkflowContextOrganizationId } from '@/lib/workflows/inputDisplay/resolveWorkflowContextOrganizationId';
import type { UserOrganizationGroupMembershipDto } from '@/types/user';
import {
  WORKFLOW_TYPE_IDS,
  type WorkflowDto,
  type WorkflowMediaSource,
  type WorkflowParticipantDto,
} from '@/types/workflow';

export interface WorkflowDetailViewProps {
  workflowId: number;
}

function isApproverParticipant(
  p: WorkflowParticipantDto,
  wf: WorkflowDto,
  userId: number,
  groupMemberships: UserOrganizationGroupMembershipDto[] | undefined,
): boolean {
  const st = normalizeWorkflowStatus(wf.status);
  if (st !== 'OPEN' || blocksSelfApprove(p, wf, userId)) return false;
  if (p.role !== 'APPROVER') return false;
  if (p.principalType === 'ORGANIZATION_GROUP') {
    return isActiveMemberOfOrganizationGroup(groupMemberships, p.principalId);
  }
  return p.principalId === userId;
}

export function WorkflowDetailView({ workflowId }: WorkflowDetailViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const { tenantId } = useAuth();
  const workflowTopic = tenantId != null ? buildWorkflowTopic(tenantId, workflowId) : null;
  useRealtimeTopic(workflowTopic);
  const { showError, showSuccess } = useToast();
  const { data: self } = useSelf();
  const { data: wf, isLoading, error } = useWorkflow(workflowId);
  const { data: attachments = [], isLoading: attachmentsLoading } =
    useWorkflowAttachments(workflowId);
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
  const updateDocumentPromotionsMutation = useUpdateWorkflowDocumentPromotions();

  const claimMutation = useClaimWorkflowParticipant();
  const unclaimMutation = useUnclaimWorkflowParticipant();
  const decideMutation = useSubmitWorkflowDecision();
  const cancelMutation = useCancelWorkflow();
  const requestInputChangeMutation = useRequestWorkflowInputChange();
  const withdrawInputChangeMutation = useWithdrawWorkflowInputChange();

  const [cancelOpen, setCancelOpen] = useState(false);
  const [withdrawInputChangeOpen, setWithdrawInputChangeOpen] = useState(false);
  const [rejectOpen, setRejectOpen] = useState(false);
  const [rejectParticipantId, setRejectParticipantId] = useState<number | null>(null);
  const [rejectComment, setRejectComment] = useState('');
  const [approveOpen, setApproveOpen] = useState(false);
  const [approveParticipantId, setApproveParticipantId] = useState<number | null>(null);
  const [editInputOpen, setEditInputOpen] = useState(false);
  const [requestChangeOpen, setRequestChangeOpen] = useState(false);
  const [requestChangeMessage, setRequestChangeMessage] = useState('');
  const [selectedApprovalSlotId, setSelectedApprovalSlotId] = useState<number | null>(null);
  const currentUserId = self?.id;

  const participants = useMemo(() => wf?.participants ?? [], [wf?.participants]);

  const groupMemberships = self?.organizationGroupMemberships;

  const isOpener = currentUserId != null && wf != null && wf.openedByUserId === currentUserId;
  const status = wf ? normalizeWorkflowStatus(wf.status) : 'OPEN';

  const approverSlots = useMemo(
    () => participants.filter((p) => p.role === 'APPROVER'),
    [participants],
  );

  const sortedApproverSlots = useMemo(() => sortApproverSlots(approverSlots), [approverSlots]);

  const unclaimableSlotIds =
    !wf || currentUserId == null
      ? new Set<number>()
      : new Set(
          participants
            .filter((p) => isUnclaimableApproverSlot(p, wf, currentUserId))
            .map((p) => p.id),
        );

  const showApprovalSection = sortedApproverSlots.length > 0;

  const canCommentOnWorkflow = isWorkflowStatusActive(status);
  const commentsOrganizationId = wf != null ? resolveWorkflowContextOrganizationId(wf) : null;
  const canEditDocumentPromotion = useMemo(() => {
    if (!wf || currentUserId == null || status !== 'OPEN') return false;
    return participants.some((p) => isApproverParticipant(p, wf, currentUserId, groupMemberships));
  }, [wf, currentUserId, status, participants, groupMemberships]);
  const showDocumentPromotionState = status === 'OPEN' && promotionItems.length > 0;
  const canEditInput = wf?.canEditInput === true;
  const canRequestInputChange = wf?.canRequestInputChange === true;
  const canWithdrawInputChange = wf?.canWithdrawInputChange === true;
  const mapsRejectToInputChange = wf?.mapsRejectToInputChange === true;
  const isAwaitingInputChange = status === 'AWAITING_INPUT_CHANGE';
  const isActiveWorkflow = isWorkflowStatusActive(status);
  const onboardingInvitationId =
    wf?.workflowTypeId === WORKFLOW_TYPE_IDS.ONBOARDING_SUBMISSION
      ? parseOnboardingSubmissionPayload(wf.payload)?.invitationId
      : undefined;

  const toggleDocumentPromotion = useCallback(
    (source: WorkflowMediaSource, attachmentId: number) => {
      const key = workflowPromotionKey(source, attachmentId);
      const nextKeys = new Set(promotionSelectedKeys);
      if (nextKeys.has(key)) {
        nextKeys.delete(key);
      } else {
        nextKeys.add(key);
      }
      const selections = buildWorkflowDocumentPromotionSelections(promotionItems, nextKeys);
      updateDocumentPromotionsMutation.mutate({ workflowId, selections });
    },
    [promotionItems, promotionSelectedKeys, updateDocumentPromotionsMutation, workflowId],
  );

  const handleClaim = async (participantId: number) => {
    try {
      await claimMutation.mutateAsync({ workflowId, participantId });
      showSuccess(t('myArea.workflowDetail.claimSuccess'));
    } catch (e) {
      showError(e instanceof Error ? e.message : t('myArea.error.loadFailed'));
    }
  };

  const handleUnclaim = async (participantId: number) => {
    try {
      await unclaimMutation.mutateAsync({ workflowId, participantId });
      showSuccess(t('myArea.workflowDetail.unclaimSuccess'));
    } catch (e) {
      showError(e instanceof Error ? e.message : t('myArea.error.loadFailed'));
    }
  };

  const openApprove = (participantId: number) => {
    setApproveParticipantId(participantId);
    setApproveOpen(true);
  };

  const handleApproveConfirm = async () => {
    if (approveParticipantId == null) return;
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
        body: { participantId: approveParticipantId, decision: 'APPROVED' },
      });
      showSuccess(t('myArea.workflowDetail.approveSuccess'));
      setApproveOpen(false);
      setApproveParticipantId(null);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('myArea.error.loadFailed'));
    }
  };

  const openReject = (participantId: number) => {
    setRejectParticipantId(participantId);
    setRejectComment('');
    setRejectOpen(true);
  };

  const handleRejectSubmit = async () => {
    if (rejectParticipantId == null) return;
    const comment = rejectComment.trim();
    if (mapsRejectToInputChange && !comment) return;
    try {
      await decideMutation.mutateAsync({
        workflowId,
        body: {
          participantId: rejectParticipantId,
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
      setRejectParticipantId(null);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('myArea.error.loadFailed'));
    }
  };

  const handleCancelConfirm = async () => {
    try {
      await cancelMutation.mutateAsync(workflowId);
      showSuccess(t('myArea.workflowDetail.cancelSuccess'));
      setCancelOpen(false);
      router.push(getMyAreaWorkflowInboxPath());
    } catch (e) {
      showError(e instanceof Error ? e.message : t('myArea.error.loadFailed'));
    }
  };

  const handleRequestInputChange = async () => {
    const message = requestChangeMessage.trim();
    if (!message) return;
    try {
      await requestInputChangeMutation.mutateAsync({
        workflowId,
        message,
      });
      showSuccess(t('myArea.workflowDetail.requestInputChangeSuccess'));
      setRequestChangeOpen(false);
      setRequestChangeMessage('');
    } catch (e) {
      showError(e instanceof Error ? e.message : t('myArea.error.loadFailed'));
    }
  };

  const handleWithdrawInputChangeConfirm = async () => {
    try {
      await withdrawInputChangeMutation.mutateAsync(workflowId);
      showSuccess(t('myArea.workflowDetail.withdrawInputChangeSuccess'));
      setWithdrawInputChangeOpen(false);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('myArea.error.loadFailed'));
    }
  };

  const actionableSlots = useMemo(
    () =>
      wf != null && currentUserId != null && status === 'OPEN'
        ? listActionableApproverSlots(wf, currentUserId, groupMemberships)
        : [],
    [wf, currentUserId, groupMemberships, status],
  );

  const selectedActionable = useMemo(() => {
    if (actionableSlots.length === 0) return null;
    return (
      actionableSlots.find((entry) => entry.slot.id === selectedApprovalSlotId) ??
      actionableSlots[0]
    );
  }, [actionableSlots, selectedApprovalSlotId]);

  const formatSlotLabel = useCallback(
    (slot: (typeof actionableSlots)[number]['slot']) => {
      const org = slotOrgName(slot, t);
      const group = slotGroupName(slot);
      return group ? `${org} · ${group}` : org;
    },
    [t],
  );

  const slotOptions = useMemo(
    () =>
      actionableSlots.map((entry) => ({
        id: entry.slot.id,
        label: formatSlotLabel(entry.slot),
        kind: entry.kind,
      })),
    [actionableSlots, formatSlotLabel],
  );

  if (isLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (error) {
    return <ErrorState message={t('myArea.workflowDetail.loadFailed')} />;
  }

  if (!wf) {
    return <NotFoundState message={t('myArea.workflowDetail.notFound')} hint={false} />;
  }

  const statusBadge = workflowParticipationListStatusLabel(wf, t);
  const typeLabel = t(workflowTypeTranslationKey(wf.workflowTypeId));

  const openerLabel = wf.openedByDisplayName?.trim() || t('common.formerMember');

  const showProofs = workflowInputPayloadCountsAsProof(wf) || attachments.length > 0;

  const actionableParticipantId = selectedActionable?.slot.id ?? null;
  const isActionableClaimable = selectedActionable?.kind === 'claim';
  const isActionableDecidable = selectedActionable?.kind === 'decide';
  const isActionableUnclaimable =
    actionableParticipantId != null && unclaimableSlotIds.has(actionableParticipantId);
  const showPrimaryApprovalActions =
    actionableSlots.length > 0 &&
    (isActionableClaimable ||
      isActionableDecidable ||
      isActionableUnclaimable ||
      actionableSlots.length > 1);
  const primaryActionPending =
    claimMutation.isPending || unclaimMutation.isPending || decideMutation.isPending;

  const primaryActionProps =
    actionableParticipantId == null && actionableSlots.length <= 1
      ? null
      : {
          isClaimable: isActionableClaimable,
          isDecidable: isActionableDecidable,
          isUnclaimable: isActionableUnclaimable,
          isWithdrawable: false,
          actionPending: primaryActionPending,
          claimLoading: claimMutation.isPending,
          decideLoading: decideMutation.isPending,
          unclaimLoading: unclaimMutation.isPending,
          onClaim: () => {
            if (actionableParticipantId != null) void handleClaim(actionableParticipantId);
          },
          onApprove: () => {
            if (actionableParticipantId != null) openApprove(actionableParticipantId);
          },
          onReject: () => {
            if (actionableParticipantId != null) openReject(actionableParticipantId);
          },
          onUnclaim: () => {
            if (actionableParticipantId != null) void handleUnclaim(actionableParticipantId);
          },
          onWithdrawApproval: () => undefined,
          mapsRejectToInputChange,
          slotLabel:
            selectedActionable && actionableSlots.length === 1
              ? formatSlotLabel(selectedActionable.slot)
              : null,
          slotOptions: actionableSlots.length > 1 ? slotOptions : undefined,
          selectedSlotId: actionableParticipantId,
          onSelectSlot: setSelectedApprovalSlotId,
        };

  return (
    <div className="w-full space-y-3 sm:space-y-4">
      <PageHeader
        title={`${typeLabel} · #${wf.id}`}
        size="compact"
        action={
          <Badge tone={statusBadge.tone} pill>
            {statusBadge.label}
          </Badge>
        }
      />

      {showPrimaryApprovalActions && primaryActionProps ? (
        <WorkflowApprovalActionsSection {...primaryActionProps} layout="card" />
      ) : null}

      {isAwaitingInputChange ? (
        <div className="space-y-3 rounded-xl border border-[var(--toast-warning-border)] bg-[var(--toast-warning-bg)] p-3 sm:p-4">
          <div className="flex gap-3">
            <div
              className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[var(--status-warning-surface)] text-[var(--status-warning-accent)]"
              aria-hidden
            >
              <PencilLine className="h-4 w-4" />
            </div>
            <div className="min-w-0 flex-1 space-y-3">
              <div className="space-y-1">
                <p className="font-semibold text-[var(--text-primary)]">
                  {t('myArea.workflowDetail.awaitingInputChangeTitle')}
                </p>
                <p className="text-sm text-[var(--text-secondary)]">
                  {t('myArea.workflowDetail.awaitingInputChangeBody')}
                </p>
              </div>
              {wf.activeInputChange?.message ? (
                <WorkflowInputChangeRequestNote message={wf.activeInputChange.message} />
              ) : null}
              {canEditInput ? (
                <Button
                  type="button"
                  size="sm"
                  fullWidth="responsive"
                  icon={<PencilLine className="h-4 w-4" aria-hidden />}
                  onClick={() => {
                    if (onboardingInvitationId != null) {
                      router.push(getMyAreaOnboardingDetailPath(onboardingInvitationId));
                      return;
                    }
                    setEditInputOpen(true);
                  }}
                >
                  {t('myArea.workflowDetail.awaitingInputChangeAction')}
                </Button>
              ) : null}
            </div>
          </div>
        </div>
      ) : null}

      <Card className="space-y-3 p-3 sm:p-4">
        <h2 className="text-sm font-semibold text-[var(--text-primary)]">
          {t('myArea.workflowDetail.sectionOverview')}
        </h2>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <FieldDisplay label={t('myArea.workflowDetail.openedBy')} value={openerLabel} />
          {wf.decidedAt ? (
            <FieldDisplay
              label={t('myArea.workflowDetail.decidedAt')}
              value={formatDateTime(wf.decidedAt)}
            />
          ) : null}
        </div>
      </Card>

      <WorkflowInputDisplay
        workflow={wf}
        self={
          self
            ? {
                id: self.id,
                firstName: self.firstName,
                lastName: self.lastName,
                contactInformation: self.contactInformation,
              }
            : undefined
        }
      />

      <WorkflowMediaSection
        workflowId={workflowId}
        workflowAttachments={attachments}
        attachmentsLoading={attachmentsLoading}
        showProofs={showProofs}
        variant="all"
        promotionItems={promotionItems}
        selectedPromotionKeys={promotionSelectedKeys}
        onTogglePromotion={canEditDocumentPromotion ? toggleDocumentPromotion : undefined}
        canEditPromotion={canEditDocumentPromotion}
        promotionSaving={updateDocumentPromotionsMutation.isPending}
        showPromotionState={showDocumentPromotionState}
      />

      {showApprovalSection ? (
        <Card className="space-y-3 p-3 sm:p-4">
          <h2 className="text-sm font-semibold text-[var(--text-primary)]">
            {t('myArea.workflowDetail.approvalSectionTitle')}
          </h2>
          <ListContainer variant="inset" label={t('myArea.workflowDetail.approvalSectionTitle')}>
            {sortedApproverSlots.map((slot) => {
              const badge = slotStatusBadge(slot, t);
              const groupName = slotGroupName(slot);

              return (
                <ListRow
                  key={slot.id}
                  tone={slotListRowTone(slot)}
                  title={slotOrgName(slot, t)}
                  badges={
                    <>
                      {groupName ? (
                        <Badge tone="neutral" className="py-px text-[10px]">
                          {groupName}
                        </Badge>
                      ) : null}
                      <Badge tone={badge.tone} pill className="py-px text-[10px]">
                        {badge.label}
                      </Badge>
                    </>
                  }
                  subtitle={slotHandlerStatusLine(slot, t)}
                />
              );
            })}
          </ListContainer>
        </Card>
      ) : null}

      <WorkflowCommentsSection
        workflowId={workflowId}
        organizationId={commentsOrganizationId}
        canComment={canCommentOnWorkflow}
      />

      {canEditInput && !isAwaitingInputChange ? (
        <Card className="p-3 sm:p-4">
          <div className="flex flex-col gap-3">
            <p className="text-sm text-[var(--text-secondary)]">
              {t('myArea.workflowDetail.editInputHint')}
            </p>
            <Button
              type="button"
              variant="secondary"
              size="sm"
              fullWidth="responsive"
              onClick={() => setEditInputOpen(true)}
            >
              {t('myArea.workflowDetail.editInput')}
            </Button>
          </div>
        </Card>
      ) : null}

      {canRequestInputChange ? (
        <Card className="p-3 sm:p-4">
          <div className="flex flex-col gap-3">
            <p className="text-sm text-[var(--text-secondary)]">
              {t('myArea.workflowDetail.requestInputChangeHint')}
            </p>
            <Button
              type="button"
              variant="outline"
              size="sm"
              fullWidth="responsive"
              onClick={() => {
                setRequestChangeMessage('');
                setRequestChangeOpen(true);
              }}
            >
              {t('myArea.workflowDetail.requestInputChange')}
            </Button>
          </div>
        </Card>
      ) : null}

      {canWithdrawInputChange ? (
        <Card className="p-3 sm:p-4">
          <div className="flex flex-col gap-3">
            <p className="text-sm text-[var(--text-secondary)]">
              {t('myArea.workflowDetail.withdrawInputChangeHint')}
            </p>
            <Button
              type="button"
              variant="outline"
              size="sm"
              fullWidth="responsive"
              onClick={() => setWithdrawInputChangeOpen(true)}
            >
              {t('myArea.workflowDetail.withdrawInputChange')}
            </Button>
          </div>
        </Card>
      ) : null}

      {isOpener && isActiveWorkflow ? (
        <Card className="p-3 sm:p-4">
          <div className="flex flex-col gap-3">
            <p className="text-sm text-[var(--text-secondary)]">
              {t('myArea.workflowDetail.cancelHint')}
            </p>
            <Button
              type="button"
              variant="outline"
              size="sm"
              fullWidth="responsive"
              onClick={() => setCancelOpen(true)}
            >
              {t('myArea.workflowDetail.cancelWorkflow')}
            </Button>
          </div>
        </Card>
      ) : null}

      <WorkflowEditInputModalHost
        workflow={wf}
        isOpen={editInputOpen}
        onClose={() => setEditInputOpen(false)}
      />

      <ConfirmDialog
        isOpen={cancelOpen}
        onClose={() => setCancelOpen(false)}
        onConfirm={() => void handleCancelConfirm()}
        title={t('myArea.workflowDetail.cancelConfirmTitle')}
        message={t('myArea.workflowDetail.cancelConfirmMessage')}
        confirmText={t('myArea.workflowDetail.cancelWorkflow')}
        isLoading={cancelMutation.isPending}
        variant="warning"
      />

      <ConfirmDialog
        isOpen={withdrawInputChangeOpen}
        onClose={() => setWithdrawInputChangeOpen(false)}
        onConfirm={() => void handleWithdrawInputChangeConfirm()}
        title={t('myArea.workflowDetail.withdrawInputChangeConfirmTitle')}
        message={t('myArea.workflowDetail.withdrawInputChangeConfirmMessage')}
        confirmText={t('myArea.workflowDetail.withdrawInputChangeConfirmAction')}
        isLoading={withdrawInputChangeMutation.isPending}
        variant="warning"
      />

      <Modal
        isOpen={requestChangeOpen}
        onClose={() => setRequestChangeOpen(false)}
        title={t('myArea.workflowDetail.requestInputChangeTitle')}
        size="sm"
        footer={
          <ModalFooter
            onCancel={() => setRequestChangeOpen(false)}
            onSave={() => void handleRequestInputChange()}
            isValid={requestChangeMessage.trim().length > 0}
            isLoading={requestInputChangeMutation.isPending}
            saveText={t('myArea.workflowDetail.requestInputChangeSubmit')}
          />
        }
      >
        <div className="space-y-4">
          <p className="text-sm text-[var(--text-secondary)]">
            {t('myArea.workflowDetail.requestInputChangeIntro')}
          </p>
          <Textarea
            label={t('myArea.workflowDetail.requestInputChangeMessageLabel')}
            value={requestChangeMessage}
            onChange={(e) => setRequestChangeMessage(e.target.value)}
            placeholder={t('myArea.workflowDetail.requestInputChangePlaceholder')}
            rows={4}
            maxLength={2000}
            className="w-full"
            required
          />
        </div>
      </Modal>

      <WorkflowApproveConfirmDialog
        isOpen={approveOpen}
        isLoading={decideMutation.isPending || updateDocumentPromotionsMutation.isPending}
        items={promotionItems}
        selectedKeys={promotionSelectedKeys}
        onClose={() => {
          setApproveOpen(false);
          setApproveParticipantId(null);
        }}
        onConfirm={() => void handleApproveConfirm()}
      />

      <Modal
        isOpen={rejectOpen}
        onClose={() => setRejectOpen(false)}
        title={t(
          mapsRejectToInputChange
            ? 'myArea.workflowDetail.requestInputChangeTitle'
            : 'myArea.workflowDetail.rejectModalTitle',
        )}
        size="sm"
        footer={
          <ModalFooter
            onCancel={() => setRejectOpen(false)}
            onSave={() => void handleRejectSubmit()}
            isValid={!mapsRejectToInputChange || rejectComment.trim().length > 0}
            isLoading={decideMutation.isPending}
            saveText={t(
              mapsRejectToInputChange
                ? 'myArea.workflowDetail.requestInputChangeSubmit'
                : 'myArea.workflowDetail.rejectConfirm',
            )}
          />
        }
      >
        <div className="space-y-4">
          <p className="text-sm text-[var(--text-secondary)]">
            {t(
              mapsRejectToInputChange
                ? 'myArea.workflowDetail.requestInputChangeIntro'
                : 'myArea.workflowDetail.rejectModalIntro',
            )}
          </p>
          <Textarea
            label={
              mapsRejectToInputChange
                ? t('myArea.workflowDetail.requestInputChangeMessageLabel')
                : undefined
            }
            value={rejectComment}
            onChange={(e) => setRejectComment(e.target.value)}
            placeholder={t(
              mapsRejectToInputChange
                ? 'myArea.workflowDetail.requestInputChangePlaceholder'
                : 'myArea.workflowDetail.rejectCommentPlaceholder',
            )}
            rows={4}
            maxLength={2000}
            className="w-full"
            required={mapsRejectToInputChange}
          />
        </div>
      </Modal>
    </div>
  );
}
