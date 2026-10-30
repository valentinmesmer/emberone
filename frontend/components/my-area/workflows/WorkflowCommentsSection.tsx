/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { MessageSquare, Paperclip, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { FileInput } from '@/components/ui/file-input';
import { LoadingState } from '@/components/ui/loading';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { UserAvatar } from '@/components/ui/user-avatar';
import { useTranslation } from '@/hooks/useTranslation';
import {
  useCreateWorkflowComment,
  useUploadWorkflowCommentAttachment,
  useWorkflowComments,
} from '@/lib/store';
import type { WorkflowCommentAuthorRole, WorkflowCommentDto } from '@/types/workflow';

const COMMENT_MAX_LENGTH = 4000;

const AUTHOR_ROLE_TONE: Record<WorkflowCommentAuthorRole, 'neutral' | 'accent' | 'warning'> = {
  OPENER: 'accent',
  APPROVER: 'warning',
  VIEWER: 'neutral',
};

const WORKFLOW_FILE_ACCEPT = '.pdf,image/jpeg,image/png,image/gif,image/webp';

interface WorkflowCommentsSectionProps {
  workflowId: number;
  organizationId: number | null;
  canComment: boolean;
}

function authorRoleLabelKey(role: WorkflowCommentAuthorRole): string {
  return `myArea.workflowDetail.commentRole.${role}`;
}

function CommentItem({
  comment,
  organizationId,
}: {
  comment: WorkflowCommentDto;
  organizationId: number | null;
}) {
  const t = useTranslation();
  const displayName =
    comment.userDisplayName?.trim() ||
    `${comment.userFirstName ?? ''} ${comment.userLastName ?? ''}`.trim() ||
    (comment.userId == null
      ? t('common.formerMember')
      : t('myArea.workflowDetail.commentAuthorFallback', { id: comment.userId }));

  return (
    <article className="flex gap-3">
      <UserAvatar
        firstName={comment.userFirstName ?? undefined}
        lastName={comment.userLastName ?? undefined}
        size="md"
        className="mt-0.5"
        organizationId={organizationId ?? undefined}
        userId={comment.userId}
      />
      <div className="min-w-0 flex-1 space-y-1.5">
        <div className="flex flex-wrap items-baseline justify-between gap-x-2 gap-y-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-sm font-semibold text-[var(--text-primary)]">{displayName}</span>
            <Badge
              tone={AUTHOR_ROLE_TONE[comment.authorRole]}
              className="py-px text-[10px] uppercase tracking-wide"
            >
              {t(authorRoleLabelKey(comment.authorRole))}
            </Badge>
          </div>
        </div>
        <p className="whitespace-pre-wrap break-words text-sm text-[var(--text-primary)]">
          {comment.body}
        </p>
      </div>
    </article>
  );
}

export function WorkflowCommentsSection({
  workflowId,
  organizationId,
  canComment,
}: WorkflowCommentsSectionProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();

  const { data: comments = [], isLoading } = useWorkflowComments(workflowId);
  const createMutation = useCreateWorkflowComment();
  const uploadMutation = useUploadWorkflowCommentAttachment();

  const [draft, setDraft] = useState('');
  const [pendingFile, setPendingFile] = useState<File | null>(null);
  const [pendingAttachmentId, setPendingAttachmentId] = useState<number | null>(null);

  const submitting = createMutation.isPending || uploadMutation.isPending;
  const trimmedDraft = draft.trim();
  const canSubmit = canComment && trimmedDraft.length > 0 && !submitting;

  const handleSelectFile = async (file: File) => {
    setPendingFile(file);
    setPendingAttachmentId(null);
    try {
      const uploaded = await uploadMutation.mutateAsync({ workflowId, file });
      setPendingAttachmentId(uploaded.id);
    } catch (err) {
      setPendingFile(null);
      showError(
        err instanceof Error
          ? err.message
          : t('myArea.workflowDetail.commentAttachmentUploadFailed'),
      );
    }
  };

  const clearPendingFile = () => {
    setPendingFile(null);
    setPendingAttachmentId(null);
  };

  const handleSubmit = async () => {
    if (!canSubmit) return;
    try {
      await createMutation.mutateAsync({
        workflowId,
        body: {
          body: trimmedDraft,
          attachmentId: pendingAttachmentId ?? undefined,
        },
      });
      setDraft('');
      clearPendingFile();
      showSuccess(t('myArea.workflowDetail.commentPosted'));
    } catch (err) {
      showError(err instanceof Error ? err.message : t('myArea.workflowDetail.commentPostFailed'));
    }
  };

  return (
    <Card className="space-y-3 p-3 sm:p-4">
      <h2 className="flex items-center gap-2 text-sm font-semibold text-[var(--text-primary)]">
        <MessageSquare className="h-4 w-4 text-[var(--text-secondary)]" aria-hidden />
        {t('myArea.workflowDetail.commentsSectionTitle')}
        {comments.length > 0 ? (
          <span className="font-normal text-[var(--text-secondary)]">({comments.length})</span>
        ) : null}
      </h2>

      {isLoading ? (
        <LoadingState variant="embedded" size="sm" />
      ) : comments.length > 0 ? (
        <ul className="space-y-4">
          {comments.map((comment) => (
            <li key={comment.id}>
              <CommentItem comment={comment} organizationId={organizationId} />
            </li>
          ))}
        </ul>
      ) : !canComment ? (
        <p className="text-sm text-[var(--text-secondary)]">
          {t('myArea.workflowDetail.commentsEmpty')}
        </p>
      ) : null}

      {canComment ? (
        <div className="space-y-2 border-t border-[var(--border-subtle)] pt-3">
          <Textarea
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            placeholder={t('myArea.workflowDetail.commentPlaceholder')}
            rows={3}
            maxLength={COMMENT_MAX_LENGTH}
            disabled={submitting}
          />

          {pendingFile ? (
            <div className="flex items-center justify-between gap-2 rounded-md border border-[var(--border-subtle)] px-2 py-1 text-xs">
              <div className="flex min-w-0 items-center gap-1.5">
                <Paperclip
                  className="h-3.5 w-3.5 shrink-0 text-[var(--text-secondary)]"
                  aria-hidden
                />
                <span className="truncate text-[var(--text-primary)]">{pendingFile.name}</span>
                {uploadMutation.isPending ? (
                  <span className="text-[var(--text-secondary)]">{t('common.loading')}</span>
                ) : null}
              </div>
              <Button
                type="button"
                variant="ghost"
                size="xs"
                className="h-7 w-7 min-w-7 p-0"
                onClick={clearPendingFile}
                disabled={submitting}
                aria-label={t('common.delete')}
              >
                <Trash2 className="h-3.5 w-3.5" aria-hidden />
              </Button>
            </div>
          ) : null}

          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <FileInput
              variant="inline"
              files={[]}
              onChange={(selected) => {
                const file = selected[0];
                if (file) void handleSelectFile(file);
              }}
              accept={WORKFLOW_FILE_ACCEPT}
              chooseLabel={t('myArea.workflowDetail.commentAttachFile')}
              hideFileList
              disabled={submitting || pendingFile != null}
            />
            <Button
              type="button"
              size="sm"
              fullWidth="responsive"
              disabled={!canSubmit}
              isLoading={submitting}
              onClick={() => void handleSubmit()}
            >
              {t('myArea.workflowDetail.commentSubmit')}
            </Button>
          </div>
        </div>
      ) : comments.length === 0 ? (
        <p className="text-sm text-[var(--text-secondary)]">
          {t('myArea.workflowDetail.commentsClosedHint')}
        </p>
      ) : null}
    </Card>
  );
}
