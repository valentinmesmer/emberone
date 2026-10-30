/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useTranslation } from '@/hooks/useTranslation';
import { EventType } from '@/types/event';
import { EventListView } from './EventListView';
import { getServiceEventLabel } from './eventLabels';
import { useEventList } from './useEventList';

interface ServiceEventsViewProps {
  organizationId: number;
}

export function ServiceEventsView({ organizationId }: ServiceEventsViewProps) {
  const t = useTranslation();
  const list = useEventList(organizationId, EventType.SERVICE);

  return (
    <EventListView
      title={t('events.menu.service')}
      getEventLabel={getServiceEventLabel}
      list={list}
    />
  );
}
