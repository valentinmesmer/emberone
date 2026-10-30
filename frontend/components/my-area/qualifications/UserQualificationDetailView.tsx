/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Upload } from 'lucide-react';
import { useMemo, useState } from 'react';
import { CertificationRecordSubmitModal } from '@/components/my-area/certifications/CertificationRecordSubmitModal';
import { DrivingLicenseRecordSubmitModal } from '@/components/my-area/drivingLicenses/DrivingLicenseRecordSubmitModal';
import { QualificationDetailStatusCard } from '@/components/my-area/qualifications/QualificationDetailStatusCard';
import { QualificationPrerequisiteSection } from '@/components/my-area/qualifications/QualificationPrerequisiteSection';
import { TrainingCourseRecordSubmitModal } from '@/components/my-area/trainingCourses/TrainingCourseRecordSubmitModal';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyState, NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import type {
  CertificationDefinitionDto,
  CertificationStatusItemDto,
} from '@/lib/api/certifications';
import type { DrivingLicenseDefinitionDto } from '@/lib/api/drivingLicenses';
import type {
  QualificationPrerequisiteDetailDto,
  QualificationStatusItemDto,
} from '@/lib/api/qualifications';
import type { TrainingCourseTypeDto, TrainingCourseUserRecordDto } from '@/lib/api/trainingCourses';
import { CERTIFICATION_STATUS_BADGE_TONE } from '@/lib/certifications/statusPresentation';
import {
  findOpenCertificationWorkflow,
  mergeSubmittableCertificationOptions,
} from '@/lib/certifications/submittableCertifications';
import {
  findClass,
  isDrivingLicenseClassValidOn,
  isDrivingLicenseDocumentValidOn,
} from '@/lib/drivingLicenses/drivingLicenseValidity';
import { getOpenDrivingLicenseWorkflow } from '@/lib/drivingLicenses/submittableDrivingLicenses';
import {
  getMyAreaCertificationDetailPath,
  getMyAreaDrivingLicensesPath,
  getMyAreaTrainingCourseRecordDetailPath,
  getMyAreaWorkflowDetailPath,
} from '@/lib/my-area/myAreaSections';
import {
  aggregateSelfQualificationStatuses,
  qualificationDetailItemFromStatuses,
  qualificationListBadgeLabel,
  qualificationOrgEntriesFromStatuses,
} from '@/lib/qualifications/aggregateQualifications';
import { QUALIFICATION_STATUS_ROW_TONE } from '@/lib/qualifications/statusPresentation';
import {
  useMyAvailableDrivingLicenseDefinitions,
  useMyAvailableTrainingCourseTypes,
  useMyBeginnableWorkflowTypes,
  useMyCertificationStatus,
  useMyDrivingLicense,
  useMyOrganizations,
  useMyRequestableCertificationDefinitions,
  useMyTrainingCourseRecords,
  useMyWorkflowOpenedPage,
  useSelfQualificationStatus,
  useWorkflowOpenConcurrencyKeys,
  useWorkflowOpenState,
} from '@/lib/store';
import {
  filterSubmittableTrainingCourseTypes,
  findOpenTrainingCourseWorkflow,
} from '@/lib/trainingCourses/submittableTrainingCourses';
import { WORKFLOW_TYPE_IDS } from '@/types/workflow';

type PrerequisiteWorkflowAction =
  | { kind: 'submitTraining'; typeId: number }
  | { kind: 'submitCert'; definitionId: number }
  | { kind: 'submitDrivingLicense'; definitionId: number };

export interface UserQualificationDetailViewProps {
  definitionId: number;
}

