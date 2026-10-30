/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type { ReactNode } from 'react';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import type { CertificationStatus } from '@/lib/api/certifications';
import { CERTIFICATION_STATUS_BADGE_TONE } from '@/lib/certifications/statusPresentation';

export interface CertificationDetailStatusCardProps {
  title: string;
  status: CertificationStatus;
  statusLabel?: string;
  meta?: ReactNode;
  hints?: ReactNode;
  footer?: ReactNode;
}

export function CertificationDetailStatusCard({
  title,
  status,
  statusLabel,
  meta,
  hints,
  footer,
}: CertificationDetailStatusCardProps) {
  return (
    <Card density="compact">
      <div className="flex flex-wrap items-start justify-between gap-x-3 gap-y-1.5">
        <h1 className="min-w-0 flex-1 break-words text-base font-semibold text-[var(--text-primary)]">
          {title}
        </h1>
        {statusLabel ? (
          <Badge tone={CERTIFICATION_STATUS_BADGE_TONE[status]} pill className="shrink-0">
            {statusLabel}
          </Badge>
        ) : null}
      </div>
      {meta}
      {hints}
      {footer}
    </Card>
  );
}
