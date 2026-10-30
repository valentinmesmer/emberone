/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Download, Paperclip, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { FileInput } from '@/components/ui/file-input';
import { List, ListRow } from '@/components/ui/list';
import { useTranslation } from '@/hooks/useTranslation';
import type { EntityAttachmentDto } from '@/types/attachment';

interface EntityAttachmentListProps {
  attachments: EntityAttachmentDto[];
  loading?: boolean;
  canManage?: boolean;
  compact?: boolean;
  title?: string;
  uploadLabel?: string;
  emptyLabel?: string;
  downloadLabel?: string;
  multiple?: boolean;
  onUpload?: (file: File) => Promise<void>;
  onDownload?: (attachment: EntityAttachmentDto) => Promise<void>;
  onDelete?: (attachmentId: number) => Promise<void>;
}

export function EntityAttachmentList({
  attachments,
  loading = false,
  canManage = false,
  compact = false,
  title,
  uploadLabel,
  emptyLabel,
  downloadLabel,
  multiple = false,
  onUpload,
  onDownload,
  onDelete,
}: EntityAttachmentListProps) {
  const t = useTranslation();
  const resolvedTitle = title ?? t('common.attachments.title');
  const resolvedUploadLabel = uploadLabel ?? t('common.attachments.upload');
  const resolvedEmptyLabel = emptyLabel ?? t('common.attachments.empty');
  const resolvedDownloadLabel = downloadLabel ?? t('common.attachments.download');
  const iconBtnClass = compact ? 'h-7 w-7 min-w-7 p-0' : 'h-8 w-8 min-w-8 p-0';

  const handleUpload = async (selected: File[]) => {
    const file = selected[0];
    if (!file || !onUpload) return;
    await onUpload(file);
  };

  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center justify-between gap-2">
        <h4 className={`font-medium flex items-center gap-1.5 ${compact ? 'text-xs' : 'text-sm'}`}>
          <Paperclip className={compact ? 'h-3.5 w-3.5' : 'h-4 w-4'} />
          {resolvedTitle}
        </h4>
        {canManage && onUpload ? (
          <FileInput
            variant="compact"
            files={[]}
            onChange={(selected) => void handleUpload(selected)}
            multiple={multiple}
            chooseLabel={resolvedUploadLabel}
            hideFileList
            triggerClassName={compact ? 'text-xs' : undefined}
          />
        ) : null}
      </div>
      {loading ? (
        <p className={`text-[var(--text-secondary)] ${compact ? 'text-xs' : 'text-sm'}`}>
          {t('common.loading')}
        </p>
      ) : attachments.length === 0 ? (
        <p className={`text-[var(--text-secondary)] ${compact ? 'text-xs' : 'text-sm'}`}>
          {resolvedEmptyLabel}
        </p>
      ) : (
        <List variant="inset" label={resolvedTitle}>
          {attachments.map((attachment) => (
            <ListRow
              key={attachment.id}
              compact={compact}
              accent={false}
              title={attachment.fileName}
              actions={
                <>
                  {onDownload ? (
                    <Button
                      variant="ghost"
                      size={compact ? 'xs' : 'sm'}
                      className={iconBtnClass}
                      onClick={() => void onDownload(attachment)}
                      aria-label={resolvedDownloadLabel}
                    >
                      <Download className={compact ? 'h-3.5 w-3.5' : 'h-4 w-4'} />
                    </Button>
                  ) : null}
                  {canManage && onDelete ? (
                    <Button
                      variant="ghost"
                      size={compact ? 'xs' : 'sm'}
                      className={iconBtnClass}
                      onClick={() => void onDelete(attachment.id)}
                      aria-label={t('common.delete')}
                    >
                      <Trash2 className={compact ? 'h-3.5 w-3.5' : 'h-4 w-4'} />
                    </Button>
                  ) : null}
                </>
              }
            />
          ))}
        </List>
      )}
    </div>
  );
}
