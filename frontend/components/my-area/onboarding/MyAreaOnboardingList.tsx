/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Badge } from '@/components/ui/badge';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard, ErrorState } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { useTranslation } from '@/hooks/useTranslation';
import { getMyAreaOnboardingDetailPath } from '@/lib/my-area/myAreaSections';
import { useSelfOnboardingInvitations } from '@/lib/store';
import { formatDateShort } from '@/lib/utils/date';
import type {
  OnboardingInvitationStatus,
  SelfOnboardingInvitationDto,
} from '@/types/onboardingInvitation';

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

export function MyAreaOnboardingList() {
  const t = useTranslation();
  const { data: invitations = [], isLoading, error } = useSelfOnboardingInvitations();

  const subtitle =
    invitations.length > 0
      ? t('myArea.onboarding.count', { count: invitations.length })
      : t('myArea.onboarding.subtitle');

  if (error) {
    return (
      <>
        <PageHeader title={t('myArea.onboarding.title')} description={subtitle} size="compact" />
        <ErrorState message={t('myArea.error.loadFailed')} className="mt-1 sm:mt-0" />
      </>
    );
  }

  return (
    <>
      <PageHeader title={t('myArea.onboarding.title')} description={subtitle} size="compact" />

      {isLoading ? (
        <LoadingState variant="embedded" size="sm" />
      ) : invitations.length === 0 ? (
        <EmptyStateCard className="mt-1 sm:mt-0">{t('myArea.onboarding.empty')}</EmptyStateCard>
      ) : (
        <ListContainer label={t('myArea.onboarding.listLabel')} className="mt-1 sm:mt-0">
          {invitations.map((invitation) => (
            <OnboardingInvitationRow key={invitation.id} invitation={invitation} />
          ))}
        </ListContainer>
      )}
    </>
  );
}

function OnboardingInvitationRow({ invitation }: { invitation: SelfOnboardingInvitationDto }) {
  const t = useTranslation();
  return (
    <ListRow
      href={getMyAreaOnboardingDetailPath(invitation.id)}
      ariaLabel={invitation.targetOrganizationName}
      title={invitation.targetOrganizationName}
      subtitle={t('myArea.onboarding.invitedAt', {
        date: formatDateShort(invitation.invitedAt),
      })}
      tone="active"
      accent={invitation.status === 'OPEN'}
      badges={
        <Badge tone={statusTone(invitation.status)} pill>
          {t(`myArea.onboarding.status.${invitation.status}`)}
        </Badge>
      }
    />
  );
}
