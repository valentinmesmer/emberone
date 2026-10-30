/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { FieldDisplay } from '@/components/ui/field-display';
import { NoticeCard } from '@/components/ui/message-cards';
import {
  type CourseOfferingRegistrationWorkflowPayload,
  WORKFLOW_TYPE_IDS,
} from '@/types/workflow';
import { parseCourseOfferingRegistrationPayload } from '../payloadParsers';
import type { WorkflowInputDisplayContext, WorkflowInputDisplayPlugin } from '../types';
import { WorkflowInputSectionCard } from '../WorkflowInputSectionCard';

function CourseOfferingRegistrationInputDisplay({
  ctx,
  payload,
}: {
  ctx: WorkflowInputDisplayContext;
  payload: CourseOfferingRegistrationWorkflowPayload;
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

  return (
    <WorkflowInputSectionCard>
      <div className="space-y-3">
        <FieldDisplay
          label={t('courseOfferings.workflow.registration.courseType')}
          value={typeName}
        />
        {offeringOrg ? (
          <FieldDisplay
            label={t('courseOfferings.workflow.registration.offeringOrganization')}
            value={offeringOrg}
          />
        ) : null}
        {homeOrg && payload.homeOrganizationId !== payload.offeringOrganizationId ? (
          <FieldDisplay
            label={t('courseOfferings.workflow.registration.homeOrganization')}
            value={homeOrg}
          />
        ) : null}
        {payload.alreadyHeld ? (
          <NoticeCard>{t('courseOfferings.workflow.registration.alreadyHeld')}</NoticeCard>
        ) : null}
      </div>
    </WorkflowInputSectionCard>
  );
}

export const courseOfferingRegistrationInputDisplayPlugin: WorkflowInputDisplayPlugin<CourseOfferingRegistrationWorkflowPayload> =
  {
    workflowTypeId: WORKFLOW_TYPE_IDS.COURSE_OFFERING_REGISTRATION,
    parsePayload: parseCourseOfferingRegistrationPayload,
    render(ctx, payload) {
      return <CourseOfferingRegistrationInputDisplay ctx={ctx} payload={payload} />;
    },
  };
