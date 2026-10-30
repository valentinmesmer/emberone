/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useCallback, useEffect, useState } from 'react';
import { CheckboxControl } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import { LoadingState } from '@/components/ui/loading';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Select } from '@/components/ui/select';
import { SwitchField } from '@/components/ui/switch';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { tenantCertificationsApi } from '@/lib/api/tenantCertifications';
import { tenantDrivingLicenseDefinitionsApi } from '@/lib/api/tenantDrivingLicenseDefinitions';
import {
  type QualificationRequirementMode,
  type TenantQualificationDefinitionDto,
  tenantQualificationsApi,
} from '@/lib/api/tenantQualifications';
import { tenantTrainingCourseTypesApi } from '@/lib/api/tenantTrainingCourseTypes';
import { cn } from '@/lib/utils/cn';

export interface AdminTenantQualificationEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSaved: () => void;
  tenantId: number;
  definition: TenantQualificationDefinitionDto | null;
}

export function AdminTenantQualificationEditModal({
  isOpen,
  ...props
}: AdminTenantQualificationEditModalProps) {
  if (!isOpen) return null;
  return (
    <AdminTenantQualificationEditModalBody
      key={props.definition?.configKey ?? 'create'}
      isOpen={isOpen}
      {...props}
    />
  );
}

