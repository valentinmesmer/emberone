/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { QualificationDetailStatusCard } from '@/components/my-area/qualifications/QualificationDetailStatusCard';
import { QualificationPrerequisiteSection } from '@/components/my-area/qualifications/QualificationPrerequisiteSection';
import { QualificationOrgDecisionPanel } from '@/components/organization/user-management/QualificationOrgDecisionPanel';
import { Badge } from '@/components/ui/badge';
import { LoadingState } from '@/components/ui/loading';
import { EmptyState, NotFoundState } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import type { CertificationStatusItemDto } from '@/lib/api/certifications';
import type { DrivingLicenseDefinitionDto } from '@/lib/api/drivingLicenses';
import type { QualificationPrerequisiteDetailDto } from '@/lib/api/qualifications';
import type { TrainingCourseUserRecordDto } from '@/lib/api/trainingCourses';
import { CERTIFICATION_STATUS_BADGE_TONE } from '@/lib/certifications/statusPresentation';
import {
  findClass,
  isDrivingLicenseClassValidOn,
  isDrivingLicenseDocumentValidOn,
} from '@/lib/drivingLicenses/drivingLicenseValidity';
import {
  getOrganizationUserCertificationDetailPath,
  getOrganizationUserSectionPath,
  getOrganizationUserTrainingCourseRecordDetailPath,
} from '@/lib/navigation/userManagementSections';
import {
  useGrantQualificationOrgDecision,
  useOrgDrivingLicenseCatalog,
  useUpdateQualificationOrgDecision,
  useUserCertificationStatus,
  useUserDrivingLicense,
  useUserQualificationStatus,
  useUserTrainingCourseRecords,
} from '@/lib/store';

export interface OrgUserQualificationDetailViewProps {
  organizationId: number;
  userId: number;
  definitionId: number;
  canWrite: boolean;
}

export function OrgUserQualificationDetailView({
  organizationId,
  userId,
  definitionId,
  canWrite,
}: OrgUserQualificationDetailViewProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const { data: status, isLoading: statusLoading } = useUserQualificationStatus(
    organizationId,
    userId,
  );
  const { data: certStatus, isLoading: certLoading } = useUserCertificationStatus(
    organizationId,
    userId,
  );
  const { data: trainingRecords = [], isLoading: trainingLoading } = useUserTrainingCourseRecords(
    organizationId,
    userId,
  );
  const { data: drivingLicense, isLoading: drivingLicenseLoading } = useUserDrivingLicense(
    organizationId,
    userId,
  );
  const { data: drivingLicenseCatalog, isLoading: drivingLicenseCatalogLoading } =
    useOrgDrivingLicenseCatalog(organizationId, userId);
  const grant = useGrantQualificationOrgDecision(organizationId, userId);
  const updateOrgDecision = useUpdateQualificationOrgDecision(organizationId, userId);

  const item = status?.items.find(
    (entry) => entry.tenantQualificationDefinitionId === definitionId,
  );

  const certByName = useMemo(
    () => new Map((certStatus?.items ?? []).map((entry) => [entry.certDefinitionName, entry])),
    [certStatus?.items],
  );

  const trainingRecordByTypeName = useMemo(() => {
    const map = new Map<string, TrainingCourseUserRecordDto>();
    for (const record of trainingRecords) {
      for (const granted of record.grantedTypes) {
        const typeName = granted.courseTypeName?.trim();
        if (typeName && !map.has(typeName)) {
          map.set(typeName, record);
        }
      }
    }
    return map;
  }, [trainingRecords]);

  const drivingLicenseHref = drivingLicense
    ? getOrganizationUserSectionPath(organizationId, userId, 'driving-licenses')
    : null;

  const drivingLicenseClassExpired = (definitionId: number) => {
    if (
      drivingLicense == null ||
      !isDrivingLicenseDocumentValidOn(drivingLicense.documentExpirationDate)
    ) {
      return true;
    }
    const licenseClass = findClass(drivingLicense, definitionId);
    if (licenseClass == null) {
      return false;
    }
    return !isDrivingLicenseClassValidOn(licenseClass.expirationDate);
  };

  const drivingLicenseDefinitionByConfigKey = useMemo(() => {
    const map = new Map<string, DrivingLicenseDefinitionDto>();
    for (const definition of drivingLicenseCatalog?.definitions ?? []) {
      const key = definition.configKey?.trim();
      if (key) map.set(key, definition);
    }
    return map;
  }, [drivingLicenseCatalog?.definitions]);

  const drivingLicenseDefinitionByName = useMemo(
    () =>
      new Map(
        (drivingLicenseCatalog?.definitions ?? []).map((definition) => [
          definition.name,
          definition,
        ]),
      ),
    [drivingLicenseCatalog?.definitions],
  );

  const resolveDrivingLicenseDefinition = (detail: QualificationPrerequisiteDetailDto) =>
    drivingLicenseDefinitionByConfigKey.get(detail.configKey) ??
    drivingLicenseDefinitionByName.get(detail.name) ??
    null;

  const handleGrant = async (params: { startDate: string; endDate?: string }) => {
    if (!item) return;
    try {
      await grant.mutateAsync({
        qualificationConfigKey: item.configKey,
        startDate: params.startDate,
        endDate: params.endDate,
      });
      showSuccess(t('qualifications.orgDecision.granted'));
    } catch (e) {
      showError(e instanceof Error ? e.message : t('qualifications.orgDecision.grantFailed'));
      throw e;
    }
  };

  const handleSetEndDate = async (params: {
    decisionId: number;
    startDate: string;
    endDate: string;
  }) => {
    if (!item) return;
    try {
      await updateOrgDecision.mutateAsync({
        qualificationConfigKey: item.configKey,
        id: params.decisionId,
        startDate: params.startDate,
        endDate: params.endDate,
      });
      showSuccess(t('qualifications.orgDecision.endDateSaved'));
    } catch (e) {
      showError(e instanceof Error ? e.message : t('qualifications.orgDecision.endDateSaveFailed'));
      throw e;
    }
  };

  const loading =
    statusLoading ||
    certLoading ||
    trainingLoading ||
    drivingLicenseLoading ||
    drivingLicenseCatalogLoading;

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!item) {
    return <NotFoundState message={t('qualifications.detail.notFound')} />;
  }

  const statusLabel = t(`qualifications.status.${item.status}`);
  const hasPrerequisiteDetails =
    item.trainingCourseDetails.length > 0 ||
    item.certDefinitionDetails.length > 0 ||
    item.drivingLicenseDetails.length > 0;

  return (
    <div className="w-full space-y-4">
      <QualificationDetailStatusCard
        title={item.name}
        abbreviation={item.abbreviation}
        status={item.status}
        statusLabel={statusLabel}
        showStatusBadge={!item.requiresOrgDecision}
        footer={
          <QualificationOrgDecisionPanel
            item={item}
            canWrite={canWrite}
            isGrantPending={grant.isPending}
            isEndDatePending={updateOrgDecision.isPending}
            onGrant={(params) => void handleGrant(params)}
            onSetEndDate={(params) => void handleSetEndDate(params)}
          />
        }
      />

      {(item.trainingCourseDetails.length > 0 ||
        item.certDefinitionDetails.length > 0 ||
        item.drivingLicenseDetails.length > 0) && (
        <section className="space-y-4">
          {item.trainingCourseDetails.length > 0 ? (
            <QualificationPrerequisiteSection
              title={t('qualifications.prerequisite.trainingCoursesGroup')}
              items={item.trainingCourseDetails}
              requirementMode={item.trainingCourseRequirementMode ?? 'ALL'}
              groupMet={item.trainingCoursesMet}
              resolveHref={(detail) =>
                resolveTrainingCourseHref(detail, trainingRecordByTypeName, organizationId, userId)
              }
            />
          ) : null}
          {item.drivingLicenseDetails.length > 0 ? (
            <QualificationPrerequisiteSection
              title={t('qualifications.prerequisite.drivingLicensesGroup')}
              items={item.drivingLicenseDetails}
              requirementMode={item.drivingLicenseRequirementMode ?? 'ALL'}
              groupMet={item.drivingLicensesMet}
              resolveHref={() => drivingLicenseHref}
              resolveStatusBadge={(detail) =>
                resolveDrivingLicenseStatusBadge(
                  detail,
                  resolveDrivingLicenseDefinition,
                  drivingLicenseClassExpired,
                  t,
                )
              }
            />
          ) : null}
          {item.certDefinitionDetails.length > 0 ? (
            <QualificationPrerequisiteSection
              title={t('qualifications.prerequisite.certificationsGroup')}
              items={item.certDefinitionDetails}
              requirementMode={item.certRequirementMode ?? 'ALL'}
              groupMet={item.certDefinitionsMet}
              resolveHref={(detail) =>
                resolveCertificationHref(detail, certByName, organizationId, userId)
              }
              resolveStatusBadge={(detail) =>
                resolveCertificationStatusBadge(detail, certByName, t)
              }
            />
          ) : null}
        </section>
      )}

      {!hasPrerequisiteDetails ? (
        <section>
          <h2 className="mb-1.5 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
            {t('qualifications.detail.prerequisitesTitle')}
          </h2>
          <EmptyState>{t('qualifications.detail.noPrerequisites')}</EmptyState>
        </section>
      ) : null}
    </div>
  );
}

