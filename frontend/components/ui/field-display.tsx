/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { FormLabel } from './form-label';

export interface FieldDisplayProps {
  label: string;
  value?: string | null;
  fallback?: string;
  className?: string;
  multiline?: boolean;
}

export function FieldDisplay({
  label,
  value,
  fallback,
  className,
  multiline = false,
}: FieldDisplayProps) {
  const displayValue = value || fallback || '-';

  return (
    <div className={className}>
      <FormLabel>{label}</FormLabel>
      <p
        className={`text-sm font-medium text-[var(--text-primary)] break-words${
          multiline ? ' whitespace-pre-line' : ''
        }`}
      >
        {displayValue}
      </p>
    </div>
  );
}
