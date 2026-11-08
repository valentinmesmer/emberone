/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Paperclip, Upload, X } from 'lucide-react';
import { useId, useRef, useState } from 'react';
import { Button } from '@/components/ui/button';
import { useFocusLastAddedListItem } from '@/hooks/useFocusLastAddedListItem';
import { useTranslation } from '@/hooks/useTranslation';
import { cn } from '@/lib/utils/cn';
import { toUploadableFile, withHeifAccept } from '@/lib/utils/heic';

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

  const [converting, setConverting] = useState(false);
  const [convertFailed, setConvertFailed] = useState(false);

  const handleSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const input = e.target;
    const selected = Array.from(input.files ?? []);
    input.value = '';
    if (selected.length === 0) return;
    setConvertFailed(false);
    setConverting(true);
    try {
      // iPhone photos (HEIC) become JPEG here, so every upload and AI read gets a readable image.
      const ready = await Promise.all(
        (multiple ? selected : selected.slice(0, 1)).map(toUploadableFile),
      );
      onChange(multiple ? [...files, ...ready] : ready);
    } catch {
      setConvertFailed(true);
    } finally {
      setConverting(false);
    }
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
      disabled={disabled || converting}
      isLoading={converting}
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
        accept={withHeifAccept(accept)}
        className="hidden"
        disabled={disabled}
        onChange={(event) => void handleSelect(event)}
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

      {converting ? (
        <p className="text-xs text-[var(--text-secondary)]" role="status">
          {t('common.attachments.converting')}
        </p>
      ) : null}
      {convertFailed ? (
        <p className="text-sm text-red-600 dark:text-red-400 font-medium" role="alert">
          {t('common.attachments.convertFailed')}
        </p>
      ) : null}
      {error ? <p className="text-sm text-red-600 dark:text-red-400 font-medium">{error}</p> : null}
    </div>
  );
}
