/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { FieldDisplay } from '@/components/ui/field-display';
import {
  useMyCertificationStatus,
  useMyOrganizations,
  useMyRequestableCertificationDefinitions,
  useOrgCertificationCatalog,
  useUserCertificationStatus,
} from '@/lib/store';
import { formatDateLong } from '@/lib/utils';
import { type CertificationRecordWorkflowPayload, WORKFLOW_TYPE_IDS } from '@/types/workflow';
import { parseCertificationRecordPayload } from '../payloadParsers';
import { resolveWorkflowContextOrganizationId } from '../resolveWorkflowContextOrganizationId';
import type { WorkflowInputDisplayContext, WorkflowInputDisplayPlugin } from '../types';
import { WorkflowInputSectionCard } from '../WorkflowInputSectionCard';

function CertificationRecordInputDisplay({
  ctx,
  payload,
}: {
  ctx: WorkflowInputDisplayContext;
  payload: CertificationRecordWorkflowPayload;
}) {
  const { workflow, self, t } = ctx;
  const isOpener = self?.id === workflow.openedByUserId;
  const orgFromWorkflow = resolveWorkflowContextOrganizationId(workflow);
  const useOrgStatus = !isOpener && orgFromWorkflow != null;

  const { data: organizations = [] } = useMyOrganizations();
  const catalogOrgId = useMemo(() => {
    if (orgFromWorkflow != null) return orgFromWorkflow;
    const main = organizations.find((membership) => membership.main)?.organization?.id;
    if (main != null) return main;
    return organizations[0]?.organization?.id ?? null;
  }, [orgFromWorkflow, organizations]);

  const { data: myStatus } = useMyCertificationStatus();
  const { data: userStatus } = useUserCertificationStatus(
    orgFromWorkflow ?? 0,
    workflow.openedByUserId,
    useOrgStatus,
  );
  const { data: requestableDefinitions = [] } = useMyRequestableCertificationDefinitions(isOpener);
  const { data: orgCatalog } = useOrgCertificationCatalog(
    catalogOrgId ?? 0,
    workflow.openedByUserId,
    useOrgStatus && catalogOrgId != null,
  );

  const definitionLabel = useMemo(() => {
    const definitionId = payload.tenantCertDefinitionId;
    const fromStatus = (useOrgStatus ? userStatus : myStatus)?.items ?? [];
    const statusName = fromStatus.find(
      (item) => item.tenantCertDefinitionId === definitionId,
    )?.certDefinitionName;
    if (statusName?.trim()) return statusName.trim();

    const fromRequestable = requestableDefinitions.find(
      (definition) => definition.id === definitionId,
    );
    if (fromRequestable?.name?.trim()) return fromRequestable.name.trim();

    const fromCatalog = orgCatalog?.definitions.find(
      (definition) => definition.id === definitionId,
    );
    if (fromCatalog?.name?.trim()) return fromCatalog.name.trim();

    return `#${definitionId}`;
  }, [
    myStatus,
    orgCatalog?.definitions,
    payload.tenantCertDefinitionId,
    requestableDefinitions,
    useOrgStatus,
    userStatus,
  ]);

  return (
    <WorkflowInputSectionCard>
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <FieldDisplay label={t('certifications.submit.definitionLabel')} value={definitionLabel} />
        <FieldDisplay
          label={t('certifications.submit.completedDateLabel')}
          value={formatDateLong(payload.completedDate)}
        />
      </div>
      {payload.notes?.trim() ? (
        <div className="border-t border-[var(--border-color)]/30 pt-3">
          <FieldDisplay label={t('certifications.submit.notesLabel')} value={payload.notes} />
        </div>
      ) : null}
    </WorkflowInputSectionCard>
  );
}

export const certificationRecordInputDisplayPlugin: WorkflowInputDisplayPlugin<CertificationRecordWorkflowPayload> =
  {
    workflowTypeId: WORKFLOW_TYPE_IDS.CERTIFICATION_RECORD,
    parsePayload: parseCertificationRecordPayload,
    render(ctx, payload) {
      return <CertificationRecordInputDisplay ctx={ctx} payload={payload} />;
    },
  };
