/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { DateSegment, type DateSegmentProps } from 'react-aria-components';
import { cn } from '@/lib/utils/cn';

/** Shared DateSegment styles so DatePicker and DateTimePicker match normal Input text. */
export function dateFieldSegmentClass(segment: DateSegmentProps['segment']): string {
  if (segment.type === 'literal') {
    return 'shrink-0 whitespace-pre px-0.5 text-[var(--text-primary)] select-none';
  }

  return cn(
    'inline-block shrink-0 whitespace-nowrap rounded px-0.5 outline-none',
    'text-center tabular-nums text-[var(--text-primary)]',
    'focus:bg-[var(--fire-red)]/10 focus:text-[var(--fire-red)]',
    'data-[placeholder]:text-[var(--text-secondary)]',
  );
}

export function DateFieldSegment({ segment }: { segment: DateSegmentProps['segment'] }) {
  return <DateSegment segment={segment} className={dateFieldSegmentClass(segment)} />;
}
