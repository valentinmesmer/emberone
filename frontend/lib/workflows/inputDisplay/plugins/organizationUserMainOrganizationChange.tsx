/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { FieldDisplay } from '@/components/ui/field-display';
import {
  type OrganizationUserMainOrganizationChangeWorkflowPayload,
  WORKFLOW_TYPE_IDS,
} from '@/types/workflow';
import { parseOrganizationUserMainOrganizationChangePayload } from '../payloadParsers';
import type { WorkflowInputDisplayContext, WorkflowInputDisplayPlugin } from '../types';
import { WorkflowInputSectionCard } from '../WorkflowInputSectionCard';

function OrganizationUserMainOrganizationChangeInputDisplay({
  ctx,
  payload,
}: {
  ctx: WorkflowInputDisplayContext;
  payload: OrganizationUserMainOrganizationChangeWorkflowPayload;
}) {
  const { t } = ctx;
  const memberName =
    payload.targetUserDisplayName?.trim() ||
    (payload.targetUserId != null ? `#${payload.targetUserId}` : '-');
  const newMainName =
    payload.newMainOrganizationName?.trim() ||
    (payload.newMainOrganizationId != null ? `#${payload.newMainOrganizationId}` : '-');

  return (
    <WorkflowInputSectionCard>
      <div className="space-y-3">
        <FieldDisplay
          label={t('userManagement.userDeactivationWorkflow.memberLabel')}
          value={memberName}
        />
        <FieldDisplay
          label={t('userManagement.mainOrganizationChange.targetLabel')}
          value={newMainName}
        />
        {payload.reason?.trim() ? (
          <FieldDisplay
            label={t('userManagement.mainOrganizationChange.reasonLabel')}
            value={payload.reason.trim()}
          />
        ) : null}
      </div>
    </WorkflowInputSectionCard>
  );
}

export const organizationUserMainOrganizationChangeInputDisplayPlugin: WorkflowInputDisplayPlugin<OrganizationUserMainOrganizationChangeWorkflowPayload> =
  {
    workflowTypeId: WORKFLOW_TYPE_IDS.ORGANIZATION_USER_MAIN_ORGANIZATION_CHANGE,
    parsePayload: parseOrganizationUserMainOrganizationChangePayload,
    render(ctx, payload) {
      return <OrganizationUserMainOrganizationChangeInputDisplay ctx={ctx} payload={payload} />;
    },
  };
