/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo, useState } from 'react';
import { CheckboxControl } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { SwitchField } from '@/components/ui/switch';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import type { CertEvidenceType } from '@/lib/api/tenantCertifications';
import {
  type TenantCertDefinitionDto,
  tenantCertificationsApi,
} from '@/lib/api/tenantCertifications';
import { cn } from '@/lib/utils/cn';
import { isOptionalPositiveInteger } from '@/lib/utils/validation';

const EVIDENCE_TYPE_OPTIONS: CertEvidenceType[] = ['DOCUMENT', 'SERVICE_EVENT', 'OPERATION_EVENT'];

export interface AdminTenantCertificationEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSaved: (configKey: string) => void;
  tenantId: number;
  definition: TenantCertDefinitionDto | null;
}

export function AdminTenantCertificationEditModal({
  isOpen,
  ...props
}: AdminTenantCertificationEditModalProps) {
  if (!isOpen) {
    return null;
  }

  return (
    <AdminTenantCertificationEditModalBody
      key={props.definition?.configKey ?? 'create'}
      isOpen={isOpen}
      {...props}
    />
  );
}

function AdminTenantCertificationEditModalBody({
  onClose,
  onSaved,
  tenantId,
  definition,
}: Omit<AdminTenantCertificationEditModalProps, 'isOpen'> & { isOpen: boolean }) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const [busy, setBusy] = useState(false);
  const [abbreviation, setAbbreviation] = useState(() => definition?.abbreviation ?? '');
  const [name, setName] = useState(() => definition?.name ?? '');
  const [description, setDescription] = useState(() => definition?.description ?? '');
  const [interval, setInterval] = useState(() =>
    definition?.intervalDays != null ? String(definition.intervalDays) : '',
  );
  const [reminder, setReminder] = useState(() =>
    String(definition?.reminderDaysBeforeExpiry ?? 30),
  );
  const [active, setActive] = useState(() => definition?.active ?? true);
  const [evidenceTypes, setEvidenceTypes] = useState<Set<CertEvidenceType>>(
    () => new Set(definition?.evidenceTypes ?? (definition ? [] : ['DOCUMENT'])),
  );

  const intervalInvalid = interval.trim() !== '' && !isOptionalPositiveInteger(interval);
  const reminderInvalid = reminder.trim() !== '' && !isOptionalPositiveInteger(reminder);

  const isValid = useMemo(
    () =>
      name.trim().length > 0 &&
      abbreviation.trim().length > 0 &&
      !intervalInvalid &&
      !reminderInvalid,
    [abbreviation, intervalInvalid, name, reminderInvalid],
  );

  const toggleEvidenceType = (type: CertEvidenceType) => {
    setEvidenceTypes((prev) => {
      const next = new Set(prev);
      if (next.has(type)) {
        next.delete(type);
      } else {
        next.add(type);
      }
      return next;
    });
  };

  const handleSave = async () => {
    if (!isValid) return;
    setBusy(true);
    try {
      const payload = {
        abbreviation: abbreviation.trim(),
        name: name.trim(),
        description: description.trim() || undefined,
        intervalDays: interval.trim() ? Number.parseInt(interval, 10) : null,
        reminderDaysBeforeExpiry: reminder.trim() ? Number.parseInt(reminder, 10) : 30,
        active,
        evidenceTypes: Array.from(evidenceTypes),
      };
      let savedConfigKey = definition?.configKey ?? '';
      if (definition) {
        const saved = await tenantCertificationsApi.update(tenantId, definition.configKey, payload);
        savedConfigKey = saved.configKey;
      } else {
        const created = await tenantCertificationsApi.create(tenantId, payload);
        savedConfigKey = created.configKey;
      }
      showSuccess(t('admin.tenantStandards.certifications.saved'));
      onSaved(savedConfigKey);
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.certifications.saveFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={
        definition
          ? t('admin.tenantStandards.certifications.editTitle')
          : t('admin.tenantStandards.certifications.createTitle')
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
          label={t('admin.tenantStandards.certifications.abbreviationLabel')}
          value={abbreviation}
          onChange={(e) => setAbbreviation(e.target.value)}
          placeholder={t('admin.tenantStandards.certifications.abbreviationPlaceholder')}
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

        <div className="w-full space-y-2">
          <p className="text-sm font-semibold text-[var(--text-primary)]">
            {t('admin.tenantStandards.certifications.evidenceTypesLabel')}
          </p>
          <p className="text-xs text-[var(--text-secondary)]">
            {t('admin.tenantStandards.certifications.evidenceTypesHint')}
          </p>
          <fieldset
            className="space-y-1 rounded-xl border border-[var(--border-color)] p-1"
            aria-label={t('admin.tenantStandards.certifications.evidenceTypesLabel')}
          >
            {EVIDENCE_TYPE_OPTIONS.map((type) => {
              const label = t(`admin.tenantStandards.certifications.evidenceType.${type}`);
              const controlId = `cert-evidence-type-${type}`;
              const isSelected = evidenceTypes.has(type);

              return (
                <label
                  key={type}
                  htmlFor={controlId}
                  className={cn(
                    'flex cursor-pointer items-center gap-3 rounded-lg px-3 py-2 transition-colors',
                    isSelected ? 'bg-[var(--fire-red)]/10' : 'hover:bg-[var(--hover-bg)]/60',
                  )}
                >
                  <CheckboxControl
                    id={controlId}
                    checked={isSelected}
                    onCheckedChange={() => toggleEvidenceType(type)}
                    label={label}
                    size="sm"
                  />
                  <span className="text-sm font-medium text-[var(--text-primary)]">{label}</span>
                </label>
              );
            })}
          </fieldset>
        </div>

        <Input
          label={t('admin.tenantStandards.certifications.intervalDaysLabel')}
          value={interval}
          onChange={(e) => setInterval(e.target.value)}
          inputMode="numeric"
          hint={t('admin.tenantStandards.certifications.intervalHint')}
          error={intervalInvalid ? t('common.validation.positiveInteger') : undefined}
        />
        <Input
          label={t('admin.tenantStandards.certifications.reminderDaysLabel')}
          value={reminder}
          onChange={(e) => setReminder(e.target.value)}
          inputMode="numeric"
          error={reminderInvalid ? t('common.validation.positiveInteger') : undefined}
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
