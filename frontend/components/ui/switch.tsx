/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { HoverHint } from '@/components/ui/hover-hint';
import { useActionHoverTitle } from '@/hooks/useDisabledHoverTitle';
import { cn } from '@/lib/utils/cn';

export interface SwitchProps {
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  disabled?: boolean;
  id?: string;
  'aria-label'?: string;
  className?: string;
}

export function Switch({
  checked,
  onCheckedChange,
  disabled = false,
  id,
  'aria-label': ariaLabel,
  className,
}: SwitchProps) {
  return (
    <button
      type="button"
      id={id}
      role="switch"
      aria-checked={checked}
      aria-label={ariaLabel}
      disabled={disabled}
      title={undefined}
      onClick={() => onCheckedChange(!checked)}
      className={cn(
        'relative box-border h-7 w-12 shrink-0 rounded-full border-2 transition-colors duration-200',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--focus-ring-offset)]',
        checked
          ? 'border-[var(--fire-red)] bg-[var(--fire-red)]'
          : 'border-[var(--border-color)] bg-[var(--surface-muted)]',
        disabled && 'cursor-not-allowed opacity-50',
        className,
      )}
    >
      <span
        aria-hidden
        className={cn(
          'pointer-events-none absolute top-1/2 h-5 w-5 -translate-y-1/2 rounded-full bg-white shadow-sm',
          // Padding-box `left`: 4px insets (left-1 / matching calc) clear border + pill ends.
          'transition-[left] duration-200 ease-out',
          checked ? 'left-[calc(100%-1.25rem-4px)]' : 'left-1',
        )}
      />
    </button>
  );
}

export interface SwitchFieldProps extends SwitchProps {
  label: string;
  description?: string;
  title?: string;
  density?: 'default' | 'compact';
}

export function SwitchField({
  label,
  description,
  checked,
  onCheckedChange,
  disabled,
  id,
  title,
  density = 'default',
}: SwitchFieldProps) {
  const switchId = id ?? label.replace(/\s+/g, '-').toLowerCase();
  const hint = useActionHoverTitle({ disabled, isLoading: false, title });
  const compact = density === 'compact';

  return (
    <HoverHint
      hint={hint}
      capturePointer={!!disabled}
      className={cn(
        'flex w-full items-center justify-between gap-3',
        compact ? 'py-1.5' : 'gap-4 py-2 sm:min-h-[52px] sm:py-3',
      )}
      shape="rounded-xl"
    >
      <div className="min-w-0 flex-1">
        <label
          htmlFor={switchId}
          className={cn(
            'block text-sm font-medium text-[var(--text-primary)]',
            disabled ? 'cursor-not-allowed' : 'cursor-pointer',
          )}
        >
          {label}
        </label>
        {description && (
          <p className="mt-0.5 text-xs text-[var(--text-secondary)]">{description}</p>
        )}
      </div>
      <Switch
        id={switchId}
        checked={checked}
        onCheckedChange={onCheckedChange}
        disabled={disabled}
        aria-label={label}
      />
    </HoverHint>
  );
}
