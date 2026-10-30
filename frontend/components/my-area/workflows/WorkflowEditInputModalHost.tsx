/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { AddressChangeRequestModal } from '@/components/my-area/AddressChangeRequestModal';
import { CertificationRecordSubmitModal } from '@/components/my-area/certifications/CertificationRecordSubmitModal';
import { UserDocumentSubmissionRequestModal } from '@/components/my-area/documents/UserDocumentSubmissionRequestModal';
import { DrivingLicenseRecordSubmitModal } from '@/components/my-area/drivingLicenses/DrivingLicenseRecordSubmitModal';
import { MainOrganizationChangeModal } from '@/components/my-area/MainOrganizationChangeModal';
import { NameChangeRequestModal } from '@/components/my-area/NameChangeRequestModal';
import { OrganizationGroupJoinRequestModal } from '@/components/my-area/OrganizationGroupJoinRequestModal';
import { OrganizationGroupLeaveRequestModal } from '@/components/my-area/OrganizationGroupLeaveRequestModal';
import { OrganizationJoinRequestModal } from '@/components/my-area/OrganizationJoinRequestModal';
import { OrganizationLeaveRequestModal } from '@/components/my-area/OrganizationLeaveRequestModal';
import { TrainingCourseRecordSubmitModal } from '@/components/my-area/trainingCourses/TrainingCourseRecordSubmitModal';
import type { CertificationStatusItemDto } from '@/lib/api/certifications';
import {
  certificationDefinitionIdFromPayload,
  mergeSubmittableCertificationOptions,
} from '@/lib/certifications/submittableCertifications';
import {
  useMyAvailableDrivingLicenseDefinitions,
  useMyAvailableTrainingCourseTypes,
  useMyCertificationStatus,
  useMyRequestableCertificationDefinitions,
  useSelf,
  useWorkflowOpenConcurrencyKeys,
} from '@/lib/store';
import { filterSubmittableTrainingCourseTypes } from '@/lib/trainingCourses/submittableTrainingCourses';
import {
  parseAddressChangePayload,
  parseCertificationRecordPayload,
  parseDrivingLicenseRecordPayload,
  parseMainOrganizationChangePayload,
  parseNameChangePayload,
  parseOrganizationGroupJoinRequestPayload,
  parseOrganizationGroupLeaveRequestPayload,
  parseOrganizationJoinRequestPayload,
  parseOrganizationLeaveRequestPayload,
  parseTrainingCourseRecordPayload,
  parseUserDocumentSubmissionPayload,
} from '@/lib/workflows/inputDisplay/payloadParsers';
import { blockingConcurrencyKeys } from '@/lib/workflows/openConcurrency';
import { WORKFLOW_TYPE_IDS, type WorkflowDto } from '@/types/workflow';

export interface WorkflowEditInputModalHostProps {
  workflow: WorkflowDto;
  isOpen: boolean;
  onClose: () => void;
}

function syntheticCertStatusItem(
  tenantCertDefinitionId: number,
  certDefinitionName: string,
  evidenceTypes?: CertificationStatusItemDto['evidenceTypes'],
): CertificationStatusItemDto {
  return {
    tenantCertDefinitionId,
    certDefinitionName,
    status: 'MISSING',
    mandatory: false,
    responsibleOrganizationId: null,
    completedDate: null,
    expiryDate: null,
    recordId: null,
    archived: false,
    assigned: false,
    evidenceTypes: evidenceTypes ?? [],
  };
}

/**
 * Opens the same type-specific input UI as begin, prefilled for payload edit.
 * Workflow type is fixed by the existing instance.
 */
