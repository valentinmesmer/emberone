/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { FieldDisplay } from '@/components/ui/field-display';
import { formatDateShort } from '@/lib/utils/date';
import {
  type CourseRegistrationCancellationWorkflowPayload,
  WORKFLOW_TYPE_IDS,
} from '@/types/workflow';
import { parseCourseRegistrationCancellationPayload } from '../payloadParsers';
import type { WorkflowInputDisplayContext, WorkflowInputDisplayPlugin } from '../types';
import { WorkflowInputSectionCard } from '../WorkflowInputSectionCard';

function CourseRegistrationCancellationInputDisplay({
  ctx,
  payload,
}: {
  ctx: WorkflowInputDisplayContext;
  payload: CourseRegistrationCancellationWorkflowPayload;
}) {
  const { t } = ctx;
  const typeName =
    payload.trainingCourseTypeName?.trim() ||
    (payload.trainingCourseTypeId != null ? `#${payload.trainingCourseTypeId}` : '-');
  const offeringOrg =
    payload.offeringOrganizationName?.trim() ||
    (payload.offeringOrganizationId != null ? `#${payload.offeringOrganizationId}` : null);
  const homeOrg =
    payload.homeOrganizationName?.trim() ||
    (payload.homeOrganizationId != null ? `#${payload.homeOrganizationId}` : null);
  const sessionStart = payload.firstSessionStart
    ? formatDateShort(payload.firstSessionStart)
    : null;

  return (
    <WorkflowInputSectionCard>
      <div className="space-y-3">
        <FieldDisplay
          label={t('courseOfferings.workflow.cancellation.courseType')}
          value={typeName}
        />
        {sessionStart ? (
          <FieldDisplay
            label={t('courseOfferings.workflow.cancellation.firstSession')}
            value={sessionStart}
          />
        ) : null}
        {offeringOrg ? (
          <FieldDisplay
            label={t('courseOfferings.workflow.cancellation.offeringOrganization')}
            value={offeringOrg}
          />
        ) : null}
        {homeOrg ? (
          <FieldDisplay
            label={t('courseOfferings.workflow.cancellation.homeOrganization')}
            value={homeOrg}
          />
        ) : null}
      </div>
    </WorkflowInputSectionCard>
  );
}

export const courseRegistrationCancellationInputDisplayPlugin: WorkflowInputDisplayPlugin<CourseRegistrationCancellationWorkflowPayload> =
  {
    workflowTypeId: WORKFLOW_TYPE_IDS.COURSE_REGISTRATION_CANCELLATION,
    parsePayload: parseCourseRegistrationCancellationPayload,
    render(ctx, payload) {
      return <CourseRegistrationCancellationInputDisplay ctx={ctx} payload={payload} />;
    },
  };
