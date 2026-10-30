/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { FieldDisplay } from '@/components/ui/field-display';
import {
  useMyAvailableTrainingCourseTypes,
  useMyOrganizations,
  useOrgTrainingCourseCatalog,
} from '@/lib/store';
import { trainingCourseTypeIdsFromPayload } from '@/lib/trainingCourses/submittableTrainingCourses';
import { formatDateLong } from '@/lib/utils';
import { type TrainingCourseRecordWorkflowPayload, WORKFLOW_TYPE_IDS } from '@/types/workflow';
import { parseTrainingCourseRecordPayload } from '../payloadParsers';
import { resolveWorkflowContextOrganizationId } from '../resolveWorkflowContextOrganizationId';
import type { WorkflowInputDisplayContext, WorkflowInputDisplayPlugin } from '../types';
import { WorkflowInputSectionCard } from '../WorkflowInputSectionCard';

function TrainingCourseRecordInputDisplay({
  ctx,
  payload,
}: {
  ctx: WorkflowInputDisplayContext;
  payload: TrainingCourseRecordWorkflowPayload;
}) {
  const { workflow, self, t } = ctx;
  const isOpener = self?.id === workflow.openedByUserId;
  const orgFromWorkflow = resolveWorkflowContextOrganizationId(workflow);
  const useOrgCatalog = !isOpener && orgFromWorkflow != null;

  const { data: organizations = [] } = useMyOrganizations();
  const catalogOrgId = useMemo(() => {
    if (orgFromWorkflow != null) return orgFromWorkflow;
    const main = organizations.find((membership) => membership.main)?.organization?.id;
    if (main != null) return main;
    return organizations[0]?.organization?.id ?? null;
  }, [orgFromWorkflow, organizations]);

  const { data: myTypes = [] } = useMyAvailableTrainingCourseTypes(isOpener);
  const { data: orgCatalog } = useOrgTrainingCourseCatalog(
    catalogOrgId ?? 0,
    workflow.openedByUserId,
    useOrgCatalog && catalogOrgId != null,
  );

  const typeIds = trainingCourseTypeIdsFromPayload(payload);
  const resolveTypeName = (typeId: number) => {
    const fromSelf = myTypes.find((type) => type.id === typeId)?.name;
    if (fromSelf?.trim()) return fromSelf.trim();
    const fromOrg = orgCatalog?.types.find((type) => type.id === typeId)?.name;
    if (fromOrg?.trim()) return fromOrg.trim();
    return `#${typeId}`;
  };

  const courseName =
    payload.name?.trim() || (typeIds.length === 1 ? resolveTypeName(typeIds[0]) : null);

  return (
    <WorkflowInputSectionCard>
      {courseName ? (
        <FieldDisplay label={t('trainingCourse.submissionName.label')} value={courseName} />
      ) : null}
      {typeIds.length > 1 ? (
        <div className="space-y-1.5">
          <p className="text-xs font-semibold uppercase tracking-wide text-[var(--text-secondary)]">
            {t('trainingCourse.grantedTypes.label')}
          </p>
          <ul className="space-y-1">
            {typeIds.map((typeId) => (
              <li key={typeId} className="text-sm font-medium text-[var(--text-primary)]">
                {resolveTypeName(typeId)}
              </li>
            ))}
          </ul>
        </div>
      ) : null}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <FieldDisplay
          label={t('trainingCourse.record.startDateLabel')}
          value={formatDateLong(payload.startDate)}
        />
        <FieldDisplay
          label={t('trainingCourse.record.endDateLabel')}
          value={formatDateLong(payload.endDate)}
        />
        <FieldDisplay label={t('trainingCourse.record.locationLabel')} value={payload.location} />
        {payload.hours != null ? (
          <FieldDisplay
            label={t('trainingCourse.record.hoursLabel')}
            value={t('trainingCourse.record.hours', { hours: payload.hours })}
          />
        ) : null}
      </div>
      {payload.notes?.trim() ? (
        <div className="border-t border-[var(--border-color)]/30 pt-3">
          <FieldDisplay label={t('common.record.notesLabel')} value={payload.notes} />
        </div>
      ) : null}
    </WorkflowInputSectionCard>
  );
}

export const trainingCourseRecordInputDisplayPlugin: WorkflowInputDisplayPlugin<TrainingCourseRecordWorkflowPayload> =
  {
    workflowTypeId: WORKFLOW_TYPE_IDS.TRAINING_COURSE_RECORD,
    parsePayload: parseTrainingCourseRecordPayload,
    render(ctx, payload) {
      return <TrainingCourseRecordInputDisplay ctx={ctx} payload={payload} />;
    },
  };
