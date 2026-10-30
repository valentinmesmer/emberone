/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type {
  CertificationDefinitionDto,
  CertificationStatusItemDto,
} from '@/lib/api/certifications';
import { allowsSubmittableEvidence } from '@/lib/certifications/evidenceTypes';
import { isWorkflowStatusActive } from '@/lib/utils/workflowOpen';
import { parseCertificationRecordPayload } from '@/lib/workflows/inputDisplay/payloadParsers';
import {
  certificationDefinitionConcurrencyKey,
  findWorkflowByConcurrencyKey,
  idsFromConcurrencyKeys,
} from '@/lib/workflows/openConcurrency';
import type { WorkflowDto } from '@/types/workflow';
import { type CertificationRecordWorkflowPayload, WORKFLOW_TYPE_IDS } from '@/types/workflow';

export function certificationDefinitionIdFromPayload(
  payload: CertificationRecordWorkflowPayload,
): number {
  return payload.tenantCertDefinitionId;
}

/** Open CERTIFICATION_RECORD workflow that includes a specific definition, if any. */
export function findOpenCertificationWorkflow(
  workflows: WorkflowDto[],
  definitionId: number,
): WorkflowDto | undefined {
  return findWorkflowByConcurrencyKey(
    workflows,
    WORKFLOW_TYPE_IDS.CERTIFICATION_RECORD,
    certificationDefinitionConcurrencyKey(definitionId),
  );
}

/** Open CERTIFICATION_RECORD workflows started by the user. */
export function filterOpenCertificationRecordWorkflows(workflows: WorkflowDto[]): WorkflowDto[] {
  return workflows.filter(
    (workflow) =>
      isWorkflowStatusActive(workflow.status) &&
      workflow.workflowTypeId === WORKFLOW_TYPE_IDS.CERTIFICATION_RECORD,
  );
}

/** Entity-scoped list title for an open certification submission (not the workflow type label). */
export function resolveCertificationRecordWorkflowEntityTitle(
  workflow: WorkflowDto,
  statusItems: CertificationStatusItemDto[],
  catalogDefinitions: Pick<CertificationDefinitionDto, 'id' | 'name'>[] = [],
): string {
  const keyIds = idsFromConcurrencyKeys(workflow.openConcurrencyKeys ?? [], 'cert');
  const definitionIds =
    keyIds.size > 0
      ? [...keyIds]
      : (() => {
          const parsed = parseCertificationRecordPayload(workflow.payload);
          return parsed != null ? [certificationDefinitionIdFromPayload(parsed)] : [];
        })();
  const names = definitionIds.map((definitionId) => {
    const fromStatus = statusItems.find((item) => item.tenantCertDefinitionId === definitionId);
    if (fromStatus?.certDefinitionName?.trim()) {
      return fromStatus.certDefinitionName.trim();
    }
    const fromCatalog = catalogDefinitions.find((definition) => definition.id === definitionId);
    if (fromCatalog?.name?.trim()) {
      return fromCatalog.name.trim();
    }
    return `#${definitionId}`;
  });
  return names.join(', ');
}

/** Items the user may submit now (excludes definitions with an open submission workflow). */
export function filterSubmittableCertificationItems(
  items: CertificationStatusItemDto[],
  openConcurrencyKeys: readonly string[] = [],
): CertificationStatusItemDto[] {
  const blockedDefinitionIds = idsFromConcurrencyKeys(openConcurrencyKeys, 'cert');
  return items.filter(
    (item) =>
      item.status !== 'ARCHIVED' &&
      !blockedDefinitionIds.has(item.tenantCertDefinitionId) &&
      allowsSubmittableEvidence(item.evidenceTypes),
  );
}

/**
 * Merges assigned status items with catalog-available definitions not yet assigned,
 * for self-service submission (also without prior assignment).
 */
export function mergeSubmittableCertificationOptions(
  statusItems: CertificationStatusItemDto[],
  catalogDefinitions: CertificationDefinitionDto[],
  openConcurrencyKeys: readonly string[] = [],
): CertificationStatusItemDto[] {
  const blockedDefinitionIds = idsFromConcurrencyKeys(openConcurrencyKeys, 'cert');
  const byId = new Map<number, CertificationStatusItemDto>();

  for (const item of statusItems) {
    if (item.status === 'ARCHIVED' || blockedDefinitionIds.has(item.tenantCertDefinitionId)) {
      continue;
    }
    if (!allowsSubmittableEvidence(item.evidenceTypes)) {
      continue;
    }
    byId.set(item.tenantCertDefinitionId, item);
  }

  for (const definition of catalogDefinitions) {
    if (!definition.active || blockedDefinitionIds.has(definition.id)) {
      continue;
    }
    if (!allowsSubmittableEvidence(definition.evidenceTypes)) {
      continue;
    }
    if (byId.has(definition.id)) {
      continue;
    }
    byId.set(definition.id, {
      tenantCertDefinitionId: definition.id,
      certDefinitionName: definition.name,
      status: 'MISSING',
      mandatory: false,
      responsibleOrganizationId: null,
      completedDate: null,
      expiryDate: null,
      recordId: null,
      archived: false,
      assigned: false,
      evidenceTypes: definition.evidenceTypes ?? [],
    });
  }

  return Array.from(byId.values()).sort((a, b) =>
    a.certDefinitionName.localeCompare(b.certDefinitionName, undefined, { sensitivity: 'base' }),
  );
}
