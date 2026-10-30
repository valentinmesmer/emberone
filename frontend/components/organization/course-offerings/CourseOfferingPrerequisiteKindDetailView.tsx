/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useCallback, useEffect, useLayoutEffect, useMemo, useState } from 'react';
import { Card } from '@/components/ui/card';
import { CheckboxControl } from '@/components/ui/checkbox';
import { DetailCardHeader } from '@/components/ui/detail-card-header';
import { LoadingState } from '@/components/ui/loading';
import { ErrorState, NotFoundState } from '@/components/ui/message-cards';
import { Select } from '@/components/ui/select';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { tenantCertificationsApi } from '@/lib/api/tenantCertifications';
import { tenantDrivingLicenseDefinitionsApi } from '@/lib/api/tenantDrivingLicenseDefinitions';
import type { QualificationRequirementMode } from '@/lib/api/tenantQualifications';
import { tenantTrainingCourseTypesApi } from '@/lib/api/tenantTrainingCourseTypes';
import { useAuth } from '@/lib/auth/AuthContext';
import { useSetBackHref } from '@/lib/navigation/backHrefOverride';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { useOrganizationCourseOffering, useReplaceCourseOfferingPrerequisites } from '@/lib/store';
import { cn } from '@/lib/utils/cn';
import {
  canEditCourseOffering,
  getOrganizationCourseOfferingDetailPath,
} from '@/lib/utils/courseOffering';
import type {
  CourseOfferingDto,
  CoursePrerequisiteKind,
  ReplaceCoursePrerequisitesRequest,
} from '@/types/courseOffering';
import { PermissionTypeEnum } from '@/types/permission';

interface CatalogItem {
  configKey: string;
  abbreviation: string;
  name: string;
}

interface Props {
  organizationId: number;
  offeringId: number;
  kind: CoursePrerequisiteKind;
}

const TITLE_KEY: Record<CoursePrerequisiteKind, string> = {
  TRAINING_COURSE: 'courseOfferings.prerequisites.trainingCourses',
  CERTIFICATION: 'courseOfferings.prerequisites.certifications',
  DRIVING_LICENSE: 'courseOfferings.prerequisites.drivingLicenses',
};

const ABBREVIATION_KEY: Record<CoursePrerequisiteKind, string> = {
  TRAINING_COURSE: 'admin.tenantStandards.trainingCourses.abbreviation',
  CERTIFICATION: 'admin.tenantStandards.certifications.abbreviation',
  DRIVING_LICENSE: 'admin.tenantStandards.drivingLicenses.abbreviation',
};

function modeForKind(
  prereq: CourseOfferingDto['prerequisites'],
  kind: CoursePrerequisiteKind,
): QualificationRequirementMode {
  if (!prereq) return 'ALL';
  if (kind === 'TRAINING_COURSE') return prereq.trainingCourseRequirementMode;
  if (kind === 'CERTIFICATION') return prereq.certRequirementMode;
  return prereq.drivingLicenseRequirementMode;
}

function withKindMode(
  payload: ReplaceCoursePrerequisitesRequest,
  kind: CoursePrerequisiteKind,
  mode: QualificationRequirementMode,
): ReplaceCoursePrerequisitesRequest {
  if (kind === 'TRAINING_COURSE') {
    return { ...payload, trainingCourseRequirementMode: mode };
  }
  if (kind === 'CERTIFICATION') {
    return { ...payload, certRequirementMode: mode };
  }
  return { ...payload, drivingLicenseRequirementMode: mode };
}

