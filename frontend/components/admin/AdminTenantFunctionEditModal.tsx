/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useEffect, useState } from 'react';
import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import {
  type CreateTenantFunctionRequest,
  type TenantFunctionDto,
  tenantFunctionsApi,
} from '@/lib/api/tenantFunctions';
import { formBlockers } from '@/lib/utils';

interface Props {
  tenantId: number;
  editing: TenantFunctionDto | null;
  isOpen: boolean;
  onClose: () => void;
  onSaved: () => void;
}

export function AdminTenantFunctionEditModal({
  tenantId,
  editing,
  isOpen,
  onClose,
  onSaved,
}: Props) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const [busy, setBusy] = useState(false);
  const [configKey, setConfigKey] = useState('');
  const [name, setName] = useState('');
  const [abbreviation, setAbbreviation] = useState('');
  const [sortOrder, setSortOrder] = useState('0');
  const [active, setActive] = useState(true);

  useEffect(() => {
    if (!isOpen) return;
    setConfigKey(editing?.configKey ?? '');
    setName(editing?.name ?? '');
    setAbbreviation(editing?.abbreviation ?? '');
    setSortOrder(String(editing?.sortOrder ?? 0));
    setActive(editing?.active ?? true);
  }, [isOpen, editing]);

  const blockers = formBlockers([
    ...(editing
      ? []
      : ([[t('admin.tenantStandards.functions.configKey'), configKey.trim() !== '']] as const)),
    [t('common.name'), name.trim() !== ''],
    [t('admin.tenantStandards.functions.abbreviation'), abbreviation.trim() !== ''],
  ]);

  const handleSave = async () => {
    if (blockers.length > 0) return;
    setBusy(true);
    try {
      const payload: CreateTenantFunctionRequest = {
        configKey: (editing?.configKey ?? configKey).trim(),
        name: name.trim(),
        abbreviation: abbreviation.trim(),
        sortOrder: Number.parseInt(sortOrder, 10) || 0,
        active,
      };
      if (editing) {
        await tenantFunctionsApi.update(tenantId, editing.configKey, payload);
      } else {
        await tenantFunctionsApi.create(tenantId, payload);
      }
      showSuccess(t('admin.tenantStandards.functions.saved'));
      onClose();
      onSaved();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.tenantStandards.functions.saveFailed'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={
        editing
          ? t('admin.tenantStandards.functions.editTitle')
          : t('admin.tenantStandards.functions.createTitle')
      }
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleSave()}
          blockers={blockers}
          isLoading={busy}
        />
      }
    >
      <div className="space-y-3">
        {!editing ? (
          <Input
            label={t('admin.tenantStandards.functions.configKey')}
            value={configKey}
            onChange={(e) => setConfigKey(e.target.value)}
            disabled={busy}
            required
          />
        ) : null}
        <Input
          label={t('common.name')}
          value={name}
          onChange={(e) => setName(e.target.value)}
          disabled={busy}
          required
        />
        <Input
          label={t('admin.tenantStandards.functions.abbreviation')}
          value={abbreviation}
          onChange={(e) => setAbbreviation(e.target.value)}
          disabled={busy}
          required
        />
        <Input
          label={t('admin.tenantStandards.functions.sortOrder')}
          value={sortOrder}
          onChange={(e) => setSortOrder(e.target.value)}
          disabled={busy}
        />
        <Checkbox
          checked={active}
          onCheckedChange={(value) => setActive(value === true)}
          label={t('common.active')}
          disabled={busy}
        />
      </div>
    </Modal>
  );
}
