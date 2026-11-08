/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { isEndStrictlyBeforeStart } from '@/lib/utils/date';
import { formBlockers, type TranslationFunction } from '@/lib/utils/validation';
import type { EventFormState, EventType } from '@/types/event';
import { EventType as EventTypeEnum } from '@/types/event';

function hasValidLeader(leaderId: EventFormState['leaderId']): boolean {
  return !!leaderId && leaderId !== '0' && leaderId !== 0;
}

function hasLocation(formData: EventFormState): boolean {
  return Boolean(formData.location.trim());
}

/** Labels of missing or invalid fields, in form order; empty when the form can be saved. */
export function getEventFormBlockers(
  formData: EventFormState,
  eventType: EventType,
  t: TranslationFunction,
): string[] {
  const isService = eventType === EventTypeEnum.SERVICE;
  const isOperation = eventType === EventTypeEnum.OPERATION;
  if (!isService && !isOperation) return [];

  return formBlockers([
    [t('events.fields.start'), !!formData.start],
    [
      t('events.fields.end'),
      !!formData.end && !isEndStrictlyBeforeStart(formData.start, formData.end),
    ],
    ...(isService
      ? [[t('events.fields.topic'), Boolean(formData.topic?.trim())] as const]
      : [
          [t('events.fields.number'), Boolean(formData.number?.trim())] as const,
          [t('events.fields.keyword'), Boolean(formData.keyword?.trim())] as const,
          [t('events.fields.tag'), Boolean(formData.tag?.trim())] as const,
        ]),
    [t('events.fields.location'), hasLocation(formData)],
    [t('events.fields.leader'), hasValidLeader(formData.leaderId)],
  ]);
}
