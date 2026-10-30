/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { Badge } from '@/components/ui/badge';
import { FieldDisplay } from '@/components/ui/field-display';
import {
  useMyAvailableDrivingLicenseDefinitions,
  useMyOrganizations,
  useOrgDrivingLicenseCatalog,
} from '@/lib/store';
import { formatDateLong } from '@/lib/utils';
import { type DrivingLicenseRecordWorkflowPayload, WORKFLOW_TYPE_IDS } from '@/types/workflow';
import { parseDrivingLicenseRecordPayload } from '../payloadParsers';
import { resolveWorkflowContextOrganizationId } from '../resolveWorkflowContextOrganizationId';
import type { WorkflowInputDisplayContext, WorkflowInputDisplayPlugin } from '../types';
import { WorkflowInputSectionCard } from '../WorkflowInputSectionCard';

function DrivingLicenseRecordInputDisplay({
  ctx,
  payload,
}: {
  ctx: WorkflowInputDisplayContext;
  payload: DrivingLicenseRecordWorkflowPayload;
}) {
  const { workflow, self, t } = ctx;
  const isOpener = self?.id === workflow.openedByUserId;
  const orgFromWorkflow = resolveWorkflowContextOrganizationId(workflow);
  const useOrgCatalog = !isOpener && orgFromWorkflow != null;

  const { data: organizations = [] } = useMyOrganizations();
  const catalogOrgId = useMemo(() => {
    if (orgFromWorkflow != null) return orgFromWorkflow;
    const main = organizations.find((membership) => membership.main)?.organization?.id;
    if (main != null) return main;
    return organizations[0]?.organization?.id ?? null;
  }, [orgFromWorkflow, organizations]);

  const { data: myDefinitions = [] } = useMyAvailableDrivingLicenseDefinitions(isOpener);
  const { data: orgCatalog } = useOrgDrivingLicenseCatalog(
    catalogOrgId ?? 0,
    workflow.openedByUserId,
    useOrgCatalog && catalogOrgId != null,
  );

  const resolveClassName = (definitionId: number) => {
    const fromSelf = myDefinitions.find((definition) => definition.id === definitionId)?.name;
    if (fromSelf?.trim()) return fromSelf.trim();
    const fromOrg = orgCatalog?.definitions.find(
      (definition) => definition.id === definitionId,
    )?.name;
    if (fromOrg?.trim()) return fromOrg.trim();
    return `#${definitionId}`;
  };

  return (
    <WorkflowInputSectionCard>
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <FieldDisplay
          label={t('drivingLicense.record.issueDateLabel')}
          value={formatDateLong(payload.issueDate)}
        />
        {payload.documentExpirationDate?.trim() ? (
          <FieldDisplay
            label={t('drivingLicense.record.documentExpirationDateLabel')}
            value={formatDateLong(payload.documentExpirationDate)}
          />
        ) : null}
        {payload.licenseNumber?.trim() ? (
          <FieldDisplay
            label={t('drivingLicense.record.licenseNumberLabel')}
            value={payload.licenseNumber}
          />
        ) : null}
        {payload.issuingAuthority?.trim() ? (
          <FieldDisplay
            label={t('drivingLicense.record.issuingAuthorityLabel')}
            value={payload.issuingAuthority}
          />
        ) : null}
      </div>
      <div className="border-t border-[var(--border-color)]/30 pt-3">
        <p className="mb-2 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
          {t('drivingLicense.classes.label')}
        </p>
        <div className="flex flex-wrap gap-1.5">
          {payload.classes.map((licenseClass) => (
            <Badge key={licenseClass.tenantDrivingLicenseDefinitionId} tone="neutral">
              {resolveClassName(licenseClass.tenantDrivingLicenseDefinitionId)}
            </Badge>
          ))}
        </div>
      </div>
      {payload.notes?.trim() ? (
        <div className="border-t border-[var(--border-color)]/30 pt-3">
          <FieldDisplay label={t('common.record.notesLabel')} value={payload.notes} />
        </div>
      ) : null}
    </WorkflowInputSectionCard>
  );
}

export const drivingLicenseRecordInputDisplayPlugin: WorkflowInputDisplayPlugin<DrivingLicenseRecordWorkflowPayload> =
  {
    workflowTypeId: WORKFLOW_TYPE_IDS.DRIVING_LICENSE_RECORD,
    parsePayload: parseDrivingLicenseRecordPayload,
    render(ctx, payload) {
      return <DrivingLicenseRecordInputDisplay ctx={ctx} payload={payload} />;
    },
  };
