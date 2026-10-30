/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Check, Circle } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useCallback, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { onboardingStepTypeKey } from '@/components/admin/standards/onboardingStepUtils';
import { OnboardingInvitationOverviewCard } from '@/components/my-area/onboarding/OnboardingInvitationOverviewCard';
import { OnboardingInvitationStepsReview } from '@/components/my-area/onboarding/OnboardingInvitationStepsReview';
import { OnboardingStepBody } from '@/components/my-area/onboarding/steps/OnboardingStepBody';
import type { OnboardingStepHandle } from '@/components/my-area/onboarding/steps/onboardingStepHandle';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard, ErrorState, NotFoundState } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { ApiRequestError } from '@/lib/api/client';
import { useSetBackHref } from '@/lib/navigation/backHrefOverride';
import {
  useAdminOnboardingInvitationDetail,
  useCompleteAdminOnboardingInvitation,
  useSaveOnboardingStep,
  useUploadOnboardingDocument,
} from '@/lib/store';
import { getAdminOrganizationDetailPath } from '@/lib/utils/adminPaths';
import { cn } from '@/lib/utils/cn';
import type { OnboardingInvitationAccess } from '@/types/onboardingInvitation';

interface AdminRecordedOnboardingWizardProps {
  tenantId: number;
  organizationId: number;
  invitationId: number;
  /** Member user id - used to refresh admin-by-user caches after complete/cancel. */
  userId?: number;
  /** Where Zurück / complete / cancel should go (defaults to org detail). */
  returnHref?: string;
}