export function CourseOfferingPrerequisiteKindDetailView({
  organizationId,
  offeringId,
  kind,
}: Props) {
  const t = useTranslation();
  const { tenantId } = useAuth();
  const { showSuccess, showError } = useToast();
  const { hasPermission } = usePermissions();
  const setBackHref = useSetBackHref();

  const canRead = hasPermission(organizationId, PermissionTypeEnum.COURSE_OFFERING_READ);
  const canWrite = hasPermission(organizationId, PermissionTypeEnum.COURSE_OFFERING_WRITE);

  const offeringPath = useMemo(
    () => getOrganizationCourseOfferingDetailPath(organizationId, offeringId),
    [organizationId, offeringId],
  );

  useLayoutEffect(() => {
    setBackHref(offeringPath);
    return () => setBackHref(null);
  }, [offeringPath, setBackHref]);

  const {
    data: offering,
    isLoading,
    isError,
  } = useOrganizationCourseOffering(canRead ? organizationId : null, offeringId, canRead);
  const replace = useReplaceCourseOfferingPrerequisites(organizationId, offeringId);

  const [catalog, setCatalog] = useState<CatalogItem[]>([]);
  const [loadingCatalog, setLoadingCatalog] = useState(false);

  const editable = Boolean(canWrite && offering && canEditCourseOffering(offering.status));
  const prereq = offering?.prerequisites ?? null;
  const selectedKeys = useMemo(
    () =>
      new Set(
        prereq?.entries.filter((entry) => entry.kind === kind).map((entry) => entry.configKey) ??
          [],
      ),
    [prereq, kind],
  );
  const mode = modeForKind(prereq, kind);

  const loadCatalog = useCallback(async () => {
    if (!tenantId) return;
    setLoadingCatalog(true);
    try {
      if (kind === 'TRAINING_COURSE') {
        const courses = await tenantTrainingCourseTypesApi.list(tenantId);
        setCatalog(
          courses
            .filter((item) => !item.archived)
            .map((item) => ({
              configKey: item.configKey,
              abbreviation: item.abbreviation,
              name: item.name,
            })),
        );
      } else if (kind === 'CERTIFICATION') {
        const certs = await tenantCertificationsApi.list(tenantId);
        setCatalog(
          certs
            .filter((item) => !item.archived)
            .map((item) => ({
              configKey: item.configKey,
              abbreviation: item.abbreviation,
              name: item.name,
            })),
        );
      } else {
        const licenses = await tenantDrivingLicenseDefinitionsApi.list(tenantId);
        setCatalog(
          licenses
            .filter((item) => !item.archived)
            .map((item) => ({
              configKey: item.configKey,
              abbreviation: item.abbreviation,
              name: item.name,
            })),
        );
      }
    } finally {
      setLoadingCatalog(false);
    }
  }, [tenantId, kind]);

  useEffect(() => {
    void loadCatalog();
  }, [loadCatalog]);

  const modeOptions = [
    { value: 'ALL', label: t('admin.tenantStandards.qualifications.modeAll') },
    { value: 'ANY', label: t('admin.tenantStandards.qualifications.modeAny') },
  ];

  const persist = async (nextSelected: Set<string>, nextMode: QualificationRequirementMode) => {
    if (!offering) return;
    const otherEntries =
      prereq?.entries
        .filter((entry) => entry.kind !== kind)
        .map((entry) => ({ kind: entry.kind, configKey: entry.configKey })) ?? [];
    const kindEntries = [...nextSelected].map((configKey) => ({ kind, configKey }));
    const base: ReplaceCoursePrerequisitesRequest = {
      trainingCourseRequirementMode: modeForKind(prereq, 'TRAINING_COURSE'),
      certRequirementMode: modeForKind(prereq, 'CERTIFICATION'),
      drivingLicenseRequirementMode: modeForKind(prereq, 'DRIVING_LICENSE'),
      entries: [...otherEntries, ...kindEntries],
    };
    try {
      await replace.mutateAsync(withKindMode(base, kind, nextMode));
      showSuccess(t('courseOfferings.prerequisites.saved'));
    } catch (e) {
      showError(e instanceof Error ? e.message : t('courseOfferings.prerequisites.saveFailed'));
    }
  };

  const toggle = (configKey: string, checked: boolean) => {
    if (!editable || replace.isPending) return;
    const next = new Set(selectedKeys);
    if (checked) next.add(configKey);
    else next.delete(configKey);
    void persist(next, mode);
  };

  const changeMode = (nextMode: QualificationRequirementMode) => {
    if (!editable || replace.isPending || nextMode === mode) return;
    void persist(selectedKeys, nextMode);
  };

  if (isLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (isError) {
    return <ErrorState message={t('courseOfferings.list.loadFailed')} />;
  }

  if (!offering) {
    return <NotFoundState message={t('courseOfferings.prerequisites.notFound')} />;
  }

  const title = t(TITLE_KEY[kind]);
  const busy = replace.isPending;

  return (
    <Card className="p-3 sm:p-4">
      <DetailCardHeader
        className="mb-2"
        eyebrow={t('courseOfferings.prerequisites.title')}
        title={title}
      />

      {loadingCatalog ? (
        <div className="border-t border-[var(--border-color)] pt-4">
          <LoadingState variant="embedded" size="sm" />
        </div>
      ) : (
        <div className="space-y-4 border-t border-[var(--border-color)] pt-4">
          <div className="flex items-center justify-between gap-2">
            <p className="text-sm font-medium text-[var(--text-primary)]">{title}</p>
            <Select
              value={mode}
              onChange={(e) => changeMode(e.target.value as QualificationRequirementMode)}
              options={modeOptions}
              disabled={!editable || busy}
            />
          </div>
          <div className="max-h-80 space-y-1 overflow-y-auto rounded border border-[var(--border)] p-2">
            {catalog.map((item) => {
              const controlId = `co-prereq-${kind}-${item.configKey}`;
              const checked = selectedKeys.has(item.configKey);
              return (
                <label
                  key={item.configKey}
                  htmlFor={controlId}
                  className={cn(
                    'flex items-center gap-3 rounded-lg px-2 py-1.5 transition-colors',
                    editable ? 'cursor-pointer' : 'cursor-default',
                    checked
                      ? 'bg-[var(--fire-red)]/10'
                      : editable && 'hover:bg-[var(--hover-bg)]/60',
                  )}
                >
                  <CheckboxControl
                    id={controlId}
                    label={item.name}
                    checked={checked}
                    onCheckedChange={(next) => toggle(item.configKey, next)}
                    size="sm"
                    disabled={!editable || busy}
                  />
                  <span className="min-w-0 flex-1">
                    <span className="block text-sm font-medium text-[var(--text-primary)]">
                      {item.name}
                    </span>
                    <span className="mt-0.5 block text-xs text-[var(--text-secondary)]">
                      {t(ABBREVIATION_KEY[kind], { key: item.abbreviation })}
                    </span>
                  </span>
                </label>
              );
            })}
          </div>
        </div>
      )}
    </Card>
  );
}
