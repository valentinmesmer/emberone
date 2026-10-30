/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
  type CreateTenantRankRequest,
  type TenantRankDto,
  tenantRanksApi,
  type UpdateTenantRankRequest,
} from '@/lib/api/tenantRanks';

interface Props {
  tenantId: number;
  editing: TenantRankDto | null;
  isOpen: boolean;
  onClose: () => void;
  onSaved: () => void;
}

export function AdminTenantRankEditModal({ tenantId, editing, isOpen, onClose, onSaved }: Props) {
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

  const handleSave = async () => {
    if (!name.trim() || !abbreviation.trim()) return;
    setBusy(true);
    try {
      if (editing) {
        const payload: UpdateTenantRankRequest = {
          name: name.trim(),
          abbreviation: abbreviation.trim(),
          sortOrder: Number.parseInt(sortOrder, 10) || 0,
          active,
        };
        await tenantRanksApi.updateRank(tenantId, editing.configKey, payload);
      } else {
        if (!configKey.trim()) return;
        const payload: CreateTenantRankRequest = {
          configKey: configKey.trim(),
          name: name.trim(),
          abbreviation: abbreviation.trim(),
          sortOrder: Number.parseInt(sortOrder, 10) || 0,
          active,
        };
        await tenantRanksApi.createRank(tenantId, payload);
      }
      showSuccess(t('admin.tenantStandards.ranks.saved'));
      onClose();
      onSaved();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.tenantStandards.ranks.saveFailed'));
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
          ? t('admin.tenantStandards.ranks.editTitle')
          : t('admin.tenantStandards.ranks.createTitle')
      }
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleSave()}
          isValid={Boolean(name.trim() && abbreviation.trim() && (editing || configKey.trim()))}
          isLoading={busy}
        />
      }
    >
      <div className="space-y-3">
        {!editing ? (
          <Input
            label={t('admin.tenantStandards.ranks.configKey')}
            value={configKey}
            onChange={(e) => setConfigKey(e.target.value)}
            disabled={busy}
          />
        ) : null}
        <Input
          label={t('common.name')}
          value={name}
          onChange={(e) => setName(e.target.value)}
          disabled={busy}
        />
        <Input
          label={t('admin.tenantStandards.ranks.abbreviation')}
          value={abbreviation}
          onChange={(e) => setAbbreviation(e.target.value)}
          disabled={busy}
        />
        <Input
          label={t('admin.tenantStandards.ranks.sortOrder')}
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
