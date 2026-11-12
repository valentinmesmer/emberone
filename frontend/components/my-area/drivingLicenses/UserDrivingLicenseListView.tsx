/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Upload } from 'lucide-react';
import { useMemo, useState } from 'react';
import {
  DrivingLicensePendingWorkflowCard,
  mapPendingWorkflowClasses,
} from '@/components/my-area/drivingLicenses/DrivingLicensePendingWorkflowCard';
import { DrivingLicenseRecordSubmitModal } from '@/components/my-area/drivingLicenses/DrivingLicenseRecordSubmitModal';
import { DrivingLicenseView } from '@/components/my-area/drivingLicenses/DrivingLicenseView';
import { Button } from '@/components/ui/button';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { useTranslation } from '@/hooks/useTranslation';
import { getOpenDrivingLicenseWorkflow } from '@/lib/drivingLicenses/submittableDrivingLicenses';
import { getMyAreaWorkflowDetailPath, MY_AREA_BASE_PATH } from '@/lib/my-area/myAreaSections';
import {
  useMyAvailableDrivingLicenseDefinitions,
  useMyBeginnableWorkflowTypes,
  useMyDrivingLicense,
  useMyWorkflowOpenedPage,
  useUserDocumentsByIds,
} from '@/lib/store';
import { WORKFLOW_TYPE_IDS } from '@/types/workflow';

export function UserDrivingLicenseListView() {
  const t = useTranslation();
  const { data: license, isLoading: licenseLoading } = useMyDrivingLicense();
  const { data: openedWorkflows = [], isPending: workflowInboxLoading } = useMyWorkflowOpenedPage();
  const { data: beginnableTypes = [] } = useMyBeginnableWorkflowTypes();
  const { data: availableDefinitions = [], isLoading: catalogLoading } =
    useMyAvailableDrivingLicenseDefinitions();

  const [submitOpen, setSubmitOpen] = useState(false);

  const openDrivingLicenseWorkflow = useMemo(
    () => getOpenDrivingLicenseWorkflow(openedWorkflows),
    [openedWorkflows],
  );

  const drivingLicenseType = beginnableTypes.find(
    (type) => type.workflowTypeId === WORKFLOW_TYPE_IDS.DRIVING_LICENSE_RECORD,
  );
  const canOpenSubmitWorkflow = drivingLicenseType != null && !drivingLicenseType.typeBlocked;

  const classDefinitions = useMemo(
    () => availableDefinitions.filter((definition) => definition.active),
    [availableDefinitions],
  );

  const resolveClassLabel = (definitionId: number) =>
    classDefinitions.find((definition) => definition.id === definitionId)?.name ??
    `#${definitionId}`;

  const { data: linkedDocuments = [] } = useUserDocumentsByIds(license?.userDocumentIds ?? []);

  const isLoading = licenseLoading || workflowInboxLoading || catalogLoading;

  const hasContent = license != null || openDrivingLicenseWorkflow != null;

  return (
    <>
      <PageHeader
        title={t('drivingLicense.title')}
        description={t('drivingLicense.subtitle')}
        size="compact"
        action={
          canOpenSubmitWorkflow ? (
            <Button
              type="button"
              onClick={() => setSubmitOpen(true)}
              size="xs"
              fullWidth="responsive"
            >
              <Upload className="mr-1 h-3.5 w-3.5" aria-hidden />
              {license != null
                ? t('drivingLicense.update.action')
                : t('drivingLicense.submit.action')}
            </Button>
          ) : undefined
        }
      />

      <div className="mt-1 w-full sm:mt-0">
        {isLoading ? (
          <LoadingState variant="embedded" size="sm" />
        ) : !hasContent ? (
          <EmptyStateCard>{t('drivingLicense.empty')}</EmptyStateCard>
        ) : (
          <div className="space-y-4">
            {openDrivingLicenseWorkflow ? (
              <DrivingLicensePendingWorkflowCard
                issueDate={openDrivingLicenseWorkflow.payload.issueDate}
                documentExpirationDate={openDrivingLicenseWorkflow.payload.documentExpirationDate}
                licenseNumber={openDrivingLicenseWorkflow.payload.licenseNumber}
                issuingAuthority={openDrivingLicenseWorkflow.payload.issuingAuthority}
                classes={mapPendingWorkflowClasses(
                  openDrivingLicenseWorkflow.payload.classes,
                  resolveClassLabel,
                )}
                href={getMyAreaWorkflowDetailPath(openDrivingLicenseWorkflow.workflow.id)}
              />
            ) : null}
            {license ? (
              <DrivingLicenseView
                license={license}
                linkedDocuments={linkedDocuments}
                getDocumentHref={(documentId) => `${MY_AREA_BASE_PATH}/documents/${documentId}`}
              />
            ) : null}
          </div>
        )}
      </div>

      <DrivingLicenseRecordSubmitModal
        isOpen={submitOpen}
        onClose={() => setSubmitOpen(false)}
        classDefinitions={classDefinitions}
        renewal={license != null}
      />
    </>
  );
}
