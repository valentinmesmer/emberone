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
  type TenantTrainingCourseTypeDto,
  tenantTrainingCourseTypesApi,
} from '@/lib/api/tenantTrainingCourseTypes';

export interface AdminTenantTrainingCourseTypeEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSaved: (configKey: string) => void;
  tenantId: number;
  type: TenantTrainingCourseTypeDto | null;
}

export function AdminTenantTrainingCourseTypeEditModal({
  isOpen,
  ...props
}: AdminTenantTrainingCourseTypeEditModalProps) {
  if (!isOpen) {
    return null;
  }

  return (
    <AdminTenantTrainingCourseTypeEditModalBody
      key={props.type?.configKey ?? 'create'}
      isOpen={isOpen}
      {...props}
    />
  );
}

function AdminTenantTrainingCourseTypeEditModalBody({
  onClose,
  onSaved,
  tenantId,
  type,
}: Omit<AdminTenantTrainingCourseTypeEditModalProps, 'isOpen'> & { isOpen: boolean }) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const [busy, setBusy] = useState(false);
  const [abbreviation, setAbbreviation] = useState(() => type?.abbreviation ?? '');
  const [name, setName] = useState(() => type?.name ?? '');
  const [active, setActive] = useState(() => type?.active ?? true);

  const handleSave = async () => {
    if (!name.trim() || !abbreviation.trim()) return;
    setBusy(true);
    try {
      const payload = {
        abbreviation: abbreviation.trim(),
        name: name.trim(),
        active,
      };
      let savedConfigKey = type?.configKey ?? '';
      if (type) {
        const saved = await tenantTrainingCourseTypesApi.update(tenantId, type.configKey, payload);
        savedConfigKey = saved.configKey;
      } else {
        const created = await tenantTrainingCourseTypesApi.create(tenantId, payload);
        savedConfigKey = created.configKey;
      }
      showSuccess(t('admin.tenantStandards.trainingCourses.saved'));
      onSaved(savedConfigKey);
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.trainingCourses.saveFailed'),
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
        type
          ? t('admin.tenantStandards.trainingCourses.editTitle')
          : t('admin.tenantStandards.trainingCourses.createTitle')
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
          label={t('admin.tenantStandards.trainingCourses.abbreviationLabel')}
          value={abbreviation}
          onChange={(e) => setAbbreviation(e.target.value.toUpperCase())}
          placeholder={t('admin.tenantStandards.trainingCourses.abbreviationPlaceholder')}
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
