/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Upload } from 'lucide-react';
import { useMemo, useState } from 'react';
import { CertificationDetailStatusCard } from '@/components/certifications/CertificationDetailStatusCard';
import { CertificationRecordSubmitModal } from '@/components/my-area/certifications/CertificationRecordSubmitModal';
import { Button } from '@/components/ui/button';
import { FieldDisplay } from '@/components/ui/field-display';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyState, NotFoundState } from '@/components/ui/message-cards';
import { SectionActions } from '@/components/ui/section-header';
import { useTranslation } from '@/hooks/useTranslation';
import type { CertificationStatus } from '@/lib/api/certifications';
import {
  computeRecordCertificationStatus,
  isActiveCertificationRecord,
} from '@/lib/certifications/recordValidity';
import { CERTIFICATION_STATUS_ROW_TONE } from '@/lib/certifications/statusPresentation';
import {
  filterSubmittableCertificationItems,
  findOpenCertificationWorkflow,
} from '@/lib/certifications/submittableCertifications';
import {
  getMyAreaCertificationRecordDetailPath,
  getMyAreaWorkflowDetailPath,
} from '@/lib/my-area/myAreaSections';
import {
  useMyBeginnableWorkflowTypes,
  useMyCertificationRecords,
  useMyCertificationStatus,
  useMyWorkflowOpenedPage,
  useWorkflowOpenConcurrencyKeys,
} from '@/lib/store';
import { formatDateLong } from '@/lib/utils';
import { WORKFLOW_TYPE_IDS } from '@/types/workflow';

export interface UserCertificationDetailViewProps {
  definitionId: number;
}

export function UserCertificationDetailView({ definitionId }: UserCertificationDetailViewProps) {
  const t = useTranslation();
  const { data: status, isLoading: statusLoading } = useMyCertificationStatus();
  const { data: allRecords = [], isLoading: recordsLoading } = useMyCertificationRecords();
  const { data: openedWorkflows = [] } = useMyWorkflowOpenedPage();
  const { data: beginnableTypes = [] } = useMyBeginnableWorkflowTypes();

  const [submitOpen, setSubmitOpen] = useState(false);

  const item = status?.items.find((i) => i.tenantCertDefinitionId === definitionId);
  const openWorkflow = useMemo(
    () => findOpenCertificationWorkflow(openedWorkflows, definitionId),
    [definitionId, openedWorkflows],
  );

  const certificationWorkflowBeginnable = useMemo(
    () =>
      beginnableTypes.some(
        (type) => type.workflowTypeId === WORKFLOW_TYPE_IDS.CERTIFICATION_RECORD,
      ),
    [beginnableTypes],
  );
  const { data: certOpenKeys = [] } = useWorkflowOpenConcurrencyKeys(
    WORKFLOW_TYPE_IDS.CERTIFICATION_RECORD,
    certificationWorkflowBeginnable,
  );

  const submittableItems = useMemo(
    () => filterSubmittableCertificationItems(status?.items ?? [], certOpenKeys),
    [certOpenKeys, status?.items],
  );
  const canSubmitThisDefinition = submittableItems.some(
    (i) => i.tenantCertDefinitionId === definitionId,
  );
  const showSubmitButton =
    item != null &&
    certificationWorkflowBeginnable &&
    canSubmitThisDefinition &&
    item.status !== 'ARCHIVED' &&
    item.assigned !== false;

  const records = useMemo(
    () =>
      allRecords
        .filter((r) => r.tenantCertDefinitionId === definitionId)
        .sort((a, b) => b.completedDate.localeCompare(a.completedDate)),
    [allRecords, definitionId],
  );

  const hasHistoryContent = records.length > 0 || !!openWorkflow;

  const statusLabel = (s: CertificationStatus) => t(`certifications.status.${s}`);

  const loading = statusLoading || recordsLoading;

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!item) {
    return <NotFoundState message={t('certifications.detail.notFound')} />;
  }
  const historical = item.assigned === false || item.archived || item.status === 'ARCHIVED';

  return (
    <div className="w-full space-y-4">
      <CertificationDetailStatusCard
        title={item.certDefinitionName}
        status={item.status}
        statusLabel={historical ? undefined : statusLabel(item.status)}
        meta={null}
        hints={
          <>
            {item.status === 'ARCHIVED' ? (
              <p className="mt-1.5 text-xs text-[var(--text-secondary)]">
                {t('certifications.detail.archivedHint')}
              </p>
            ) : null}
            {item.assigned === false && item.status !== 'ARCHIVED' ? (
              <p className="mt-1.5 text-xs text-[var(--text-secondary)]">
                {t('certifications.detail.unassignedHint')}
              </p>
            ) : null}
          </>
        }
        footer={
          item.completedDate || item.expiryDate ? (
            <div className="mt-3 grid grid-cols-1 gap-3 border-t border-[var(--border-color)]/30 pt-3 sm:grid-cols-2">
              {item.completedDate ? (
                <FieldDisplay
                  label={t('certifications.detail.completedDate')}
                  value={formatDateLong(item.completedDate)}
                />
              ) : null}
              {item.expiryDate ? (
                <FieldDisplay
                  label={t('certifications.detail.validUntil')}
                  value={formatDateLong(item.expiryDate)}
                />
              ) : null}
            </div>
          ) : null
        }
      />

      <section className="space-y-1.5">
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center sm:gap-x-3">
          <h2 className="text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
            {t('certifications.historyTitle')}
          </h2>
          {showSubmitButton ? (
            <SectionActions layout="inline" className="sm:justify-end">
              <Button
                type="button"
                variant="outline"
                onClick={() => setSubmitOpen(true)}
                size="xs"
                fullWidth="responsive"
              >
                <Upload className="h-3.5 w-3.5" aria-hidden />
                {t('certifications.submit.action')}
              </Button>
            </SectionActions>
          ) : null}
        </div>
        {!hasHistoryContent ? (
          <EmptyState>{t('certifications.noRecords')}</EmptyState>
        ) : (
          <ListContainer label={t('certifications.historyTitle')} rowSeparator="inset">
            {openWorkflow ? (
              <ListRow
                compact
                tone="warning"
                href={getMyAreaWorkflowDetailPath(openWorkflow.id)}
                chevron
                title={t('certifications.detail.pendingSubmission')}
                subtitle={t('certifications.detail.pendingSubmissionHint')}
              />
            ) : null}
            {records.map((rec) => {
              const isCurrent = isActiveCertificationRecord(rec);
              const recordStatus = computeRecordCertificationStatus(rec);

              return (
                <ListRow
                  key={rec.id}
                  compact
                  tone={isCurrent ? CERTIFICATION_STATUS_ROW_TONE[recordStatus] : 'default'}
                  href={getMyAreaCertificationRecordDetailPath(definitionId, rec.id)}
                  chevron
                  title={t('certifications.detail.recordCompleted', {
                    date: formatDateLong(rec.completedDate),
                  })}
                  subtitle={
                    rec.expiryDate
                      ? t('certifications.detail.recordValidUntil', {
                          date: formatDateLong(rec.expiryDate),
                        })
                      : undefined
                  }
                />
              );
            })}
          </ListContainer>
        )}
      </section>

      <CertificationRecordSubmitModal
        isOpen={submitOpen}
        onClose={() => setSubmitOpen(false)}
        submittableItems={submittableItems}
        presetDefinitionId={definitionId ?? undefined}
      />
    </div>
  );
}
