/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { FieldDisplay } from '@/components/ui/field-display';
import { useMyAvailableRanks } from '@/lib/store';
import { type RankRequestWorkflowPayload, WORKFLOW_TYPE_IDS } from '@/types/workflow';
import { parseRankRequestPayload } from '../payloadParsers';
import type { WorkflowInputDisplayContext, WorkflowInputDisplayPlugin } from '../types';
import { WorkflowInputSectionCard } from '../WorkflowInputSectionCard';

function RankRequestInputDisplay({
  ctx,
  payload,
}: {
  ctx: WorkflowInputDisplayContext;
  payload: RankRequestWorkflowPayload;
}) {
  const { t } = ctx;
  const { data: ranks = [] } = useMyAvailableRanks(true);
  const rank = ranks.find((item) => item.id === payload.tenantRankId);
  const rankLabel = rank ? `${rank.name} (${rank.abbreviation})` : String(payload.tenantRankId);

  return (
    <WorkflowInputSectionCard>
      <FieldDisplay label={t('ranks.request.rank')} value={rankLabel} />
      {payload.attachmentIds && payload.attachmentIds.length > 0 ? (
        <FieldDisplay
          label={t('ranks.request.proofOptional')}
          value={String(payload.attachmentIds.length)}
        />
      ) : null}
    </WorkflowInputSectionCard>
  );
}

export const rankRequestInputDisplayPlugin: WorkflowInputDisplayPlugin<RankRequestWorkflowPayload> =
  {
    workflowTypeId: WORKFLOW_TYPE_IDS.RANK_REQUEST,
    parsePayload: parseRankRequestPayload,
    render(ctx, payload) {
      return <RankRequestInputDisplay ctx={ctx} payload={payload} />;
    },
  };
