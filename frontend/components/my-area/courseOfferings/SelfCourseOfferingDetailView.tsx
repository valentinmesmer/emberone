/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { CourseOfferingRegisterDialog } from '@/components/my-area/courseOfferings/CourseOfferingRegisterDialog';
import {
  SelfCourseOfferingInfoCard,
  SelfCourseOfferingSessionsCard,
  SelfCourseOfferingSummaryCard,
} from '@/components/my-area/courseOfferings/SelfCourseOfferingDetailSections';
import {
  SelfCourseOfferingActions,
  SelfCourseOfferingParticipationCard,
} from '@/components/my-area/courseOfferings/SelfCourseOfferingParticipation';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { LoadingState } from '@/components/ui/loading';
import { ErrorState } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { getMyAreaWorkflowDetailPath } from '@/lib/my-area/myAreaSections';
import {
  useOpenCourseRegistrationCancellationWorkflow,
  useSelfCourseOffering,
  useWithdrawFromCourseWaitlist,
} from '@/lib/store';

export interface SelfCourseOfferingDetailViewProps {
  offeringId: number;
}

export function SelfCourseOfferingDetailView({ offeringId }: SelfCourseOfferingDetailViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();
  const { data: offering, isLoading, isError } = useSelfCourseOffering(offeringId);
  const [registerOpen, setRegisterOpen] = useState(false);
  const [cancelOpen, setCancelOpen] = useState(false);
  const [withdrawOpen, setWithdrawOpen] = useState(false);
  const openCancellation = useOpenCourseRegistrationCancellationWorkflow();
  const withdraw = useWithdrawFromCourseWaitlist();

  if (isLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (isError || !offering) {
    return (
      <ErrorState message={t('myArea.courseRegistrations.detail.notFound')} className="mt-4" />
    );
  }

  const myRegistration = offering.myRegistration;

  return (
    <>
      <div className="space-y-3">
        <SelfCourseOfferingSummaryCard offering={offering} />
        <SelfCourseOfferingInfoCard offering={offering} />
        <SelfCourseOfferingSessionsCard offering={offering} />
        <SelfCourseOfferingParticipationCard offering={offering} />
        <SelfCourseOfferingActions
          offering={offering}
          onRegister={() => setRegisterOpen(true)}
          onCancel={() => setCancelOpen(true)}
          onWithdraw={() => setWithdrawOpen(true)}
        />
      </div>

      {registerOpen ? (
        <CourseOfferingRegisterDialog offering={offering} onClose={() => setRegisterOpen(false)} />
      ) : null}

      {myRegistration ? (
        <>
          <ConfirmDialog
            isOpen={cancelOpen}
            onClose={() => setCancelOpen(false)}
            title={t('myArea.courseRegistrations.registrations.cancelConfirmTitle')}
            message={t('myArea.courseRegistrations.registrations.cancelConfirmMessage')}
            onConfirm={async () => {
              try {
                const workflow = await openCancellation.mutateAsync(myRegistration.id);
                showSuccess(t('myArea.courseRegistrations.registrations.cancelSuccess'));
                setCancelOpen(false);
                router.push(getMyAreaWorkflowDetailPath(workflow.id));
              } catch {
                showError(t('myArea.courseRegistrations.registrations.cancelFailed'));
              }
            }}
            isLoading={openCancellation.isPending}
          />
          <ConfirmDialog
            isOpen={withdrawOpen}
            onClose={() => setWithdrawOpen(false)}
            title={t('myArea.courseRegistrations.registrations.withdrawConfirmTitle')}
            message={t('myArea.courseRegistrations.registrations.withdrawConfirmMessage')}
            onConfirm={async () => {
              try {
                await withdraw.mutateAsync(myRegistration.id);
                showSuccess(t('myArea.courseRegistrations.registrations.withdrawSuccess'));
                setWithdrawOpen(false);
              } catch {
                showError(t('myArea.courseRegistrations.registrations.withdrawFailed'));
              }
            }}
            isLoading={withdraw.isPending}
          />
        </>
      ) : null}
    </>
  );
}
