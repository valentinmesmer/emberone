/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { NoticeCard } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { formatDateShort } from '@/lib/utils/date';
import type { CourseOfferingDto } from '@/types/courseOffering';

export function CourseOfferingCancelledBanner({ offering }: { offering: CourseOfferingDto }) {
  const t = useTranslation();
  if (offering.status !== 'CANCELLED') {
    return null;
  }
  const cancelledAt = offering.cancelledAt ? formatDateShort(offering.cancelledAt) : null;
  return (
    <NoticeCard tone="warning">
      <p className="font-medium">{t('courseOfferings.cancel.bannerTitle')}</p>
      {cancelledAt ? (
        <p className="mt-1 text-sm">
          {t('courseOfferings.cancel.cancelledAt', { date: cancelledAt })}
        </p>
      ) : null}
      {offering.cancelReason?.trim() ? (
        <p className="mt-1 text-sm">{offering.cancelReason.trim()}</p>
      ) : null}
    </NoticeCard>
  );
}
