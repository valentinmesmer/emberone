/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type React from 'react';
import { cn } from '@/lib/utils/cn';

export interface FormLabelProps {
  children: React.ReactNode;
  required?: boolean;
  className?: string;
}

export function FormLabel({ children, required, className }: FormLabelProps) {
  return (
    <span
      className={cn(
        'text-xs font-semibold text-[var(--text-secondary)] mb-1.5 block uppercase tracking-wide',
        className,
      )}
    >
      {children}
      {required && <span className="text-red-500 ml-1">*</span>}
    </span>
  );
}