function AdminTenantQualificationEditModalBody({
  onClose,
  onSaved,
  tenantId,
  definition,
}: Omit<AdminTenantQualificationEditModalProps, 'isOpen'> & { isOpen: boolean }) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const [busy, setBusy] = useState(false);
  const [loadingCatalog, setLoadingCatalog] = useState(true);
  const [trainingCourses, setTrainingCourses] = useState<
    { configKey: string; abbreviation: string; name: string }[]
  >([]);
  const [certifications, setCertifications] = useState<
    { configKey: string; abbreviation: string; name: string }[]
  >([]);
  const [drivingLicenses, setDrivingLicenses] = useState<
    { configKey: string; abbreviation: string; name: string }[]
  >([]);

  const [name, setName] = useState(() => definition?.name ?? '');
  const [abbreviation, setAbbreviation] = useState(() => definition?.abbreviation ?? '');
  const [description, setDescription] = useState(() => definition?.description ?? '');
  const [requiresOrgDecision, setRequiresOrgDecision] = useState(
    () => definition?.requiresOrgDecision ?? false,
  );
  const [trainingMode, setTrainingMode] = useState<QualificationRequirementMode>(
    () => definition?.trainingCourseRequirementMode ?? 'ALL',
  );
  const [certMode, setCertMode] = useState<QualificationRequirementMode>(
    () => definition?.certRequirementMode ?? 'ALL',
  );
  const [drivingLicenseMode, setDrivingLicenseMode] = useState<QualificationRequirementMode>(
    () => definition?.drivingLicenseRequirementMode ?? 'ALL',
  );
  const [selectedTraining, setSelectedTraining] = useState<Set<string>>(
    () => new Set(definition?.trainingCourseConfigKeys ?? []),
  );
  const [selectedCerts, setSelectedCerts] = useState<Set<string>>(
    () => new Set(definition?.certDefinitionConfigKeys ?? []),
  );
  const [selectedDrivingLicenses, setSelectedDrivingLicenses] = useState<Set<string>>(
    () => new Set(definition?.drivingLicenseDefinitionConfigKeys ?? []),
  );
  const [active, setActive] = useState(() => definition?.active ?? true);

  const loadCatalog = useCallback(async () => {
    setLoadingCatalog(true);
    try {
      const [courses, certs, licenses] = await Promise.all([
        tenantTrainingCourseTypesApi.list(tenantId),
        tenantCertificationsApi.list(tenantId),
        tenantDrivingLicenseDefinitionsApi.list(tenantId),
      ]);
      setTrainingCourses(
        courses
          .filter((c) => !c.archived)
          .map((c) => ({ configKey: c.configKey, abbreviation: c.abbreviation, name: c.name })),
      );
      setCertifications(
        certs
          .filter((c) => !c.archived)
          .map((c) => ({ configKey: c.configKey, abbreviation: c.abbreviation, name: c.name })),
      );
      setDrivingLicenses(
        licenses
          .filter((c) => !c.archived)
          .map((c) => ({ configKey: c.configKey, abbreviation: c.abbreviation, name: c.name })),
      );
    } catch {
      setTrainingCourses([]);
      setCertifications([]);
    } finally {
      setLoadingCatalog(false);
    }
  }, [tenantId]);

  useEffect(() => {
    queueMicrotask(() => {
      void loadCatalog();
    });
  }, [loadCatalog]);

  const toggleSet = (set: Set<string>, key: string, checked: boolean) => {
    const next = new Set(set);
    if (checked) next.add(key);
    else next.delete(key);
    return next;
  };

  const isValid =
    name.trim().length > 0 &&
    abbreviation.trim().length > 0 &&
    (selectedTraining.size > 0 ||
      selectedCerts.size > 0 ||
      selectedDrivingLicenses.size > 0 ||
      requiresOrgDecision);

  const handleSave = async () => {
    if (!isValid) return;
    setBusy(true);
    try {
      const payload = {
        abbreviation: abbreviation.trim(),
        name: name.trim(),
        description: description.trim() || undefined,
        requiresOrgDecision,
        trainingCourseRequirementMode: trainingMode,
        certRequirementMode: certMode,
        drivingLicenseRequirementMode: drivingLicenseMode,
        trainingCourseConfigKeys: [...selectedTraining],
        certDefinitionConfigKeys: [...selectedCerts],
        drivingLicenseDefinitionConfigKeys: [...selectedDrivingLicenses],
        active,
      };
      if (definition) {
        await tenantQualificationsApi.update(tenantId, definition.configKey, payload);
      } else {
        await tenantQualificationsApi.create(tenantId, payload);
      }
      showSuccess(t('admin.tenantStandards.qualifications.saved'));
      onSaved();
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.qualifications.saveFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const modeOptions = [
    { value: 'ALL', label: t('admin.tenantStandards.qualifications.modeAll') },
    { value: 'ANY', label: t('admin.tenantStandards.qualifications.modeAny') },
  ];

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={
        definition
          ? t('admin.tenantStandards.qualifications.editTitle')
          : t('admin.tenantStandards.qualifications.createTitle')
      }
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleSave()}
          isValid={isValid}
          isLoading={busy}
        />
      }
    >
      <div className="space-y-4">
        <Input
          label={t('admin.tenantStandards.qualifications.abbreviationLabel')}
          value={abbreviation}
          onChange={(e) => setAbbreviation(e.target.value.toUpperCase())}
          required
        />
        <Input
          label={t('admin.standards.name')}
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />
        <Textarea
          label={t('admin.standards.description')}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />

        {loadingCatalog ? (
          <LoadingState variant="embedded" />
        ) : (
          <>
            <p className="text-xs text-[var(--text-secondary)]">
              {t('admin.tenantStandards.qualifications.requirementsHint')}
              <span className="text-[var(--fire-red)] ml-0.5">*</span>
            </p>
            <div className="space-y-2">
              <div className="flex items-center justify-between gap-2">
                <p className="text-sm font-medium text-[var(--text-primary)]">
                  {t('admin.tenantStandards.qualifications.trainingCourses')}
                </p>
                <Select
                  value={trainingMode}
                  onChange={(e) => setTrainingMode(e.target.value as QualificationRequirementMode)}
                  options={modeOptions}
                />
              </div>
              <div className="max-h-40 space-y-1 overflow-y-auto rounded border border-[var(--border)] p-2">
                {trainingCourses.map((course) => {
                  const controlId = `qualification-training-${course.configKey}`;
                  return (
                    <label
                      key={course.configKey}
                      htmlFor={controlId}
                      className={cn(
                        'flex cursor-pointer items-center gap-3 rounded-lg px-2 py-1.5 transition-colors',
                        selectedTraining.has(course.configKey)
                          ? 'bg-[var(--fire-red)]/10'
                          : 'hover:bg-[var(--hover-bg)]/60',
                      )}
                    >
                      <CheckboxControl
                        id={controlId}
                        label={course.name}
                        checked={selectedTraining.has(course.configKey)}
                        onCheckedChange={(checked) =>
                          setSelectedTraining(
                            toggleSet(selectedTraining, course.configKey, checked),
                          )
                        }
                        size="sm"
                      />
                      <span className="min-w-0 flex-1">
                        <span className="block text-sm font-medium text-[var(--text-primary)]">
                          {course.name}
                        </span>
                        <span className="mt-0.5 block text-xs text-[var(--text-secondary)]">
                          {t('admin.tenantStandards.trainingCourses.abbreviation', {
                            key: course.abbreviation,
                          })}
                        </span>
                      </span>
                    </label>
                  );
                })}
              </div>
            </div>

            <div className="space-y-2">
              <div className="flex items-center justify-between gap-2">
                <p className="text-sm font-medium text-[var(--text-primary)]">
                  {t('admin.tenantStandards.qualifications.drivingLicenses')}
                </p>
                <Select
                  value={drivingLicenseMode}
                  onChange={(e) =>
                    setDrivingLicenseMode(e.target.value as QualificationRequirementMode)
                  }
                  options={modeOptions}
                />
              </div>
              <div className="max-h-40 space-y-1 overflow-y-auto rounded border border-[var(--border)] p-2">
                {drivingLicenses.map((license) => {
                  const controlId = `qualification-driving-license-${license.configKey}`;
                  return (
                    <label
                      key={license.configKey}
                      htmlFor={controlId}
                      className={cn(
                        'flex cursor-pointer items-center gap-3 rounded-lg px-2 py-1.5 transition-colors',
                        selectedDrivingLicenses.has(license.configKey)
                          ? 'bg-[var(--fire-red)]/10'
                          : 'hover:bg-[var(--hover-bg)]/60',
                      )}
                    >
                      <CheckboxControl
                        id={controlId}
                        label={license.name}
                        checked={selectedDrivingLicenses.has(license.configKey)}
                        onCheckedChange={(checked) =>
                          setSelectedDrivingLicenses(
                            toggleSet(selectedDrivingLicenses, license.configKey, checked),
                          )
                        }
                        size="sm"
                      />
                      <span className="min-w-0 flex-1">
                        <span className="block text-sm font-medium text-[var(--text-primary)]">
                          {license.name}
                        </span>
                        <span className="mt-0.5 block text-xs text-[var(--text-secondary)]">
                          {t('admin.tenantStandards.drivingLicenses.abbreviation', {
                            key: license.abbreviation,
                          })}
                        </span>
                      </span>
                    </label>
                  );
                })}
              </div>
            </div>

            <div className="space-y-2">
              <div className="flex items-center justify-between gap-2">
                <p className="text-sm font-medium text-[var(--text-primary)]">
                  {t('admin.tenantStandards.qualifications.certifications')}
                </p>
                <Select
                  value={certMode}
                  onChange={(e) => setCertMode(e.target.value as QualificationRequirementMode)}
                  options={modeOptions}
                />
              </div>
              <div className="max-h-40 space-y-1 overflow-y-auto rounded border border-[var(--border)] p-2">
                {certifications.map((cert) => {
                  const controlId = `qualification-cert-${cert.configKey}`;
                  return (
                    <label
                      key={cert.configKey}
                      htmlFor={controlId}
                      className={cn(
                        'flex cursor-pointer items-center gap-3 rounded-lg px-2 py-1.5 transition-colors',
                        selectedCerts.has(cert.configKey)
                          ? 'bg-[var(--fire-red)]/10'
                          : 'hover:bg-[var(--hover-bg)]/60',
                      )}
                    >
                      <CheckboxControl
                        id={controlId}
                        label={cert.name}
                        checked={selectedCerts.has(cert.configKey)}
                        onCheckedChange={(checked) =>
                          setSelectedCerts(toggleSet(selectedCerts, cert.configKey, checked))
                        }
                        size="sm"
                      />
                      <span className="min-w-0 flex-1">
                        <span className="block text-sm font-medium text-[var(--text-primary)]">
                          {cert.name}
                        </span>
                        <span className="mt-0.5 block text-xs text-[var(--text-secondary)]">
                          {t('admin.tenantStandards.certifications.abbreviation', {
                            key: cert.abbreviation,
                          })}
                        </span>
                      </span>
                    </label>
                  );
                })}
              </div>
            </div>
          </>
        )}

        <SwitchField
          label={t('admin.tenantStandards.qualifications.requiresOrgDecision')}
          checked={requiresOrgDecision}
          onCheckedChange={setRequiresOrgDecision}
        />
        <SwitchField
          label={t('admin.standards.active')}
          checked={active}
          onCheckedChange={setActive}
        />
      </div>
    </Modal>
  );
}
