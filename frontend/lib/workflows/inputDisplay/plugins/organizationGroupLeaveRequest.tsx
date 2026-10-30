/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { FieldDisplay } from '@/components/ui/field-display';
import {
  type OrganizationGroupLeaveRequestWorkflowPayload,
  WORKFLOW_TYPE_IDS,
} from '@/types/workflow';
import { parseOrganizationGroupLeaveRequestPayload } from '../payloadParsers';
import type { WorkflowInputDisplayContext, WorkflowInputDisplayPlugin } from '../types';
import { WorkflowInputSectionCard } from '../WorkflowInputSectionCard';

function OrganizationGroupLeaveRequestInputDisplay({
  ctx,
  payload,
}: {
  ctx: WorkflowInputDisplayContext;
  payload: OrganizationGroupLeaveRequestWorkflowPayload;
}) {
  const { t } = ctx;
  const organizationName =
    payload.organizationName?.trim() ||
    (payload.organizationId != null ? `#${payload.organizationId}` : '-');
  const groupName =
    payload.organizationGroupName?.trim() ||
    (payload.organizationGroupId != null ? `#${payload.organizationGroupId}` : '-');

  return (
    <WorkflowInputSectionCard>
      <div className="space-y-3">
        <FieldDisplay
          label={t('myArea.organizationGroupLeaveRequest.organizationLabel')}
          value={organizationName}
        />
        <FieldDisplay
          label={t('myArea.organizationGroupLeaveRequest.organizationGroupLabel')}
          value={groupName}
        />
        {payload.reason?.trim() ? (
          <FieldDisplay label={t('common.request.reasonLabel')} value={payload.reason.trim()} />
        ) : null}
      </div>
    </WorkflowInputSectionCard>
  );
}

export const organizationGroupLeaveRequestInputDisplayPlugin: WorkflowInputDisplayPlugin<OrganizationGroupLeaveRequestWorkflowPayload> =
  {
    workflowTypeId: WORKFLOW_TYPE_IDS.ORGANIZATION_GROUP_LEAVE_REQUEST,
    parsePayload: parseOrganizationGroupLeaveRequestPayload,
    render(ctx, payload) {
      return <OrganizationGroupLeaveRequestInputDisplay ctx={ctx} payload={payload} />;
    },
  };
