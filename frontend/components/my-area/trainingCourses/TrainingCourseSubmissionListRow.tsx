/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type { ReactNode } from 'react';
import { ListRow } from '@/components/ui/list';
import type { TrainingCourseUserRecordDto } from '@/lib/api/trainingCourses';

export interface TrainingCourseSubmissionListRowProps {
  record: TrainingCourseUserRecordDto;
  href?: string;
  chevron?: boolean;
  tone?: 'default' | 'warning';
  /** Extra status/label chips (right of the title). */
  badges?: ReactNode;
  actions?: ReactNode;
}

export function TrainingCourseSubmissionListRow({
  record,
  href,
  chevron = true,
  tone = 'default',
  badges,
  actions,
}: TrainingCourseSubmissionListRowProps) {
  const location = record.location?.trim() || undefined;

  return (
    <ListRow
      href={href}
      chevron={chevron}
      tone={tone}
      title={record.name}
      subtitle={location}
      badges={badges}
      actions={actions}
    />
  );
}
