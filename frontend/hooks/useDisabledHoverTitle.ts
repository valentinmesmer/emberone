/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useTranslation } from '@/hooks/useTranslation';
import { mergeActionHoverTitle } from '@/lib/utils/hoverTitle';

/** Buttons, icon buttons, dropdown triggers - floating hint only when disabled or loading. */
export function useActionHoverTitle(options: {
  disabled?: boolean;
  isLoading?: boolean;
  title?: string;
}): string | undefined {
  const t = useTranslation();
  const { disabled, isLoading, title } = options;
  const nonInteractive = Boolean(disabled || isLoading);
  if (!nonInteractive) {
    return undefined;
  }
  return mergeActionHoverTitle(true, Boolean(isLoading), title, {
    loading: t('common.processing'),
    unavailable: t('common.hoverDisabledAction'),
  });
}

/** Inputs, selects, groups - floating hint only when disabled (optional `title` overrides default). */
export function useFieldHoverTitle(
  disabled: boolean | undefined,
  title?: string,
): string | undefined {
  const t = useTranslation();
  if (!disabled) return undefined;
  const explicit = title?.trim();
  if (explicit) return explicit;
  return t('common.hoverDisabledField');
}