export function UserQualificationDetailView({ definitionId }: UserQualificationDetailViewProps) {
  const t = useTranslation();
  const { data: statuses = [], isLoading: statusLoading } = useSelfQualificationStatus();
  const { data: memberships = [], isLoading: orgsLoading } = useMyOrganizations();
  const { data: certStatus, isLoading: certLoading } = useMyCertificationStatus();
  const { data: trainingRecords = [], isLoading: trainingLoading } = useMyTrainingCourseRecords();
  const { data: drivingLicense, isLoading: drivingLicenseLoading } = useMyDrivingLicense();
  const { data: openedWorkflows = [] } = useMyWorkflowOpenedPage();
  const { data: beginnableTypes = [] } = useMyBeginnableWorkflowTypes();
  const { data: availableTrainingTypes = [] } = useMyAvailableTrainingCourseTypes();
  const { data: availableDrivingDefinitions = [] } = useMyAvailableDrivingLicenseDefinitions();

  const [trainingSubmitTypeId, setTrainingSubmitTypeId] = useState<number | null>(null);
  const [certSubmitDefinitionId, setCertSubmitDefinitionId] = useState<number | null>(null);
  const [drivingLicenseSubmitDefinitionId, setDrivingLicenseSubmitDefinitionId] = useState<
    number | null
  >(null);

  const organizationNameById = useMemo(
    () =>
      new Map(
        memberships
          .map((membership) => membership.organization)
          .filter(
            (organization): organization is NonNullable<typeof organization> =>
              organization != null,
          )
          .map((organization) => [organization.id, organization.name] as const),
      ),
    [memberships],
  );

  const item = useMemo(
    () => qualificationDetailItemFromStatuses(statuses, definitionId),
    [definitionId, statuses],
  );

  const aggregated = useMemo(() => {
    const items = aggregateSelfQualificationStatuses(statuses, organizationNameById);
    return items.find((entry) => entry.tenantQualificationDefinitionId === definitionId) ?? null;
  }, [definitionId, organizationNameById, statuses]);

  const orgEntries = useMemo(
    () => qualificationOrgEntriesFromStatuses(statuses, definitionId, organizationNameById),
    [definitionId, organizationNameById, statuses],
  );

  const trainingWorkflowBeginnable = useMemo(
    () =>
      beginnableTypes.some(
        (type) => type.workflowTypeId === WORKFLOW_TYPE_IDS.TRAINING_COURSE_RECORD,
      ),
    [beginnableTypes],
  );
  const certificationWorkflowBeginnable = useMemo(
    () =>
      beginnableTypes.some(
        (type) => type.workflowTypeId === WORKFLOW_TYPE_IDS.CERTIFICATION_RECORD,
      ),
    [beginnableTypes],
  );
  const drivingLicenseWorkflowBeginnable = useMemo(
    () =>
      beginnableTypes.some(
        (type) => type.workflowTypeId === WORKFLOW_TYPE_IDS.DRIVING_LICENSE_RECORD,
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
  const { data: trainingOpenKeys = [] } = useWorkflowOpenConcurrencyKeys(
    WORKFLOW_TYPE_IDS.TRAINING_COURSE_RECORD,
    trainingWorkflowBeginnable,
  );
  const { data: drivingLicenseOpenState } = useWorkflowOpenState(
    WORKFLOW_TYPE_IDS.DRIVING_LICENSE_RECORD,
    drivingLicenseWorkflowBeginnable,
  );

  const submittableTrainingTypes = useMemo(
    () => filterSubmittableTrainingCourseTypes(availableTrainingTypes, trainingOpenKeys),
    [availableTrainingTypes, trainingOpenKeys],
  );
  const openDrivingLicenseWorkflow = useMemo(
    () => getOpenDrivingLicenseWorkflow(openedWorkflows),
    [openedWorkflows],
  );
  const canOpenDrivingLicenseWorkflow =
    drivingLicenseWorkflowBeginnable && !(drivingLicenseOpenState?.typeBlocked ?? false);
  const submittableCertItems = useMemo(
    () =>
      mergeSubmittableCertificationOptions(
        certStatus?.items ?? [],
        catalogDefinitions,
        certOpenKeys,
      ),
    [catalogDefinitions, certOpenKeys, certStatus?.items],
  );

  const certByName = useMemo(
    () => new Map((certStatus?.items ?? []).map((entry) => [entry.certDefinitionName, entry])),
    [certStatus?.items],
  );

  const certDefinitionByConfigKey = useMemo(() => {
    const map = new Map<string, CertificationDefinitionDto>();
    for (const definition of catalogDefinitions) {
      const key = definition.configKey?.trim();
      if (key) map.set(key, definition);
    }
    return map;
  }, [catalogDefinitions]);

  const certDefinitionByName = useMemo(
    () => new Map(catalogDefinitions.map((definition) => [definition.name, definition])),
    [catalogDefinitions],
  );

  const submittableByConfigKey = useMemo(() => {
    const map = new Map<string, CertificationStatusItemDto>();
    for (const definition of catalogDefinitions) {
      const key = definition.configKey?.trim();
      if (!key) continue;
      const item = submittableCertItems.find(
        (entry) => entry.tenantCertDefinitionId === definition.id,
      );
      if (item) map.set(key, item);
    }
    return map;
  }, [catalogDefinitions, submittableCertItems]);

  const submittableByName = useMemo(
    () => new Map(submittableCertItems.map((item) => [item.certDefinitionName, item])),
    [submittableCertItems],
  );

  const trainingTypeByConfigKey = useMemo(() => {
    const map = new Map<string, TrainingCourseTypeDto>();
    for (const type of availableTrainingTypes) {
      const key = type.configKey?.trim();
      if (key) map.set(key, type);
    }
    return map;
  }, [availableTrainingTypes]);

  const trainingTypeByName = useMemo(
    () => new Map(availableTrainingTypes.map((type) => [type.name, type])),
    [availableTrainingTypes],
  );

  const drivingLicenseDefinitionByConfigKey = useMemo(() => {
    const map = new Map<string, DrivingLicenseDefinitionDto>();
    for (const definition of availableDrivingDefinitions) {
      const key = definition.configKey?.trim();
      if (key) map.set(key, definition);
    }
    return map;
  }, [availableDrivingDefinitions]);

  const drivingLicenseDefinitionByName = useMemo(
    () => new Map(availableDrivingDefinitions.map((definition) => [definition.name, definition])),
    [availableDrivingDefinitions],
  );

  const drivingLicenseClassExpired = (definitionId: number) => {
    if (
      drivingLicense == null ||
      !isDrivingLicenseDocumentValidOn(drivingLicense.documentExpirationDate)
    ) {
      return false;
    }
    const licenseClass = findClass(drivingLicense, definitionId);
    return licenseClass != null && !isDrivingLicenseClassValidOn(licenseClass.expirationDate);
  };

  const resolveDrivingLicenseDefinition = (detail: QualificationPrerequisiteDetailDto) =>
    drivingLicenseDefinitionByConfigKey.get(detail.configKey) ??
    drivingLicenseDefinitionByName.get(detail.name) ??
    null;

  const resolveTrainingType = (detail: QualificationPrerequisiteDetailDto) =>
    trainingTypeByConfigKey.get(detail.configKey) ?? trainingTypeByName.get(detail.name) ?? null;

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

  const drivingLicenseSubmitPresetClasses = useMemo(() => {
    const existing =
      drivingLicense?.classes.map((licenseClass) => ({
        tenantDrivingLicenseDefinitionId: licenseClass.tenantDrivingLicenseDefinitionId,
        grantDate: licenseClass.grantDate,
        expirationDate: licenseClass.expirationDate,
      })) ?? [];
    if (drivingLicenseSubmitDefinitionId == null) {
      return existing;
    }
    if (
      existing.some(
        (entry) => entry.tenantDrivingLicenseDefinitionId === drivingLicenseSubmitDefinitionId,
      )
    ) {
      return existing;
    }
    return [
      ...existing,
      {
        tenantDrivingLicenseDefinitionId: drivingLicenseSubmitDefinitionId,
        grantDate: null,
        expirationDate: null,
      },
    ];
  }, [drivingLicense, drivingLicenseSubmitDefinitionId]);

  const resolveSubmittableDefinition = (detail: QualificationPrerequisiteDetailDto) =>
    (detail.configKey ? submittableByConfigKey.get(detail.configKey) : undefined) ??
    submittableByName.get(detail.name) ??
    null;

  const resolveCertDefinitionId = (detail: QualificationPrerequisiteDetailDto): number | null => {
    const fromConfigKey = detail.configKey
      ? certDefinitionByConfigKey.get(detail.configKey)
      : undefined;
    if (fromConfigKey) return fromConfigKey.id;
    const fromCatalogName = certDefinitionByName.get(detail.name);
    if (fromCatalogName) return fromCatalogName.id;
    const fromStatus = certByName.get(detail.name);
    if (fromStatus) return fromStatus.tenantCertDefinitionId;
    const fromSubmittable = resolveSubmittableDefinition(detail);
    if (fromSubmittable) return fromSubmittable.tenantCertDefinitionId;
    return null;
  };

  const resolveTrainingAction = (
    detail: QualificationPrerequisiteDetailDto,
  ): PrerequisiteWorkflowAction | null => {
    if (detail.met || !trainingWorkflowBeginnable) return null;
    const type = resolveTrainingType(detail);
    if (!type) return null;
    if (findOpenTrainingCourseWorkflow(openedWorkflows, type.id)) return null;
    if (!submittableTrainingTypes.some((entry) => entry.id === type.id)) return null;
    return { kind: 'submitTraining', typeId: type.id };
  };

  const resolveCertificationAction = (
    detail: QualificationPrerequisiteDetailDto,
  ): PrerequisiteWorkflowAction | null => {
    if (detail.met || !certificationWorkflowBeginnable) return null;
    const item = resolveSubmittableDefinition(detail);
    if (!item) return null;
    if (findOpenCertificationWorkflow(openedWorkflows, item.tenantCertDefinitionId)) {
      return null;
    }
    return { kind: 'submitCert', definitionId: item.tenantCertDefinitionId };
  };

  const resolveDrivingLicenseAction = (
    detail: QualificationPrerequisiteDetailDto,
  ): PrerequisiteWorkflowAction | null => {
    if (detail.met || !canOpenDrivingLicenseWorkflow || openDrivingLicenseWorkflow != null)
      return null;
    const definition = resolveDrivingLicenseDefinition(detail);
    if (!definition) return null;
    return { kind: 'submitDrivingLicense', definitionId: definition.id };
  };

  const resolveTrainingOpenWorkflowId = (detail: QualificationPrerequisiteDetailDto) => {
    const type = resolveTrainingType(detail);
    if (!type) return null;
    return findOpenTrainingCourseWorkflow(openedWorkflows, type.id)?.id ?? null;
  };

  const resolveCertificationOpenWorkflowId = (detail: QualificationPrerequisiteDetailDto) => {
    const definitionId = resolveCertDefinitionId(detail);
    if (definitionId == null) return null;
    return findOpenCertificationWorkflow(openedWorkflows, definitionId)?.id ?? null;
  };

  const resolveDrivingLicenseOpenWorkflowId = () => openDrivingLicenseWorkflow?.workflow.id ?? null;

  const resolveTrainingHref = (detail: QualificationPrerequisiteDetailDto) => {
    const openWorkflowId = resolveTrainingOpenWorkflowId(detail);
    if (openWorkflowId != null) return getMyAreaWorkflowDetailPath(openWorkflowId);
    return resolveTrainingCourseHref(detail, trainingRecordByTypeName);
  };

  const resolveCertificationHref = (detail: QualificationPrerequisiteDetailDto) => {
    const openWorkflowId = resolveCertificationOpenWorkflowId(detail);
    if (openWorkflowId != null) return getMyAreaWorkflowDetailPath(openWorkflowId);
    return resolveCertificationDetailHref(detail, certByName);
  };

  const resolveDrivingLicenseHref = () => {
    const openWorkflowId = resolveDrivingLicenseOpenWorkflowId();
    if (openWorkflowId != null) return getMyAreaWorkflowDetailPath(openWorkflowId);
    if (drivingLicense == null) return null;
    return getMyAreaDrivingLicensesPath();
  };

  const openWorkflowAction = (action: PrerequisiteWorkflowAction) => {
    if (action.kind === 'submitTraining') {
      setTrainingSubmitTypeId(action.typeId);
      return;
    }
    if (action.kind === 'submitCert') {
      setCertSubmitDefinitionId(action.definitionId);
      return;
    }
    setDrivingLicenseSubmitDefinitionId(action.definitionId);
  };

  const loading =
    statusLoading || orgsLoading || certLoading || trainingLoading || drivingLicenseLoading;

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!item || !aggregated) {
    return <NotFoundState message={t('qualifications.detail.notFound')} />;
  }

  const summaryLabel = qualificationListBadgeLabel(aggregated, t);
  const trainingSubmitTypes =
    trainingSubmitTypeId != null
      ? submittableTrainingTypes.filter((type) => type.id === trainingSubmitTypeId)
      : [];

  const hasPrerequisiteDetails =
    item.trainingCourseDetails.length > 0 ||
    item.certDefinitionDetails.length > 0 ||
    item.drivingLicenseDetails.length > 0;

  return (
    <div className="w-full space-y-4">
      <QualificationDetailStatusCard
        title={item.name}
        abbreviation={item.abbreviation}
        status={aggregated.aggregateStatus}
        statusLabel={summaryLabel}
        badgeTone={aggregated.badgeTone}
        hints={
          item.requiresOrgDecision ? (
            <p className="mt-1.5 text-xs text-[var(--text-secondary)]">
              {t('qualifications.detail.orgDecisionRequired')}
            </p>
          ) : null
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
              resolveHref={resolveTrainingHref}
              resolveAction={(detail) => {
                const action = resolveTrainingAction(detail);
                if (!action) return null;
                return (
                  <Button
                    type="button"
                    size="sm"
                    fullWidth
                    onClick={() => openWorkflowAction(action)}
                  >
                    <Upload className="h-3.5 w-3.5" aria-hidden />
                    {t('trainingCourse.submit.action')}
                  </Button>
                );
              }}
              resolveStatusBadge={(detail) =>
                resolveOpenWorkflowBadge(detail, resolveTrainingOpenWorkflowId(detail) != null, t)
              }
              resolveInProgress={(detail) => resolveTrainingOpenWorkflowId(detail) != null}
            />
          ) : null}
          {item.drivingLicenseDetails.length > 0 ? (
            <QualificationPrerequisiteSection
              title={t('qualifications.prerequisite.drivingLicensesGroup')}
              items={item.drivingLicenseDetails}
              requirementMode={item.drivingLicenseRequirementMode ?? 'ALL'}
              groupMet={item.drivingLicensesMet}
              resolveHref={resolveDrivingLicenseHref}
              resolveAction={(detail) => {
                const action = resolveDrivingLicenseAction(detail);
                if (!action) return null;
                return (
                  <Button
                    type="button"
                    size="sm"
                    fullWidth
                    onClick={() => openWorkflowAction(action)}
                  >
                    <Upload className="h-3.5 w-3.5" aria-hidden />
                    {drivingLicense != null
                      ? t('drivingLicense.update.action')
                      : t('drivingLicense.submit.action')}
                  </Button>
                );
              }}
              resolveStatusBadge={(detail) => {
                if (resolveDrivingLicenseOpenWorkflowId() != null) {
                  return resolveOpenWorkflowBadge(detail, true, t);
                }
                return resolveDrivingLicenseStatusBadge(
                  detail,
                  resolveDrivingLicenseDefinition,
                  drivingLicenseClassExpired,
                  t,
                );
              }}
              resolveInProgress={() => resolveDrivingLicenseOpenWorkflowId() != null}
            />
          ) : null}
          {item.certDefinitionDetails.length > 0 ? (
            <QualificationPrerequisiteSection
              title={t('qualifications.prerequisite.certificationsGroup')}
              items={item.certDefinitionDetails}
              requirementMode={item.certRequirementMode ?? 'ALL'}
              groupMet={item.certDefinitionsMet}
              resolveHref={resolveCertificationHref}
              resolveAction={(detail) => {
                const action = resolveCertificationAction(detail);
                if (!action) return null;
                return (
                  <Button
                    type="button"
                    size="sm"
                    fullWidth
                    onClick={() => openWorkflowAction(action)}
                  >
                    <Upload className="h-3.5 w-3.5" aria-hidden />
                    {t('certifications.submit.action')}
                  </Button>
                );
              }}
              resolveStatusBadge={(detail) => {
                if (resolveCertificationOpenWorkflowId(detail) != null) {
                  return resolveOpenWorkflowBadge(detail, true, t);
                }
                return resolveCertificationStatusBadge(detail, certByName, t);
              }}
              resolveInProgress={(detail) => resolveCertificationOpenWorkflowId(detail) != null}
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

      {item.requiresOrgDecision ? (
        <section>
          <h2 className="mb-1.5 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
            {t('qualifications.detail.orgDecisionTitle')}
          </h2>
          <ListContainer label={t('qualifications.detail.orgDecisionTitle')} rowSeparator="inset">
            {orgEntries.map((entry) => (
              <ListRow
                key={entry.organizationId}
                compact
                tone={QUALIFICATION_STATUS_ROW_TONE[entry.status]}
                title={entry.organizationName}
              />
            ))}
          </ListContainer>
        </section>
      ) : null}

      <TrainingCourseRecordSubmitModal
        isOpen={trainingSubmitTypeId != null}
        onClose={() => setTrainingSubmitTypeId(null)}
        submittableTypes={trainingSubmitTypes}
        presetTypeIds={trainingSubmitTypeId != null ? [trainingSubmitTypeId] : undefined}
        lockTypes
      />
      <CertificationRecordSubmitModal
        isOpen={certSubmitDefinitionId != null}
        onClose={() => setCertSubmitDefinitionId(null)}
        submittableItems={submittableCertItems}
        presetDefinitionId={certSubmitDefinitionId ?? undefined}
      />
      <DrivingLicenseRecordSubmitModal
        isOpen={drivingLicenseSubmitDefinitionId != null}
        onClose={() => setDrivingLicenseSubmitDefinitionId(null)}
        classDefinitions={availableDrivingDefinitions.filter((definition) => definition.active)}
        presetClasses={drivingLicenseSubmitPresetClasses}
        renewal={drivingLicense != null}
      />
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

function resolveCertificationDetailHref(
  detail: QualificationStatusItemDto['certDefinitionDetails'][number],
  certByName: Map<string | null, CertificationStatusItemDto>,
) {
  const certItem = certByName.get(detail.name);
  if (!certItem) return null;
  return getMyAreaCertificationDetailPath(certItem.tenantCertDefinitionId);
}

function resolveCertificationStatusBadge(
  detail: QualificationStatusItemDto['certDefinitionDetails'][number],
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

function resolveOpenWorkflowBadge(
  detail: QualificationPrerequisiteDetailDto,
  hasOpenWorkflow: boolean,
  t: (key: string) => string,
) {
  if (detail.met || !hasOpenWorkflow) return null;
  return (
    <Badge tone="warning" pill>
      {t('certifications.detail.inProgress')}
    </Badge>
  );
}

function resolveTrainingCourseHref(
  detail: QualificationStatusItemDto['trainingCourseDetails'][number],
  trainingRecordByTypeName: Map<string, TrainingCourseUserRecordDto>,
) {
  const record = trainingRecordByTypeName.get(detail.name);
  if (!record) return null;
  return getMyAreaTrainingCourseRecordDetailPath(record.id);
}
