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
import { RankRequestSubmitModal } from '@/components/my-area/rank/RankRequestSubmitModal';
import { TrainingCourseRecordSubmitModal } from '@/components/my-area/trainingCourses/TrainingCourseRecordSubmitModal';
import { mergeSubmittableCertificationOptions } from '@/lib/certifications/submittableCertifications';
import {
  useMyAvailableDrivingLicenseDefinitions,
  useMyAvailableTrainingCourseTypes,
  useMyCertificationStatus,
  useMyDrivingLicense,
  useMyRequestableCertificationDefinitions,
  useWorkflowOpenConcurrencyKeys,
} from '@/lib/store';
import { filterSubmittableTrainingCourseTypes } from '@/lib/trainingCourses/submittableTrainingCourses';
import type { UserDto } from '@/types/user';
import { WORKFLOW_TYPE_IDS } from '@/types/workflow';

export interface WorkflowBeginModalHostProps {
  workflowTypeId: number | null;
  isOpen: boolean;
  onClose: () => void;
  userData: UserDto;
  /** Remounts the modal form when starting a new request. */
  modalKey?: number;
}

/** Renders the self-service open modal for the given workflow type. */
export function WorkflowBeginModalHost({
  workflowTypeId,
  isOpen,
  onClose,
  userData,
  modalKey = 0,
}: WorkflowBeginModalHostProps) {
  const { data: certStatus } = useMyCertificationStatus();
  const catalogDefinitionsEnabled =
    workflowTypeId === WORKFLOW_TYPE_IDS.CERTIFICATION_RECORD && isOpen;
  const { data: catalogDefinitions = [] } =
    useMyRequestableCertificationDefinitions(catalogDefinitionsEnabled);
  const { data: certOpenKeys = [] } = useWorkflowOpenConcurrencyKeys(
    WORKFLOW_TYPE_IDS.CERTIFICATION_RECORD,
    catalogDefinitionsEnabled,
  );

  const submittableCertItems = useMemo(
    () =>
      mergeSubmittableCertificationOptions(
        certStatus?.items ?? [],
        catalogDefinitions,
        certOpenKeys,
      ),
    [catalogDefinitions, certStatus?.items, certOpenKeys],
  );

  const trainingCatalogEnabled =
    workflowTypeId === WORKFLOW_TYPE_IDS.TRAINING_COURSE_RECORD && isOpen;
  const { data: availableTrainingTypes = [] } =
    useMyAvailableTrainingCourseTypes(trainingCatalogEnabled);
  const { data: trainingOpenKeys = [] } = useWorkflowOpenConcurrencyKeys(
    WORKFLOW_TYPE_IDS.TRAINING_COURSE_RECORD,
    trainingCatalogEnabled,
  );

  const submittableTrainingTypes = useMemo(
    () => filterSubmittableTrainingCourseTypes(availableTrainingTypes, trainingOpenKeys),
    [availableTrainingTypes, trainingOpenKeys],
  );

  const drivingLicenseCatalogEnabled =
    workflowTypeId === WORKFLOW_TYPE_IDS.DRIVING_LICENSE_RECORD && isOpen;
  const { data: availableDrivingDefinitions = [] } = useMyAvailableDrivingLicenseDefinitions(
    drivingLicenseCatalogEnabled,
  );

  const { data: drivingLicense } = useMyDrivingLicense(drivingLicenseCatalogEnabled);

  const drivingLicenseClassDefinitions = useMemo(
    () => availableDrivingDefinitions.filter((definition) => definition.active),
    [availableDrivingDefinitions],
  );

  if (workflowTypeId === WORKFLOW_TYPE_IDS.NAME_CHANGE) {
    return (
      <NameChangeRequestModal
        key={modalKey}
        isOpen={isOpen}
        onClose={onClose}
        userData={userData}
      />
    );
  }

  if (workflowTypeId === WORKFLOW_TYPE_IDS.ADDRESS_CHANGE) {
    return (
      <AddressChangeRequestModal
        key={modalKey}
        isOpen={isOpen}
        onClose={onClose}
        userData={userData}
      />
    );
  }

  if (workflowTypeId === WORKFLOW_TYPE_IDS.ORGANIZATION_JOIN_REQUEST) {
    return (
      <OrganizationJoinRequestModal
        key={modalKey}
        isOpen={isOpen}
        onClose={onClose}
        userData={userData}
      />
    );
  }

  if (workflowTypeId === WORKFLOW_TYPE_IDS.MAIN_ORGANIZATION_CHANGE) {
    return (
      <MainOrganizationChangeModal
        key={modalKey}
        isOpen={isOpen}
        onClose={onClose}
        userData={userData}
      />
    );
  }

  if (workflowTypeId === WORKFLOW_TYPE_IDS.ORGANIZATION_LEAVE_REQUEST) {
    return (
      <OrganizationLeaveRequestModal
        key={modalKey}
        isOpen={isOpen}
        onClose={onClose}
        userData={userData}
      />
    );
  }

  if (workflowTypeId === WORKFLOW_TYPE_IDS.ORGANIZATION_GROUP_LEAVE_REQUEST) {
    return (
      <OrganizationGroupLeaveRequestModal
        key={modalKey}
        isOpen={isOpen}
        onClose={onClose}
        userData={userData}
      />
    );
  }

  if (workflowTypeId === WORKFLOW_TYPE_IDS.ORGANIZATION_GROUP_JOIN_REQUEST) {
    return (
      <OrganizationGroupJoinRequestModal
        key={modalKey}
        isOpen={isOpen}
        onClose={onClose}
        userData={userData}
      />
    );
  }

  if (workflowTypeId === WORKFLOW_TYPE_IDS.CERTIFICATION_RECORD) {
    return (
      <CertificationRecordSubmitModal
        key={modalKey}
        isOpen={isOpen}
        onClose={onClose}
        submittableItems={submittableCertItems}
      />
    );
  }

  if (workflowTypeId === WORKFLOW_TYPE_IDS.TRAINING_COURSE_RECORD) {
    return (
      <TrainingCourseRecordSubmitModal
        key={modalKey}
        isOpen={isOpen}
        onClose={onClose}
        submittableTypes={submittableTrainingTypes}
      />
    );
  }

  if (workflowTypeId === WORKFLOW_TYPE_IDS.DRIVING_LICENSE_RECORD) {
    return (
      <DrivingLicenseRecordSubmitModal
        key={modalKey}
        isOpen={isOpen}
        onClose={onClose}
        classDefinitions={drivingLicenseClassDefinitions}
        renewal={drivingLicense != null}
      />
    );
  }

  if (workflowTypeId === WORKFLOW_TYPE_IDS.USER_DOCUMENT_SUBMISSION) {
    return <UserDocumentSubmissionRequestModal key={modalKey} isOpen={isOpen} onClose={onClose} />;
  }

  if (workflowTypeId === WORKFLOW_TYPE_IDS.RANK_REQUEST) {
    return <RankRequestSubmitModal key={modalKey} isOpen={isOpen} onClose={onClose} />;
  }

  return null;
}
