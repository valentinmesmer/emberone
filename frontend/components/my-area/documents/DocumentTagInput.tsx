/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { X } from 'lucide-react';
import { type KeyboardEvent, useMemo, useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { useTranslation } from '@/hooks/useTranslation';
import { cn } from '@/lib/utils/cn';

interface DocumentTagInputProps {
  tags: string[];
  onChange: (tags: string[]) => void;
  suggestions?: string[];
  disabled?: boolean;
}

function normalizeTag(value: string): string {
  return value.trim();
}

export function DocumentTagInput({
  tags,
  onChange,
  suggestions = [],
  disabled = false,
}: DocumentTagInputProps) {
  const t = useTranslation();
  const [inputValue, setInputValue] = useState('');

  const unusedSuggestions = useMemo(() => {
    const current = new Set(tags.map((tag) => tag.toLowerCase()));
    return suggestions.filter((tag) => !current.has(tag.toLowerCase()));
  }, [suggestions, tags]);

  const addTag = (raw: string) => {
    const tag = normalizeTag(raw);
    if (!tag || tag.length > 128) return;
    const exists = tags.some((existing) => existing.toLowerCase() === tag.toLowerCase());
    if (exists) {
      setInputValue('');
      return;
    }
    onChange([...tags, tag]);
    setInputValue('');
  };

  const removeTag = (tagToRemove: string) => {
    onChange(tags.filter((tag) => tag !== tagToRemove));
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      addTag(inputValue);
    } else if (event.key === 'Backspace' && !inputValue && tags.length > 0) {
      removeTag(tags[tags.length - 1]);
    }
  };

  return (
    <div className="space-y-2">
      <span className="block text-sm font-medium text-[var(--text-primary)]">
        {t('myArea.documents.tagsLabel')}
      </span>

      <div
        className={cn(
          'flex min-h-10 flex-wrap items-center gap-1.5 rounded-lg border border-[var(--border-color)] bg-[var(--surface-primary)] px-2 py-1.5',
          disabled && 'opacity-60',
        )}
      >
        {tags.map((tag) => (
          <Badge key={tag} tone="accent" pill className="gap-1 pr-1">
            {tag}
            {!disabled ? (
              <button
                type="button"
                onClick={() => removeTag(tag)}
                className="rounded-full p-0.5 hover:bg-[var(--fire-red)]/20"
                aria-label={t('myArea.documents.removeTag', { tag })}
              >
                <X className="h-3 w-3" aria-hidden />
              </button>
            ) : null}
          </Badge>
        ))}
        <input
          type="text"
          value={inputValue}
          disabled={disabled}
          placeholder={
            tags.length === 0
              ? t('myArea.documents.tagsPlaceholder')
              : t('myArea.documents.tagsAddMore')
          }
          className="min-w-[8rem] flex-1 border-0 bg-transparent px-1 py-0.5 text-sm text-[var(--text-primary)] outline-none placeholder:text-[var(--text-secondary)]"
          onChange={(event) => setInputValue(event.target.value)}
          onKeyDown={handleKeyDown}
          onBlur={() => {
            if (inputValue.trim()) addTag(inputValue);
          }}
        />
      </div>

      <p className="text-xs text-[var(--text-secondary)]">{t('myArea.documents.tagsHint')}</p>

      {unusedSuggestions.length > 0 ? (
        <div className="space-y-1">
          <p className="text-xs text-[var(--text-secondary)]">
            {t('myArea.documents.tagsSuggestions')}
          </p>
          <div className="flex flex-wrap gap-1.5">
            {unusedSuggestions.map((tag) => (
              <button
                key={tag}
                type="button"
                disabled={disabled}
                onClick={() => addTag(tag)}
                className="disabled:opacity-60"
              >
                <Badge tone="neutral" pill className="transition-opacity hover:opacity-80">
                  {tag}
                </Badge>
              </button>
            ))}
          </div>
        </div>
      ) : null}
    </div>
  );
}
