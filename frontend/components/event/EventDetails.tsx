/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { OperationEventTags } from '@/components/event/OperationEventTags';
import { useTranslation } from '@/hooks/useTranslation';
import { resolveUserDisplayName } from '@/lib/utils/userDisplayName';
import type { EventDto, EventType, OperationEventDto } from '@/types/event';
import { EventType as EventTypeEnum } from '@/types/event';

interface EventDetailsProps {
  event: EventDto;
  selectedEventType: EventType;
  users: Array<{ id: number; firstName: string; lastName: string }>;
  isMobile?: boolean;
}

export function EventDetails({
  event,
  selectedEventType,
  users,
  isMobile = false,
}: EventDetailsProps) {
  const t = useTranslation();

  const operationEvent = event as OperationEventDto;

  const getLeaderName = (leaderId?: number): string =>
    resolveUserDisplayName(users, leaderId, t('common.formerMember'));

  const calculateDuration = (start: string, end?: string): string => {
    if (!end) return '-';
    const startDate = new Date(start);
    const endDate = new Date(end);
    const diffMs = endDate.getTime() - startDate.getTime();
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffMinutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));

    if (diffHours > 0) {
      return `${diffHours}h ${diffMinutes}m`;
    }
    return `${diffMinutes}m`;
  };

  const containerClass = isMobile
    ? `space-y-2.5 text-xs sm:text-sm ${selectedEventType === EventTypeEnum.OPERATION ? 'border-t border-[var(--border-color)] pt-4 mt-4' : ''}`
    : `grid grid-cols-1 lg:grid-cols-2 gap-3 text-sm ${selectedEventType === EventTypeEnum.OPERATION ? 'border-t border-[var(--border-color)] pt-4 mt-4' : ''}`;

  return (
    <>
      {/* Operation-specific fields */}
      {selectedEventType === EventTypeEnum.OPERATION && (
        <div
          className={
            isMobile
              ? 'space-y-2.5 text-xs sm:text-sm'
              : 'grid grid-cols-1 lg:grid-cols-2 gap-3 text-sm'
          }
        >
          {operationEvent.number && (
            <div>
              <span className="font-semibold text-[var(--text-secondary)] mr-2">
                {t('events.fields.number')}:
              </span>
              <span className="text-[var(--text-primary)]">{operationEvent.number}</span>
            </div>
          )}
          {operationEvent.keyword && (
            <div>
              <span className="font-semibold text-[var(--text-secondary)] mr-2">
                {t('events.fields.keyword')}:
              </span>
              <span className="text-[var(--text-primary)]">{operationEvent.keyword}</span>
            </div>
          )}
          {operationEvent.tag && (
            <div className="flex flex-col gap-1.5 sm:flex-row sm:items-start sm:gap-2">
              <span className="shrink-0 font-semibold text-[var(--text-secondary)]">
                {t('events.fields.tag')}:
              </span>
              <OperationEventTags tag={operationEvent.tag} className="flex-1" />
            </div>
          )}
          <div>
            <span className="font-semibold text-[var(--text-secondary)] mr-2">
              {t('events.fields.specialSignal')}:
            </span>
            <span className="text-[var(--text-primary)]">
              {operationEvent.specialSignal ? t('common.yes') : t('common.no')}
            </span>
          </div>
        </div>
      )}

      {/* Common fields for all event types */}
      <div className={containerClass}>
        {/* Location */}
        {event.location && (
          <div>
            <span className="font-semibold text-[var(--text-secondary)] mr-2">
              {t('events.fields.location')}:
            </span>
            <span className="text-[var(--text-primary)]">{event.location}</span>
          </div>
        )}

        {/* Leader */}
        {event.leaderId && (
          <div>
            <span className="font-semibold text-[var(--text-secondary)] mr-2">
              {t('events.fields.leader')}:
            </span>
            <span className="text-[var(--text-primary)]">{getLeaderName(event.leaderId)}</span>
          </div>
        )}

        {/* Duration */}
        <div>
          <span className="font-semibold text-[var(--text-secondary)] mr-2">
            {t('events.fields.duration')}:
          </span>
          <span className="text-[var(--text-primary)]">
            {calculateDuration(event.start, event.end)}
          </span>
        </div>
      </div>

      {/* Description/Message */}
      {event.description && (
        <div
          className={`${isMobile ? 'text-xs sm:text-sm' : 'text-sm'} border-t border-[var(--border-color)] pt-4 mt-4`}
        >
          <span className="font-semibold text-[var(--text-secondary)] mr-2 block mb-2">
            {selectedEventType === EventTypeEnum.OPERATION
              ? t('events.fields.message')
              : t('events.fields.description')}
            :
          </span>
          <span
            className={`text-[var(--text-primary)] whitespace-pre-wrap ${isMobile ? 'break-words' : ''}`}
          >
            {event.description}
          </span>
        </div>
      )}
    </>
  );
}
