/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { FileText, UserMinus, UserPlus, XCircle } from 'lucide-react';
import { offeringRegistrationStateBadge } from '@/components/my-area/courseOfferings/selfCourseOfferingDisplay';
import { ActionRowIcon, ActionsSection } from '@/components/ui/actions-section';
import type { BadgeTone } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { ListRow } from '@/components/ui/list';
import { statusCardToneClasses } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { getMyAreaWorkflowDetailPath } from '@/lib/my-area/myAreaSections';
import { cn } from '@/lib/utils/cn';
import { formatDateShort } from '@/lib/utils/date';
import type { SelfCourseOfferingDto } from '@/types/courseOffering';

/** Card surfaces matching the badge tones, so state card and chips read as one. */
const badgeToneCardClasses: Record<BadgeTone, string> = {
  neutral: '',
  accent: 'border-[var(--fire-red)]/40 bg-[var(--fire-red)]/10',
  success: statusCardToneClasses.success,
  warning: statusCardToneClasses.warning,
  danger: 'border-[var(--status-danger-accent)]/40 bg-[var(--status-danger-surface)]',
};

function canRegisterForOffering(offering: SelfCourseOfferingDto): boolean {
  return offering.registrationOpen && offering.prerequisitesMet && offering.myRegistration == null;
}

/** The caller's registration state, or why they cannot register. Empty when registering is possible. */
export function SelfCourseOfferingParticipationCard({
  offering,
}: {
  offering: SelfCourseOfferingDto;
}) {
  const t = useTranslation();
  const myRegistration = offering.myRegistration;

  if (myRegistration) {
    const stateBadge = offeringRegistrationStateBadge(offering, t);
    return (
      <Card className={cn('p-3 sm:p-4', badgeToneCardClasses[stateBadge.tone])}>
        <p className="text-sm font-semibold text-[var(--text-primary)]">
          {myRegistration.cancellationPending
            ? t('myArea.courseRegistrations.detail.cancellationPendingHint')
            : t(`myArea.courseRegistrations.detail.status.${myRegistration.status}`)}
        </p>
        <p className="mt-0.5 text-xs text-[var(--text-secondary)]">
          {[
            t('myArea.courseRegistrations.registrations.registeredAt', {
              date: formatDateShort(myRegistration.registeredAt),
            }),
            myRegistration.waitlistPosition != null
              ? t('myArea.courseRegistrations.registrations.waitlistPosition', {
                  position: myRegistration.waitlistPosition,
                })
              : null,
          ]
            .filter(Boolean)
            .join(' · ')}
        </p>
        {myRegistration.endReason ? (
          <p className="mt-1 text-xs text-[var(--text-secondary)]">{myRegistration.endReason}</p>
        ) : null}
      </Card>
    );
  }

  if (canRegisterForOffering(offering)) {
    return null;
  }

  const blockedKey = offering.registrationOpen
    ? 'prerequisitesMissingHint'
    : 'registrationClosedHint';

  return (
    <Card
      className={cn(
        'p-3 sm:p-4',
        offering.registrationOpen ? statusCardToneClasses.warning : undefined,
      )}
    >
      <p className="text-sm text-[var(--text-primary)]">
        {t(`myArea.courseRegistrations.detail.${blockedKey}`)}
      </p>
    </Card>
  );
}

interface ActionsProps {
  offering: SelfCourseOfferingDto;
  onRegister: () => void;
  onCancel: () => void;
  onWithdraw: () => void;
}

/** Everything the caller can do with this offering: register, open requests, cancel, withdraw. */
export function SelfCourseOfferingActions({
  offering,
  onRegister,
  onCancel,
  onWithdraw,
}: ActionsProps) {
  const t = useTranslation();
  const myRegistration = offering.myRegistration;

  return (
    <ActionsSection>
      {canRegisterForOffering(offering) ? (
        <ListRow
          leading={<ActionRowIcon icon={UserPlus} />}
          title={t('myArea.courseRegistrations.register.action')}
          subtitle={
            offering.full
              ? t('myArea.courseRegistrations.detail.waitlistHint')
              : offering.registrationEnd
                ? t('myArea.courseRegistrations.detail.registrationUntil', {
                    date: formatDateShort(offering.registrationEnd),
                  })
                : undefined
          }
          onClick={onRegister}
        />
      ) : null}
      {myRegistration?.workflowId != null ? (
        <ListRow
          leading={<ActionRowIcon icon={FileText} />}
          title={t('myArea.courseRegistrations.registrations.openRegistrationWorkflow')}
          subtitle={t('myArea.courseRegistrations.registrations.openRegistrationWorkflowHint')}
          href={getMyAreaWorkflowDetailPath(myRegistration.workflowId)}
        />
      ) : null}
      {myRegistration?.cancellationWorkflowId != null ? (
        <ListRow
          leading={<ActionRowIcon icon={FileText} />}
          title={t('myArea.courseRegistrations.registrations.openCancellationWorkflow')}
          subtitle={t('myArea.courseRegistrations.registrations.openCancellationWorkflowHint')}
          href={getMyAreaWorkflowDetailPath(myRegistration.cancellationWorkflowId)}
        />
      ) : null}
      {myRegistration?.canCancel ? (
        <ListRow
          leading={<ActionRowIcon icon={XCircle} />}
          title={t('myArea.courseRegistrations.registrations.cancel')}
          subtitle={t('myArea.courseRegistrations.registrations.cancelHint')}
          onClick={onCancel}
        />
      ) : null}
      {myRegistration?.canWithdraw ? (
        <ListRow
          leading={<ActionRowIcon icon={UserMinus} />}
          title={t('myArea.courseRegistrations.registrations.withdrawWaitlist')}
          subtitle={t('myArea.courseRegistrations.registrations.withdrawWaitlistHint')}
          onClick={onWithdraw}
        />
      ) : null}
    </ActionsSection>
  );
}
