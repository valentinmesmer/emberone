/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { Badge } from '@/components/ui/badge';
import { FieldDisplay } from '@/components/ui/field-display';
import { FormLabel } from '@/components/ui/form-label';
import { formatUserDocumentTag } from '@/lib/utils/userDocumentFormat';
import { type UserDocumentSubmissionWorkflowPayload, WORKFLOW_TYPE_IDS } from '@/types/workflow';
import { parseUserDocumentSubmissionPayload } from '../payloadParsers';
import type { WorkflowInputDisplayContext, WorkflowInputDisplayPlugin } from '../types';
import { WorkflowInputSectionCard } from '../WorkflowInputSectionCard';

function UserDocumentSubmissionInputDisplay({
  ctx,
  payload,
}: {
  ctx: WorkflowInputDisplayContext;
  payload: UserDocumentSubmissionWorkflowPayload;
}) {
  const { t } = ctx;

  return (
    <WorkflowInputSectionCard>
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <FieldDisplay label={t('myArea.documents.titleLabel')} value={payload.title} />
        {payload.description?.trim() ? (
          <FieldDisplay
            label={t('myArea.documents.descriptionLabel')}
            value={payload.description}
          />
        ) : null}
        {payload.tags && payload.tags.length > 0 ? (
          <div>
            <FormLabel>{t('myArea.documents.tagsLabel')}</FormLabel>
            <div className="mt-1 flex flex-wrap gap-1">
              {payload.tags.map((tag) => (
                <Badge key={tag} tone="neutral">
                  {formatUserDocumentTag(tag, t)}
                </Badge>
              ))}
            </div>
          </div>
        ) : null}
      </div>
    </WorkflowInputSectionCard>
  );
}

export const userDocumentSubmissionInputDisplayPlugin: WorkflowInputDisplayPlugin<UserDocumentSubmissionWorkflowPayload> =
  {
    workflowTypeId: WORKFLOW_TYPE_IDS.USER_DOCUMENT_SUBMISSION,
    parsePayload: parseUserDocumentSubmissionPayload,
    render(ctx, payload) {
      return <UserDocumentSubmissionInputDisplay ctx={ctx} payload={payload} />;
    },
  };
