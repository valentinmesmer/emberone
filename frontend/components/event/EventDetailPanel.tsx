/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { EventDetails } from '@/components/event/EventDetails';
import { getEventMenuLabelKey } from '@/components/event/eventLabels';
import { Badge } from '@/components/ui/badge';
import { useTranslation } from '@/hooks/useTranslation';
import { formatAppointmentDateTimeRange } from '@/lib/utils/date';
import type { EventDto, EventType } from '@/types/event';

interface EventDetailPanelProps {
  event: EventDto;
  eventType: EventType;
  users: Array<{ id: number; firstName: string; lastName: string }>;
}

/**
 * Body content for a department event detail page (badge + range like appointments, then fields).
 */
export function EventDetailPanel({ event, eventType, users }: EventDetailPanelProps) {
  const t = useTranslation();
  const dateTimeRange = useMemo(
    () => formatAppointmentDateTimeRange(event.start, event.end),
    [event.start, event.end],
  );

  return (
    <div className="space-y-4 text-sm">
      <div className="space-y-1.5">
        <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
          <Badge tone="accent" className="shrink-0">
            {t(getEventMenuLabelKey(eventType))}
          </Badge>
          <span className="text-sm text-[var(--text-primary)]">{dateTimeRange}</span>
        </div>
      </div>

      <EventDetails event={event} selectedEventType={eventType} users={users} isMobile={false} />
    </div>
  );
}