export function AdminRecordedOnboardingWizard({
  tenantId,
  organizationId,
  invitationId,
  userId,
  returnHref,
}: AdminRecordedOnboardingWizardProps) {
  const t = useTranslation();
  const router = useRouter();
  const setBackHref = useSetBackHref();
  const { showError, showSuccess } = useToast();
  const access: OnboardingInvitationAccess = {
    kind: 'admin',
    tenantId,
    organizationId,
    invitationId,
  };
  const { data, isLoading, error } = useAdminOnboardingInvitationDetail(
    tenantId,
    organizationId,
    invitationId,
  );
  const saveStep = useSaveOnboardingStep(access);
  const uploadDocument = useUploadOnboardingDocument(access);
  const completeOnboarding = useCompleteAdminOnboardingInvitation(tenantId, organizationId, userId);
  const stepRef = useRef<OnboardingStepHandle>(null);
  const initialIndexRef = useRef<number | null>(null);
  const [index, setIndex] = useState<number | null>(null);
  const [isNavigating, setIsNavigating] = useState(false);
  const [adminRecordReason, setAdminRecordReason] = useState('');

  const steps = data?.steps ?? [];
  const reviewIndex = steps.length;
  const showVerificationOnly = Boolean(data && !data.editable);
  const exitHref = returnHref ?? getAdminOrganizationDetailPath(tenantId, organizationId);

  useLayoutEffect(() => {
    setBackHref(exitHref);
    return () => setBackHref(null);
  }, [exitHref, setBackHref]);

  if (data && initialIndexRef.current === null) {
    initialIndexRef.current = firstOpenIndex(steps);
  }

  const currentIndex = index ?? initialIndexRef.current ?? 0;
  const isReview = currentIndex >= reviewIndex;
  const currentStep = !isReview ? steps[currentIndex] : undefined;

  const saveCurrentStepIfChanged = useCallback(async (): Promise<boolean> => {
    if (!data?.editable || isReview || !currentStep) return true;
    const handle = stepRef.current;
    if (handle?.prepareSave) {
      const prepared = await handle.prepareSave();
      if (!prepared) return false;
    }
    if (!handle?.hasChanges()) return true;
    try {
      await saveStep.mutateAsync({
        stepType: currentStep.stepType,
        request: { payload: handle.getPayload() },
      });
      showSuccess(t('myArea.onboarding.wizard.saved'));
      return true;
    } catch (err) {
      showError(err instanceof Error ? err.message : t('admin.errors.orgDetailLoadFailed'));
      return false;
    }
  }, [currentStep, data?.editable, isReview, saveStep, showError, showSuccess, t]);

  const handleComplete = useCallback(async () => {
    if (!data?.completionEnabled) return;
    const reason = adminRecordReason.trim();
    if (!reason) {
      showError(t('myArea.onboarding.wizard.entryDocuments.adminRecordReasonRequired'));
      return;
    }
    setIsNavigating(true);
    try {
      const saved = await saveCurrentStepIfChanged();
      if (!saved) return;
      await completeOnboarding.mutateAsync({ invitationId, adminRecordReason: reason });
      showSuccess(t('admin.leader.onboarding.completed'));
      router.push(exitHref);
    } catch (err) {
      showError(err instanceof Error ? err.message : t('admin.leader.onboarding.completeFailed'));
    } finally {
      setIsNavigating(false);
    }
  }, [
    adminRecordReason,
    completeOnboarding,
    data?.completionEnabled,
    exitHref,
    invitationId,
    router,
    saveCurrentStepIfChanged,
    showError,
    showSuccess,
    t,
  ]);

  const navigateTo = useCallback(
    async (newIndex: number) => {
      if (newIndex === currentIndex || isNavigating) return;
      setIsNavigating(true);
      try {
        const saved = await saveCurrentStepIfChanged();
        if (saved) setIndex(newIndex);
      } finally {
        setIsNavigating(false);
      }
    },
    [currentIndex, isNavigating, saveCurrentStepIfChanged],
  );

  const progressLabel = useMemo(() => {
    if (!data) return '';
    if (steps.length === 0 && !data.allStepsFulfilled) {
      return t('admin.leader.onboarding.noStepsHint');
    }
    if (isReview || steps.length === 0) return t('myArea.onboarding.wizard.reviewProgress');
    return t('myArea.onboarding.wizard.progress', {
      current: currentIndex + 1,
      total: steps.length,
    });
  }, [currentIndex, data, isReview, steps.length, t]);

  const uploadSignedDocument = useCallback(
    async (configKey: string, file: File) => {
      const scope =
        currentStep?.stepType === 'ORGANIZATION_ENTRY_DOCUMENTS' ? 'organization' : 'tenant';
      await uploadDocument.mutateAsync({ configKey, file, scope });
    },
    [currentStep?.stepType, uploadDocument],
  );

  const pageTitle = showVerificationOnly
    ? t('admin.leader.onboarding.titleCompleted')
    : t('admin.leader.onboarding.title');

  if (error) {
    const notFound = error instanceof ApiRequestError && error.status === 404;
    const message = notFound
      ? t('myArea.onboarding.wizard.notFound')
      : t('admin.errors.orgDetailLoadFailed');
    return (
      <>
        <PageHeader title={pageTitle} size="compact" />
        {notFound ? (
          <NotFoundState message={message} className="mt-1 sm:mt-0" />
        ) : (
          <ErrorState message={message} className="mt-1 sm:mt-0" />
        )}
      </>
    );
  }

  if (isLoading || !data) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (showVerificationOnly) {
    return (
      <div className="w-full space-y-4">
        <PageHeader title={pageTitle} size="compact" />
        <OnboardingInvitationOverviewCard
          organizationName={data.targetOrganizationName}
          status={data.status}
          invitedAt={data.invitedAt}
        />
        <OnboardingInvitationStepsReview
          access={access}
          status={data.status}
          invitationId={data.id}
          targetOrganizationId={data.targetOrganizationId}
          subjectUserId={data.subjectUserId}
          steps={steps}
          catalog={data.catalog}
          alreadyFulfilledStepTypes={data.alreadyFulfilledStepTypes}
        />
      </div>
    );
  }

  return (
    <>
      <PageHeader title={pageTitle} description={progressLabel} size="compact" />

      {steps.length === 0 && !data.allStepsFulfilled ? (
        <EmptyStateCard className="mt-1 sm:mt-0">
          {t('admin.leader.onboarding.noStepsHint')}
        </EmptyStateCard>
      ) : (
        <div className="mt-1 space-y-4 sm:mt-0">
          <ol
            className="flex flex-wrap gap-2"
            aria-label={t('myArea.onboarding.wizard.stepsLabel')}
          >
            {steps.map((step, stepIndex) => {
              const active = stepIndex === currentIndex;
              return (
                <li key={step.stepType}>
                  <button
                    type="button"
                    onClick={() => void navigateTo(stepIndex)}
                    className={cn(
                      'flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-medium',
                      active
                        ? 'border-[var(--fire-red)] bg-[var(--fire-red)]/10 text-[var(--text-primary)]'
                        : 'border-[var(--border-color)] text-[var(--text-secondary)]',
                    )}
                  >
                    {step.fulfilled ? (
                      <Check className="h-3.5 w-3.5 text-[var(--fire-red)]" aria-hidden />
                    ) : (
                      <Circle className="h-3.5 w-3.5" aria-hidden />
                    )}
                    {t(
                      `myArea.onboarding.wizard.stepTitles.${onboardingStepTypeKey(step.stepType)}`,
                    )}
                  </button>
                </li>
              );
            })}
            <li>
              <button
                type="button"
                onClick={() => void navigateTo(reviewIndex)}
                className={cn(
                  'flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-medium',
                  isReview
                    ? 'border-[var(--fire-red)] bg-[var(--fire-red)]/10 text-[var(--text-primary)]'
                    : 'border-[var(--border-color)] text-[var(--text-secondary)]',
                )}
              >
                {data.allStepsFulfilled ? (
                  <Check className="h-3.5 w-3.5 text-[var(--fire-red)]" aria-hidden />
                ) : (
                  <Circle className="h-3.5 w-3.5" aria-hidden />
                )}
                {t('myArea.onboarding.wizard.reviewTitle')}
              </button>
            </li>
          </ol>

          {isReview || !currentStep ? (
            <div className="space-y-3">
              <p className="text-sm text-[var(--text-secondary)]">
                {data.allStepsFulfilled
                  ? t('admin.leader.onboarding.reviewReady')
                  : t('myArea.onboarding.wizard.reviewMissing')}
              </p>
              <Textarea
                label={t('myArea.onboarding.wizard.entryDocuments.adminRecordReasonLabel')}
                hint={t('myArea.onboarding.wizard.entryDocuments.adminRecordReasonHint')}
                value={adminRecordReason}
                onChange={(event) => setAdminRecordReason(event.target.value)}
                disabled={!data.completionEnabled || isNavigating}
              />
              <OnboardingInvitationStepsReview
                access={access}
                status={data.status}
                invitationId={data.id}
                targetOrganizationId={data.targetOrganizationId}
                subjectUserId={data.subjectUserId}
                steps={steps}
                catalog={data.catalog}
                alreadyFulfilledStepTypes={data.alreadyFulfilledStepTypes}
              />
            </div>
          ) : (
            <Card
              title={t(
                `myArea.onboarding.wizard.stepTitles.${onboardingStepTypeKey(currentStep.stepType)}`,
              )}
              density="compact"
            >
              <OnboardingStepBody
                ref={stepRef}
                key={currentStep.stepType}
                access={access}
                stepType={currentStep.stepType}
                payload={currentStep.payload}
                config={currentStep.config}
                catalog={data.catalog}
                editable={data.editable}
                isSaving={saveStep.isPending || isNavigating}
                onUpload={uploadSignedDocument}
              />
            </Card>
          )}

          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-between">
            <Button
              variant="secondary"
              disabled={currentIndex === 0 || isNavigating}
              onClick={() => void navigateTo(Math.max(0, currentIndex - 1))}
              fullWidth="responsive"
            >
              {t('common.back')}
            </Button>
            {isReview ? (
              <Button
                disabled={!data.completionEnabled || completeOnboarding.isPending || isNavigating}
                isLoading={completeOnboarding.isPending || isNavigating}
                onClick={() => void handleComplete()}
                fullWidth="responsive"
              >
                {t('admin.leader.onboarding.complete')}
              </Button>
            ) : (
              <Button
                onClick={() => void navigateTo(currentIndex + 1)}
                isLoading={isNavigating}
                disabled={isNavigating}
                fullWidth="responsive"
              >
                {t('myArea.onboarding.wizard.next')}
              </Button>
            )}
          </div>
        </div>
      )}
    </>
  );
}

function firstOpenIndex(steps: { fulfilled: boolean }[]): number {
  const open = steps.findIndex((step) => !step.fulfilled);
  return open === -1 ? steps.length : open;
}
