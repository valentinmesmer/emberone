/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Eye, Image as ImageIcon } from 'lucide-react';
import { useMemo } from 'react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { CheckboxControl } from '@/components/ui/checkbox';
import { List, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { useTranslation } from '@/hooks/useTranslation';
import { useViewAttachment } from '@/hooks/useViewAttachment';
import { workflowsApi } from '@/lib/api/workflows';
import { useWorkflowComments } from '@/lib/store';
import { workflowPromotionKey } from '@/lib/utils/workflowDocumentPromotion';
import type { EntityAttachmentDto } from '@/types/attachment';
import type {
  WorkflowCommentDto,
  WorkflowDocumentPromotionItemDto,
  WorkflowMediaSource,
} from '@/types/workflow';

interface CommentMediaItem {
  commentId: number;
  authorName: string;
  attachment: EntityAttachmentDto;
}

type WorkflowMediaVariant = 'all' | 'proofs' | 'discussion';

interface WorkflowMediaSectionProps {
  workflowId: number;
  workflowAttachments: EntityAttachmentDto[];
  attachmentsLoading: boolean;
  showProofs: boolean;
  variant?: WorkflowMediaVariant;
  promotionItems?: WorkflowDocumentPromotionItemDto[];
  selectedPromotionKeys?: ReadonlySet<string>;
  onTogglePromotion?: (source: WorkflowMediaSource, attachmentId: number) => void;
  canEditPromotion?: boolean;
  promotionSaving?: boolean;
  showPromotionState?: boolean;
}

function AttachmentRow({
  fileName,
  subtitle,
  attachmentId,
  source,
  viewingAttachmentId,
  onView,
  isSelectedForUpload,
  showPromotionState,
  canEditPromotion,
  promotionSaving,
  onTogglePromotion,
}: {
  fileName: string;
  subtitle?: string;
  attachmentId: number;
  source?: WorkflowMediaSource;
  viewingAttachmentId: number | null;
  onView: (attachmentId: number) => void;
  isSelectedForUpload?: boolean;
  showPromotionState?: boolean;
  canEditPromotion?: boolean;
  promotionSaving?: boolean;
  onTogglePromotion?: (source: WorkflowMediaSource, attachmentId: number) => void;
}) {
  const t = useTranslation();
  const showUploadState = showPromotionState && source != null;
  const selected = showUploadState && isSelectedForUpload === true;

  return (
    <ListRow
      compact
      accent={showUploadState}
      tone={selected ? 'success' : showUploadState ? 'neutral' : 'default'}
      keepActionsOnMobile
      leading={
        canEditPromotion && source != null ? (
          <CheckboxControl
            size="sm"
            className="shrink-0"
            checked={isSelectedForUpload === true}
            disabled={promotionSaving}
            onCheckedChange={() => onTogglePromotion?.(source, attachmentId)}
            label={t('myArea.workflowDetail.documentPromotionToggle', { name: fileName })}
          />
        ) : undefined
      }
      title={fileName}
      subtitle={subtitle}
      badges={
        showUploadState ? (
          <Badge tone={selected ? 'success' : 'neutral'} className="py-px text-[10px] leading-none">
            {selected
              ? t('myArea.workflowDetail.documentPromotionWillUpload')
              : t('myArea.workflowDetail.documentPromotionWillSkip')}
          </Badge>
        ) : undefined
      }
      actions={
        <Button
          type="button"
          variant="ghost"
          size="xs"
          className="h-7 w-7 min-w-7 shrink-0 p-0"
          isLoading={viewingAttachmentId === attachmentId}
          onClick={() => onView(attachmentId)}
          aria-label={t('myArea.workflowDetail.viewAttachment')}
          title={t('myArea.workflowDetail.viewAttachment')}
        >
          <Eye className="h-3.5 w-3.5" aria-hidden />
        </Button>
      }
    />
  );
}

function commentAuthorName(
  comment: WorkflowCommentDto,
  t: (key: string, params?: Record<string, string | number>) => string,
): string {
  return (
    comment.userDisplayName?.trim() ||
    `${comment.userFirstName ?? ''} ${comment.userLastName ?? ''}`.trim() ||
    (comment.userId == null
      ? t('common.formerMember')
      : t('myArea.workflowDetail.commentAuthorFallback', { id: comment.userId }))
  );
}

export function WorkflowMediaSection({
  workflowId,
  workflowAttachments,
  attachmentsLoading,
  showProofs,
  variant = 'all',
  promotionItems = [],
  selectedPromotionKeys,
  onTogglePromotion,
  canEditPromotion = false,
  promotionSaving = false,
  showPromotionState = false,
}: WorkflowMediaSectionProps) {
  const t = useTranslation();
  const { view, viewingKey } = useViewAttachment();
  const { data: comments = [], isLoading: commentsLoading } = useWorkflowComments(workflowId);
  const viewingAttachmentId = viewingKey != null ? Number(viewingKey) : null;

  const promotionByKey = useMemo(() => {
    const map = new Map<string, WorkflowDocumentPromotionItemDto>();
    for (const item of promotionItems) {
      map.set(workflowPromotionKey(item.source, item.attachmentId), item);
    }
    return map;
  }, [promotionItems]);

  const isSelectedForUpload = (source: WorkflowMediaSource, attachmentId: number): boolean => {
    if (selectedPromotionKeys != null) {
      return selectedPromotionKeys.has(workflowPromotionKey(source, attachmentId));
    }
    const item = promotionByKey.get(workflowPromotionKey(source, attachmentId));
    return item?.selected === true;
  };

  const commentMedia = useMemo<CommentMediaItem[]>(
    () =>
      comments.flatMap((comment) =>
        comment.attachment
          ? [
              {
                commentId: comment.id,
                authorName: commentAuthorName(comment, t),
                attachment: comment.attachment,
              },
            ]
          : [],
      ),
    [comments, t],
  );

  const loading =
    variant === 'proofs'
      ? attachmentsLoading
      : variant === 'discussion'
        ? commentsLoading
        : attachmentsLoading || commentsLoading;
  const showProofsBlock = showProofs && (variant === 'all' || variant === 'proofs');
  const showDiscussionBlock = variant === 'all' || variant === 'discussion';
  const hasCommentMedia = commentMedia.length > 0;
  const hasWorkflowAttachments = workflowAttachments.length > 0;

  const hasProofContent = showProofsBlock && (hasWorkflowAttachments || attachmentsLoading);
  const hasDiscussionContent = showDiscussionBlock && (hasCommentMedia || commentsLoading);
  const hasContent =
    variant === 'proofs'
      ? hasProofContent
      : variant === 'discussion'
        ? hasDiscussionContent
        : hasProofContent || hasDiscussionContent;

  if (!hasContent) {
    return null;
  }

  const handleViewAttachment = async (attachmentId: number) => {
    await view(String(attachmentId), (targetWindow) =>
      workflowsApi.viewWorkflowAttachment(workflowId, attachmentId, targetWindow),
    );
  };

  const sectionTitle =
    variant === 'proofs'
      ? t('myArea.workflowDetail.mediaProofsTitle')
      : variant === 'discussion'
        ? t('myArea.workflowDetail.mediaDiscussionTitle')
        : t('myArea.workflowDetail.mediaSectionTitle');

  const showPromotionSummary = showPromotionState && promotionItems.length > 0;
  const selectedCount = showPromotionSummary
    ? promotionItems.filter((item) => isSelectedForUpload(item.source, item.attachmentId)).length
    : 0;
  const promotionTotal = promotionItems.length;

  // Group labels only when both blocks are actually on screen - avoids "NACHWEISE" under an already clear title.
  const showGroupLabels =
    variant === 'all' && showProofsBlock && hasWorkflowAttachments && hasCommentMedia;

  return (
    <Card className="space-y-3 !p-4 sm:space-y-3.5">
      <div className="space-y-1.5">
        <div className="flex items-center gap-2">
          <h2 className="flex min-w-0 items-center gap-2 text-sm font-semibold leading-none text-[var(--text-primary)]">
            <ImageIcon className="h-4 w-4 shrink-0 text-[var(--text-secondary)]" aria-hidden />
            <span className="truncate">{sectionTitle}</span>
          </h2>
          {showPromotionSummary ? (
            <Badge
              tone={selectedCount > 0 ? 'success' : 'neutral'}
              pill
              aria-label={t('myArea.workflowDetail.documentPromotionSummary', {
                selected: selectedCount,
                total: promotionTotal,
              })}
              className="h-5 shrink-0 px-2 py-0 text-[11px] leading-none tabular-nums"
            >
              {selectedCount}/{promotionTotal}
            </Badge>
          ) : null}
        </div>

        {showPromotionState && canEditPromotion ? (
          <p className="text-xs text-[var(--text-secondary)]">
            {t('myArea.workflowDetail.documentPromotionEditHint')}
          </p>
        ) : null}
      </div>

      {loading ? (
        <LoadingState variant="embedded" size="sm" />
      ) : (
        <div className="space-y-3">
          {showProofsBlock && hasWorkflowAttachments ? (
            <div className="space-y-1.5">
              {showGroupLabels ? (
                <h3 className="text-xs font-semibold uppercase tracking-wide text-[var(--text-secondary)]">
                  {t('myArea.workflowDetail.mediaProofsTitle')}
                </h3>
              ) : null}
              <List
                variant="inset"
                rowSeparator="inset"
                label={t('myArea.workflowDetail.mediaProofsTitle')}
              >
                {workflowAttachments.map((attachment) => (
                  <AttachmentRow
                    key={attachment.id}
                    fileName={attachment.fileName}
                    attachmentId={attachment.id}
                    source="WORKFLOW"
                    viewingAttachmentId={viewingAttachmentId}
                    onView={handleViewAttachment}
                    isSelectedForUpload={isSelectedForUpload('WORKFLOW', attachment.id)}
                    showPromotionState={
                      showPromotionState &&
                      promotionByKey.has(workflowPromotionKey('WORKFLOW', attachment.id))
                    }
                    canEditPromotion={canEditPromotion}
                    promotionSaving={promotionSaving}
                    onTogglePromotion={onTogglePromotion}
                  />
                ))}
              </List>
            </div>
          ) : null}

          {hasCommentMedia ? (
            <div className="space-y-1.5">
              {showGroupLabels ? (
                <h3 className="text-xs font-semibold uppercase tracking-wide text-[var(--text-secondary)]">
                  {t('myArea.workflowDetail.mediaDiscussionTitle')}
                </h3>
              ) : null}
              <List
                variant="inset"
                rowSeparator="inset"
                label={t('myArea.workflowDetail.mediaDiscussionTitle')}
              >
                {commentMedia.map((item) => (
                  <AttachmentRow
                    key={`${item.commentId}-${item.attachment.id}`}
                    fileName={item.attachment.fileName}
                    subtitle={item.authorName}
                    attachmentId={item.attachment.id}
                    source="COMMENT"
                    viewingAttachmentId={viewingAttachmentId}
                    onView={handleViewAttachment}
                    isSelectedForUpload={isSelectedForUpload('COMMENT', item.attachment.id)}
                    showPromotionState={
                      showPromotionState &&
                      promotionByKey.has(workflowPromotionKey('COMMENT', item.attachment.id))
                    }
                    canEditPromotion={canEditPromotion}
                    promotionSaving={promotionSaving}
                    onTogglePromotion={onTogglePromotion}
                  />
                ))}
              </List>
            </div>
          ) : null}
        </div>
      )}
    </Card>
  );
}
