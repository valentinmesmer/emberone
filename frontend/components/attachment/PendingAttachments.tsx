/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Paperclip, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { FileInput } from '@/components/ui/file-input';
import { List, ListRow } from '@/components/ui/list';
import { EmptyState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

const WORKFLOW_FILE_ACCEPT = '.pdf,image/jpeg,image/png,image/gif,image/webp';

type PendingAttachmentsVariant = 'section' | 'field';

interface PendingAttachmentsProps {
  files: File[];
  onChange: (files: File[]) => void;
  variant?: PendingAttachmentsVariant;
  multiple?: boolean;
  accept?: string;
  inputId?: string;
  title?: string;
  label?: string;
  hint?: string;
  required?: boolean;
  optionalLabel?: string;
  uploadLabel?: string;
  emptyLabel?: string;
  removeFileLabel?: string;
  readOnly?: boolean;
  disabled?: boolean;
  storedFileNames?: string[];
  onRemoveStored?: (index: number) => void;
}

export function PendingAttachments({
  files,
  onChange,
  variant = 'section',
  multiple = true,
  accept = WORKFLOW_FILE_ACCEPT,
  inputId,
  title,
  label,
  hint,
  required = false,
  optionalLabel,
  uploadLabel,
  emptyLabel,
  removeFileLabel,
  readOnly = false,
  disabled = false,
  storedFileNames = [],
  onRemoveStored,
}: PendingAttachmentsProps) {
  const t = useTranslation();

  const resolvedTitle = title ?? t('common.attachments.title');
  const resolvedUploadLabel = uploadLabel ?? t('common.attachments.upload');
  const resolvedEmptyLabel = emptyLabel ?? t('common.attachments.empty');
  const resolvedRemoveFileLabel = removeFileLabel ?? t('common.attachments.removeFile');

  const handleRemove = (index: number) => {
    onChange(files.filter((_, i) => i !== index));
  };

  if (variant === 'field') {
    return (
      <FileInput
        id={inputId}
        files={files}
        onChange={onChange}
        label={label}
        hint={hint}
        required={required}
        optionalLabel={optionalLabel}
        multiple={multiple}
        accept={accept}
        disabled={disabled}
        readOnly={readOnly}
        storedFileNames={storedFileNames}
        onRemoveStored={onRemoveStored}
        removeFileLabel={removeFileLabel}
      />
    );
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center justify-between gap-2">
        <h4 className="text-sm font-medium flex items-center gap-2">
          <Paperclip className="h-4 w-4" />
          {resolvedTitle}
        </h4>
        <FileInput
          id={inputId}
          variant="compact"
          files={files}
          onChange={onChange}
          multiple={multiple}
          accept={accept}
          chooseLabel={resolvedUploadLabel}
          hideFileList
        />
      </div>
      {files.length === 0 ? (
        <EmptyState>{resolvedEmptyLabel}</EmptyState>
      ) : (
        <List variant="inset" label={resolvedTitle}>
          {files.map((file, index) => (
            <ListRow
              key={`${file.name}-${file.size}-${file.lastModified}`}
              compact
              accent={false}
              title={file.name}
              actions={
                <Button
                  variant="ghost"
                  onClick={() => handleRemove(index)}
                  aria-label={resolvedRemoveFileLabel}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              }
            />
          ))}
        </List>
      )}
    </div>
  );
}
