/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { isEndStrictlyBeforeStart } from '@/lib/utils/date';
import type { EventFormState, EventType } from '@/types/event';
import { EventType as EventTypeEnum } from '@/types/event';

function hasValidLeader(leaderId: EventFormState['leaderId']): boolean {
  return !!leaderId && leaderId !== '0' && leaderId !== 0;
}

function hasLocation(formData: EventFormState): boolean {
  return Boolean(formData.location.trim());
}

function isServiceEventFormValid(formData: EventFormState): boolean {
  if (!formData.start || !formData.end) return false;
  if (isEndStrictlyBeforeStart(formData.start, formData.end)) return false;
  if (!hasValidLeader(formData.leaderId)) return false;
  if (!hasLocation(formData)) return false;
  return Boolean(formData.topic?.trim());
}

function isOperationEventFormValid(formData: EventFormState): boolean {
  if (!formData.start || !formData.end) return false;
  if (isEndStrictlyBeforeStart(formData.start, formData.end)) return false;
  if (!hasValidLeader(formData.leaderId)) return false;
  if (!hasLocation(formData)) return false;
  return Boolean(formData.number?.trim() && formData.keyword?.trim() && formData.tag?.trim());
}

export function isEventFormValid(formData: EventFormState, eventType: EventType): boolean {
  if (eventType === EventTypeEnum.SERVICE) {
    return isServiceEventFormValid(formData);
  }
  if (eventType === EventTypeEnum.OPERATION) {
    return isOperationEventFormValid(formData);
  }
  return true;
}

export function validateEventFormForSubmit(
  formData: EventFormState,
  eventType: EventType,
): 'ok' | 'required' | 'endBeforeStart' {
  if (!formData.start) return 'required';
  if (formData.end && isEndStrictlyBeforeStart(formData.start, formData.end)) {
    return 'endBeforeStart';
  }

  const resolvedLocation = formData.location.trim();

  if (eventType === EventTypeEnum.SERVICE) {
    if (
      !formData.end ||
      !formData.topic?.trim() ||
      !resolvedLocation ||
      !hasValidLeader(formData.leaderId)
    ) {
      return 'required';
    }
    return 'ok';
  }

  if (eventType === EventTypeEnum.OPERATION) {
    if (
      !formData.end ||
      !formData.number?.trim() ||
      !formData.keyword?.trim() ||
      !formData.tag?.trim() ||
      !resolvedLocation ||
      !hasValidLeader(formData.leaderId)
    ) {
      return 'required';
    }
    return 'ok';
  }

  return 'ok';
}
