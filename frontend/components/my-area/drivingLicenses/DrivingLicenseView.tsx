/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { FileText } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { DetailCardHeader } from '@/components/ui/detail-card-header';
import { FieldDisplay } from '@/components/ui/field-display';
import { ListContainer, ListRow } from '@/components/ui/list';
import { EmptyState } from '@/components/ui/message-cards';
import { RecordSourceLinkSection } from '@/components/ui/record-source-link-section';
import { useTranslation } from '@/hooks/useTranslation';
import type { DrivingLicenseDto } from '@/lib/api/drivingLicenses';
import {
  isDrivingLicenseClassValidOn,
  isDrivingLicenseDocumentValidOn,
} from '@/lib/drivingLicenses/drivingLicenseValidity';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { formatDateLong } from '@/lib/utils';
import type { AppNavigationContext } from '@/lib/utils/appLink';
import { resolveOnboardingSourceHref, resolveWorkflowSourceHref } from '@/lib/utils/sourceLink';
import { formatFileSize } from '@/lib/utils/userDocumentFormat';

interface DrivingLicenseLinkedDocument {
  id: number;
  title: string;
  fileName: string;
  sizeBytes: number;
}

export interface DrivingLicenseViewProps {
  license: DrivingLicenseDto;
  linkedDocuments?: DrivingLicenseLinkedDocument[];
  getDocumentHref?: (documentId: number) => string;
  headerActions?: React.ReactNode;
  navigationContext?: AppNavigationContext;
}

export function DrivingLicenseView({
  license,
  linkedDocuments = [],
  getDocumentHref,
  headerActions,
  navigationContext = { scope: 'self' },
}: DrivingLicenseViewProps) {
  const t = useTranslation();
  const { hasPermission } = usePermissions();
  const documentValid = isDrivingLicenseDocumentValidOn(license.documentExpirationDate);
  const workflowHref = resolveWorkflowSourceHref(license, navigationContext, hasPermission);
  const onboardingHref = resolveOnboardingSourceHref(license, navigationContext, hasPermission);

  return (
    <div className="w-full space-y-4">
      <Card density="compact">
        <DetailCardHeader
          className="mb-0"
          title={t('drivingLicense.record.detailTitle')}
          badges={
            !documentValid ? (
              <Badge tone="danger">{t('drivingLicense.documentExpired')}</Badge>
            ) : undefined
          }
          actions={headerActions}
        />
        <div className="mt-3 grid grid-cols-1 gap-3 border-t border-[var(--border-color)]/30 pt-3 sm:grid-cols-2">
          <FieldDisplay
            label={t('drivingLicense.record.issueDateLabel')}
            value={formatDateLong(license.issueDate)}
          />
          {license.documentExpirationDate?.trim() ? (
            <FieldDisplay
              label={t('drivingLicense.record.documentExpirationDateLabel')}
              value={formatDateLong(license.documentExpirationDate)}
            />
          ) : null}
          {license.licenseNumber?.trim() ? (
            <FieldDisplay
              label={t('drivingLicense.record.licenseNumberLabel')}
              value={license.licenseNumber}
            />
          ) : null}
          {license.issuingAuthority?.trim() ? (
            <FieldDisplay
              label={t('drivingLicense.record.issuingAuthorityLabel')}
              value={license.issuingAuthority}
            />
          ) : null}
        </div>
        {license.notes?.trim() ? (
          <div className="mt-3 border-t border-[var(--border-color)]/30 pt-3">
            <FieldDisplay label={t('common.record.notesLabel')} value={license.notes} />
          </div>
        ) : null}
      </Card>

      {workflowHref ? <RecordSourceLinkSection kind="WORKFLOW" href={workflowHref} /> : null}
      {onboardingHref ? <RecordSourceLinkSection kind="ONBOARDING" href={onboardingHref} /> : null}

      <section>
        <h3 className="mb-1.5 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
          {t('drivingLicense.classes.label')}
        </h3>
        {(license.classes?.length ?? 0) === 0 ? (
          <EmptyState>{t('drivingLicense.grantedClasses.empty')}</EmptyState>
        ) : (
          <ListContainer label={t('drivingLicense.classes.label')} rowSeparator="inset">
            {license.classes.map((licenseClass) => {
              const classValid =
                documentValid && isDrivingLicenseClassValidOn(licenseClass.expirationDate);
              return (
                <ListRow
                  key={licenseClass.tenantDrivingLicenseDefinitionId}
                  compact
                  title={
                    licenseClass.className ??
                    licenseClass.abbreviation ??
                    `#${licenseClass.tenantDrivingLicenseDefinitionId}`
                  }
                  subtitle={[
                    licenseClass.grantDate?.trim()
                      ? `${t('drivingLicense.classes.grantDateLabel')}: ${formatDateLong(licenseClass.grantDate)}`
                      : null,
                    licenseClass.expirationDate?.trim()
                      ? `${t('drivingLicense.classes.expirationDateLabel')}: ${formatDateLong(licenseClass.expirationDate)}`
                      : null,
                  ]
                    .filter(Boolean)
                    .join(' · ')}
                  badges={
                    !classValid ? (
                      <Badge tone="danger">{t('drivingLicense.classExpired')}</Badge>
                    ) : null
                  }
                />
              );
            })}
          </ListContainer>
        )}
      </section>

      {getDocumentHref ? (
        <section>
          <h3 className="mb-1.5 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
            {t('common.record.documentsTitle')}
          </h3>
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
