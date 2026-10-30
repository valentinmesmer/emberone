/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { FieldDisplay } from '@/components/ui/field-display';
import { type OrganizationJoinRequestWorkflowPayload, WORKFLOW_TYPE_IDS } from '@/types/workflow';
import { parseOrganizationJoinRequestPayload } from '../payloadParsers';
import type { WorkflowInputDisplayContext, WorkflowInputDisplayPlugin } from '../types';
import { WorkflowInputSectionCard } from '../WorkflowInputSectionCard';

function OrganizationJoinRequestInputDisplay({
  ctx,
  payload,
}: {
  ctx: WorkflowInputDisplayContext;
  payload: OrganizationJoinRequestWorkflowPayload;
}) {
  const { t } = ctx;
  const organizationName =
    payload.targetOrganizationName?.trim() ||
    (payload.targetOrganizationId != null ? `#${payload.targetOrganizationId}` : '-');

  return (
    <WorkflowInputSectionCard>
      <div className="space-y-3">
        <FieldDisplay
          label={t('myArea.organizationJoinRequest.targetOrganizationLabel')}
          value={organizationName}
        />
        {payload.reason?.trim() ? (
          <FieldDisplay label={t('common.request.reasonLabel')} value={payload.reason.trim()} />
        ) : null}
      </div>
    </WorkflowInputSectionCard>
  );
}

export const organizationJoinRequestInputDisplayPlugin: WorkflowInputDisplayPlugin<OrganizationJoinRequestWorkflowPayload> =
  {
    workflowTypeId: WORKFLOW_TYPE_IDS.ORGANIZATION_JOIN_REQUEST,
    parsePayload: parseOrganizationJoinRequestPayload,
    render(ctx, payload) {
      return <OrganizationJoinRequestInputDisplay ctx={ctx} payload={payload} />;
    },
  };
