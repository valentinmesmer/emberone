/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Plus, X } from 'lucide-react';
import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { IconButton } from '@/components/ui/icon-button';
import { Select } from '@/components/ui/select';
import type { SelectFieldOption } from '@/lib/ui/selectFieldState';

export interface RepeatableSelectProps {
  label: string;
  options: SelectFieldOption[];
  values: string[];
  onChange: (values: string[]) => void;
  placeholder: string;
  addLabel: string;
  removeLabel: string;
  name?: string;
  disabled?: boolean;
  required?: boolean;
}

export function RepeatableSelect({
  label,
  options,
  values,
  onChange,
  placeholder,
  addLabel,
  removeLabel,
  name,
  disabled = false,
  required = false,
}: RepeatableSelectProps) {
  const [draft, setDraft] = useState(false);
  const slots = draft || values.length === 0 ? [...values, ''] : values;
  const selected = new Set(values);
  const canAdd = !disabled && !draft && values.length > 0 && values.length < options.length;

  const emit = (nextSlots: string[]) => {
    onChange(nextSlots.filter((value) => value !== ''));
  };

  const handleChange = (index: number, value: string) => {
    const next = [...slots];
    next[index] = value;
    if (value !== '') {
      setDraft(false);
    }
    emit(next);
  };

  const handleRemove = (index: number) => {
    const next = slots.filter((_, slotIndex) => slotIndex !== index);
    setDraft(next.includes(''));
    emit(next);
  };

  return (
    <div className="w-full space-y-2">
      <p className="block text-sm font-semibold text-[var(--text-primary)]">
        {label}
        {required ? <span className="ml-1 text-[var(--fire-red)]">*</span> : null}
      </p>
      {slots.map((value, index) => {
        const slotOptions = [
          { value: '', label: placeholder },
          ...options.filter((option) => option.value === value || !selected.has(option.value)),
        ];
        return (
          <div key={`${name ?? 'slot'}-${value}`} className="flex items-center gap-2">
            <div className="min-w-0 flex-1">
              <Select
                name={name ? `${name}-${index}` : undefined}
                aria-label={label}
                value={value}
                options={slotOptions}
                onChange={(event) => handleChange(index, event.target.value)}
                disabled={disabled}
                required={required && index === 0}
              />
            </div>
            {slots.length > 1 ? (
              <IconButton
                type="button"
                icon={<X />}
                aria-label={removeLabel}
                variant="ghost"
                size="md"
                onClick={() => handleRemove(index)}
                disabled={disabled}
              />
            ) : null}
          </div>
        );
      })}
      {canAdd ? (
        <Button
          type="button"
          variant="ghost"
          size="sm"
          icon={<Plus className="h-3.5 w-3.5" aria-hidden />}
          onClick={() => setDraft(true)}
          disabled={disabled}
        >
          {addLabel}
        </Button>
      ) : null}
    </div>
  );
}
