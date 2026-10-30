/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { ApiRequestError } from '@/lib/api/client';
import { WORKFLOW_CLIENT_ERROR_CODES } from '@/types/workflow';

export type WorkflowErrorMessageScope = 'self' | 'organization';

/** Maps stable workflow validation codes to i18n keys; returns null when unmapped. */
function workflowErrorTranslationKey(
  code: string | undefined,
  scope: WorkflowErrorMessageScope = 'organization',
): string | null {
  if (!code) {
    return null;
  }
  switch (code) {
    case WORKFLOW_CLIENT_ERROR_CODES.ORGANIZATION_LEAVE_ALREADY_OPEN:
      return scope === 'self'
        ? 'myArea.organizationLeaveRequest.errorAlreadyOpen'
        : 'userManagement.organizationMembership.errors.exitWorkflowAlreadyOpen';
    case WORKFLOW_CLIENT_ERROR_CODES.ORGANIZATION_MEMBERSHIP_EXIT_ALREADY_OPEN:
    case WORKFLOW_CLIENT_ERROR_CODES.ORGANIZATION_MEMBERSHIP_END_ALREADY_OPEN:
      return scope === 'self'
        ? 'myArea.organizationLeaveRequest.errorExitAlreadyOpen'
        : 'userManagement.organizationMembership.errors.exitWorkflowAlreadyOpen';
    case WORKFLOW_CLIENT_ERROR_CODES.MAIN_ORGANIZATION_CHANGE_ALREADY_OPEN:
    case WORKFLOW_CLIENT_ERROR_CODES.OPEN_EXCLUSIVITY_ALREADY_OPEN:
      return scope === 'self'
        ? 'myArea.mainOrganizationChange.errorAlreadyOpen'
        : 'userManagement.mainOrganizationChange.errorAlreadyOpen';
    default:
      return null;
  }
}

export function resolveWorkflowErrorMessage(
  error: unknown,
  t: (key: string) => string,
  fallback: string,
  scope: WorkflowErrorMessageScope = 'organization',
): string {
  if (error instanceof ApiRequestError) {
    const key = workflowErrorTranslationKey(error.code, scope);
    if (key) {
      return t(key);
    }
  }
  if (error instanceof Error) {
    return error.message;
  }
  return fallback;
}
