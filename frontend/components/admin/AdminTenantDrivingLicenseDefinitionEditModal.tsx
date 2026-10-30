/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useState } from 'react';
import { Input } from '@/components/ui/input';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { SwitchField } from '@/components/ui/switch';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import {
  type TenantDrivingLicenseDefinitionDto,
  tenantDrivingLicenseDefinitionsApi,
} from '@/lib/api/tenantDrivingLicenseDefinitions';

export interface AdminTenantDrivingLicenseDefinitionEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSaved: (configKey: string) => void;
  tenantId: number;
  definition: TenantDrivingLicenseDefinitionDto | null;
}

export function AdminTenantDrivingLicenseDefinitionEditModal({
  isOpen,
  ...props
}: AdminTenantDrivingLicenseDefinitionEditModalProps) {
  if (!isOpen) {
    return null;
  }

  return (
    <AdminTenantDrivingLicenseDefinitionEditModalBody
      key={props.definition?.configKey ?? 'create'}
      isOpen={isOpen}
      {...props}
    />
  );
}

function AdminTenantDrivingLicenseDefinitionEditModalBody({
  onClose,
  onSaved,
  tenantId,
  definition,
}: Omit<AdminTenantDrivingLicenseDefinitionEditModalProps, 'isOpen'> & { isOpen: boolean }) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const [busy, setBusy] = useState(false);
  const [abbreviation, setAbbreviation] = useState(() => definition?.abbreviation ?? '');
  const [name, setName] = useState(() => definition?.name ?? '');
  const [active, setActive] = useState(() => definition?.active ?? true);

  const handleSave = async () => {
    if (!name.trim() || !abbreviation.trim()) return;
    setBusy(true);
    try {
      const payload = {
        abbreviation: abbreviation.trim(),
        name: name.trim(),
        active,
      };
      let savedConfigKey = definition?.configKey ?? '';
      if (definition) {
        const saved = await tenantDrivingLicenseDefinitionsApi.update(
          tenantId,
          definition.configKey,
          payload,
        );
        savedConfigKey = saved.configKey;
      } else {
        const created = await tenantDrivingLicenseDefinitionsApi.create(tenantId, payload);
        savedConfigKey = created.configKey;
      }
      showSuccess(t('admin.tenantStandards.drivingLicenses.saved'));
      onSaved(savedConfigKey);
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.drivingLicenses.saveFailed'),
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
          ? t('admin.tenantStandards.drivingLicenses.editTitle')
          : t('admin.tenantStandards.drivingLicenses.createTitle')
      }
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleSave()}
          isValid={name.trim().length > 0 && abbreviation.trim().length > 0}
          isLoading={busy}
        />
      }
    >
      <div className="space-y-3">
        <Input
          label={t('admin.tenantStandards.drivingLicenses.abbreviationLabel')}
          value={abbreviation}
          onChange={(e) => setAbbreviation(e.target.value.toUpperCase())}
          placeholder={t('admin.tenantStandards.drivingLicenses.abbreviationPlaceholder')}
          required
        />
        <Input
          label={t('admin.standards.name')}
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
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
