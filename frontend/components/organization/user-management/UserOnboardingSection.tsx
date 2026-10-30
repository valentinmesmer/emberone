/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ListChecks } from 'lucide-react';
import { MyAreaOnboardingWizard } from '@/components/my-area/onboarding/MyAreaOnboardingWizard';
import { LoadingState } from '@/components/ui/loading';
import { ErrorState, NoticeCard } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { ApiRequestError } from '@/lib/api/client';
import {
  useOrganizationOnboardingAvailable,
  useOrganizationUserOnboardingDetail,
} from '@/lib/store';

interface UserOnboardingSectionProps {
  organizationId: number;
  userId: number;
}

export function UserOnboardingSection({ organizationId, userId }: UserOnboardingSectionProps) {
  const t = useTranslation();
  const { isLoading, error } = useOrganizationUserOnboardingDetail(organizationId, userId);
  const notFound = error instanceof ApiRequestError && error.status === 404;
  const { data: onboardingAvailability, isLoading: availabilityLoading } =
    useOrganizationOnboardingAvailable(organizationId, notFound);

  if (isLoading || (notFound && availabilityLoading)) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (error) {
    if (notFound && onboardingAvailability?.available === false) {
      return (
        <NoticeCard>
          <div className="flex items-start gap-3">
            <ListChecks
              className="mt-0.5 h-5 w-5 shrink-0 text-[var(--text-secondary)]"
              aria-hidden
            />
            <p className="text-sm text-[var(--text-secondary)]">
              {t('userManagement.onboarding.noSteps')}
            </p>
          </div>
        </NoticeCard>
      );
    }
    if (notFound) {
      return (
        <NoticeCard>
          <p className="text-sm text-[var(--text-secondary)]">
            {t('userManagement.onboarding.noEntry')}
          </p>
        </NoticeCard>
      );
    }
    return <ErrorState message={t('userManagement.error.loadFailed')} />;
  }

  return <MyAreaOnboardingWizard organizationId={organizationId} userId={userId} />;
}
