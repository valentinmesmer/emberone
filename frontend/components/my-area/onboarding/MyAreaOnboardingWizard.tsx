/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Check, Circle } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useCallback, useMemo, useRef, useState } from 'react';
import { onboardingStepTypeKey } from '@/components/admin/standards/onboardingStepUtils';
import { WorkflowInputChangeRequestNote } from '@/components/my-area/workflows/WorkflowInputChangeRequestNote';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard, ErrorState, NotFoundState } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { ApiRequestError } from '@/lib/api/client';
import { getMyAreaWorkflowDetailPath } from '@/lib/my-area/myAreaSections';
import {
  useOrganizationUserOnboardingDetail,
  useSaveOnboardingStep,
  useSelfOnboardingInvitationDetail,
  useSubmitOnboardingSubmissionWorkflow,
  useUploadOnboardingDocument,
} from '@/lib/store';
import { cn } from '@/lib/utils/cn';
import type { OnboardingInvitationAccess } from '@/types/onboardingInvitation';
import { OnboardingInvitationOverviewCard } from './OnboardingInvitationOverviewCard';
import { OnboardingInvitationStepsReview } from './OnboardingInvitationStepsReview';
import { OnboardingStepBody } from './steps/OnboardingStepBody';
import type { OnboardingStepHandle } from './steps/onboardingStepHandle';

interface MyAreaOnboardingWizardProps {
  invitationId?: number;
  organizationId?: number;
  userId?: number;
}

export function MyAreaOnboardingWizard({
  invitationId,
  organizationId,
  userId,
}: MyAreaOnboardingWizardProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();
  const readOnly = organizationId != null && userId != null;
  const selfDetail = useSelfOnboardingInvitationDetail(readOnly ? null : invitationId);
  const orgDetail = useOrganizationUserOnboardingDetail(
    readOnly ? organizationId : null,
    readOnly ? userId : null,
  );
  const detailQuery = readOnly ? orgDetail : selfDetail;
  const { data, isLoading, error } = detailQuery;
  const resolvedInvitationId = data?.id ?? invitationId ?? 0;
  const access: OnboardingInvitationAccess | null = readOnly
    ? organizationId != null && resolvedInvitationId > 0
      ? { kind: 'organization', organizationId, invitationId: resolvedInvitationId }
      : null
    : resolvedInvitationId > 0
      ? { kind: 'self', invitationId: resolvedInvitationId }
      : null;
  const saveStep = useSaveOnboardingStep(access);
  const uploadDocument = useUploadOnboardingDocument(access);
  const submitOnboarding = useSubmitOnboardingSubmissionWorkflow();
  const stepRef = useRef<OnboardingStepHandle>(null);
  const initialIndexRef = useRef<number | null>(null);
  const [index, setIndex] = useState<number | null>(null);
  const [isNavigating, setIsNavigating] = useState(false);

  const steps = data?.steps ?? [];
  const reviewIndex = steps.length;

  if (data && initialIndexRef.current === null) {
    initialIndexRef.current = firstOpenIndex(steps);
  }

  const currentIndex = index ?? initialIndexRef.current ?? 0;
  const isReview = currentIndex >= reviewIndex;
  const currentStep = !isReview ? steps[currentIndex] : undefined;
  const showVerificationOnly = Boolean((readOnly || (data && !data.editable)) && access);

  const saveCurrentStepIfChanged = useCallback(async (): Promise<boolean> => {
    if (readOnly || !data?.editable || isReview || !currentStep) return true;
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
      showError(err instanceof Error ? err.message : t('myArea.error.loadFailed'));
      return false;
    }
  }, [currentStep, data?.editable, isReview, readOnly, saveStep, showError, showSuccess, t]);

  const handleComplete = useCallback(async () => {
    if (!data?.completionEnabled || readOnly) return;
    setIsNavigating(true);
    try {
      const saved = await saveCurrentStepIfChanged();
      if (!saved) return;
      const workflow = await submitOnboarding.mutateAsync(data.id);
      showSuccess(t('myArea.onboarding.wizard.submitted'));
      router.push(getMyAreaWorkflowDetailPath(workflow.id));
    } catch (err) {
      showError(err instanceof Error ? err.message : t('myArea.error.loadFailed'));
    } finally {
      setIsNavigating(false);
    }
  }, [
    data?.completionEnabled,
    data?.id,
    readOnly,
    router,
    saveCurrentStepIfChanged,
    showError,
    showSuccess,
    submitOnboarding,
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
      return t('myArea.onboarding.wizard.noSteps');
    }
    if (readOnly || isReview || steps.length === 0 || showVerificationOnly)
      return t('myArea.onboarding.wizard.reviewProgress');
    return t('myArea.onboarding.wizard.progress', {
      current: currentIndex + 1,
      total: steps.length,
    });
  }, [currentIndex, data, isReview, readOnly, showVerificationOnly, steps.length, t]);

  const uploadSignedDocument = useCallback(
    async (configKey: string, file: File) => {
      const scope =
        currentStep?.stepType === 'ORGANIZATION_ENTRY_DOCUMENTS' ? 'organization' : 'tenant';
      await uploadDocument.mutateAsync({ configKey, file, scope });
    },
    [currentStep?.stepType, uploadDocument],
  );

  if (error) {
    const notFound = error instanceof ApiRequestError && error.status === 404;
    const message = notFound
      ? t('myArea.onboarding.wizard.notFound')
      : t('myArea.error.loadFailed');
    if (notFound) {
      return <NotFoundState message={message} className="mt-1 sm:mt-0" />;
    }
    return <ErrorState message={message} className="mt-1 sm:mt-0" />;
  }

  if (isLoading || !data) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  return (
    <>
      {!showVerificationOnly ? (
        <PageHeader
          title={data.targetOrganizationName}
          description={progressLabel}
          size="compact"
        />
      ) : null}

      {steps.length === 0 && !data.allStepsFulfilled ? (
        <EmptyStateCard className="mt-1 sm:mt-0">
          {t('myArea.onboarding.wizard.noSteps')}
        </EmptyStateCard>
      ) : showVerificationOnly && access ? (
        <div className="w-full space-y-4">
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
      ) : (
        <div className="mt-1 space-y-4 sm:mt-0">
          {data.inputChangeMessage ? (
            <WorkflowInputChangeRequestNote message={data.inputChangeMessage} />
          ) : null}
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
                  ? t('myArea.onboarding.wizard.reviewReady')
                  : t('myArea.onboarding.wizard.reviewMissing')}
              </p>
              {access ? (
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
              ) : null}
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
                access={access ?? { kind: 'self', invitationId: resolvedInvitationId }}
                stepType={currentStep.stepType}
                payload={currentStep.payload}
                config={currentStep.config}
                catalog={data.catalog}
                editable={!readOnly && data.editable}
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
                disabled={!data.completionEnabled || submitOnboarding.isPending || isNavigating}
                isLoading={submitOnboarding.isPending || isNavigating}
                onClick={() => void handleComplete()}
                fullWidth="responsive"
              >
                {t('myArea.onboarding.wizard.complete')}
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