function resolveDrivingLicenseStatusBadge(
  detail: QualificationPrerequisiteDetailDto,
  resolveDefinition: (
    detail: QualificationPrerequisiteDetailDto,
  ) => DrivingLicenseDefinitionDto | null,
  isClassExpired: (definitionId: number) => boolean,
  t: (key: string) => string,
) {
  if (detail.met) return null;
  const definition = resolveDefinition(detail);
  if (!definition || !isClassExpired(definition.id)) return null;
  return (
    <Badge tone="danger" pill>
      {t('qualifications.prerequisite.drivingLicenseExpired')}
    </Badge>
  );
}

function resolveCertificationHref(
  detail: QualificationPrerequisiteDetailDto,
  certByName: Map<string | null, CertificationStatusItemDto>,
  organizationId: number,
  userId: number,
) {
  const certItem = certByName.get(detail.name);
  if (!certItem) return null;
  return getOrganizationUserCertificationDetailPath(
    organizationId,
    userId,
    certItem.tenantCertDefinitionId,
  );
}

function resolveCertificationStatusBadge(
  detail: QualificationPrerequisiteDetailDto,
  certByName: Map<string | null, CertificationStatusItemDto>,
  t: (key: string) => string,
) {
  const certItem = certByName.get(detail.name);
  if (!certItem || detail.met) return null;
  return (
    <Badge tone={CERTIFICATION_STATUS_BADGE_TONE[certItem.status]} pill>
      {t(`certifications.status.${certItem.status}`)}
    </Badge>
  );
}

function resolveTrainingCourseHref(
  detail: QualificationPrerequisiteDetailDto,
  trainingRecordByTypeName: Map<string, TrainingCourseUserRecordDto>,
  organizationId: number,
  userId: number,
) {
  const record = trainingRecordByTypeName.get(detail.name);
  if (!record) return null;
  return getOrganizationUserTrainingCourseRecordDetailPath(organizationId, userId, record.id);
}
