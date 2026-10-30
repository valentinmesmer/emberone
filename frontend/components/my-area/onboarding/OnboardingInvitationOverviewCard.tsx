/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { FieldDisplay } from '@/components/ui/field-display';
import { SectionHeader } from '@/components/ui/section-header';
import { useTranslation } from '@/hooks/useTranslation';
import { formatDateLong } from '@/lib/utils/date';
import type { OnboardingInvitationStatus } from '@/types/onboardingInvitation';

function statusTone(status: OnboardingInvitationStatus): 'accent' | 'warning' | 'neutral' {
  switch (status) {
    case 'OPEN':
      return 'accent';
    case 'SUBMITTED':
      return 'warning';
    default:
      return 'neutral';
  }
}

interface OnboardingInvitationOverviewCardProps {
  organizationName: string;
  status: OnboardingInvitationStatus;
  invitedAt: string;
}

export function OnboardingInvitationOverviewCard({
  organizationName,
  status,
  invitedAt,
}: OnboardingInvitationOverviewCardProps) {
  const t = useTranslation();

  return (
    <Card className="space-y-3 p-3 sm:p-4">
      <SectionHeader
        title={t('myArea.workflowDetail.sectionOverview')}
        action={
          <Badge tone={statusTone(status)} pill>
            {t(`myArea.onboarding.status.${status}`)}
          </Badge>
        }
      />
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <FieldDisplay label={t('myArea.onboarding.detail.organization')} value={organizationName} />
        <FieldDisplay
          label={t('myArea.onboarding.detail.invitedAt')}
          value={formatDateLong(invitedAt)}
        />
      </div>
    </Card>
  );
}
