/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { WorkflowDto } from '@/types/workflow';
import { addressChangeInputDisplayPlugin } from './plugins/addressChange';
import { certificationRecordInputDisplayPlugin } from './plugins/certificationRecord';
import { courseOfferingRegistrationInputDisplayPlugin } from './plugins/courseOfferingRegistration';
import { courseRegistrationCancellationInputDisplayPlugin } from './plugins/courseRegistrationCancellation';
import { drivingLicenseRecordInputDisplayPlugin } from './plugins/drivingLicenseRecord';
import { mainOrganizationChangeInputDisplayPlugin } from './plugins/mainOrganizationChange';
import { nameChangeInputDisplayPlugin } from './plugins/nameChange';
import { onboardingSubmissionInputDisplayPlugin } from './plugins/onboardingSubmission';
import { organizationGroupJoinRequestInputDisplayPlugin } from './plugins/organizationGroupJoinRequest';
import { organizationGroupLeaveRequestInputDisplayPlugin } from './plugins/organizationGroupLeaveRequest';
import { organizationJoinRequestInputDisplayPlugin } from './plugins/organizationJoinRequest';
import { organizationLeaveRequestInputDisplayPlugin } from './plugins/organizationLeaveRequest';
import { organizationMembershipEndRequestInputDisplayPlugin } from './plugins/organizationMembershipEndRequest';
import { organizationUserMainOrganizationChangeInputDisplayPlugin } from './plugins/organizationUserMainOrganizationChange';
import { rankRequestInputDisplayPlugin } from './plugins/rankRequest';
import { trainingCourseRecordInputDisplayPlugin } from './plugins/trainingCourseRecord';
import { userDocumentSubmissionInputDisplayPlugin } from './plugins/userDocumentSubmission';
import type { WorkflowInputDisplayPlugin } from './types';

const plugins: WorkflowInputDisplayPlugin[] = [
  nameChangeInputDisplayPlugin,
  addressChangeInputDisplayPlugin,
  organizationJoinRequestInputDisplayPlugin,
  mainOrganizationChangeInputDisplayPlugin,
  organizationLeaveRequestInputDisplayPlugin,
  organizationGroupLeaveRequestInputDisplayPlugin,
  organizationGroupJoinRequestInputDisplayPlugin,
  certificationRecordInputDisplayPlugin,
  trainingCourseRecordInputDisplayPlugin,
  drivingLicenseRecordInputDisplayPlugin,
  onboardingSubmissionInputDisplayPlugin,
  organizationMembershipEndRequestInputDisplayPlugin,
  organizationUserMainOrganizationChangeInputDisplayPlugin,
  userDocumentSubmissionInputDisplayPlugin,
  rankRequestInputDisplayPlugin,
  courseOfferingRegistrationInputDisplayPlugin,
  courseRegistrationCancellationInputDisplayPlugin,
];

const byWorkflowTypeId = new Map<number, WorkflowInputDisplayPlugin>(
  plugins.map((plugin) => [plugin.workflowTypeId, plugin]),
);

export function getWorkflowInputDisplayPlugin(
  workflowTypeId: number,
): WorkflowInputDisplayPlugin | undefined {
  return byWorkflowTypeId.get(workflowTypeId);
}

export function workflowInputPayloadCountsAsProof(workflow: WorkflowDto): boolean {
  const plugin = getWorkflowInputDisplayPlugin(workflow.workflowTypeId);
  if (!plugin) return false;
  const payload = plugin.parsePayload(workflow.payload);
  if (payload == null) return false;
  return plugin.treatParsedAsProof !== false;
}
