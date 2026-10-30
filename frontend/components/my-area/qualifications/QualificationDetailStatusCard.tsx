/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type { ReactNode } from 'react';
import { Badge, type BadgeTone } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { DetailCardHeader } from '@/components/ui/detail-card-header';
import type { QualificationExerciseStatus } from '@/lib/api/qualifications';
import { QUALIFICATION_STATUS_BADGE_TONE } from '@/lib/qualifications/statusPresentation';

export interface QualificationDetailStatusCardProps {
  title: string;
  abbreviation: string;
  status: QualificationExerciseStatus;
  statusLabel: string;
  badgeTone?: BadgeTone;
  /** Hide the header status badge (e.g. when org decision panel shows status below). */
  showStatusBadge?: boolean;
  hints?: ReactNode;
  footer?: ReactNode;
}

export function QualificationDetailStatusCard({
  title,
  abbreviation,
  status,
  statusLabel,
  badgeTone,
  showStatusBadge = true,
  hints,
  footer,
}: QualificationDetailStatusCardProps) {
  const tone = badgeTone ?? QUALIFICATION_STATUS_BADGE_TONE[status];

  return (
    <Card density="compact">
      <DetailCardHeader
        className="mb-0"
        title={title}
        subtitle={abbreviation}
        actions={
          showStatusBadge ? (
            <Badge tone={tone} pill className="shrink-0">
              {statusLabel}
            </Badge>
          ) : undefined
        }
      />
      {hints ? <div className="mt-3 space-y-2">{hints}</div> : null}
      {footer}
    </Card>
  );
}
