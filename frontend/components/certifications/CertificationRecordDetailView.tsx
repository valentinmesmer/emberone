/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { FileText } from 'lucide-react';
import { useMemo } from 'react';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { FieldDisplay } from '@/components/ui/field-display';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyState, NotFoundState } from '@/components/ui/message-cards';
import { RecordSourceLinkSection } from '@/components/ui/record-source-link-section';
import { useTranslation } from '@/hooks/useTranslation';
import { resolveRecordEvidenceOrigin } from '@/lib/certifications/evidenceTypes';
import { computeRecordCertificationStatus } from '@/lib/certifications/recordValidity';
import { CERTIFICATION_STATUS_BADGE_TONE } from '@/lib/certifications/statusPresentation';
import { MY_AREA_BASE_PATH } from '@/lib/my-area/myAreaSections';
import { getOrganizationUserDocumentDetailPath } from '@/lib/navigation/userManagementSections';
import { usePermissions } from '@/lib/permission/PermissionContext';
import {
  useMyCertificationRecords,
  useOrganizationUserDocuments,
  useUserCertificationRecords,
  useUserDocuments,
} from '@/lib/store';
import { formatDateLong } from '@/lib/utils';
import type { AppNavigationContext } from '@/lib/utils/appLink';
import {
  resolveEventSourceHref,
  resolveOnboardingSourceHref,
  resolveWorkflowSourceHref,
} from '@/lib/utils/sourceLink';
import { formatFileSize } from '@/lib/utils/userDocumentFormat';

export type CertificationRecordDetailViewProps =
  | {
      scope: 'self';
      definitionId: number;
      recordId: number;
    }
  | {
      scope: 'org';
      organizationId: number;
      userId: number;
      definitionId: number;
      recordId: number;
    };

export function CertificationRecordDetailView(props: CertificationRecordDetailViewProps) {
  const t = useTranslation();
  const { hasPermission } = usePermissions();
  const isSelf = props.scope === 'self';
  const navigationContext: AppNavigationContext = isSelf
    ? { scope: 'self' }
    : { scope: 'org', organizationId: props.organizationId, userId: props.userId };

  const { data: myRecords = [], isLoading: myRecordsLoading } = useMyCertificationRecords();
  const { data: orgRecords = [], isLoading: orgRecordsLoading } = useUserCertificationRecords(
    isSelf ? 0 : props.organizationId,
    isSelf ? 0 : props.userId,
    !isSelf,
  );
  const { data: myDocuments = [], isLoading: myDocumentsLoading } = useUserDocuments();
  const { data: orgDocuments = [], isLoading: orgDocumentsLoading } = useOrganizationUserDocuments(
    isSelf ? null : props.organizationId,
    isSelf ? null : props.userId,
  );

  const records = isSelf ? myRecords : orgRecords;
  const documents = isSelf ? myDocuments : orgDocuments;
  const loading = isSelf
    ? myRecordsLoading || myDocumentsLoading
    : orgRecordsLoading || orgDocumentsLoading;

  const record = useMemo(
    () =>
      records.find(
        (entry) =>
          entry.id === props.recordId && entry.tenantCertDefinitionId === props.definitionId,
      ),
    [props.definitionId, props.recordId, records],
  );

  const linkedDocuments = useMemo(() => {
    const documentIds = new Set(record?.userDocumentIds ?? []);
    return documents.filter((doc) => documentIds.has(doc.id));
  }, [documents, record?.userDocumentIds]);

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!record) {
    return <NotFoundState message={t('certifications.record.notFound')} />;
  }

  const recordStatus = computeRecordCertificationStatus(record);
  const origin = resolveRecordEvidenceOrigin(record);
  const isEventOrigin = origin === 'SERVICE_EVENT' || origin === 'OPERATION_EVENT';
  const showDocumentsSection = linkedDocuments.length > 0 || origin === 'DOCUMENT';

  const eventHref = isEventOrigin ? resolveEventSourceHref(record, hasPermission) : null;
  const workflowHref = resolveWorkflowSourceHref(record, navigationContext, hasPermission);
  const onboardingHref = resolveOnboardingSourceHref(record, navigationContext, hasPermission);

  const getDocumentHref = (documentId: number) =>
    props.scope === 'self'
      ? `${MY_AREA_BASE_PATH}/documents/${documentId}`
      : getOrganizationUserDocumentDetailPath(props.organizationId, props.userId, documentId);

  return (
    <div className="w-full space-y-4">
      <Card density="compact">
        <div className="flex flex-wrap items-start justify-between gap-x-3 gap-y-2">
          <h1 className="min-w-0 flex-1 break-words text-base font-semibold text-[var(--text-primary)]">
            {record.certDefinitionName ?? t('certifications.record.detailTitle')}
          </h1>
          <Badge tone={CERTIFICATION_STATUS_BADGE_TONE[recordStatus]} pill className="shrink-0">
            {t(`certifications.status.${recordStatus}`)}
          </Badge>
        </div>
        <div className="mt-3 grid grid-cols-1 gap-3 border-t border-[var(--border-color)]/30 pt-3 sm:grid-cols-2">
          <FieldDisplay
            label={t('certifications.record.completedDateLabel')}
            value={formatDateLong(record.completedDate)}
          />
          {record.expiryDate ? (
            <FieldDisplay
              label={t('certifications.detail.validUntil')}
              value={formatDateLong(record.expiryDate)}
            />
          ) : null}
          <FieldDisplay
            label={t('certifications.record.acquiredViaLabel')}
            value={t(`certifications.record.acquiredVia.${origin}`)}
          />
        </div>
        {record.notes?.trim() ? (
          <div className="mt-3 border-t border-[var(--border-color)]/30 pt-3">
            <FieldDisplay label={t('common.record.notesLabel')} value={record.notes} />
          </div>
        ) : null}
      </Card>

      {eventHref && isEventOrigin ? (
        <RecordSourceLinkSection kind={origin} href={eventHref} />
      ) : null}
      {workflowHref ? <RecordSourceLinkSection kind="WORKFLOW" href={workflowHref} /> : null}
      {onboardingHref ? <RecordSourceLinkSection kind="ONBOARDING" href={onboardingHref} /> : null}

      {showDocumentsSection ? (
        <section>
          <h2 className="mb-1.5 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
            {t('common.record.documentsTitle')}
          </h2>
          {linkedDocuments.length === 0 ? (
            <EmptyState>{t('common.record.noDocuments')}</EmptyState>
          ) : (
            <ListContainer label={t('common.record.documentsTitle')} rowSeparator="inset">
              {linkedDocuments.map((doc) => (
                <ListRow
                  key={doc.id}
                  compact
                  href={getDocumentHref(doc.id)}
                  chevron
                  leading={
                    <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-[var(--surface-muted)] text-[var(--text-secondary)]">
                      <FileText className="h-4 w-4" aria-hidden />
                    </div>
                  }
                  title={doc.title}
                  subtitle={
                    <span className="flex min-w-0 flex-wrap items-center gap-x-1.5 gap-y-0.5">
                      <span className="truncate">{doc.fileName}</span>
                      <span className="shrink-0 text-[var(--text-secondary)]/50" aria-hidden>
                        ·
                      </span>
                      <span className="shrink-0 tabular-nums">{formatFileSize(doc.sizeBytes)}</span>
                    </span>
                  }
                />
              ))}
            </ListContainer>
          )}
        </section>
      ) : null}
    </div>
  );
}
