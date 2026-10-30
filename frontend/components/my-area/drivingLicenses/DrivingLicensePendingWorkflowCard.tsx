/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import Link from 'next/link';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { FieldDisplay } from '@/components/ui/field-display';
import { ListContainer, ListRow } from '@/components/ui/list';
import { useTranslation } from '@/hooks/useTranslation';
import type { DrivingLicenseClassInput } from '@/lib/api/drivingLicenses';
import { formatDateLong } from '@/lib/utils';

interface DrivingLicensePendingWorkflowClass {
  label: string;
  grantDate?: string | null;
  expirationDate?: string | null;
}

export interface DrivingLicensePendingWorkflowCardProps {
  issueDate: string;
  documentExpirationDate?: string | null;
  licenseNumber?: string | null;
  issuingAuthority?: string | null;
  classes: DrivingLicensePendingWorkflowClass[];
  href?: string;
}

export function DrivingLicensePendingWorkflowCard({
  issueDate,
  documentExpirationDate,
  licenseNumber,
  issuingAuthority,
  classes,
  href,
}: DrivingLicensePendingWorkflowCardProps) {
  const t = useTranslation();

  const content = (
    <>
      <div className="flex flex-wrap items-start justify-between gap-x-3 gap-y-2">
        <h2 className="min-w-0 flex-1 break-words text-base font-semibold text-[var(--text-primary)]">
          {t('drivingLicense.record.detailTitle')}
        </h2>
        <Badge tone="warning">{t('certifications.status.IN_REVIEW')}</Badge>
      </div>
      <div className="mt-3 grid grid-cols-1 gap-3 border-t border-[var(--border-color)]/30 pt-3 sm:grid-cols-2">
        <FieldDisplay
          label={t('drivingLicense.record.issueDateLabel')}
          value={formatDateLong(issueDate)}
        />
        {documentExpirationDate?.trim() ? (
          <FieldDisplay
            label={t('drivingLicense.record.documentExpirationDateLabel')}
            value={formatDateLong(documentExpirationDate)}
          />
        ) : null}
        {licenseNumber?.trim() ? (
          <FieldDisplay
            label={t('drivingLicense.record.licenseNumberLabel')}
            value={licenseNumber}
          />
        ) : null}
        {issuingAuthority?.trim() ? (
          <FieldDisplay
            label={t('drivingLicense.record.issuingAuthorityLabel')}
            value={issuingAuthority}
          />
        ) : null}
      </div>
      {classes.length > 0 ? (
        <div className="mt-3 border-t border-[var(--border-color)]/30 pt-3">
          <ListContainer label={t('drivingLicense.classes.label')} rowSeparator="inset">
            {classes.map((licenseClass) => (
              <ListRow
                key={licenseClass.label}
                compact
                title={licenseClass.label}
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
              />
            ))}
          </ListContainer>
        </div>
      ) : null}
    </>
  );

  if (href) {
    return (
      <Link href={href} className="block rounded-lg transition-opacity hover:opacity-90">
        <Card
          density="compact"
          className="border-[var(--toast-warning-border)] bg-[var(--toast-warning-bg)]"
        >
          {content}
        </Card>
      </Link>
    );
  }

  return (
    <Card
      density="compact"
      className="border-[var(--toast-warning-border)] bg-[var(--toast-warning-bg)]"
    >
      {content}
    </Card>
  );
}

export function mapPendingWorkflowClasses(
  classes: DrivingLicenseClassInput[],
  resolveLabel: (definitionId: number) => string,
): DrivingLicensePendingWorkflowClass[] {
  return classes.map((licenseClass) => ({
    label: resolveLabel(licenseClass.tenantDrivingLicenseDefinitionId),
    grantDate: licenseClass.grantDate,
    expirationDate: licenseClass.expirationDate,
  }));
}
