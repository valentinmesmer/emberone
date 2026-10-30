/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type React from 'react';
import { HoverHint } from '@/components/ui/hover-hint';
import { useFieldHoverTitle } from '@/hooks/useDisabledHoverTitle';
import { cn } from '@/lib/utils/cn';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
}

export function Input({
  label,
  error,
  className = '',
  hint,
  disabled,
  title,
  ...props
}: InputProps) {
  const inputId = props.id || props.name;
  const hoverTitle = useFieldHoverTitle(disabled, title);
  // Keep controlled inputs stable: `value={undefined}` must not flip to a string later.
  const inputProps = 'value' in props ? { ...props, value: props.value ?? '' } : props;

  return (
    <div className="w-full space-y-2">
      {label && (
        <label htmlFor={inputId} className="block text-sm font-semibold text-[var(--text-primary)]">
          {label}
          {props.required && <span className="text-[var(--fire-red)] ml-1">*</span>}
        </label>
      )}
      {hint && <p className="text-xs text-[var(--text-secondary)] mt-1">{hint}</p>}
      <HoverHint
        hint={hoverTitle}
        capturePointer={!!disabled}
        className="block w-full"
        shape="rounded-xl"
      >
        <input
          id={inputId}
          className={cn(
            `
          w-full px-4 py-3 rounded-xl border
          bg-[var(--card-bg)] text-[var(--text-primary)]
          border-[var(--border-color)]
          focus:border-[var(--fire-red)] focus:outline-none focus:ring-2 focus:ring-[var(--focus-ring)]
          transition-colors duration-200
          placeholder:text-[var(--text-secondary)]
          ${error ? 'border-red-500 focus:ring-red-500/25' : ''}
        `,
            className,
          )}
          disabled={disabled}
          {...inputProps}
          title={undefined}
        />
      </HoverHint>
      {error && <p className="text-sm text-red-600 dark:text-red-400 font-medium">{error}</p>}
    </div>
  );
}
