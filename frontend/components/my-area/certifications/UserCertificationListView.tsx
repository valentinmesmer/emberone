/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Upload } from 'lucide-react';
import { useMemo, useState } from 'react';
import { CertificationExpiryDate } from '@/components/my-area/certifications/CertificationExpiryDate';
import { CertificationRecordSubmitModal } from '@/components/my-area/certifications/CertificationRecordSubmitModal';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { useTranslation } from '@/hooks/useTranslation';
import type { CertificationStatus } from '@/lib/api/certifications';
import { partitionCertificationStatusItems } from '@/lib/certifications/statusPartition';
import {
  CERTIFICATION_STATUS_BADGE_TONE,
  CERTIFICATION_STATUS_ROW_TONE,
} from '@/lib/certifications/statusPresentation';
import {
  filterOpenCertificationRecordWorkflows,
  mergeSubmittableCertificationOptions,
  resolveCertificationRecordWorkflowEntityTitle,
} from '@/lib/certifications/submittableCertifications';
import {
  getMyAreaCertificationDetailPath,
  getMyAreaWorkflowDetailPath,
} from '@/lib/my-area/myAreaSections';
import {
  useMyBeginnableWorkflowTypes,
  useMyCertificationStatus,
  useMyRequestableCertificationDefinitions,
  useMyWorkflowOpenedPage,
  useWorkflowOpenConcurrencyKeys,
} from '@/lib/store';
import { WORKFLOW_TYPE_IDS, type WorkflowDto } from '@/types/workflow';
export function UserCertificationListView() {
  const t = useTranslation();
  const { data: status, isLoading } = useMyCertificationStatus();
  const { data: openedWorkflows = [] } = useMyWorkflowOpenedPage();
  const { data: beginnableTypes = [] } = useMyBeginnableWorkflowTypes();

  const [submitOpen, setSubmitOpen] = useState(false);

  const items = useMemo(() => status?.items ?? [], [status?.items]);
  const { active: activeItems, history: historyItems } = useMemo(
    () => partitionCertificationStatusItems(items),
    [items],
  );
  const orderedItems = useMemo(
    () => [...activeItems, ...historyItems],
    [activeItems, historyItems],
  );
  const openCertWorkflows = useMemo(
    () => filterOpenCertificationRecordWorkflows(openedWorkflows),
    [openedWorkflows],
  );

  const certificationWorkflowBeginnable = useMemo(
    () =>
      beginnableTypes.some(
        (type) => type.workflowTypeId === WORKFLOW_TYPE_IDS.CERTIFICATION_RECORD,
      ),
    [beginnableTypes],
  );

  const { data: catalogDefinitions = [] } = useMyRequestableCertificationDefinitions(
    certificationWorkflowBeginnable,
  );
  const { data: certOpenKeys = [] } = useWorkflowOpenConcurrencyKeys(
    WORKFLOW_TYPE_IDS.CERTIFICATION_RECORD,
    certificationWorkflowBeginnable,
  );

  const submittableItems = useMemo(
    () => mergeSubmittableCertificationOptions(items, catalogDefinitions, certOpenKeys),
    [catalogDefinitions, certOpenKeys, items],
  );

  const canOpenSubmitWorkflow = certificationWorkflowBeginnable && submittableItems.length > 0;

  const subtitle = useMemo(() => {
    if (activeItems.length > 0) {
      return t('certifications.count', { count: activeItems.length });
    }
    if (openCertWorkflows.length > 0) {
      return t('certifications.openWorkflowsCount', { count: openCertWorkflows.length });
    }
    return t('certifications.subtitle');
  }, [activeItems.length, openCertWorkflows.length, t]);

  const statusLabel = (s: CertificationStatus) => t(`certifications.status.${s}`);

  const workflowTitle = (workflow: WorkflowDto) =>
    resolveCertificationRecordWorkflowEntityTitle(workflow, items, catalogDefinitions);

  const renderStatusRow = (item: (typeof activeItems)[number], historical: boolean) => (
    <ListRow
      key={item.tenantCertDefinitionId}
      href={getMyAreaCertificationDetailPath(item.tenantCertDefinitionId)}
      tone={historical ? 'default' : CERTIFICATION_STATUS_ROW_TONE[item.status]}
      title={item.certDefinitionName}
      badges={
        historical ? (
          <Badge tone="neutral">{t('certifications.historicalTag')}</Badge>
        ) : (
          <Badge tone={CERTIFICATION_STATUS_BADGE_TONE[item.status]}>
            {statusLabel(item.status)}
          </Badge>
        )
      }
      meta={
        item.expiryDate ? (
          <div className="mt-0.5">
            <CertificationExpiryDate expiryDate={item.expiryDate} />
          </div>
        ) : undefined
      }
    />
  );

  const renderWorkflowRow = (workflow: WorkflowDto) => {
    return (
      <ListRow
        key={workflow.id}
        tone="warning"
        href={getMyAreaWorkflowDetailPath(workflow.id)}
        title={workflowTitle(workflow)}
      />
    );
  };

  const headerActions = canOpenSubmitWorkflow ? (
    <Button type="button" onClick={() => setSubmitOpen(true)} size="xs" fullWidth="responsive">
      <Upload className="mr-1 h-3.5 w-3.5" aria-hidden />
      {t('certifications.submit.action')}
    </Button>
  ) : undefined;

  const showEmptyAssignments =
    !isLoading && orderedItems.length === 0 && openCertWorkflows.length === 0;

  return (
    <>
      <PageHeader
        title={t('certifications.title')}
        description={subtitle}
        size="compact"
        action={headerActions}
      />

      <div className="mt-1 w-full space-y-4 sm:mt-0">
        {isLoading ? (
          <LoadingState variant="embedded" size="sm" />
        ) : showEmptyAssignments ? (
          <EmptyStateCard>{t('certifications.empty')}</EmptyStateCard>
        ) : (
          <>
            {openCertWorkflows.length > 0 ? (
              <section>
                <h2 className="mb-1.5 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
                  {t('certifications.openWorkflowsTitle')}
                </h2>
                <ListContainer label={t('certifications.openWorkflowsTitle')}>
                  {openCertWorkflows.map(renderWorkflowRow)}
                </ListContainer>
              </section>
            ) : null}

            {orderedItems.length > 0 ? (
              <section>
                {openCertWorkflows.length > 0 ? (
                  <h2 className="mb-1.5 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
                    {t('certifications.title')}
                  </h2>
                ) : null}
                <ListContainer label={t('certifications.title')}>
                  {orderedItems.map((item) => renderStatusRow(item, item.assigned === false))}
                </ListContainer>
              </section>
            ) : null}
          </>
        )}
      </div>

      <CertificationRecordSubmitModal
        isOpen={submitOpen}
        onClose={() => setSubmitOpen(false)}
        submittableItems={submittableItems}
      />
    </>
  );
}
