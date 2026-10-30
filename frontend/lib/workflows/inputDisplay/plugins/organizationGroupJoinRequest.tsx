/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { FieldDisplay } from '@/components/ui/field-display';
import {
  type OrganizationGroupJoinRequestWorkflowPayload,
  WORKFLOW_TYPE_IDS,
} from '@/types/workflow';
import { parseOrganizationGroupJoinRequestPayload } from '../payloadParsers';
import type { WorkflowInputDisplayContext, WorkflowInputDisplayPlugin } from '../types';
import { WorkflowInputSectionCard } from '../WorkflowInputSectionCard';

function OrganizationGroupJoinRequestInputDisplay({
  ctx,
  payload,
}: {
  ctx: WorkflowInputDisplayContext;
  payload: OrganizationGroupJoinRequestWorkflowPayload;
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
          label={t('myArea.organizationGroupJoinRequest.organizationLabel')}
          value={organizationName}
        />
        <FieldDisplay
          label={t('myArea.organizationGroupJoinRequest.organizationGroupLabel')}
          value={groupName}
        />
        {payload.reason?.trim() ? (
          <FieldDisplay label={t('common.request.reasonLabel')} value={payload.reason.trim()} />
        ) : null}
      </div>
    </WorkflowInputSectionCard>
  );
}

export const organizationGroupJoinRequestInputDisplayPlugin: WorkflowInputDisplayPlugin<OrganizationGroupJoinRequestWorkflowPayload> =
  {
    workflowTypeId: WORKFLOW_TYPE_IDS.ORGANIZATION_GROUP_JOIN_REQUEST,
    parsePayload: parseOrganizationGroupJoinRequestPayload,
    render(ctx, payload) {
      return <OrganizationGroupJoinRequestInputDisplay ctx={ctx} payload={payload} />;
    },
  };
