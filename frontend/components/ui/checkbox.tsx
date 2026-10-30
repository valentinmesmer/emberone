/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Check } from 'lucide-react';
import { HoverHint } from '@/components/ui/hover-hint';
import { useFieldHoverTitle } from '@/hooks/useDisabledHoverTitle';
import { cn } from '@/lib/utils/cn';

export interface CheckboxControlProps {
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  label: string;
  disabled?: boolean;
  id?: string;
  className?: string;
  size?: 'sm' | 'md';
}

export function CheckboxControl({
  checked,
  onCheckedChange,
  label,
  disabled = false,
  id,
  className,
  size = 'md',
}: CheckboxControlProps) {
  const checkboxId = id ?? label.replace(/\s+/g, '-').toLowerCase();

  return (
    <button
      type="button"
      id={checkboxId}
      aria-label={label}
      aria-pressed={checked}
      disabled={disabled}
      onClick={() => onCheckedChange(!checked)}
      className={cn(
        'flex shrink-0 items-center justify-center rounded-md border-2 transition-colors duration-200',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--focus-ring-offset)]',
        size === 'sm' ? 'h-4 w-4' : 'h-5 w-5',
        checked
          ? 'border-[var(--fire-red)] bg-[var(--fire-red)] text-white'
          : 'border-[var(--border-color)] bg-[var(--card-bg)]',
        disabled && 'cursor-not-allowed opacity-50',
        className,
      )}
    >
      {checked && (
        <Check
          className={cn('stroke-[3]', size === 'sm' ? 'h-2.5 w-2.5' : 'h-3.5 w-3.5')}
          aria-hidden
        />
      )}
    </button>
  );
}

export interface CheckboxProps {
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  label: string;
  disabled?: boolean;
  id?: string;
  className?: string;
  title?: string;
  size?: 'sm' | 'md';
}

export function Checkbox({
  checked,
  onCheckedChange,
  label,
  disabled = false,
  id,
  className,
  title,
  size = 'md',
}: CheckboxProps) {
  const checkboxId = id ?? label.replace(/\s+/g, '-').toLowerCase();
  const hint = useFieldHoverTitle(disabled, title);

  return (
    <label
      htmlFor={checkboxId}
      className={cn(
        'flex cursor-pointer items-center text-[var(--text-primary)]',
        size === 'sm' ? 'gap-2 text-sm' : 'gap-3 text-sm',
        disabled && 'cursor-not-allowed opacity-50',
        className,
      )}
    >
      <HoverHint hint={hint} capturePointer={!!disabled} shape="rounded-lg">
        <CheckboxControl
          id={checkboxId}
          checked={checked}
          onCheckedChange={onCheckedChange}
          label={label}
          disabled={disabled}
          size={size}
        />
      </HoverHint>
      <span className={size === 'sm' ? 'font-normal leading-snug' : 'font-medium'}>{label}</span>
    </label>
  );
}
