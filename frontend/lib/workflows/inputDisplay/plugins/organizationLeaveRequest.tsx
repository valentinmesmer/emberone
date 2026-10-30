/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { FieldDisplay } from '@/components/ui/field-display';
import { type OrganizationLeaveRequestWorkflowPayload, WORKFLOW_TYPE_IDS } from '@/types/workflow';
import { parseOrganizationLeaveRequestPayload } from '../payloadParsers';
import type { WorkflowInputDisplayContext, WorkflowInputDisplayPlugin } from '../types';
import { WorkflowInputSectionCard } from '../WorkflowInputSectionCard';

function OrganizationLeaveRequestInputDisplay({
  ctx,
  payload,
}: {
  ctx: WorkflowInputDisplayContext;
  payload: OrganizationLeaveRequestWorkflowPayload;
}) {
  const { t } = ctx;
  const organizationName =
    payload.organizationName?.trim() ||
    (payload.organizationId != null ? `#${payload.organizationId}` : '-');

  return (
    <WorkflowInputSectionCard>
      <div className="space-y-3">
        <FieldDisplay
          label={t('myArea.organizationLeaveRequest.organizationLabel')}
          value={organizationName}
        />
        {payload.reason?.trim() ? (
          <FieldDisplay label={t('common.request.reasonLabel')} value={payload.reason.trim()} />
        ) : null}
      </div>
    </WorkflowInputSectionCard>
  );
}

export const organizationLeaveRequestInputDisplayPlugin: WorkflowInputDisplayPlugin<OrganizationLeaveRequestWorkflowPayload> =
  {
    workflowTypeId: WORKFLOW_TYPE_IDS.ORGANIZATION_LEAVE_REQUEST,
    parsePayload: parseOrganizationLeaveRequestPayload,
    render(ctx, payload) {
      return <OrganizationLeaveRequestInputDisplay ctx={ctx} payload={payload} />;
    },
  };
