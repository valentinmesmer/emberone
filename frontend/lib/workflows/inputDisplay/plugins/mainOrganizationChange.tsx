/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { FieldDisplay } from '@/components/ui/field-display';
import { type MainOrganizationChangeWorkflowPayload, WORKFLOW_TYPE_IDS } from '@/types/workflow';
import { parseMainOrganizationChangePayload } from '../payloadParsers';
import type { WorkflowInputDisplayContext, WorkflowInputDisplayPlugin } from '../types';
import { WorkflowInputSectionCard } from '../WorkflowInputSectionCard';

function MainOrganizationChangeInputDisplay({
  ctx,
  payload,
}: {
  ctx: WorkflowInputDisplayContext;
  payload: MainOrganizationChangeWorkflowPayload;
}) {
  const { t } = ctx;
  const organizationName =
    payload.newMainOrganizationName?.trim() ||
    (payload.newMainOrganizationId != null ? `#${payload.newMainOrganizationId}` : '-');

  return (
    <WorkflowInputSectionCard>
      <div className="space-y-3">
        <FieldDisplay
          label={t('myArea.mainOrganizationChange.newMainOrganizationLabel')}
          value={organizationName}
        />
        {payload.reason?.trim() ? (
          <FieldDisplay label={t('common.request.reasonLabel')} value={payload.reason.trim()} />
        ) : null}
      </div>
    </WorkflowInputSectionCard>
  );
}

export const mainOrganizationChangeInputDisplayPlugin: WorkflowInputDisplayPlugin<MainOrganizationChangeWorkflowPayload> =
  {
    workflowTypeId: WORKFLOW_TYPE_IDS.MAIN_ORGANIZATION_CHANGE,
    parsePayload: parseMainOrganizationChangePayload,
    render(ctx, payload) {
      return <MainOrganizationChangeInputDisplay ctx={ctx} payload={payload} />;
    },
  };
