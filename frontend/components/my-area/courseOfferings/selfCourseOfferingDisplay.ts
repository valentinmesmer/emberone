/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { BadgeTone } from '@/components/ui/badge';
import {
  courseRegistrationStatusBadgeTone,
  courseRegistrationStatusLabelKey,
} from '@/lib/utils/courseOffering';
import { formatDateShort } from '@/lib/utils/date';
import type { SelfCourseOfferingDto } from '@/types/courseOffering';

export type Translate = (key: string, params?: Record<string, string | number>) => string;

export type OfferingStatusBadge = { tone: BadgeTone; label: string };

function formatPeriod(start: string | null, end: string | null, t: Translate): string {
  if (!start && !end) {
    return t('common.notAvailable');
  }
  const startLabel = start ? formatDateShort(start) : t('common.notAvailable');
  const endLabel = end ? formatDateShort(end) : t('common.notAvailable');
  if (startLabel === endLabel) {
    return startLabel;
  }
  return t('myArea.courseRegistrations.detail.periodRange', { start: startLabel, end: endLabel });
}

export function formatOfferingCoursePeriod(offering: SelfCourseOfferingDto, t: Translate): string {
  return formatPeriod(offering.firstSessionStart, offering.lastSessionEnd, t);
}

export function formatOfferingRegistrationPeriod(
  offering: SelfCourseOfferingDto,
  t: Translate,
): string {
  return formatPeriod(offering.registrationStart, offering.registrationEnd, t);
}

/** The caller's own registration state if present, otherwise the offering's availability. */
export function offeringRegistrationStateBadge(
  offering: SelfCourseOfferingDto,
  t: Translate,
): OfferingStatusBadge {
  const myRegistration = offering.myRegistration;

  if (myRegistration?.cancellationPending) {
    return {
      tone: 'accent',
      label: t('myArea.courseRegistrations.registrations.cancellationPending'),
    };
  }
  if (myRegistration) {
    return {
      tone: courseRegistrationStatusBadgeTone(myRegistration.status),
      label: t(courseRegistrationStatusLabelKey(myRegistration.status)),
    };
  }
  if (offering.full) {
    return { tone: 'warning', label: t('myArea.courseRegistrations.offerings.full') };
  }
  if (!offering.registrationOpen) {
    return { tone: 'neutral', label: t('myArea.courseRegistrations.offerings.registrationClosed') };
  }
  return { tone: 'success', label: t('myArea.courseRegistrations.detail.registrationOpen') };
}
