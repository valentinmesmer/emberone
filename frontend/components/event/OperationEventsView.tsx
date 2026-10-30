/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useTranslation } from '@/hooks/useTranslation';
import { EventType } from '@/types/event';
import { EventListView } from './EventListView';
import { getOperationEventLabel } from './eventLabels';
import { useEventList } from './useEventList';

interface OperationEventsViewProps {
  organizationId: number;
}

export function OperationEventsView({ organizationId }: OperationEventsViewProps) {
  const t = useTranslation();
  const list = useEventList(organizationId, EventType.OPERATION);

  return (
    <EventListView
      title={t('events.menu.operation')}
      getEventLabel={getOperationEventLabel}
      list={list}
    />
  );
}
