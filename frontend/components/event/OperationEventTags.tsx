/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { parseOperationEventTagSegments } from '@/components/event/eventLabels';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils/cn';

interface OperationEventTagsProps {
  tag: string;
  className?: string;
}

export function OperationEventTags({ tag, className }: OperationEventTagsProps) {
  const segments = parseOperationEventTagSegments(tag);
  if (segments.length === 0) return null;

  return (
    <span className={cn('inline-flex min-w-0 flex-wrap gap-1.5', className)}>
      {segments.map((segment) => (
        <Badge key={segment} tone="neutral" className="max-w-full break-all text-left">
          {segment}
        </Badge>
      ))}
    </span>
  );
}
