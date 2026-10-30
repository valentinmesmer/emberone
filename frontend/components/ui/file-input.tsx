/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Paperclip, Upload, X } from 'lucide-react';
import { useId, useRef } from 'react';
import { Button } from '@/components/ui/button';
import { useFocusLastAddedListItem } from '@/hooks/useFocusLastAddedListItem';
import { useTranslation } from '@/hooks/useTranslation';
import { cn } from '@/lib/utils/cn';

export type FileInputVariant = 'field' | 'compact' | 'inline';

const fileListItemClassName =
  'flex items-center justify-between gap-2 rounded-lg px-2 py-1.5 text-sm text-[var(--text-primary)] scroll-my-4';

export interface FileInputProps {
  id?: string;
  variant?: FileInputVariant;
  label?: string;
  hint?: string;
  error?: string;
  required?: boolean;
  optionalLabel?: string;
  accept?: string;
  multiple?: boolean;
  disabled?: boolean;
  readOnly?: boolean;
  /** Already uploaded file names shown in the same list as pending files. */
  storedFileNames?: string[];
  onRemoveStored?: (index: number) => void;
  files: File[];
  onChange: (files: File[]) => void;
  chooseLabel?: string;
  removeFileLabel?: string;
  /** Hide the selected-files list (e.g. when the parent renders its own list). */
  hideFileList?: boolean;
  className?: string;
  triggerClassName?: string;
}

export function FileInput({
  id,
  variant = 'field',
  label,
  hint,
  error,
  required = false,
  optionalLabel,
  accept,
  multiple = false,
  disabled = false,
  readOnly = false,
  storedFileNames = [],
  onRemoveStored,
  files,
  onChange,
  chooseLabel,
  removeFileLabel,
  hideFileList = false,
  className,
  triggerClassName,
}: FileInputProps) {
  const t = useTranslation();
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const inputRef = useRef<HTMLInputElement>(null);
  const fileListRef = useFocusLastAddedListItem<HTMLUListElement>(
    storedFileNames.length + files.length,
  );
  const resolvedChooseLabel = chooseLabel ?? t('common.attachments.chooseFile');
  const resolvedRemoveFileLabel = removeFileLabel ?? t('common.attachments.removeFile');
  const isField = variant === 'field';
  const hasAttachments = storedFileNames.length > 0 || files.length > 0;

  const handleSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selected = e.target.files;
    if (!selected?.length) return;
    onChange(multiple ? [...files, ...Array.from(selected)] : [selected[0]]);
    e.target.value = '';
  };

  const handleRemove = (index: number) => {
    onChange(files.filter((_, i) => i !== index));
  };

  const triggerButton = (
    <Button
      type="button"
      variant={variant === 'inline' ? 'ghost' : 'outline'}
      size={isField ? 'md' : 'sm'}
      fullWidth={isField ? true : variant === 'inline' ? 'responsive' : false}
      disabled={disabled}
      className={cn(variant === 'inline' && 'justify-start sm:justify-center', triggerClassName)}
      onClick={() => inputRef.current?.click()}
      icon={
        variant === 'inline' ? (
          <Paperclip className="h-4 w-4" aria-hidden />
        ) : (
          <Upload className={isField ? 'h-4 w-4' : 'h-3.5 w-3.5'} aria-hidden />
        )
      }
    >
      {resolvedChooseLabel}
    </Button>
  );

  return (
    <div className={cn(isField ? 'w-full space-y-2' : 'inline-flex shrink-0', className)}>
      {label ? (
        <label className="block text-sm font-semibold text-[var(--text-primary)]" htmlFor={inputId}>
          {label}
          {required && !hasAttachments ? (
            <span className="text-[var(--fire-red)] ml-1">*</span>
          ) : null}
          {optionalLabel ? (
            <span className="ml-1 text-xs font-normal text-[var(--text-secondary)]">
              ({optionalLabel})
            </span>
          ) : null}
        </label>
      ) : null}
      {hint ? <p className="text-xs text-[var(--text-secondary)]">{hint}</p> : null}

      <input
        ref={inputRef}
        id={inputId}
        type="file"
        multiple={multiple}
        accept={accept}
        className="hidden"
        disabled={disabled}
        onChange={handleSelect}
      />
      {readOnly ? null : triggerButton}

      {!hideFileList && isField && hasAttachments ? (
        <ul
          ref={fileListRef}
          className="space-y-1 rounded-xl border border-[var(--border-color)] bg-[var(--card-bg)] p-2"
        >
          {storedFileNames.map((fileName, index) => (
            <li key={`stored-${fileName}`} className={fileListItemClassName}>
              <span className="min-w-0 flex-1 truncate">{fileName}</span>
              {onRemoveStored && !readOnly ? (
                <button
                  type="button"
                  className="shrink-0 rounded-lg p-1 text-[var(--text-secondary)] hover:bg-[var(--surface-muted)] hover:text-[var(--text-primary)]"
                  onClick={() => onRemoveStored(index)}
                  disabled={disabled}
                  aria-label={resolvedRemoveFileLabel}
                >
                  <X className="h-4 w-4" aria-hidden />
                </button>
              ) : null}
            </li>
          ))}
          {files.map((file, index) => (
            <li
              key={`${file.name}-${file.size}-${file.lastModified}`}
              className={fileListItemClassName}
            >
              <span className="min-w-0 flex-1 truncate">{file.name}</span>
              {readOnly ? null : (
                <button
                  type="button"
                  className="shrink-0 rounded-lg p-1 text-[var(--text-secondary)] hover:bg-[var(--surface-muted)] hover:text-[var(--text-primary)]"
                  onClick={() => handleRemove(index)}
                  disabled={disabled}
                  aria-label={resolvedRemoveFileLabel}
                >
                  <X className="h-4 w-4" aria-hidden />
                </button>
              )}
            </li>
          ))}
        </ul>
      ) : null}

      {error ? <p className="text-sm text-red-600 dark:text-red-400 font-medium">{error}</p> : null}
    </div>
  );
}
