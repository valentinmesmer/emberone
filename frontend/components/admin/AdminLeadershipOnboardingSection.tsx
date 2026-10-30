/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ListChecks } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { ErrorState } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { ApiRequestError } from '@/lib/api/client';
import {
  useAdminOnboardingRecordingAvailable,
  useAdminUserOnboardingInvitation,
  useStartAdminOnboardingRecording,
} from '@/lib/store';
import { getAdminRecordedOnboardingPath } from '@/lib/utils/adminPaths';
import { formatDateShort } from '@/lib/utils/date';
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

interface AdminLeadershipOnboardingSectionProps {
  tenantId: number;
  organizationId: number;
  membershipId: number;
  userId: number;
  /** Start CTA only when the leadership membership is still active. */
  canStart: boolean;
}

export function AdminLeadershipOnboardingSection({
  tenantId,
  organizationId,
  membershipId,
  userId,
  canStart,
}: AdminLeadershipOnboardingSectionProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError } = useToast();
  const [startOpen, setStartOpen] = useState(false);

  const {
    data: invitation,
    isLoading,
    error,
  } = useAdminUserOnboardingInvitation(tenantId, organizationId, userId);

  const notFound = error instanceof ApiRequestError && error.status === 404;
  const { data: recordingAvailability, isLoading: availabilityLoading } =
    useAdminOnboardingRecordingAvailable(tenantId, organizationId, userId, canStart && notFound);
  const startRecording = useStartAdminOnboardingRecording(tenantId, organizationId, userId);
  const canCatchUp = canStart && notFound && recordingAvailability?.available === true;
  const noStepsConfigured = canStart && notFound && recordingAvailability?.available === false;

  const handleStart = async () => {
    try {
      const detail = await startRecording.mutateAsync();
      setStartOpen(false);
      router.push(
        getAdminRecordedOnboardingPath(tenantId, organizationId, detail.id, { membershipId }),
      );
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.leader.onboarding.startFailed'));
    }
  };

  if (isLoading || (canStart && notFound && availabilityLoading)) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (error && !notFound) {
    return <ErrorState message={t('admin.errors.orgDetailLoadFailed')} />;
  }

  if (!invitation && !canCatchUp && !noStepsConfigured) {
    return null;
  }

  return (
    <section className="space-y-1.5">
      <h2 className="text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
        {t('admin.leader.onboarding.sectionTitle')}
      </h2>

      {invitation ? (
        <ListContainer label={t('admin.leader.onboarding.sectionTitle')}>
          <ListRow
            href={getAdminRecordedOnboardingPath(tenantId, organizationId, invitation.id, {
              membershipId,
            })}
            ariaLabel={t('admin.leader.onboarding.titleCompleted')}
            title={t('admin.leader.onboarding.titleCompleted')}
            subtitle={t('admin.leader.onboarding.tileInvitedAt', {
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
        </ListContainer>
      ) : noStepsConfigured ? (
        <ListContainer label={t('admin.leader.onboarding.sectionTitle')}>
          <ListRow
            ariaLabel={t('admin.leader.onboarding.noStepsTitle')}
            title={t('admin.leader.onboarding.noStepsTitle')}
            subtitle={t('admin.leader.onboarding.noStepsHint')}
            leading={<ListChecks className="h-4 w-4 text-[var(--text-secondary)]" aria-hidden />}
            chevron={false}
          />
        </ListContainer>
      ) : (
        <>
          <ListContainer label={t('admin.leader.onboarding.sectionTitle')}>
            <ListRow
              onClick={() => setStartOpen(true)}
              ariaLabel={t('admin.leader.onboarding.catchUp')}
              title={t('admin.leader.onboarding.catchUp')}
              subtitle={t('admin.leader.onboarding.catchUpHint')}
              tone="active"
              accent
              leading={<ListChecks className="h-4 w-4 text-[var(--fire-red)]" aria-hidden />}
            />
          </ListContainer>

          <ConfirmDialog
            isOpen={startOpen}
            onClose={() => {
              if (startRecording.isPending) return;
              setStartOpen(false);
            }}
            onConfirm={() => void handleStart()}
            title={t('admin.leader.onboarding.catchUpTitle')}
            message={t('admin.leader.onboarding.catchUpMessage')}
            confirmText={t('admin.leader.onboarding.catchUpConfirm')}
            cancelText={t('common.cancel')}
            variant="info"
            isLoading={startRecording.isPending}
          />
        </>
      )}
    </section>
  );
}
