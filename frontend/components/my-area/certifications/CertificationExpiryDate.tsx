/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Calendar } from 'lucide-react';
import { useTranslation } from '@/hooks/useTranslation';
import { formatDateShort } from '@/lib/utils';

interface CertificationExpiryDateProps {
  expiryDate: string;
  className?: string;
}

export function CertificationExpiryDate({ expiryDate, className }: CertificationExpiryDateProps) {
  const t = useTranslation();
  const formatted = formatDateShort(expiryDate);

  return (
    <span
      className={className ?? 'inline-flex items-center gap-1 text-xs text-[var(--text-secondary)]'}
      title={t('certifications.validUntil', { date: formatted })}
    >
      <Calendar className="h-3 w-3 shrink-0" aria-hidden />
      <span className="tabular-nums">{formatted}</span>
    </span>
  );
}
