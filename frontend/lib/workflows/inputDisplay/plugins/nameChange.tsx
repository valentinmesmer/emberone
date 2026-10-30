/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { FieldDisplay } from '@/components/ui/field-display';
import { normalizeWorkflowStatus } from '@/lib/utils/workflowApprovalSlot';
import {
  type NameChangeWorkflowPayload,
  WORKFLOW_TYPE_IDS,
  type WorkflowDto,
} from '@/types/workflow';
import { parseNameChangePayload } from '../payloadParsers';
import type {
  WorkflowInputDisplayContext,
  WorkflowInputDisplayPlugin,
  WorkflowInputSelfContext,
} from '../types';
import { WorkflowInputSectionCard } from '../WorkflowInputSectionCard';

function resolveNameChangePreviousNames(
  payload: NameChangeWorkflowPayload,
  wf: WorkflowDto,
  self?: WorkflowInputSelfContext,
): { firstName: string; lastName: string } {
  if (payload.previousFirstName != null && payload.previousLastName != null) {
    return { firstName: payload.previousFirstName, lastName: payload.previousLastName };
  }
  if (
    self != null &&
    wf.openedByUserId === self.id &&
    normalizeWorkflowStatus(wf.status) === 'OPEN'
  ) {
    return { firstName: self.firstName?.trim() ?? '', lastName: self.lastName?.trim() ?? '' };
  }
  return { firstName: '', lastName: '' };
}

function NameChangeInputDisplay({
  ctx,
  payload,
}: {
  ctx: WorkflowInputDisplayContext;
  payload: NameChangeWorkflowPayload;
}) {
  const { workflow, self, t } = ctx;
  const previous = resolveNameChangePreviousNames(payload, workflow, self);

  return (
    <WorkflowInputSectionCard>
      <div className="flex flex-col gap-4 sm:grid sm:grid-cols-2">
        <div className="space-y-3">
          <h3 className="text-xs font-semibold uppercase tracking-wide text-[var(--text-secondary)]">
            {t('myArea.workflowDetail.nameChangeBeforeSection')}
          </h3>
          <div className="grid grid-cols-2 gap-3">
            <FieldDisplay label={t('setup.user.firstName')} value={previous.firstName} />
            <FieldDisplay label={t('setup.user.lastName')} value={previous.lastName} />
          </div>
        </div>
        <div className="space-y-3">
          <h3 className="text-xs font-semibold uppercase tracking-wide text-[var(--text-secondary)]">
            {t('myArea.workflowDetail.nameChangeAfterSection')}
          </h3>
          <div className="grid grid-cols-2 gap-3">
            <FieldDisplay label={t('setup.user.firstName')} value={payload.firstName} />
            <FieldDisplay label={t('setup.user.lastName')} value={payload.lastName} />
          </div>
        </div>
      </div>
    </WorkflowInputSectionCard>
  );
}

export const nameChangeInputDisplayPlugin: WorkflowInputDisplayPlugin<NameChangeWorkflowPayload> = {
  workflowTypeId: WORKFLOW_TYPE_IDS.NAME_CHANGE,
  parsePayload: parseNameChangePayload,
  render(ctx, payload) {
    return <NameChangeInputDisplay ctx={ctx} payload={payload} />;
  },
};
