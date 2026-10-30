/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { FieldDisplay } from '@/components/ui/field-display';
import {
  type OrganizationMembershipEndRequestWorkflowPayload,
  WORKFLOW_TYPE_IDS,
} from '@/types/workflow';
import { parseOrganizationMembershipEndRequestPayload } from '../payloadParsers';
import type { WorkflowInputDisplayContext, WorkflowInputDisplayPlugin } from '../types';
import { WorkflowInputSectionCard } from '../WorkflowInputSectionCard';

function OrganizationMembershipEndRequestInputDisplay({
  ctx,
  payload,
}: {
  ctx: WorkflowInputDisplayContext;
  payload: OrganizationMembershipEndRequestWorkflowPayload;
}) {
  const { t } = ctx;
  const memberName =
    payload.targetUserDisplayName?.trim() ||
    (payload.targetUserId != null ? `#${payload.targetUserId}` : '-');
  const organizationName =
    payload.organizationName?.trim() ||
    (payload.organizationId != null ? `#${payload.organizationId}` : '-');

  return (
    <WorkflowInputSectionCard>
      <div className="space-y-3">
        <FieldDisplay
          label={t('userManagement.organizationMembershipEndWorkflow.memberLabel')}
          value={memberName}
        />
        <FieldDisplay
          label={t('userManagement.organizationMembershipEndWorkflow.organizationLabel')}
          value={organizationName}
        />
        {payload.reason?.trim() ? (
          <FieldDisplay
            label={t('userManagement.organizationMembershipEndWorkflow.reasonLabel')}
            value={payload.reason.trim()}
          />
        ) : null}
      </div>
    </WorkflowInputSectionCard>
  );
}

export const organizationMembershipEndRequestInputDisplayPlugin: WorkflowInputDisplayPlugin<OrganizationMembershipEndRequestWorkflowPayload> =
  {
    workflowTypeId: WORKFLOW_TYPE_IDS.ORGANIZATION_MEMBERSHIP_END_REQUEST,
    parsePayload: parseOrganizationMembershipEndRequestPayload,
    treatParsedAsProof: false,
    render(ctx, payload) {
      return <OrganizationMembershipEndRequestInputDisplay ctx={ctx} payload={payload} />;
    },
  };
