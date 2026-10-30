/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type { TextareaHTMLAttributes } from 'react';
import { HoverHint } from '@/components/ui/hover-hint';
import { useFieldHoverTitle } from '@/hooks/useDisabledHoverTitle';
import { cn } from '@/lib/utils/cn';

export interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
  hint?: string;
}

export function Textarea({
  label,
  error,
  hint,
  className = '',
  disabled,
  title,
  ...props
}: TextareaProps) {
  const textareaId = props.id || props.name;
  const hoverTitle = useFieldHoverTitle(disabled, title);
  // Keep controlled textareas stable: `value={undefined}` must not flip to a string later.
  const textareaProps = 'value' in props ? { ...props, value: props.value ?? '' } : props;

  return (
    <div className="w-full space-y-2">
      {label && (
        <label
          htmlFor={textareaId}
          className="flex items-center gap-2 text-sm font-semibold text-[var(--text-primary)]"
        >
          <span>
            {label}
            {props.required && <span className="text-[var(--fire-red)] ml-1">*</span>}
          </span>
        </label>
      )}
      {hint && <p className="text-xs text-[var(--text-secondary)]">{hint}</p>}
      <HoverHint
        hint={hoverTitle}
        capturePointer={!!disabled}
        className="block w-full"
        shape="rounded-xl"
      >
        <textarea
          id={textareaId}
          className={cn(
            'w-full min-h-[100px] resize-y rounded-xl border px-4 py-3 text-sm',
            'bg-[var(--card-bg)] text-[var(--text-primary)] border-[var(--border-color)]',
            'placeholder:text-[var(--text-secondary)]',
            'focus:border-[var(--fire-red)] focus:outline-none focus:ring-2 focus:ring-[var(--focus-ring)]',
            'transition-colors duration-200',
            error && 'border-red-500 focus:ring-red-500/25',
            className,
          )}
          disabled={disabled}
          {...textareaProps}
          title={undefined}
        />
      </HoverHint>
      {error && <p className="text-sm font-medium text-red-600 dark:text-red-400">{error}</p>}
    </div>
  );
}
