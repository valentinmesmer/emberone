/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type {
  TenantWorkflowDefinitionDto,
  UpdateWorkflowDefinitionRequest,
} from '@/types/workflow';

interface WorkflowDefinitionFormState {
  staticApproversExclusive: boolean;
  staticOrgIds: Set<number>;
}

type WorkflowDefinitionFormSource = Pick<
  TenantWorkflowDefinitionDto,
  'staticApproversExclusive' | 'staticApproverOrganizationIds'
>;

export function definitionToFormState(
  definition: WorkflowDefinitionFormSource,
): WorkflowDefinitionFormState {
  return {
    staticApproversExclusive: definition.staticApproversExclusive,
    staticOrgIds: new Set(definition.staticApproverOrganizationIds ?? []),
  };
}

export function formStateToRequest(
  state: WorkflowDefinitionFormState,
): UpdateWorkflowDefinitionRequest {
  return {
    staticApproversExclusive: state.staticApproversExclusive,
    staticApproverOrganizationIds: [...state.staticOrgIds].sort((a, b) => a - b),
  };
}
