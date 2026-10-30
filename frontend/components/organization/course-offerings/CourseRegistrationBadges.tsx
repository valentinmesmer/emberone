/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Badge } from '@/components/ui/badge';
import { useTranslation } from '@/hooks/useTranslation';
import {
  courseRegistrationStatusBadgeTone,
  courseRegistrationStatusLabelKey,
} from '@/lib/utils/courseOffering';
import type { CourseRegistrationDto } from '@/types/courseOffering';

interface Props {
  registration: CourseRegistrationDto;
}

/**
 * Status chip plus the hints that matter for the current status: waitlist place, an already held
 * course while the registration is still undecided, who ended a cancelled registration.
 */
export function CourseRegistrationBadges({ registration }: Props) {
  const t = useTranslation();
  const { status } = registration;
  const undecided = status === 'PENDING' || status === 'WAITLIST';

  return (
    <>
      <Badge tone={courseRegistrationStatusBadgeTone(status)} pill>
        {status === 'WAITLIST' && registration.waitlistPosition != null
          ? t('courseOfferings.registrations.waitlistPlace', {
              position: registration.waitlistPosition,
            })
          : t(courseRegistrationStatusLabelKey(status))}
      </Badge>
      {undecided && registration.alreadyHeld ? (
        <Badge tone="neutral" pill>
          {t('courseOfferings.registrations.alreadyHeld')}
        </Badge>
      ) : null}
      {status === 'CANCELLED' && registration.endedBy ? (
        <Badge tone="neutral" pill>
          {t(`courseOfferings.registrations.endedBy.${registration.endedBy}`)}
        </Badge>
      ) : null}
    </>
  );
}
