/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { OnboardingInvitationStepsReview } from '@/components/my-area/onboarding/OnboardingInvitationStepsReview';
import { LoadingState } from '@/components/ui/loading';
import { ErrorState } from '@/components/ui/message-cards';
import {
  useOrganizationOnboardingInvitationDetail,
  useSelfOnboardingInvitationDetail,
} from '@/lib/store';
import type { OnboardingInvitationAccess } from '@/types/onboardingInvitation';
import { type OnboardingSubmissionWorkflowPayload, WORKFLOW_TYPE_IDS } from '@/types/workflow';
import { parseOnboardingSubmissionPayload } from '../payloadParsers';
import { resolveWorkflowContextOrganizationId } from '../resolveWorkflowContextOrganizationId';
import type { WorkflowInputDisplayContext, WorkflowInputDisplayPlugin } from '../types';

function OnboardingSubmissionInputDisplay({
  ctx,
  payload,
}: {
  ctx: WorkflowInputDisplayContext;
  payload: OnboardingSubmissionWorkflowPayload;
}) {
  const { workflow, self } = ctx;
  const isOpener = self?.id === workflow.openedByUserId;
  const organizationId = resolveWorkflowContextOrganizationId(workflow);
  const access: OnboardingInvitationAccess | null = isOpener
    ? { kind: 'self', invitationId: payload.invitationId }
    : organizationId != null
      ? {
          kind: 'organization',
          organizationId,
          invitationId: payload.invitationId,
        }
      : null;

  const selfDetail = useSelfOnboardingInvitationDetail(isOpener ? payload.invitationId : null);
  const orgDetail = useOrganizationOnboardingInvitationDetail(
    isOpener ? null : organizationId,
    isOpener ? null : payload.invitationId,
  );
  const { data, isLoading, error } = isOpener ? selfDetail : orgDetail;

  if (isLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (error || !data || access == null) {
    return <ErrorState message={ctx.t('myArea.error.loadFailed')} />;
  }

  const subjectUserId =
    data.subjectUserId ?? (isOpener ? ctx.self?.id : workflow.openedByUserId) ?? 0;

  return (
    <OnboardingInvitationStepsReview
      access={access}
      status={data.status}
      invitationId={data.id}
      targetOrganizationId={data.targetOrganizationId}
      subjectUserId={subjectUserId}
      steps={data.steps}
      catalog={data.catalog}
      alreadyFulfilledStepTypes={data.alreadyFulfilledStepTypes}
    />
  );
}

export const onboardingSubmissionInputDisplayPlugin: WorkflowInputDisplayPlugin<OnboardingSubmissionWorkflowPayload> =
  {
    workflowTypeId: WORKFLOW_TYPE_IDS.ONBOARDING_SUBMISSION,
    parsePayload: parseOnboardingSubmissionPayload,
    treatParsedAsProof: false,
    render(ctx, payload) {
      return <OnboardingSubmissionInputDisplay ctx={ctx} payload={payload} />;
    },
  };
