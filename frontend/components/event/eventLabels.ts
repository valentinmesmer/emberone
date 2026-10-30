/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { EventDto, OperationEventDto, ServiceEventDto } from '@/types/event';
import { EventType } from '@/types/event';

const EVENT_MENU_LABEL_KEY: Record<EventType, string> = {
  [EventType.SERVICE]: 'events.menu.service',
  [EventType.OPERATION]: 'events.menu.operation',
};

export function getEventMenuLabelKey(eventType: EventType): string {
  return EVENT_MENU_LABEL_KEY[eventType];
}

export function getServiceEventLabel(event: EventDto): string {
  return (event as ServiceEventDto).topic || '-';
}

/** Split `#foo#bar`-style Schlagwort into separate segments for display. */
export function parseOperationEventTagSegments(tag: string): string[] {
  const trimmed = tag.trim();
  if (!trimmed) return [];
  if (trimmed.includes('#')) {
    return trimmed
      .split('#')
      .map((segment) => segment.trim())
      .filter(Boolean);
  }
  return [trimmed];
}

function formatOperationEventTagForLabel(tag: string): string {
  return parseOperationEventTagSegments(tag).join(', ');
}

export function getOperationEventLabel(event: EventDto): string {
  const operationEvent = event as OperationEventDto;
  const parts: string[] = [];
  if (operationEvent.keyword) parts.push(operationEvent.keyword);
  if (operationEvent.tag) parts.push(formatOperationEventTagForLabel(operationEvent.tag));
  return parts.length > 0 ? parts.join(' - ') : '-';
}

/** Detail page title: Stichwort only so Schlagwort can use its own layout below. */
export function getOperationEventDetailTitle(event: EventDto): string {
  const operationEvent = event as OperationEventDto;
  const keyword = operationEvent.keyword?.trim();
  if (keyword) return keyword;
  return getOperationEventLabel(event);
}