export function WorkflowEditInputModalHost({
  workflow,
  isOpen,
  onClose,
}: WorkflowEditInputModalHostProps) {
  const { data: selfUser } = useSelf();
  const { data: certStatus } = useMyCertificationStatus();
  const catalogDefinitionsEnabled =
    workflow.workflowTypeId === WORKFLOW_TYPE_IDS.CERTIFICATION_RECORD && isOpen;
  const { data: catalogDefinitions = [] } =
    useMyRequestableCertificationDefinitions(catalogDefinitionsEnabled);
  const { data: certOpenKeys = [] } = useWorkflowOpenConcurrencyKeys(
    WORKFLOW_TYPE_IDS.CERTIFICATION_RECORD,
    catalogDefinitionsEnabled,
  );
  const blockingCertKeys = useMemo(
    () => blockingConcurrencyKeys(certOpenKeys, workflow.openConcurrencyKeys ?? []),
    [certOpenKeys, workflow.openConcurrencyKeys],
  );

  const submittableCertItems = useMemo(() => {
    const filtered = mergeSubmittableCertificationOptions(
      certStatus?.items ?? [],
      catalogDefinitions,
      blockingCertKeys,
    );
    const payload = parseCertificationRecordPayload(workflow.payload);
    const selectedId = payload != null ? certificationDefinitionIdFromPayload(payload) : null;
    if (selectedId == null || selectedId === 0) return filtered;
    if (filtered.some((item) => item.tenantCertDefinitionId === selectedId)) {
      return filtered;
    }
    const fromStatus = (certStatus?.items ?? []).find(
      (item) => item.tenantCertDefinitionId === selectedId,
    );
    if (fromStatus) {
      return [...filtered, fromStatus];
    }
    const fromCatalog = catalogDefinitions.find((definition) => definition.id === selectedId);
    if (fromCatalog) {
      return [
        ...filtered,
        syntheticCertStatusItem(fromCatalog.id, fromCatalog.name, fromCatalog.evidenceTypes),
      ];
    }
    return [...filtered, syntheticCertStatusItem(selectedId, `#${selectedId}`)];
  }, [blockingCertKeys, catalogDefinitions, certStatus?.items, workflow.payload]);

  const trainingCatalogEnabled =
    workflow.workflowTypeId === WORKFLOW_TYPE_IDS.TRAINING_COURSE_RECORD && isOpen;
  const { data: availableTrainingTypes = [] } =
    useMyAvailableTrainingCourseTypes(trainingCatalogEnabled);

  const { data: trainingOpenKeys = [] } = useWorkflowOpenConcurrencyKeys(
    WORKFLOW_TYPE_IDS.TRAINING_COURSE_RECORD,
    trainingCatalogEnabled,
  );
  const blockingTrainingKeys = useMemo(
    () => blockingConcurrencyKeys(trainingOpenKeys, workflow.openConcurrencyKeys ?? []),
    [trainingOpenKeys, workflow.openConcurrencyKeys],
  );

  const submittableTrainingTypes = useMemo(() => {
    const filtered = filterSubmittableTrainingCourseTypes(
      availableTrainingTypes,
      blockingTrainingKeys,
    );
    const payload = parseTrainingCourseRecordPayload(workflow.payload);
    if (!payload?.tenantTrainingCourseTypeIds?.length) return filtered;
    const byId = new Map(availableTrainingTypes.map((type) => [type.id, type]));
    const selected = payload.tenantTrainingCourseTypeIds
      .map((id) => byId.get(id))
      .filter((type): type is NonNullable<typeof type> => type != null);
    const merged = [...filtered];
    for (const type of selected) {
      if (!merged.some((item) => item.id === type.id)) {
        merged.push(type);
      }
    }
    return merged;
  }, [availableTrainingTypes, blockingTrainingKeys, workflow.payload]);

  const drivingLicenseCatalogEnabled =
    workflow.workflowTypeId === WORKFLOW_TYPE_IDS.DRIVING_LICENSE_RECORD && isOpen;
  const { data: availableDrivingDefinitions = [] } = useMyAvailableDrivingLicenseDefinitions(
    drivingLicenseCatalogEnabled,
  );

  const drivingLicenseClassDefinitions = useMemo(
    () => availableDrivingDefinitions.filter((definition) => definition.active),
    [availableDrivingDefinitions],
  );

  const requestMessage = workflow.activeInputChange?.message;

  if (!isOpen || !selfUser) {
    return null;
  }

  if (workflow.workflowTypeId === WORKFLOW_TYPE_IDS.NAME_CHANGE) {
    return (
      <NameChangeRequestModal
        isOpen
        onClose={onClose}
        userData={selfUser}
        editWorkflowId={workflow.id}
        initialPayload={parseNameChangePayload(workflow.payload)}
        requestMessage={requestMessage}
      />
    );
  }

  if (workflow.workflowTypeId === WORKFLOW_TYPE_IDS.ADDRESS_CHANGE) {
    return (
      <AddressChangeRequestModal
        isOpen
        onClose={onClose}
        userData={selfUser}
        editWorkflowId={workflow.id}
        initialPayload={parseAddressChangePayload(workflow.payload)}
        requestMessage={requestMessage}
      />
    );
  }

  if (workflow.workflowTypeId === WORKFLOW_TYPE_IDS.ORGANIZATION_JOIN_REQUEST) {
    return (
      <OrganizationJoinRequestModal
        isOpen
        onClose={onClose}
        userData={selfUser}
        editWorkflowId={workflow.id}
        initialPayload={parseOrganizationJoinRequestPayload(workflow.payload)}
        requestMessage={requestMessage}
      />
    );
  }

  if (workflow.workflowTypeId === WORKFLOW_TYPE_IDS.MAIN_ORGANIZATION_CHANGE) {
    return (
      <MainOrganizationChangeModal
        isOpen
        onClose={onClose}
        userData={selfUser}
        editWorkflowId={workflow.id}
        initialPayload={parseMainOrganizationChangePayload(workflow.payload)}
        requestMessage={requestMessage}
      />
    );
  }

  if (workflow.workflowTypeId === WORKFLOW_TYPE_IDS.ORGANIZATION_LEAVE_REQUEST) {
    return (
      <OrganizationLeaveRequestModal
        isOpen
        onClose={onClose}
        userData={selfUser}
        editWorkflowId={workflow.id}
        initialPayload={parseOrganizationLeaveRequestPayload(workflow.payload)}
        requestMessage={requestMessage}
      />
    );
  }

  if (workflow.workflowTypeId === WORKFLOW_TYPE_IDS.ORGANIZATION_GROUP_LEAVE_REQUEST) {
    return (
      <OrganizationGroupLeaveRequestModal
        isOpen
        onClose={onClose}
        userData={selfUser}
        editWorkflowId={workflow.id}
        initialPayload={parseOrganizationGroupLeaveRequestPayload(workflow.payload)}
        requestMessage={requestMessage}
      />
    );
  }

  if (workflow.workflowTypeId === WORKFLOW_TYPE_IDS.ORGANIZATION_GROUP_JOIN_REQUEST) {
    return (
      <OrganizationGroupJoinRequestModal
        isOpen
        onClose={onClose}
        userData={selfUser}
        editWorkflowId={workflow.id}
        initialPayload={parseOrganizationGroupJoinRequestPayload(workflow.payload)}
        requestMessage={requestMessage}
      />
    );
  }

  if (workflow.workflowTypeId === WORKFLOW_TYPE_IDS.CERTIFICATION_RECORD) {
    return (
      <CertificationRecordSubmitModal
        isOpen
        onClose={onClose}
        submittableItems={submittableCertItems}
        editWorkflowId={workflow.id}
        initialPayload={parseCertificationRecordPayload(workflow.payload)}
        requestMessage={requestMessage}
      />
    );
  }

  if (workflow.workflowTypeId === WORKFLOW_TYPE_IDS.TRAINING_COURSE_RECORD) {
    return (
      <TrainingCourseRecordSubmitModal
        isOpen
        onClose={onClose}
        submittableTypes={submittableTrainingTypes}
        editWorkflowId={workflow.id}
        initialPayload={parseTrainingCourseRecordPayload(workflow.payload)}
        requestMessage={requestMessage}
      />
    );
  }

  if (workflow.workflowTypeId === WORKFLOW_TYPE_IDS.DRIVING_LICENSE_RECORD) {
    return (
      <DrivingLicenseRecordSubmitModal
        isOpen
        onClose={onClose}
        classDefinitions={drivingLicenseClassDefinitions}
        editWorkflowId={workflow.id}
        initialPayload={parseDrivingLicenseRecordPayload(workflow.payload)}
        requestMessage={requestMessage}
      />
    );
  }

  if (workflow.workflowTypeId === WORKFLOW_TYPE_IDS.USER_DOCUMENT_SUBMISSION) {
    return (
      <UserDocumentSubmissionRequestModal
        isOpen
        onClose={onClose}
        editWorkflowId={workflow.id}
        initialPayload={parseUserDocumentSubmissionPayload(workflow.payload)}
        requestMessage={requestMessage}
      />
    );
  }

  return null;
}
